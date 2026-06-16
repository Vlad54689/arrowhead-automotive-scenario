#!/usr/bin/env bash
#
# run-secure-subscriber.sh - Phase 3 of the mTLS experiment.
#
# Brings up the Maintenance subscriber as a container that SHARES the core container's
# network namespace (see docker-compose-secure-subscriber.yml), wires the intra-cloud
# authorization rule over HTTPS, resubscribes so the subscription is authorized, and
# verifies end-to-end secure event delivery (QI publish -> Event Handler -> MR /notify).
#
# Preconditions (already running):
#   * the Arrowhead core in SECURE mode  (scripts/mtls/setup-scenario-secure.sh)
#   * the Quality Inspection publisher on the host in SECURE mode on https://localhost:9895
#
# Everything stays on 127.0.0.1 (the core's loopback, shared via network_mode), which is
# in every cert's SAN, so TLS hostname verification is STRICT. Nothing here touches the
# core, the publisher, or the insecure baseline.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE="$REPO_ROOT/docker-compose-secure-subscriber.yml"
CERTS="$REPO_ROOT/core-java-spring/certificates/testcloud1"
PASSWORD="123456"
MR_NAME="maintenancerecommendation"
QI_NAME="qualityinspection"
SERVICE_DEF="inspection"
INTERFACE="HTTP-SECURE-JSON"

# sysop client material (PEM) for the HTTPS management call (-legacy: 2019 RC2-40 p12)
TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -clcerts -nokeys -out "$TMP/sysop.crt" 2>/dev/null
openssl pkcs12 -legacy -in "$CERTS/sysop.p12" -passin pass:"$PASSWORD" -nocerts -nodes  -out "$TMP/sysop.key" 2>/dev/null

mq() { docker exec arrowhead_core_mysql mysql -uroot -parrowhead_secure_password arrowhead -N -B -e "$1" 2>/dev/null; }

wait_started() {  # wait for the container app to finish booting
  for _ in $(seq 1 30); do
    docker logs --since 60s maintenance-subscriber 2>&1 | grep -qE "Started Maintenance" && return 0
    docker logs --since 60s maintenance-subscriber 2>&1 | grep -qE "APPLICATION FAILED|Error starting ApplicationContext" && return 1
    sleep 2
  done
  return 1
}

echo "## 1. Bring up the subscriber (shares core net namespace)"
# --force-recreate guarantees a fresh boot so the readiness wait (which scans recent
# logs) is reliable even when a container from a previous run is still up.
docker compose -f "$COMPOSE" up -d --force-recreate
wait_started || { echo "subscriber failed to start"; docker logs maintenance-subscriber | tail -30; exit 1; }
echo "   SECURED and registered."

echo "## 2. Create the intra-cloud authorization rule (consumer=MR, provider=QI) over HTTPS"
CONSUMER=$(mq "SELECT id FROM system_ WHERE system_name='$MR_NAME' ORDER BY id DESC LIMIT 1;")
PROVIDER=$(mq "SELECT id FROM system_ WHERE system_name='$QI_NAME' ORDER BY id DESC LIMIT 1;")
SVCDEF=$(mq   "SELECT id FROM service_definition WHERE service_definition='$SERVICE_DEF';")
IFACE=$(mq    "SELECT id FROM service_interface WHERE interface_name='$INTERFACE';")
echo "   consumer(MR)=$CONSUMER provider(QI)=$PROVIDER serviceDefinition=$SVCDEF interface=$IFACE"
curl -s -k -m10 --cert "$TMP/sysop.crt" --key "$TMP/sysop.key" \
  -X POST "https://localhost:8445/authorization/mgmt/intracloud" -H "Content-Type: application/json" \
  -d "{\"consumerId\":$CONSUMER,\"providerIds\":[$PROVIDER],\"serviceDefinitionIds\":[$SVCDEF],\"interfaceIds\":[$IFACE]}" \
  -o /dev/null -w "   auth rule POST -> HTTP %{http_code}\n"

echo "## 3. Restart subscriber so subscribe() runs WITH the rule present"
docker restart maintenance-subscriber >/dev/null
wait_started || { echo "subscriber failed to restart"; exit 1; }
sleep 2

echo "## 4. Verify the subscription is authorized (the EH writes this a few seconds after resubscribe)"
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

echo "## 5. End-to-end test: POST an inspection to QI over HTTPS, expect a delivered event"
BEFORE=$(docker logs maintenance-subscriber 2>&1 | grep -c "Received event" || true)
curl -s -k -m10 --cert "$TMP/sysop.crt" --key "$TMP/sysop.key" \
  -X POST "https://localhost:9895/quality-inspections" -H "Content-Type: application/json" \
  -d '{"carId":7,"brand":"Volvo","color":"Blue","defectDetected":true,"defectType":"DENT","inspectionResult":"DEFECT_DETECTED","inspectionTimestamp":0}' \
  -o /dev/null -w "   publish POST -> HTTP %{http_code}\n"
sleep 5
AFTER=$(docker logs maintenance-subscriber 2>&1 | grep -c "Received event" || true)
echo "   'Received event' lines: before=$BEFORE after=$AFTER"
if [ "$AFTER" -gt "$BEFORE" ]; then
  echo "   SUCCESS: secure event delivered end-to-end over mTLS."
  docker logs maintenance-subscriber 2>&1 | grep "Received event" | tail -1
else
  echo "   FAIL: no new event delivered - check 'docker logs maintenance-subscriber'."
  exit 1
fi
