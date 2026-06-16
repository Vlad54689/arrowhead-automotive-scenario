#!/usr/bin/env bash
#
# setup-scenario-secure.sh - Phase 2 of the mTLS experiment.
#
# Starts ONLY the Arrowhead core (Service Registry, Authorization, Orchestrator,
# Event Handler) in SECURE mode (HTTPS + mutual TLS) and verifies that each of
# the 4 essential systems answers a secure echo. It does NOT start the demo
# application services - that is Phase 3.
#
# Differences from the insecure setup-scenario.sh (which is left untouched):
#   * the core now serves HTTPS on the same ports (8443/8445/8441/8455);
#   * the echo checks present a client certificate (mTLS) - the sysop cert,
#     converted to PEM because curl's OpenSSL backend cannot read PKCS12 directly.
#
# Requires: docker, docker compose, curl, openssl.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose-arrowhead-core.yml"
CERTS="$REPO_ROOT/core-java-spring/certificates/testcloud1"
PASSWORD="123456"

# sysop client material (PEM) for curl - created in a temp dir, cleaned on exit
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
SYSOP_CRT="$TMP/sysop.crt"
SYSOP_KEY="$TMP/sysop.key"
# -legacy: the sysop.p12 was created with legacy (RC2-40) encryption that OpenSSL 3.x
# refuses to read without it.
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -clcerts -nokeys -out "$SYSOP_CRT" 2>/dev/null
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -nocerts -nodes  -out "$SYSOP_KEY" 2>/dev/null

# label : https echo url
SYSTEMS=(
  "Service Registry|https://localhost:8443/serviceregistry/echo"
  "Authorization|https://localhost:8445/authorization/echo"
  "Orchestrator|https://localhost:8441/orchestrator/echo"
  "Event Handler|https://localhost:8455/eventhandler/echo"
)

secure_echo() {  # $1 = url -> echoes body, returns curl exit code
  curl -s -m 5 -k \
    --cert "$SYSOP_CRT" --key "$SYSOP_KEY" \
    "$1"
}

wait_for_secure_echo() {  # $1 = label, $2 = url
  for _ in $(seq 1 60); do
    if [[ "$(secure_echo "$2" 2>/dev/null)" == "Got it!" ]]; then
      echo "   $1 reachable over HTTPS/mTLS."; return 0
    fi
    sleep 3
  done
  echo "   $1 did NOT answer secure echo in time."; return 1
}

echo "## 1. Clean recreate of the Arrowhead core (fresh DB volume), SECURE mode"
docker compose -f "$COMPOSE_FILE" down
docker volume rm arrowhead_core_mysql >/dev/null 2>&1 || true
docker volume create arrowhead_core_mysql >/dev/null
docker compose -f "$COMPOSE_FILE" up -d

echo "## 2. Wait for the core systems (they come up asynchronously; SR first)"
for entry in "${SYSTEMS[@]}"; do
  IFS="|" read -r label url <<<"$entry"
  wait_for_secure_echo "$label" "$url" || { echo "   See: docker logs arrowhead_core"; exit 1; }
done

echo
echo "## 3. Verification summary (TLS handshake + secure echo)"
for entry in "${SYSTEMS[@]}"; do
  IFS="|" read -r label url <<<"$entry"
  hostport="${url#https://}"; hostport="${hostport%%/*}"
  echo "### $label ($hostport)"
  # server certificate presented during the handshake
  server_cn="$(openssl s_client -connect "$hostport" \
      -cert "$SYSOP_CRT" -key "$SYSOP_KEY" </dev/null 2>/dev/null \
      | openssl x509 -noout -subject 2>/dev/null)"
  echo "   server cert : ${server_cn:-<handshake failed>}"
  echo "   secure echo : $(secure_echo "$url")"
done

echo
echo "Core is up in SECURE mode. Application services were NOT started (that is Phase 3)."
