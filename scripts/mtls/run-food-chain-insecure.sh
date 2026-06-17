#!/usr/bin/env bash
#
# run-food-chain-insecure.sh - bring up and end-to-end verify the 3-hop food cold-chain in
# INSECURE mode (HTTP), on the existing containerized Arrowhead topology.
#
# Chain (purely event-driven):
#   ColdChainMonitor (host, 9897)  --coldchain.reading.created-->
#   RiskScoring      (container, 9898, sub+pub)  --risk.assessed-->
#   SupplyChainTrust (container, 9899, sub)  logs verdict + end-to-end latency
#
# Mirrors scripts/mtls/run-insecure-subscriber.sh, generalized to two hops: start publisher(s),
# bring up subscribers, wire ONE intra-cloud authorization rule per hop (consumer=downstream,
# provider=upstream), restart subscribers so subscribe() runs WITH the rules present, verify
# authorized=1 on both subscriptions, then drive the chain with a high-temp and a low-temp reading.
#
# Precondition: the Arrowhead core in INSECURE mode is up
#   (scripts/mtls/setup-scenario-insecure-core.sh).

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="$REPO_ROOT/docker-compose-food-chain-insecure.yml"

CC_JAR="$REPO_ROOT/demo-car-with-events/demo-food-chain/demo-cold-chain-monitor-service/target/demo-cold-chain-monitor-service-4.4.0.2.jar"
CC_LOG="/tmp/coldchain-insecure.log"

IFACE="HTTP-INSECURE-JSON"

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

# Create an intra-cloud authorization rule consumer -> provider (service def & interface are
# irrelevant for EVENT authorization - the Event Handler matches only the consumer/provider system
# pair - but the mgmt API requires valid ids, so we pass the provider's own registered service).
make_rule() {  # $1=consumerName $2=providerName $3=providerServiceDef
  local consumer provider svcdef iface
  consumer=$(mq "SELECT id FROM system_ WHERE system_name='$1' ORDER BY id DESC LIMIT 1;")
  provider=$(mq "SELECT id FROM system_ WHERE system_name='$2' ORDER BY id DESC LIMIT 1;")
  svcdef=$(mq   "SELECT id FROM service_definition WHERE service_definition='$3';")
  iface=$(mq    "SELECT id FROM service_interface WHERE interface_name='$IFACE';")
  echo "   rule consumer($1)=$consumer provider($2)=$provider serviceDef($3)=$svcdef interface=$iface"
  curl -s -m10 -X POST "http://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
    -d "{\"consumerId\":$consumer,\"providerIds\":[$provider],\"serviceDefinitionIds\":[$svcdef],\"interfaceIds\":[$iface]}" \
    -o /dev/null -w "   auth rule POST -> HTTP %{http_code}\n"
}

authorized_for() {  # $1=subscriberName -> echoes latest authorized flag
  mq "SELECT spc.authorized FROM subscription_publisher_connection spc
      JOIN subscription sub ON spc.subscription_id=sub.id
      JOIN system_ s ON sub.system_id=s.id
      WHERE s.system_name='$1' ORDER BY spc.id DESC LIMIT 1;"
}

wait_container_started() {  # $1=container $2=grep-pattern
  for _ in $(seq 1 40); do
    docker logs --since 120s "$1" 2>&1 | grep -qE "$2" && return 0
    docker logs --since 120s "$1" 2>&1 | grep -qE "APPLICATION FAILED|Error starting ApplicationContext" && return 1
    sleep 2
  done
  return 1
}

echo "## 0. Preconditions"
[ -f "$CC_JAR" ] || { echo "missing $CC_JAR - build with: mvn -pl demo-food-chain -amd install -DskipTests"; exit 1; }
[ "$(curl -s -m3 http://localhost:8443/serviceregistry/echo 2>/dev/null)" = "Got it!" ] \
  || { echo "Arrowhead core (insecure) not reachable on 127.0.0.1:8443 - run setup-scenario-insecure-core.sh first"; exit 1; }
echo "   core reachable, ColdChain jar present."

