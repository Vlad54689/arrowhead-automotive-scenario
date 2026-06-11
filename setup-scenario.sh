#!/usr/bin/env bash
#
# setup-scenario.sh - Reproducible startup for the Automotive pub/sub scenario.
#
# Brings the whole chain up from a FRESH database to a working publish/subscribe link:
#
#     POST /quality-inspections  ->  Quality Inspection (publisher)
#                                ->  Event Handler
#                                ->  Maintenance Recommendation (subscriber, /notify)
#
# It performs, in order:
#   1. Clean recreate of the Arrowhead core on a fresh MySQL volume.
#   2. Start Quality Inspection (registers `qualityinspection` + the `inspection` service).
#   3. Start Maintenance (registers `maintenancerecommendation`, advertised + bound at the Docker
#      bridge gateway IP so the Event Handler - running inside the container - can reach its /notify
#      endpoint; subscribes to quality.inspection.created).
#   4. Create the intra-cloud authorization rule (consumer=subscriber, provider=publisher) via the
#      Authorization management REST API, looking up the CURRENT system / service / interface ids
#      from the fresh DB (ids change on every fresh DB).
#   5. Restart Maintenance so subscribe() runs again WITH the rule present. This matters because the
#      Event Handler writes subscription_publisher_connection from the authorization rules that exist
#      AT SUBSCRIBE TIME - so the rule must exist before the (re)subscription for delivery to be
#      authorized.
#
# Result: exactly one `maintenancerecommendation` system, one authorization rule, and a
# subscription_publisher_connection with authorized=1.
#
# Requires: docker, docker compose, curl, ss, and the service jars built
# (run `mvn clean install` in demo-car-with-events/ first).

set -euo pipefail

# ----------------------------------------------------------------------------- configuration
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$REPO_ROOT/docker-compose-arrowhead-core.yml"
MYSQL_PW="arrowhead_secure_password"

QI_DIR="$REPO_ROOT/demo-car-with-events/demo-quality-inspection-service"
MR_DIR="$REPO_ROOT/demo-car-with-events/demo-maintenance-recommendation-service"
QI_JAR="$QI_DIR/target/demo-quality-inspection-service-4.4.0.2.jar"
MR_JAR="$MR_DIR/target/demo-maintenance-recommendation-service-4.4.0.2.jar"
QI_LOG="/tmp/quality-inspection.log"
MR_LOG="/tmp/maintenance.log"

SR_ECHO="http://localhost:8443/serviceregistry/echo"
AUTH_ECHO="http://localhost:8445/authorization/echo"
EH_ECHO="http://localhost:8455/eventhandler/echo"
AUTH_MGMT="http://localhost:8445/authorization/mgmt/intracloud"

QI_NAME="qualityinspection"
MR_NAME="maintenancerecommendation"
SERVICE_DEF="inspection"
INTERFACE="HTTP-INSECURE-JSON"

# ----------------------------------------------------------------------------- helpers
mysql_q() { docker exec arrowhead_core_mysql mysql -uroot -p"$MYSQL_PW" arrowhead -N -B -e "$1" 2>/dev/null; }
mysql_t() { docker exec arrowhead_core_mysql mysql -uroot -p"$MYSQL_PW" arrowhead -t  -e "$1" 2>/dev/null; }

wait_for_echo() {  # $1 = label, $2 = echo url
  for _ in $(seq 1 60); do
    [[ "$(curl -s -m 3 "$2" 2>/dev/null)" == "Got it!" ]] && { echo "   $1 reachable."; return 0; }
    sleep 3
  done
  echo "   $1 did NOT become reachable in time."; return 1
}

wait_for_started() {  # $1 = log file
  for _ in $(seq 1 40); do
    grep -q "Started .*Application in" "$1" 2>/dev/null && return 0
    sleep 3
  done
  return 1
}

wait_port_free() {    # $1 = port
  for _ in $(seq 1 25); do
    ss -ltn 2>/dev/null | grep -q ":$1 " || return 0
    sleep 1
  done
  return 0
}

start_jar() {         # $1 = dir, $2 = jar, $3 = log, $4.. = extra args
  local dir="$1" jar="$2" log="$3"; shift 3
  ( cd "$dir/target" && nohup java -jar "$jar" "$@" >"$log" 2>&1 & )
}

# ----------------------------------------------------------------------------- 0. pre-flight
[[ -f "$QI_JAR" ]] || { echo "Missing $QI_JAR - run 'mvn clean install' in demo-car-with-events/ first."; exit 1; }
[[ -f "$MR_JAR" ]] || { echo "Missing $MR_JAR - run 'mvn clean install' in demo-car-with-events/ first."; exit 1; }

echo "## 0. Stop any running scenario services"
pkill -f "demo-quality-inspection-service-4.4.0.2.jar" 2>/dev/null || true
pkill -f "demo-maintenance-recommendation-service-4.4.0.2.jar" 2>/dev/null || true
pkill -f "QualityInspectionServiceApplication" 2>/dev/null || true
pkill -f "MaintenanceRecommendationServiceApplication" 2>/dev/null || true
sleep 3

