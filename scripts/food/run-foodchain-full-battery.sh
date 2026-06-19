#!/usr/bin/env bash
#
# run-foodchain-full-battery.sh - full secure-vs-insecure battery for the 3-hop food cold-chain.
#
# Reuses the honest multi-hop measurement tool scripts/food/load-test-foodchain.sh (single pass)
# and the version-controlled setup scripts; it only orchestrates them. Mirrors the automotive
# methodology (run-battery.sh + run-interleaved.sh):
#   * REPS repetitions per level (mean +/- sd offline), a DISCARDED warm-up before every measure,
#   * generous delivery settle at high concurrency (MAX_WAIT 90s for level >= 20),
#   * LOW levels (1 5 10) measured in BLOCKS (one bring-up per mode, all reps),
#   * HIGH levels (20 50) measured INTERLEAVED per rep (both modes back-to-back, order flipped each
#     rep) so machine drift cannot bias the secure-minus-insecure overhead that matters most.
#
# secure and insecure share ports, so the two modes cannot run at once: each mode is brought fully
# up (fresh core + ColdChain host publisher + the 2 subscriber containers + per-hop auth rules +
# re-subscribe to authorized=1) for its turn. Rows go to a SEPARATE foodchain raw CSV.
#
# Usage (env-overridable):
#   REPS=5 REQUESTS=200 WARMUP=50 bash scripts/food/run-foodchain-full-battery.sh

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LT="$REPO_ROOT/scripts/food/load-test-foodchain.sh"
CERTS="$REPO_ROOT/core-java-spring/certificates/testcloud1"
PASSWORD="123456"

REPS="${REPS:-5}"
REQUESTS="${REQUESTS:-200}"
WARMUP="${WARMUP:-50}"
# Use ${VAR-default} (no colon) so an explicitly-empty value is HONORED (skips that phase);
# the default applies only when the variable is unset. (${VAR:-default} would wrongly replace
# an empty override with the default.)
LOW_LEVELS="${LOW_LEVELS-1 5 10}"
HIGH_LEVELS="${HIGH_LEVELS-20 50}"
RAW_CSV="${RAW_CSV:-$REPO_ROOT/rezultate-foodchain-brut.csv}"

INSEC_CORE="$REPO_ROOT/docker-compose-arrowhead-core-insecure.yml"
SECURE_CORE="$REPO_ROOT/docker-compose-arrowhead-core.yml"
INSEC_SUB="$REPO_ROOT/docker-compose-food-chain-insecure.yml"
SECURE_SUB="$REPO_ROOT/docker-compose-food-chain-secure.yml"

CC_JAR="$REPO_ROOT/demo-car-with-events/demo-food-chain/demo-cold-chain-monitor-service/target/demo-cold-chain-monitor-service-4.4.0.2.jar"
CC_CERTDIR="$REPO_ROOT/demo-car-with-events/demo-food-chain/demo-cold-chain-monitor-service/src/main/resources/certificates"

TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
# sysop client material for secure auth POSTs
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -clcerts -nokeys -out "$TMP/sysop.crt" 2>/dev/null
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -nocerts -nodes  -out "$TMP/sysop.key" 2>/dev/null

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

ts() { date '+%H:%M:%S'; }

kill_coldchain() { pgrep -f "demo-cold-chain-monitor-service-4.4.0.2.jar" 2>/dev/null | xargs -r kill 2>/dev/null || true; }

teardown_all() {
  docker compose -f "$SECURE_SUB" down >/dev/null 2>&1 || true
  docker compose -f "$INSEC_SUB"  down >/dev/null 2>&1 || true
  kill_coldchain
  docker compose -f "$SECURE_CORE" down >/dev/null 2>&1 || true
  docker compose -f "$INSEC_CORE"  down >/dev/null 2>&1 || true
  for _ in $(seq 1 15); do ss -ltn 2>/dev/null | grep -q ":9897 " || break; sleep 1; done
}

