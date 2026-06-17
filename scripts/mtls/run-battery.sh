#!/usr/bin/env bash
#
# run-battery.sh <secure|insecure> - run ONE measurement block for the controlled
# secure-vs-insecure experiment, on top of an already-running stack for that mode.
#
# It does NOT touch the scripts that work: it only orchestrates repeated calls to the
# honest measurement tool scripts/mtls/load-test-v2.sh, adding two things the plan asks for
# that load-test-v2.sh does not do by itself:
#   * REPS repetitions per concurrency level (for mean +/- standard deviation), and
#   * a DISCARDED warm-up burst before every single measurement (results thrown away).
#
# Every measurement row produced by load-test-v2.sh (one line per level) is appended to the
# shared RAW csv, tagged with the mode LABEL and the repetition number. Nothing is aggregated
# here - aggregation is a separate, offline step over the raw csv.
#
# Pre-conditions: the core + publisher + subscriber for <mode> are already up (use the
# setup-scenario / run-publisher / run-*-subscriber scripts first). This script never starts
# or stops containers.
#
# Usage (env-overridable):
#   bash run-battery.sh insecure
#   REPS=5 REQUESTS=200 WARMUP=50 LEVELS="1 5 10 20 50" LABEL=insecure_control \
#     RAW_CSV=/path/raw.csv bash run-battery.sh insecure

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LT="$REPO_ROOT/scripts/mtls/load-test-v2.sh"

MODE="${1:-}"
[ "$MODE" = secure ] || [ "$MODE" = insecure ] || { echo "usage: $0 <secure|insecure>"; exit 1; }

REPS="${REPS:-5}"
LEVELS="${LEVELS:-1 5 10 20 50}"
REQUESTS="${REQUESTS:-200}"
WARMUP="${WARMUP:-50}"
LABEL="${LABEL:-$MODE}"                                   # row tag (mode, or e.g. insecure_control)
RAW_CSV="${RAW_CSV:-$REPO_ROOT/rezultate-baterie-brut.csv}"

# raw csv header (created once; appended to thereafter)
if [ ! -f "$RAW_CSV" ]; then
  echo "mod,rep,nivel,cereri_trimise,publicare_reusite,publicare_esuate,livrate,rata_livrare,throughput_publicare,latenta_medie_ok,p50_ok,p95_ok,p99_ok" > "$RAW_CSV"
fi

echo "=== BATTERY block: MODE=$MODE LABEL=$LABEL REPS=$REPS LEVELS=[$LEVELS] REQUESTS=$REQUESTS WARMUP=$WARMUP"
echo "=== raw csv: $RAW_CSV"

for rep in $(seq 1 "$REPS"); do
  for lvl in $LEVELS; do
    # generous delivery settle at high concurrency so late deliveries are not missed
    if [ "$lvl" -ge 20 ]; then MW=60; else MW=40; fi

    echo ">>> $LABEL rep=$rep level=$lvl : warm-up($WARMUP, discarded) then measure($REQUESTS)"

    # ---- warm-up (discarded): same path, throwaway output ----
    MODE="$MODE" REQUESTS="$WARMUP" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="/tmp/battery-warmup-$MODE.csv" bash "$LT" >/dev/null 2>&1

    # ---- measurement ----
    tmpout="$(mktemp)"
    MODE="$MODE" REQUESTS="$REQUESTS" LEVELS="$lvl" MAX_WAIT="$MW" \
      OUT="$tmpout" bash "$LT" >/dev/null 2>&1
    row="$(tail -n1 "$tmpout")"
    rm -f "$tmpout"

    # guard: a data row starts with the numeric level; skip anything else (e.g. a header)
    case "$row" in
      "$lvl",*) echo "$LABEL,$rep,$row" >> "$RAW_CSV"; echo "    -> $LABEL,$rep,$row" ;;
      *)        echo "    !! no data row for rep=$rep level=$lvl (got: '$row')" ;;
    esac
  done
done

echo "=== BATTERY block done: MODE=$MODE LABEL=$LABEL"
