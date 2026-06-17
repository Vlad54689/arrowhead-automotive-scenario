#!/usr/bin/env bash
#
# run-interleaved.sh - drift-free secure-vs-insecure measurement at high concurrency.
#
# The block design (all insecure, then all secure) is vulnerable to machine drift across the
# long session: if the host slows down between blocks, the secure-minus-insecure overhead is
# biased. The control run revealed exactly that at levels 20/50. This script removes the bias
# by INTERLEAVING: per repetition it measures BOTH modes back-to-back (so both see the same
# instantaneous machine state), and it flips which mode goes first each repetition (so any
# residual within-rep ordering effect averages out).
#
# Because secure and insecure share ports, the two modes cannot run at once: each mode is
# brought fully up (fresh core + publisher + subscriber) for its turn, measured, and torn
# down before the other mode's turn. A warm-up burst is discarded before every measurement.
#
# It reuses the version-controlled setup/run scripts unchanged and the honest measurement
# tool load-test-v2.sh; it only orchestrates them. Rows go to a SEPARATE raw CSV so the block
# results are untouched.
#
# Usage:
#   LEVELS="20 50" REPS=5 REQUESTS=200 WARMUP=50 RAW_CSV=/path.csv bash run-interleaved.sh

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LT="$REPO_ROOT/scripts/mtls/load-test-v2.sh"

LEVELS="${LEVELS:-20 50}"
REPS="${REPS:-5}"
REQUESTS="${REQUESTS:-200}"
WARMUP="${WARMUP:-50}"
RAW_CSV="${RAW_CSV:-$REPO_ROOT/rezultate-intercalat-brut.csv}"

SECURE_CORE="$REPO_ROOT/docker-compose-arrowhead-core.yml"
INSEC_CORE="$REPO_ROOT/docker-compose-arrowhead-core-insecure.yml"
SECURE_SUB="$REPO_ROOT/docker-compose-secure-subscriber.yml"
INSEC_SUB="$REPO_ROOT/docker-compose-insecure-subscriber.yml"

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

authorized_now() {
  local a
  a="$(mq "SELECT spc.authorized FROM subscription_publisher_connection spc
           JOIN subscription sub ON spc.subscription_id=sub.id
           JOIN system_ s ON sub.system_id=s.id
           WHERE s.system_name='maintenancerecommendation' ORDER BY spc.id DESC LIMIT 1;")"
  [ "$a" = "1" ]
}

teardown_all() {
  docker compose -f "$SECURE_SUB"  down >/dev/null 2>&1 || true
  docker compose -f "$INSEC_SUB"   down >/dev/null 2>&1 || true
  docker compose -f "$SECURE_CORE" down >/dev/null 2>&1 || true
  docker compose -f "$INSEC_CORE"  down >/dev/null 2>&1 || true
}

bring_up() {                                   # $1 = mode ; ensures an AUTHORIZED subscriber
  local mode="$1" sub_script ok=0 attempt
  teardown_all
  if [ "$mode" = insecure ]; then
    bash "$REPO_ROOT/scripts/mtls/setup-scenario-insecure-core.sh" >/dev/null 2>&1
    sub_script="run-insecure-subscriber.sh"
  else
    bash "$REPO_ROOT/scripts/mtls/setup-scenario-secure.sh" >/dev/null 2>&1
    sub_script="run-secure-subscriber.sh"
  fi
  bash "$REPO_ROOT/scripts/mtls/run-publisher.sh" "$mode" >/dev/null 2>&1

  # subscriber + auth rule, with retry for the early-subscribe race (subscribe() can run
  # before the core's authorization-check service is registered)
  for attempt in 1 2 3 4; do
    if bash "$REPO_ROOT/scripts/mtls/$sub_script" >/dev/null 2>&1; then ok=1; break; fi
    docker restart maintenance-subscriber >/dev/null 2>&1 || true
    sleep 8
    for _ in $(seq 1 12); do authorized_now && { ok=1; break; }; sleep 2; done
    [ "$ok" = 1 ] && break
  done
  [ "$ok" = 1 ] || { echo "    !! FATAL: $mode subscriber never reached authorized=1"; return 1; }
  return 0
}

measure() {                                    # $1 = mode , $2 = rep ; one row per LEVEL
  local mode="$1" rep="$2" lvl MW tmpout row
  for lvl in $LEVELS; do
    [ "$lvl" -ge 20 ] && MW=60 || MW=40
    MODE="$mode" REQUESTS="$WARMUP" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="/tmp/il-warmup-$mode.csv" bash "$LT" >/dev/null 2>&1
    tmpout="$(mktemp)"
    MODE="$mode" REQUESTS="$REQUESTS" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="$tmpout" bash "$LT" >/dev/null 2>&1
    row="$(tail -n1 "$tmpout")"; rm -f "$tmpout"
    case "$row" in
      "$lvl",*) echo "$mode,$rep,$row" >> "$RAW_CSV"; echo "    -> $mode,$rep,$row" ;;
      *)        echo "    !! no data row for $mode rep=$rep lvl=$lvl (got: '$row')" ;;
    esac
  done
}

[ -f "$RAW_CSV" ] || echo "mod,rep,nivel,cereri_trimise,publicare_reusite,publicare_esuate,livrate,rata_livrare,throughput_publicare,latenta_medie_ok,p50_ok,p95_ok,p99_ok" > "$RAW_CSV"

echo "=== INTERLEAVED: LEVELS=[$LEVELS] REPS=$REPS REQUESTS=$REQUESTS WARMUP=$WARMUP -> $RAW_CSV"
for rep in $(seq 1 "$REPS"); do
  if [ $((rep % 2)) -eq 1 ]; then order="insecure secure"; else order="secure insecure"; fi
  echo ">>> REP $rep  (order: $order)"
  for mode in $order; do
    echo ">>> rep=$rep bring up $mode"
    bring_up "$mode" || { echo "abort: bring_up $mode failed"; exit 1; }
    measure "$mode" "$rep"
  done
done

echo "=== INTERLEAVED done ==="