start_coldchain() {  # $1 = mode
  local mode="$1" log="/tmp/coldchain-battery-$1.log"
  kill_coldchain
  : > "$log"
  if [ "$mode" = secure ]; then
    ( cd /tmp && setsid nohup java -jar "$CC_JAR" \
        --server.ssl.enabled=true --server.ssl.key-store-type=PKCS12 \
        --server.ssl.key-store=file:$CC_CERTDIR/coldchainmonitor.p12 --server.ssl.key-store-password=123456 \
        --server.ssl.key-alias=coldchainmonitor --server.ssl.key-password=123456 \
        --server.ssl.client-auth=need --server.ssl.trust-store-type=PKCS12 \
        --server.ssl.trust-store=file:$CC_CERTDIR/truststore.p12 --server.ssl.trust-store-password=123456 \
        --token.security.filter.enabled=false \
        --server.address=127.0.0.1 --server.port=9897 --sr_address=127.0.0.1 --sr_port=8443 >> "$log" 2>&1 & )
  else
    ( cd /tmp && setsid nohup java -jar "$CC_JAR" \
        --server.ssl.enabled=false --token.security.filter.enabled=false \
        --server.address=127.0.0.1 --server.port=9897 --sr_address=127.0.0.1 --sr_port=8443 >> "$log" 2>&1 & )
  fi
  for _ in $(seq 1 45); do
    grep -qE "Started ColdChainMonitor" "$log" 2>/dev/null && return 0
    grep -qE "APPLICATION FAILED|Error starting ApplicationContext" "$log" 2>/dev/null && return 1
    sleep 1
  done
  return 1
}

make_rule() {  # $1=mode $2=consumer $3=provider $4=providerServiceDef
  local mode="$1" consumer provider svcdef iface
  consumer=$(mq "SELECT id FROM system_ WHERE system_name='$2' ORDER BY id DESC LIMIT 1;")
  provider=$(mq "SELECT id FROM system_ WHERE system_name='$3' ORDER BY id DESC LIMIT 1;")
  svcdef=$(mq   "SELECT id FROM service_definition WHERE service_definition='$4';")
  if [ "$mode" = secure ]; then
    iface=$(mq "SELECT id FROM service_interface WHERE interface_name='HTTP-SECURE-JSON';")
    curl -s -k -m10 --cert "$TMP/sysop.crt" --key "$TMP/sysop.key" \
      -X POST "https://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
      -d "{\"consumerId\":$consumer,\"providerIds\":[$provider],\"serviceDefinitionIds\":[$svcdef],\"interfaceIds\":[$iface]}" -o /dev/null
  else
    iface=$(mq "SELECT id FROM service_interface WHERE interface_name='HTTP-INSECURE-JSON';")
    curl -s -m10 -X POST "http://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
      -d "{\"consumerId\":$consumer,\"providerIds\":[$provider],\"serviceDefinitionIds\":[$svcdef],\"interfaceIds\":[$iface]}" -o /dev/null
  fi
}

authorized_for() {  # $1=subscriberName
  mq "SELECT spc.authorized FROM subscription_publisher_connection spc
      JOIN subscription sub ON spc.subscription_id=sub.id
      JOIN system_ s ON sub.system_id=s.id
      WHERE s.system_name='$1' ORDER BY spc.id DESC LIMIT 1;"
}

wait_started() {  # $1=container $2=pattern
  for _ in $(seq 1 40); do
    docker logs --since 120s "$1" 2>&1 | grep -qE "$2" && return 0
    docker logs --since 120s "$1" 2>&1 | grep -qE "APPLICATION FAILED|Error starting ApplicationContext" && return 1
    sleep 2
  done
  return 1
}

bring_up() {  # $1 = mode ; ensures both subscriptions authorized=1
  local mode="$1" sub core attempt ok=0 a1 a2
  teardown_all
  if [ "$mode" = insecure ]; then
    bash "$REPO_ROOT/scripts/mtls/setup-scenario-insecure-core.sh" >/dev/null 2>&1
    sub="$INSEC_SUB"
  else
    bash "$REPO_ROOT/scripts/mtls/setup-scenario-secure.sh" >/dev/null 2>&1
    sub="$SECURE_SUB"
  fi
  start_coldchain "$mode" || { echo "  [$(ts)] !! ColdChain failed to start ($mode)"; return 1; }
  docker compose -f "$sub" up -d --force-recreate >/dev/null 2>&1
  wait_started risk-scoring-subscriber "Started RiskScoring" || { echo "  !! RiskScoring failed"; return 1; }
  wait_started supply-chain-trust-subscriber "Started SupplyChainTrust" || { echo "  !! Trust failed"; return 1; }

  for attempt in 1 2 3 4; do
    make_rule "$mode" riskscoring coldchainmonitor coldchain-reading
    make_rule "$mode" supplychaintrust riskscoring risk-scoring-notify
    docker restart risk-scoring-subscriber supply-chain-trust-subscriber >/dev/null 2>&1
    wait_started risk-scoring-subscriber "Started RiskScoring" || true
    wait_started supply-chain-trust-subscriber "Started SupplyChainTrust" || true
    a1=0; a2=0
    for _ in $(seq 1 12); do
      a1=$(authorized_for riskscoring); a2=$(authorized_for supplychaintrust)
      [ "$a1" = "1" ] && [ "$a2" = "1" ] && { ok=1; break; }
      sleep 2
    done
    [ "$ok" = 1 ] && break
  done
  [ "$ok" = 1 ] || { echo "  [$(ts)] !! FATAL: $mode never reached authorized=1 (risk=$a1 trust=$a2)"; return 1; }
  return 0
}

