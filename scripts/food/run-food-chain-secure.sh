#!/usr/bin/env bash
#
# run-food-chain-secure.sh - bring up and end-to-end verify the 3-hop food cold-chain in
# SECURE mode (HTTPS + mutual TLS), on the existing containerized Arrowhead topology.
#
# Secure counterpart of scripts/food/run-food-chain-insecure.sh. The ONLY differences are
# security: HTTPS instead of HTTP, each system presents its testcloud1-signed certificate,
# client-auth=need, the management/authorization calls use the sysop client cert, and the
# interface is HTTP-SECURE-JSON. Same topology, ports and flow.
#
# Everything is on 127.0.0.1 (the core's loopback, shared by the subscriber containers via
# network_mode: container:arrowhead_core), which is in every cert's SAN -> TLS hostname
# verification is STRICT (no disable.hostname.verifier). Full mTLS, end to end.
#
# Middle link note: RiskScoring is sub+pub. In secure it presents its cert as SERVER (receiving
# /notify from the Event Handler) AND as CLIENT (publishing risk.assessed, registering). One
# certificate (riskscoring.p12) covers both directions.
#
# Precondition: the Arrowhead core in SECURE mode is up (scripts/mtls/setup-scenario-secure.sh).

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="$REPO_ROOT/docker-compose-food-chain-secure.yml"
CERTS="$REPO_ROOT/core-java-spring/certificates/testcloud1"
PASSWORD="123456"

CC_JAR="$REPO_ROOT/demo-car-with-events/demo-food-chain/demo-cold-chain-monitor-service/target/demo-cold-chain-monitor-service-4.4.0.2.jar"
CC_CERTDIR="$REPO_ROOT/demo-car-with-events/demo-food-chain/demo-cold-chain-monitor-service/src/main/resources/certificates"
CC_LOG="/tmp/coldchain-secure.log"

IFACE="HTTP-SECURE-JSON"

# sysop client material (PEM) for the HTTPS management/POST calls (-legacy: 2019 RC2-40 p12)
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -clcerts -nokeys -out "$TMP/sysop.crt" 2>/dev/null
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -nocerts -nodes  -out "$TMP/sysop.key" 2>/dev/null
[ -s "$TMP/sysop.crt" ] && [ -s "$TMP/sysop.key" ] || { echo "could not extract sysop client cert"; exit 1; }

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

scurl() { curl -s -k --cert "$TMP/sysop.crt" --key "$TMP/sysop.key" "$@"; }  # mTLS curl as sysop

make_rule() {  # $1=consumerName $2=providerName $3=providerServiceDef
  local consumer provider svcdef iface
  consumer=$(mq "SELECT id FROM system_ WHERE system_name='$1' ORDER BY id DESC LIMIT 1;")
  provider=$(mq "SELECT id FROM system_ WHERE system_name='$2' ORDER BY id DESC LIMIT 1;")
  svcdef=$(mq   "SELECT id FROM service_definition WHERE service_definition='$3';")
  iface=$(mq    "SELECT id FROM service_interface WHERE interface_name='$IFACE';")
  echo "   rule consumer($1)=$consumer provider($2)=$provider serviceDef($3)=$svcdef interface=$iface"
  scurl -m10 -X POST "https://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
    -d "{\"consumerId\":$consumer,\"providerIds\":[$provider],\"serviceDefinitionIds\":[$svcdef],\"interfaceIds\":[$iface]}" \
    -o /dev/null -w "   auth rule POST -> HTTP %{http_code}\n"
}

authorized_for() {  # $1=subscriberName
  mq "SELECT spc.authorized FROM subscription_publisher_connection spc
      JOIN subscription sub ON spc.subscription_id=sub.id
      JOIN system_ s ON sub.system_id=s.id
      WHERE s.system_name='$1' ORDER BY spc.id DESC LIMIT 1;"
}

wait_container_started() {  # $1=container $2=pattern
  for _ in $(seq 1 40); do
    docker logs --since 120s "$1" 2>&1 | grep -qE "$2" && return 0
    docker logs --since 120s "$1" 2>&1 | grep -qE "APPLICATION FAILED|Error starting ApplicationContext" && return 1
    sleep 2
  done
  return 1
}

post_reading() {  # $1=batchId $2=originTs $3=temp $4=hum
  scurl -m10 -X POST "https://localhost:9897/cold-chain/readings" -H "Content-Type: application/json" \
    -d "{\"batchId\":\"$1\",\"originTs\":$2,\"temperatura\":$3,\"umiditate\":$4,\"locatie\":\"Depozit\",\"lot\":\"LOT\"}" \
    -o /dev/null -w "   POST -> HTTP %{http_code}\n"
}

echo "## 0. Preconditions"
[ -f "$CC_JAR" ] || { echo "missing $CC_JAR - build with: mvn -pl demo-food-chain -amd install -DskipTests"; exit 1; }
[ -f "$CC_CERTDIR/coldchainmonitor.p12" ] || { echo "missing ColdChain cert - run scripts/food/gen_food_certs.sh"; exit 1; }
[ "$(scurl -m3 https://localhost:8443/serviceregistry/echo 2>/dev/null)" = "Got it!" ] \
  || { echo "Arrowhead core (SECURE) not reachable over HTTPS on 8443 - run setup-scenario-secure.sh first"; exit 1; }
echo "   secure core reachable over mTLS, ColdChain jar + cert present."

