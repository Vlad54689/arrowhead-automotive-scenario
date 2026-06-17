#!/usr/bin/env bash
#
# load-test-foodchain.sh - HONEST multi-hop latency/delivery measurement for the food cold-chain.
#
# Same philosophy as scripts/mtls/load-test-v2.sh (the automotive single-hop tool), generalized to
# the 3-hop event chain:
#   ColdChainMonitor (host, 9897)  --coldchain.reading.created-->
#   RiskScoring      (container, 9898, sub+pub)  --risk.assessed-->
#   SupplyChainTrust (container, 9899, sub)  logs: "TRUST verdict batchId=.. latency_ms=.."
#
# WHAT IT MEASURES
#   (A) PRINCIPAL - cumulative end-to-end latency. The client POSTs to ColdChain with a UNIQUE
#       batchId and originTs=send-time (host clock, ms). SupplyChainTrust logs latency_ms =
#       now - originTs at the final verdict, on the SAME host clock (no skew). The tool parses
#       latency_ms per batchId from the Trust container logs. Stats are computed ONLY over flows
#       that reached a verdict (successful end-to-end), like load-test-v2 computes latency only
#       over 2xx requests.
#   (B) END-TO-END DELIVERY RATE. For N successful POSTs, how many distinct batchIds reach a
#       verdict in the Trust log? rate = verdicts / successful_posts. On a chain, a loss at ANY
#       hop is a loss for the whole chain, so this is stricter than single-hop. Reported honestly.
#       Asynchrony handled by a settle/poll loop (generous MAX_WAIT at high concurrency).
#   (C) ORIENTATIVE per-hop decomposition (orient_ columns). From the framework log-line
#       timestamps: RiskScoring "Received event" and Trust "Received event" (both containers, UTC,
#       same host kernel clock). orient_hop1 = t_risk_recv - originTs (client->RiskScoring receive),
#       orient_hop2 = t_trust_recv - t_risk_recv (RiskScoring receive->Trust receive). These are
#       log-EMIT times (not exact hop boundaries) at ms granularity, so they are ORIENTATIVE
#       (relative time distribution), NOT exact absolute numbers - hence the orient_ prefix.
#
# batchId is UNIQUE per request: <SESSION>-L<level>-<index>. SESSION (epoch at tool start) makes
# this run's verdicts unambiguous and disjoint from any older "TRUST verdict" lines.
#
# Modes (prepared like load-test-v2; we run insecure now):
#   insecure - POST http:// to ColdChain, no client cert.
#   secure   - POST https:// with a client cert (Phase B/secure, later).
#
# Nothing destructive: it only POSTs readings and reads logs. It does ONE pass over LEVELS (the
# automotive battery wrapper - 5 repeats, warm-up dropped, aggregate - can wrap this unchanged).
#
# Usage (env-overridable):
#   MODE=insecure REQUESTS=200 LEVELS="1 5 10 20 50" OUT=results.csv bash load-test-foodchain.sh
# Small validation:
#   MODE=insecure REQUESTS=10 LEVELS="5" OUT=/tmp/val.csv bash load-test-foodchain.sh

set -uo pipefail

# ----------------------------------------------------------------------------- config
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

MODE="${MODE:-insecure}"                      # insecure | secure
REQUESTS="${REQUESTS:-200}"                   # requests per level
LEVELS="${LEVELS:-1 5 10 20 50}"              # concurrency levels
OUT="${OUT:-$REPO_ROOT/rezultate-foodchain-${MODE}.csv}"

PUB_HOST="${PUB_HOST:-localhost}"
PUB_PORT="${PUB_PORT:-9897}"
PUB_PATH="${PUB_PATH:-/cold-chain/readings}"

# client cert (secure mode, future)
CLIENT_P12="${CLIENT_P12:-$REPO_ROOT/core-java-spring/certificates/testcloud1/sysop.p12}"
CLIENT_P12_PASS="${CLIENT_P12_PASS:-123456}"

# log sources: the two subscriber containers (verdict + per-hop receive timestamps)
TRUST_CONTAINER="${TRUST_CONTAINER:-supply-chain-trust-subscriber}"
RISK_CONTAINER="${RISK_CONTAINER:-risk-scoring-subscriber}"

# asynchronous-delivery settling (generous for a 3-hop chain at high load)
MIN_GRACE="${MIN_GRACE:-3}"          # seconds before first check
POLL_INTERVAL="${POLL_INTERVAL:-1}"
STABLE_ROUNDS="${STABLE_ROUNDS:-5}"  # consecutive unchanged polls => settled
MAX_WAIT="${MAX_WAIT:-90}"           # hard cap on settling per level (seconds)

# reading payload: temperatura fixed high (deterministic SUSPECT); value is irrelevant to latency.
READ_TEMP="${READ_TEMP:-12.5}"
READ_HUM="${READ_HUM:-70.0}"