measure() {  # $1=mode $2=rep $3=levels
  local mode="$1" rep="$2" levels="$3" lvl MW tmpout row
  for lvl in $levels; do
    [ "$lvl" -ge 20 ] && MW=90 || MW=45
    MODE="$mode" REQUESTS="$WARMUP" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="$TMP/warmup.csv" bash "$LT" >/dev/null 2>&1
    tmpout="$(mktemp)"
    MODE="$mode" REQUESTS="$REQUESTS" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="$tmpout" bash "$LT" >/dev/null 2>&1
    row="$(tail -n1 "$tmpout")"; rm -f "$tmpout"
    case "$row" in
      "$lvl",*) echo "$mode,$rep,$row" >> "$RAW_CSV"; echo "  [$(ts)] -> $mode,$rep,$row" ;;
      *)        echo "  [$(ts)] !! no data row for $mode rep=$rep lvl=$lvl (got: '$row')" ;;
    esac
  done
}

# foodchain raw header (mod,rep,<load-test row>)
[ -f "$RAW_CSV" ] || echo "mod,rep,nivel,cereri_trimise,cereri_reusite_post,verdicte_ajunse,rata_livrare_e2e,throughput_post_rps,lat_cumulata_medie_ms,p50_ms,p95_ms,p99_ms,orient_hop1_ms,orient_hop2_ms" > "$RAW_CSV"

echo "============================================================"
echo "FOODCHAIN FULL BATTERY  start=$(date '+%F %T')"
echo "  REPS=$REPS REQUESTS=$REQUESTS WARMUP=$WARMUP"
echo "  LOW(blocks)=[$LOW_LEVELS]  HIGH(interleaved)=[$HIGH_LEVELS]"
echo "  RAW=$RAW_CSV"
echo "============================================================"

# ---------- PHASE 1: LOW levels in blocks (one bring-up per mode, all reps) ----------
# Skipped when LOW_LEVELS is empty (e.g. re-running a single level interleaved instead).
if [ -n "${LOW_LEVELS// /}" ]; then
  for mode in insecure secure; do
    echo ">>> [$(ts)] PHASE1 bring up $mode for LOW levels"
    bring_up "$mode" || { echo "abort: bring_up $mode (low) failed"; exit 1; }
    for rep in $(seq 1 "$REPS"); do
      echo ">>> [$(ts)] PHASE1 $mode rep=$rep levels=[$LOW_LEVELS]"
      measure "$mode" "$rep" "$LOW_LEVELS"
    done
  done
else
  echo ">>> [$(ts)] PHASE1 skipped (LOW_LEVELS empty)"
fi

# ---------- PHASE 2: HIGH levels interleaved (both modes per rep, order flipped) ----------
# Skipped when HIGH_LEVELS is empty. Any level put here is measured drift-free (interleaved).
if [ -n "${HIGH_LEVELS// /}" ]; then
  for rep in $(seq 1 "$REPS"); do
    if [ $((rep % 2)) -eq 1 ]; then order="insecure secure"; else order="secure insecure"; fi
    echo ">>> [$(ts)] PHASE2 REP $rep (order: $order) levels=[$HIGH_LEVELS]"
    for mode in $order; do
      echo ">>> [$(ts)] PHASE2 rep=$rep bring up $mode"
      bring_up "$mode" || { echo "abort: bring_up $mode (high) failed"; exit 1; }
      measure "$mode" "$rep" "$HIGH_LEVELS"
    done
  done
else
  echo ">>> [$(ts)] PHASE2 skipped (HIGH_LEVELS empty)"
fi

echo "============================================================"
echo "FOODCHAIN FULL BATTERY  done=$(date '+%F %T')"
echo "  raw rows: $(($(wc -l < "$RAW_CSV") - 1))"
echo "============================================================"
