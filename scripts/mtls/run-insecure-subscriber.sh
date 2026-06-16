#!/usr/bin/env bash
#
# run-insecure-subscriber.sh - INSECURE counterpart of run-secure-subscriber.sh.
#
# Brings up the Maintenance subscriber as a container sharing the core's network namespace
# (docker-compose-insecure-subscriber.yml), wires the intra-cloud authorization rule over
# plain HTTP, resubscribes so the subscription is authorized, and verifies end-to-end HTTP
# event delivery (QI publish -> Event Handler -> MR /notify).
#
# The ONLY differences from the secure flow are security: HTTP instead of HTTPS, no client
# certificate, and the HTTP-INSECURE-JSON service interface. Same topology, ports and flow.
#
# Preconditions (already running):
#   * the Arrowhead core in INSECURE mode  (scripts/mtls/setup-scenario-insecure-core.sh)
#   * the Quality Inspection publisher on the host in INSECURE mode on http://localhost:9895

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="$REPO_ROOT/docker-compose-insecure-subscriber.yml"
MR_NAME="maintenancerecommendation"
QI_NAME="qualityinspection"
SERVICE_DEF="inspection"
INTERFACE="HTTP-INSECURE-JSON"

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

wait_started() {
  for _ in $(seq 1 30); do
    docker logs --since 60s maintenance-subscriber 2>&1 | grep -qE "Started Maintenance" && return 0
    docker logs --since 60s maintenance-subscriber 2>&1 | grep -qE "APPLICATION FAILED|Error starting ApplicationContext" && return 1
    sleep 2
  done
  return 1
}

echo "## 1. Bring up the subscriber (shares core net namespace, HTTP)"
docker compose -f "$COMPOSE" up -d --force-recreate
wait_started || { echo "subscriber failed to start"; docker logs maintenance-subscriber | tail -30; exit 1; }
echo "   started and registered (insecure)."

echo "## 2. Create the intra-cloud authorization rule (consumer=MR, provider=QI) over HTTP"
CONSUMER=$(mq "SELECT id FROM system_ WHERE system_name='$MR_NAME' ORDER BY id DESC LIMIT 1;")
PROVIDER=$(mq "SELECT id FROM system_ WHERE system_name='$QI_NAME' ORDER BY id DESC LIMIT 1;")
SVCDEF=$(mq   "SELECT id FROM service_definition WHERE service_definition='$SERVICE_DEF';")
IFACE=$(mq    "SELECT id FROM service_interface WHERE interface_name='$INTERFACE';")
echo "   consumer(MR)=$CONSUMER provider(QI)=$PROVIDER serviceDefinition=$SVCDEF interface=$IFACE"
curl -s -m10 \
  -X POST "http://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
  -d "{\"consumerId\":$CONSUMER,\"providerIds\":[$PROVIDER],\"serviceDefinitionIds\":[$SVCDEF],\"interfaceIds\":[$IFACE]}" \
  -o /dev/null -w "   auth rule POST -> HTTP %{http_code}\n"

echo "## 3. Restart subscriber so subscribe() runs WITH the rule present"
docker restart maintenance-subscriber >/dev/null
wait_started || { echo "subscriber failed to restart"; exit 1; }
sleep 2

echo "## 4. Verify the subscription is authorized"
AUTH=0
for _ in $(seq 1 12); do
  AUTH=$(mq "SELECT spc.authorized FROM subscription_publisher_connection spc
             JOIN subscription sub ON spc.subscription_id=sub.id
             JOIN system_ s ON sub.system_id=s.id
             WHERE s.system_name='$MR_NAME' ORDER BY spc.id DESC LIMIT 1;")
  [ "$AUTH" = "1" ] && break
  sleep 2
done
[ "$AUTH" = "1" ] \
  && echo "   authorized=1 (Event Handler will deliver to the subscriber)" \
  || { echo "   authorized!=1 - delivery would be dropped"; exit 1; }

echo "## 5. End-to-end test: POST an inspection to QI over HTTP, expect a delivered event"
BEFORE=$(docker logs maintenance-subscriber 2>&1 | grep -c "Received event" || true)
curl -s -m10 \
  -X POST "http://localhost:9895/quality-inspections" -H "Content-Type: application/json" \
  -d '{"carId":7,"brand":"Volvo","color":"Blue","defectDetected":true,"defectType":"DENT","inspectionResult":"DEFECT_DETECTED","inspectionTimestamp":0}' \
  -o /dev/null -w "   publish POST -> HTTP %{http_code}\n"
sleep 5
AFTER=$(docker logs maintenance-subscriber 2>&1 | grep -c "Received event" || true)
echo "   'Received event' lines: before=$BEFORE after=$AFTER"
if [ "$AFTER" -gt "$BEFORE" ]; then
  echo "   SUCCESS: insecure event delivered end-to-end over HTTP."
  docker logs maintenance-subscriber 2>&1 | grep "Received event" | tail -1
else
  echo "   FAIL: no new event delivered - check 'docker logs maintenance-subscriber'."
  exit 1
fi