SESSION="$(date +%s)"
URL="http://$PUB_HOST:$PUB_PORT$PUB_PATH"; [ "$MODE" = secure ] && URL="https://$PUB_HOST:$PUB_PORT$PUB_PATH"

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT

# ----------------------------------------------------------------------------- pre-flight
if [ "$MODE" = secure ]; then
  command -v openssl >/dev/null || { echo "openssl required"; exit 1; }
  openssl pkcs12 -legacy -in "$CLIENT_P12" -passin pass:"$CLIENT_P12_PASS" -clcerts -nokeys -out "$TMP/cli.crt" 2>/dev/null
  openssl pkcs12 -legacy -in "$CLIENT_P12" -passin pass:"$CLIENT_P12_PASS" -nocerts -nodes  -out "$TMP/cli.key" 2>/dev/null
  [ -s "$TMP/cli.crt" ] && [ -s "$TMP/cli.key" ] || { echo "could not extract client cert from $CLIENT_P12"; exit 1; }
  export CERT="$TMP/cli.crt" KEY="$TMP/cli.key"
fi
docker ps --format '{{.Names}}' | grep -qx "$TRUST_CONTAINER" || { echo "Trust container '$TRUST_CONTAINER' not running"; exit 1; }
docker ps --format '{{.Names}}' | grep -qx "$RISK_CONTAINER"  || { echo "Risk container '$RISK_CONTAINER' not running"; exit 1; }
[ "$(curl -s -m3 "http://$PUB_HOST:8443/serviceregistry/echo" 2>/dev/null)" = "Got it!" ] || true

# one request: $1=index. Generates a UNIQUE batchId and originTs=send-time, POSTs, prints:
#   "<batchId> <originTs> <time_total> <http_code>"
do_req() {
  local i="$1"
  local batchId="${SESSION}-L${LEVEL}-${i}"
  local originTs; originTs="$(date +%s%3N)"
  local body="{\"batchId\":\"$batchId\",\"originTs\":$originTs,\"temperatura\":$READ_TEMP,\"umiditate\":$READ_HUM,\"locatie\":\"LoadTest\",\"lot\":\"LT\"}"
  local out
  if [ "$MODE" = secure ]; then
    out="$(curl -s -k --cert "$CERT" --key "$KEY" -o /dev/null -w "%{time_total} %{http_code}" -X POST "$URL" -H 'Content-Type: application/json' -d "$body")"
  else
    out="$(curl -s -o /dev/null -w "%{time_total} %{http_code}" -X POST "$URL" -H 'Content-Type: application/json' -d "$body")"
  fi
  echo "$batchId $originTs $out"
}
export -f do_req
export SESSION URL MODE READ_TEMP READ_HUM

# count THIS run's verdicts for the current level (distinct-by-line; one verdict per batchId)
my_verdicts() {
  docker logs "$TRUST_CONTAINER" 2>&1 | grep -c "TRUST verdict batchId=${SESSION}-L${LEVEL}-"
}

# wait for asynchronous verdicts to settle; $1 = target (exit early once reached)
settle_verdicts() {
  local target="$1" prev=-1 cur stable=0 waited=0
  sleep "$MIN_GRACE"; waited="$MIN_GRACE"
  while [ "$waited" -lt "$MAX_WAIT" ]; do
    cur="$(my_verdicts)"; cur="${cur:-0}"
    [ "$cur" -ge "$target" ] && { echo "$cur"; return; }
    if [ "$cur" -eq "$prev" ]; then
      stable=$((stable + 1)); [ "$stable" -ge "$STABLE_ROUNDS" ] && break
    else
      stable=0
    fi
    prev="$cur"; sleep "$POLL_INTERVAL"; waited=$((waited + POLL_INTERVAL))
  done
  echo "${cur:-0}"
}

# ----------------------------------------------------------------------------- run
echo "MODE=$MODE URL=$URL REQUESTS=$REQUESTS LEVELS=[$LEVELS] SESSION=$SESSION OUT=$OUT"
echo "nivel,cereri_trimise,cereri_reusite_post,verdicte_ajunse,rata_livrare_e2e,throughput_post_rps,lat_cumulata_medie_ms,p50_ms,p95_ms,p99_ms,orient_hop1_ms,orient_hop2_ms" > "$OUT"

for C in $LEVELS; do
  export LEVEL="$C"
  echo "=== concurenta=$C, cereri=$REQUESTS ==="
  local_results="$TMP/res-$C.txt"; : > "$local_results"
  start="$(date +%s.%N)"
  seq "$REQUESTS" | xargs -P "$C" -I{} bash -c 'do_req "$@"' _ {} >> "$local_results"
  end="$(date +%s.%N)"

  ok="$(awk '$4 ~ /^2/ {n++} END{print n+0}' "$local_results")"
  echo "  POST: reusite=$ok / $REQUESTS trimise; astept verdictele sa se aseze (settle)..."

  after="$(settle_verdicts "$ok")"
  echo "  verdicte (THIS session, level $C): $after"

  # dump the two container logs ONCE for parsing
  docker logs "$TRUST_CONTAINER" 2>&1 > "$TMP/trust-$C.log"
  docker logs "$RISK_CONTAINER"  2>&1 > "$TMP/risk-$C.log"

  read -r sent okp verdicts rate tput mean p50 p95 p99 oh1 oh2 < <(python3 - \
      "$local_results" "$TMP/trust-$C.log" "$TMP/risk-$C.log" "$SESSION" "$C" "$start" "$end" <<'PY'
import sys, re, math
from datetime import datetime, timezone

results_f, trust_f, risk_f, session, level, start, end = sys.argv[1:8]
start = float(start); end = float(end)
prefix = f"{session}-L{level}-"

# --- sent requests: batchId -> (originTs_ms, http_ok) ---
sent = {}
ok_post = 0
total_sent = 0
for line in open(results_f):
    p = line.split()
    if len(p) < 4:        # malformed/curl error line counts as a failed send
        total_sent += 1
        continue
    bid, origin, ttot, code = p[0], p[1], p[2], p[3]
    total_sent += 1
    is_ok = code.startswith('2')
    if is_ok: ok_post += 1
    try: origin_ms = int(origin)
    except ValueError: origin_ms = None
    sent[bid] = (origin_ms, is_ok)

def to_epoch_ms(ts):  # "2026-06-17 18:51:09.669" parsed AS UTC (container TZ) -> epoch ms
    try:
        dt = datetime.strptime(ts, "%Y-%m-%d %H:%M:%S.%f").replace(tzinfo=timezone.utc)
        return dt.timestamp() * 1000.0
    except ValueError:
        return None

# --- Trust verdicts: batchId -> latency_ms (cumulative, rigorous) ---
verdict_re = re.compile(r"TRUST verdict batchId=(\S+) originTs=(\d+) latency_ms=(-?\d+) verdict=(\S+)")
lat = {}
for line in open(trust_f):
    m = verdict_re.search(line)
    if m and m.group(1).startswith(prefix):
        lat[m.group(1)] = int(m.group(3))

# --- Trust "Received event" timestamps: batchId -> t_trust_recv_ms ---
recv_re = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}).*Received event.*batchId=([^;,\s]+)")
t_trust = {}
for line in open(trust_f):
    m = recv_re.search(line)
    if m and m.group(2).startswith(prefix):
        e = to_epoch_ms(m.group(1))
        if e is not None: t_trust[m.group(2)] = e