echo "## 1. Start the ColdChain publisher on the HOST (127.0.0.1:9897)"
pkill -f "demo-cold-chain-monitor-service-4.4.0.2.jar" 2>/dev/null || true
for _ in $(seq 1 15); do ss -ltn 2>/dev/null | grep -q ":9897 " || break; sleep 1; done
: > "$CC_LOG"
( cd /tmp && setsid nohup java -jar "$CC_JAR" \
    --server.ssl.enabled=false --token.security.filter.enabled=false \
    --server.address=127.0.0.1 --server.port=9897 \
    --sr_address=127.0.0.1 --sr_port=8443 >> "$CC_LOG" 2>&1 & )
for _ in $(seq 1 40); do
  grep -qE "Started ColdChainMonitor" "$CC_LOG" 2>/dev/null && break
  grep -qE "APPLICATION FAILED|Error starting ApplicationContext" "$CC_LOG" 2>/dev/null && { echo "ColdChain FAILED:"; tail -20 "$CC_LOG"; exit 1; }
  sleep 1
done
grep -qE "Started ColdChainMonitor" "$CC_LOG" || { echo "ColdChain not ready (see $CC_LOG)"; exit 1; }
echo "   ColdChain up on http://localhost:9897 (log: $CC_LOG)"

echo "## 2. Bring up the two subscriber containers (share core net namespace, HTTP)"
docker compose -f "$COMPOSE" up -d --force-recreate
wait_container_started risk-scoring-subscriber "Started RiskScoring" || { echo "RiskScoring failed:"; docker logs risk-scoring-subscriber | tail -30; exit 1; }
wait_container_started supply-chain-trust-subscriber "Started SupplyChainTrust" || { echo "Trust failed:"; docker logs supply-chain-trust-subscriber | tail -30; exit 1; }
echo "   both subscribers started and registered (insecure)."

echo "## 3. Create the per-hop intra-cloud authorization rules over HTTP"
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

echo "## 6. End-to-end test A: HIGH temperature -> expect SUSPECT"
TRUST_BEFORE=$(docker logs supply-chain-trust-subscriber 2>&1 | grep -c "TRUST verdict" || true)
TS_A=$(date +%s%3N)
curl -s -m10 -X POST "http://localhost:9897/cold-chain/readings" -H "Content-Type: application/json" \
  -d "{\"batchId\":\"BATCH-HIGH-1\",\"originTs\":$TS_A,\"temperatura\":12.5,\"umiditate\":70.0,\"locatie\":\"Depozit-A\",\"lot\":\"LOT-001\"}" \
  -o /dev/null -w "   POST high-temp -> HTTP %{http_code}\n"
sleep 5

echo "## 7. End-to-end test B: LOW temperature -> expect DE_INCREDERE"
TS_B=$(date +%s%3N)
curl -s -m10 -X POST "http://localhost:9897/cold-chain/readings" -H "Content-Type: application/json" \
  -d "{\"batchId\":\"BATCH-LOW-1\",\"originTs\":$TS_B,\"temperatura\":4.0,\"umiditate\":50.0,\"locatie\":\"Depozit-B\",\"lot\":\"LOT-002\"}" \
  -o /dev/null -w "   POST low-temp -> HTTP %{http_code}\n"
sleep 5

echo
echo "## 8. Per-hop delivery + verdict evidence"
echo " -- ColdChain (host) published:"
grep -E "Received reading|Successfully published event 'coldchain.reading.created'" "$CC_LOG" | tail -4
echo " -- RiskScoring (hop 2) received + republished:"
docker logs risk-scoring-subscriber 2>&1 | grep -E "Received event|Computed risk|Successfully published event 'risk.assessed'" | tail -6
echo " -- SupplyChainTrust (hop 3) verdict lines:"
docker logs supply-chain-trust-subscriber 2>&1 | grep -E "TRUST verdict" | tail -4

TRUST_AFTER=$(docker logs supply-chain-trust-subscriber 2>&1 | grep -c "TRUST verdict" || true)
echo
echo "   TRUST verdict lines: before=$TRUST_BEFORE after=$TRUST_AFTER"
if [ "$TRUST_AFTER" -ge $((TRUST_BEFORE + 2)) ]; then
  echo "   SUCCESS: an event propagated through all 3 hops for both branches."
else
  echo "   FAIL: expected 2 new TRUST verdicts (high + low). Check the per-hop logs above."
  exit 1
fi
