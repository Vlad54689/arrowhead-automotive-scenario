#!/usr/bin/env bash
#
# load-test-v2.sh - HONEST load / delivery measurement for the Arrowhead pub/sub chain.
#
# Replaces the methodologically-broken load-test.sh (kept as evidence). Two fixes:
#   (#1) measures REAL end-to-end DELIVERY, not just publish-POST 2xx. It counts the
#        subscriber's "Received event" log lines before/after each level and waits for
#        asynchronous delivery to settle.
#   (#2) computes latency mean/percentiles ONLY over SUCCESSFUL (2xx) requests; failures
#        are counted separately and never pollute the latency stats.
#
# Each POST to the publisher triggers exactly one quality.inspection.created event
# (QualityInspectionService.create publishes unconditionally), so:
#     delivery rate = delivered_events / successful_publishes
#
# Modes:
#   secure   - POST https:// with a client certificate (mTLS). Subscriber runs as the
#              `maintenance-subscriber` container; delivery is read from its docker logs.
#   insecure - POST http:// with no client cert. Subscriber delivery is read from a log file.
#
# Nothing here is destructive; it only sends inspections and reads logs. It does NOT run the
# full battery by itself unless you pass the levels - it is parameterised.
#
# Usage (env-overridable):
#   MODE=secure   REQUESTS=200 LEVELS="1 5 10 20 50" OUT=results.csv  bash load-test-v2.sh
#   MODE=insecure SUB_LOG=/tmp/maintenance.log ...                    bash load-test-v2.sh
#
# Validation example (small):
#   MODE=secure REQUESTS=10 LEVELS="5" OUT=/tmp/val.csv bash load-test-v2.sh

set -uo pipefail

# ----------------------------------------------------------------------------- config
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

MODE="${MODE:-secure}"                       # secure | insecure
REQUESTS="${REQUESTS:-200}"                  # requests per level
LEVELS="${LEVELS:-1 5 10 20 50}"             # concurrency levels
OUT="${OUT:-$REPO_ROOT/rezultate-v2-${MODE}.csv}"

PUB_HOST="${PUB_HOST:-localhost}"
PUB_PORT="${PUB_PORT:-9895}"
PUB_PATH="${PUB_PATH:-/quality-inspections}"

# client cert (secure mode) - sysop from testcloud1 by default
CLIENT_P12="${CLIENT_P12:-$REPO_ROOT/core-java-spring/certificates/testcloud1/sysop.p12}"
CLIENT_P12_PASS="${CLIENT_P12_PASS:-123456}"

# delivery source
SUB_CONTAINER="${SUB_CONTAINER:-maintenance-subscriber}"   # secure mode: docker logs
SUB_LOG="${SUB_LOG:-/tmp/maintenance.log}"                 # insecure mode: log file
DELIVERY_MARK="${DELIVERY_MARK:-Received event}"

# asynchronous-delivery settling
MIN_GRACE="${MIN_GRACE:-3}"        # seconds to wait before checking (let delivery begin)
POLL_INTERVAL="${POLL_INTERVAL:-1}"
STABLE_ROUNDS="${STABLE_ROUNDS:-4}" # consecutive unchanged polls => settled
MAX_WAIT="${MAX_WAIT:-40}"          # hard cap on settling per level (seconds)

BODY='{"carId":1,"brand":"Load","color":"X","defectDetected":true,"defectType":"WEAR","inspectionResult":"DEFECT_DETECTED","inspectionTimestamp":0}'
URL="http${MODE:+}"; [ "$MODE" = secure ] && URL="https://$PUB_HOST:$PUB_PORT$PUB_PATH" || URL="http://$PUB_HOST:$PUB_PORT$PUB_PATH"

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
TIMES="$TMP/times.txt"

# ----------------------------------------------------------------------------- pre-flight
if [ "$MODE" = secure ]; then
  command -v openssl >/dev/null || { echo "openssl required"; exit 1; }
  # -legacy: the 2019 sysop.p12 uses RC2-40 which OpenSSL 3.x reads only with -legacy
  openssl pkcs12 -legacy -in "$CLIENT_P12" -passin pass:"$CLIENT_P12_PASS" -clcerts -nokeys -out "$TMP/cli.crt" 2>/dev/null
  openssl pkcs12 -legacy -in "$CLIENT_P12" -passin pass:"$CLIENT_P12_PASS" -nocerts -nodes  -out "$TMP/cli.key" 2>/dev/null
  [ -s "$TMP/cli.crt" ] && [ -s "$TMP/cli.key" ] || { echo "could not extract client cert from $CLIENT_P12"; exit 1; }
  export CERT="$TMP/cli.crt" KEY="$TMP/cli.key"
  docker ps --format '{{.Names}}' | grep -qx "$SUB_CONTAINER" || { echo "subscriber container '$SUB_CONTAINER' not running"; exit 1; }
