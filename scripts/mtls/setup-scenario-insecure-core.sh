#!/usr/bin/env bash
#
# setup-scenario-insecure-core.sh - bring up the Arrowhead core in INSECURE mode (HTTP),
# on a fresh DB volume, for the controlled secure-vs-insecure experiment.
#
# Symmetric counterpart of scripts/mtls/setup-scenario-secure.sh: same container, ports and
# flow, only ssl is off (uses docker-compose-arrowhead-core-insecure.yml). Secure and insecure
# cores share the same ports, so run them ALTERNATELY (this script's `down` clears whichever
# core is up first).
#
# Requires: docker, docker compose, curl.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose-arrowhead-core-insecure.yml"
SECURE_COMPOSE="$REPO_ROOT/docker-compose-arrowhead-core.yml"

SYSTEMS=(
  "Service Registry|http://localhost:8443/serviceregistry/echo"
  "Authorization|http://localhost:8445/authorization/echo"
  "Orchestrator|http://localhost:8441/orchestrator/echo"
  "Event Handler|http://localhost:8455/eventhandler/echo"
)

wait_for_echo() {  # $1 = label, $2 = url
  for _ in $(seq 1 60); do
    [ "$(curl -s -m3 "$2" 2>/dev/null)" = "Got it!" ] && { echo "   $1 reachable over HTTP."; return 0; }
    sleep 3
  done
  echo "   $1 did NOT answer echo in time."; return 1
}

echo "## 1. Clean recreate of the Arrowhead core (fresh DB volume), INSECURE mode"
# bring down whichever core compose might be up (secure or insecure) so ports are free
docker compose -f "$SECURE_COMPOSE" down >/dev/null 2>&1 || true
docker compose -f "$COMPOSE_FILE" down
docker volume rm arrowhead_core_mysql >/dev/null 2>&1 || true
docker volume create arrowhead_core_mysql >/dev/null
docker compose -f "$COMPOSE_FILE" up -d

echo "## 2. Wait for the core systems (asynchronous; SR first)"
for entry in "${SYSTEMS[@]}"; do
  IFS="|" read -r label url <<<"$entry"
  wait_for_echo "$label" "$url" || { echo "   See: docker logs arrowhead_core"; exit 1; }
done

echo
echo "Core is up in INSECURE mode (HTTP). Start the host publisher and the insecure subscriber"
echo "with scripts/mtls/run-insecure-subscriber.sh. Application services were NOT started here."