# ----------------------------------------------------------------------------- 1. clean core
echo "## 1. Clean recreate of the Arrowhead core (fresh DB volume)"
docker compose -f "$COMPOSE_FILE" down
docker volume rm arrowhead_core_mysql >/dev/null 2>&1 || true
docker volume create arrowhead_core_mysql >/dev/null
docker compose -f "$COMPOSE_FILE" up -d

echo "## 1b. Wait for the core systems to finish initialising."
echo "##     They come up ASYNCHRONOUSLY: the Service Registry answers first, but the Event Handler"
echo "##     and Authorization take ~30-40s more. Starting the app services (which resolve the Event"
echo "##     Handler) or creating the auth rule (which needs Authorization) too early silently fails."
wait_for_echo "Service Registry" "$SR_ECHO"   || exit 1
wait_for_echo "Authorization"    "$AUTH_ECHO" || exit 1
wait_for_echo "Event Handler"    "$EH_ECHO"   || exit 1

echo "## 1c. Resolve the Docker bridge gateway dynamically (host as seen from the container)"
NETWORK="$(docker inspect arrowhead_core --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')"
GATEWAY="$(docker network inspect "$NETWORK" --format '{{range .IPAM.Config}}{{.Gateway}}{{end}}')"
[[ -n "$GATEWAY" ]] || { echo "Could not resolve gateway for network $NETWORK"; exit 1; }
echo "   network=$NETWORK  gateway=$GATEWAY"

# ----------------------------------------------------------------------------- 2. services + auth
echo "## 2a. Start Quality Inspection (publisher) - default 127.0.0.1:9895"
start_jar "$QI_DIR" "$QI_JAR" "$QI_LOG"
wait_for_started "$QI_LOG" || { echo "Quality Inspection failed to start - see $QI_LOG"; exit 1; }
echo "   registered system:"; mysql_t "SELECT id, system_name, address, port FROM system_ WHERE system_name='$QI_NAME';"

echo "## 2b. Start Maintenance (subscriber) - bound + advertised at $GATEWAY:9896"
start_jar "$MR_DIR" "$MR_JAR" "$MR_LOG" "--server.address=$GATEWAY"
wait_for_started "$MR_LOG" || { echo "Maintenance failed to start - see $MR_LOG"; exit 1; }
echo "   registered system (subscription connection still empty - rule not created yet):"
mysql_t "SELECT id, system_name, address, port FROM system_ WHERE system_name='$MR_NAME';"

echo "## 2c. Create the intra-cloud authorization rule (consumer=subscriber, provider=publisher)"
CONSUMER_ID="$(mysql_q "SELECT id FROM system_ WHERE system_name='$MR_NAME' ORDER BY id DESC LIMIT 1;")"
PROVIDER_ID="$(mysql_q "SELECT id FROM system_ WHERE system_name='$QI_NAME' ORDER BY id DESC LIMIT 1;")"
SVCDEF_ID="$(mysql_q  "SELECT id FROM service_definition WHERE service_definition='$SERVICE_DEF';")"
IFACE_ID="$(mysql_q   "SELECT id FROM service_interface WHERE interface_name='$INTERFACE';")"
echo "   consumer=$CONSUMER_ID provider=$PROVIDER_ID serviceDefinition=$SVCDEF_ID interface=$IFACE_ID"
curl -s -m 10 -X POST "$AUTH_MGMT" -H "Content-Type: application/json" \
  -d "{\"consumerId\":$CONSUMER_ID,\"providerIds\":[$PROVIDER_ID],\"serviceDefinitionIds\":[$SVCDEF_ID],\"interfaceIds\":[$IFACE_ID]}" \
  >/dev/null && echo "   authorization rule created."

echo "## 2d. Restart Maintenance so subscribe() runs WITH the rule present"
pkill -f "demo-maintenance-recommendation-service-4.4.0.2.jar" 2>/dev/null || true
wait_port_free 9896
start_jar "$MR_DIR" "$MR_JAR" "$MR_LOG" "--server.address=$GATEWAY"
wait_for_started "$MR_LOG" || { echo "Maintenance failed to restart - see $MR_LOG"; exit 1; }
sleep 3

# ----------------------------------------------------------------------------- done
echo "## Final linkage (authorized=1 => Event Handler will deliver to the subscriber):"
mysql_t "SELECT spc.id, spc.subscription_id, pub.system_name AS publisher, spc.authorized
         FROM subscription_publisher_connection spc JOIN system_ pub ON spc.system_id=pub.id;"
echo
echo "Scenario ready. Test with:"
echo "  curl -X POST http://localhost:9895/quality-inspections -H 'Content-Type: application/json' \\"
echo "    -d '{\"carId\":1,\"brand\":\"Volvo\",\"color\":\"Blue\",\"defectDetected\":true,\"defectType\":\"DENT\",\"inspectionResult\":\"DEFECT_DETECTED\",\"inspectionTimestamp\":0}'"
echo "Maintenance log: $MR_LOG   Quality Inspection log: $QI_LOG"