else
  [ -f "$SUB_LOG" ] || echo "warning: subscriber log '$SUB_LOG' not found (delivery will read 0)"
fi

# one request -> prints "<time_total> <http_code>"
do_req() {
  if [ "$MODE" = secure ]; then
    curl -s -k --cert "$CERT" --key "$KEY" -o /dev/null -w "%{time_total} %{http_code}\n" \
      -X POST "$URL" -H 'Content-Type: application/json' -d "$BODY"
  else
    curl -s -o /dev/null -w "%{time_total} %{http_code}\n" \
      -X POST "$URL" -H 'Content-Type: application/json' -d "$BODY"
  fi
}
export -f do_req; export MODE URL BODY

delivered_count() {
  # grep -c always prints a single integer (0 when no match) but exits non-zero on 0;
  # capture the number and never rely on the exit code, so the result is one clean int.
  local n
  if [ "$MODE" = secure ]; then
    n="$(docker logs "$SUB_CONTAINER" 2>&1 | grep -c "$DELIVERY_MARK")"
  else
    n="$(grep -c "$DELIVERY_MARK" "$SUB_LOG" 2>/dev/null)"
  fi
  echo "${n:-0}"
}

# wait for asynchronous delivery to settle; $1 = target count (exit early once reached)
settle_delivery() {
  local target="$1" prev=-1 cur stable=0 waited=0
  sleep "$MIN_GRACE"; waited="$MIN_GRACE"
  while [ "$waited" -lt "$MAX_WAIT" ]; do
    cur="$(delivered_count)"
    [ "$cur" -ge "$target" ] && { echo "$cur"; return; }       # all delivered
    if [ "$cur" -eq "$prev" ]; then
      stable=$((stable + 1)); [ "$stable" -ge "$STABLE_ROUNDS" ] && break
    else
      stable=0
    fi
    prev="$cur"; sleep "$POLL_INTERVAL"; waited=$((waited + POLL_INTERVAL))
  done
  echo "$cur"
}

# ----------------------------------------------------------------------------- run
echo "MODE=$MODE  URL=$URL  REQUESTS=$REQUESTS  LEVELS=[$LEVELS]  OUT=$OUT"
echo "nivel,cereri_trimise,publicare_reusite,publicare_esuate,livrate,rata_livrare,throughput_publicare,latenta_medie_ok,p50_ok,p95_ok,p99_ok" > "$OUT"

for C in $LEVELS; do
  echo "=== concurenta=$C, cereri=$REQUESTS ==="
  before="$(delivered_count)"
  : > "$TIMES"
  start="$(date +%s.%N)"
  seq "$REQUESTS" | xargs -P "$C" -I{} bash -c 'do_req' >> "$TIMES"
  end="$(date +%s.%N)"

  # successes/failures + latency stats over 2xx ONLY
  read -r ok fail dur tput mean p50 p95 p99 < <(python3 - "$TIMES" "$start" "$end" <<'PY'
import sys, math
f, start, end = sys.argv[1], float(sys.argv[2]), float(sys.argv[3])
ok_t = []; ok = 0; fail = 0
for line in open(f):
    line = line.strip()
    if not line: continue
    p = line.split()
    if len(p) < 2: fail += 1; continue
    try: t = float(p[0]) * 1000.0
    except ValueError: fail += 1; continue
    if p[1].startswith('2'): ok += 1; ok_t.append(t)
    else: fail += 1
ok_t.sort()
def pct(q):
    if not ok_t: return 0.0
    k = (len(ok_t) - 1) * q; lo = math.floor(k); hi = math.ceil(k)
    return ok_t[int(k)] if lo == hi else ok_t[lo] + (ok_t[hi] - ok_t[lo]) * (k - lo)
dur = end - start
tput = ok / dur if dur > 0 else 0.0          # SUCCESSFUL publishes per second
mean = sum(ok_t) / len(ok_t) if ok_t else 0.0
print(f"{ok} {fail} {dur:.2f} {tput:.1f} {mean:.2f} {pct(.5):.2f} {pct(.95):.2f} {pct(.99):.2f}")
PY
)

  # real delivery (async): wait until settled, count the delta
  after="$(settle_delivery $((before + ok)))"
  delivered=$((after - before))
  [ "$delivered" -lt 0 ] && delivered=0
  if [ "$ok" -gt 0 ]; then rate="$(awk "BEGIN{printf \"%.3f\", $delivered/$ok}")"; else rate="0.000"; fi

  echo "  publicare: reusite=$ok esuate=$fail throughput=${tput}/s   latenta(ok): medie=${mean}ms p50=$p50 p95=$p95 p99=$p99"
  echo "  livrare:   inainte=$before dupa=$after livrate=$delivered  rata_livrare=$rate"
  echo "$C,$REQUESTS,$ok,$fail,$delivered,$rate,$tput,$mean,$p50,$p95,$p99" >> "$OUT"
done

echo
echo "=== $OUT ==="
cat "$OUT"