# --- RiskScoring "Received event" timestamps: batchId -> t_risk_recv_ms ---
t_risk = {}
for line in open(risk_f):
    m = recv_re.search(line)
    if m and m.group(2).startswith(prefix):
        e = to_epoch_ms(m.group(1))
        if e is not None: t_risk[m.group(2)] = e

# delivered end-to-end = successful POST AND reached a verdict
delivered = [b for b,(o,k) in sent.items() if k and b in lat]
lat_vals = sorted(lat[b] for b in delivered)

def pct(q):
    if not lat_vals: return 0.0
    k = (len(lat_vals)-1)*q; lo=math.floor(k); hi=math.ceil(k)
    return lat_vals[int(k)] if lo==hi else lat_vals[lo]+(lat_vals[hi]-lat_vals[lo])*(k-lo)

dur = end - start
tput = ok_post/dur if dur>0 else 0.0
mean = sum(lat_vals)/len(lat_vals) if lat_vals else 0.0
rate = (len(delivered)/ok_post) if ok_post>0 else 0.0

# orientative per-hop, only over delivered flows where the timestamps were found
hop1 = []  # originTs -> RiskScoring receive
hop2 = []  # RiskScoring receive -> Trust receive
for b in delivered:
    o = sent[b][0]
    if b in t_risk and o is not None: hop1.append(t_risk[b]-o)
    if b in t_risk and b in t_trust:  hop2.append(t_trust[b]-t_risk[b])
def avg(x): return sum(x)/len(x) if x else 0.0

print(f"{total_sent} {ok_post} {len(delivered)} {rate:.3f} {tput:.1f} "
      f"{mean:.2f} {pct(.5):.2f} {pct(.95):.2f} {pct(.99):.2f} {avg(hop1):.1f} {avg(hop2):.1f}")
PY
)

  echo "  livrare e2e: reusite_post=$okp verdicte=$verdicts rata=$rate"
  echo "  lat cumulata(ok e2e): medie=${mean}ms p50=$p50 p95=$p95 p99=$p99   [orient] hop1=${oh1}ms hop2=${oh2}ms"
  echo "$C,$sent,$okp,$verdicts,$rate,$tput,$mean,$p50,$p95,$p99,$oh1,$oh2" >> "$OUT"
done

echo
echo "=== $OUT ==="
cat "$OUT"
echo
echo "NOTE: orient_hop1/orient_hop2 are ORIENTATIVE (log-emit timestamps, ms granularity) - relative"
echo "      per-hop distribution, not exact absolute values. lat_cumulata_* is the rigorous measure."