echo "## 1. Start the ColdChain publisher on the HOST in SECURE mode (https://127.0.0.1:9897)"
pkill -f "demo-cold-chain-monitor-service-4.4.0.2.jar" 2>/dev/null || true
for _ in $(seq 1 15); do ss -ltn 2>/dev/null | grep -q ":9897 " || break; sleep 1; done
: > "$CC_LOG"
( cd /tmp && setsid nohup java -jar "$CC_JAR" \
    --server.ssl.enabled=true \
    --server.ssl.key-store-type=PKCS12 \
    --server.ssl.key-store=file:$CC_CERTDIR/coldchainmonitor.p12 \
    --server.ssl.key-store-password=123456 \
    --server.ssl.key-alias=coldchainmonitor \
    --server.ssl.key-password=123456 \
    --server.ssl.client-auth=need \
    --server.ssl.trust-store-type=PKCS12 \
    --server.ssl.trust-store=file:$CC_CERTDIR/truststore.p12 \
    --server.ssl.trust-store-password=123456 \
    --token.security.filter.enabled=false \
    --server.address=127.0.0.1 --server.port=9897 \
    --sr_address=127.0.0.1 --sr_port=8443 >> "$CC_LOG" 2>&1 & )
for _ in $(seq 1 40); do
  grep -qE "Started ColdChainMonitor" "$CC_LOG" 2>/dev/null && break
  grep -qE "APPLICATION FAILED|Error starting ApplicationContext" "$CC_LOG" 2>/dev/null && { echo "ColdChain FAILED:"; tail -25 "$CC_LOG"; exit 1; }
  sleep 1
done
grep -qE "Started ColdChainMonitor" "$CC_LOG" || { echo "ColdChain not ready (see $CC_LOG)"; exit 1; }
echo "   ColdChain up (HTTPS) on https://localhost:9897 (log: $CC_LOG)"

echo "## 2. Bring up the two subscriber containers in SECURE mode (share core net namespace, mTLS)"
docker compose -f "$COMPOSE" up -d --force-recreate
wait_container_started risk-scoring-subscriber "Started RiskScoring" || { echo "RiskScoring failed:"; docker logs risk-scoring-subscriber | tail -30; exit 1; }
wait_container_started supply-chain-trust-subscriber "Started SupplyChainTrust" || { echo "Trust failed:"; docker logs supply-chain-trust-subscriber | tail -30; exit 1; }
echo "   both subscribers SECURED and registered (HTTPS)."

echo "## 3. Create the per-hop intra-cloud authorization rules over HTTPS (sysop client cert)"
echo " - Hop 1 (consumer=riskscoring, provider=coldchainmonitor):"
make_rule riskscoring coldchainmonitor coldchain-reading
echo " - Hop 2 (consumer=supplychaintrust, provider=riskscoring):"
make_rule supplychaintrust riskscoring risk-scoring-notify

echo "## 4. Restart subscribers so subscribe() runs WITH the rules present"
docker restart risk-scoring-subscriber supply-chain-trust-subscriber >/dev/null
wait_container_started risk-scoring-subscriber "Started RiskScoring" || { echo "RiskScoring restart failed"; exit 1; }
wait_container_started supply-chain-trust-subscriber "Started SupplyChainTrust" || { echo "Trust restart failed"; exit 1; }
sleep 2

echo "## 5. Verify both subscriptions are authorized"
for pair in "riskscoring" "supplychaintrust"; do
  A=0
  for _ in $(seq 1 12); do A=$(authorized_for "$pair"); [ "$A" = "1" ] && break; sleep 2; done
  [ "$A" = "1" ] && echo "   $pair: authorized=1" || { echo "   $pair: authorized!=1 - delivery would be dropped"; exit 1; }
done

echo "## 6. End-to-end test A: HIGH temperature over mTLS -> expect SUSPECT"
TRUST_BEFORE=$(docker logs supply-chain-trust-subscriber 2>&1 | grep -c "TRUST verdict" || true)
post_reading "SECURE-HIGH-1" "$(date +%s%3N)" 12.5 70.0
sleep 5

echo "## 7. End-to-end test B: LOW temperature over mTLS -> expect DE_INCREDERE"
post_reading "SECURE-LOW-1" "$(date +%s%3N)" 4.0 50.0
sleep 5

echo
echo "## 8. Per-hop delivery + verdict evidence (mTLS)"
echo " -- ColdChain (host, HTTPS) published:"
grep -E "Received reading|Successfully published event 'coldchain.reading.created'" "$CC_LOG" | tail -4
echo " -- RiskScoring (hop 2) received + republished:"
docker logs risk-scoring-subscriber 2>&1 | grep -E "Received event|Computed risk|Successfully published event 'risk.assessed'" | tail -6
echo " -- SupplyChainTrust (hop 3) verdict lines:"
docker logs supply-chain-trust-subscriber 2>&1 | grep -E "TRUST verdict" | tail -4

TRUST_AFTER=$(docker logs supply-chain-trust-subscriber 2>&1 | grep -c "TRUST verdict" || true)
echo
echo "   TRUST verdict lines: before=$TRUST_BEFORE after=$TRUST_AFTER"
if [ "$TRUST_AFTER" -ge $((TRUST_BEFORE + 2)) ]; then
  echo "   SUCCESS: an event propagated through all 3 hops over STRICT mTLS, both branches."
else
  echo "   FAIL: expected 2 new TRUST verdicts (high + low). Check the per-hop logs above."
  exit 1
fi
