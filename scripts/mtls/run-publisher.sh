#!/usr/bin/env bash
#
# run-publisher.sh <secure|insecure> - launch the Quality Inspection publisher on the HOST,
# in the requested security mode, so the publisher start is identical and version-controlled
# across both experiment modes.
#
# The ONLY difference between modes is security: secure adds the ssl/keystore/truststore args
# (HTTPS + mTLS, qualityinspection cert), insecure runs plain HTTP. Same jar, host, ports,
# Service Registry target (127.0.0.1:8443) in both.
#
# Runs from a neutral working dir so the jar's bundled config is used (not any target/
# application.properties override), and detaches the JVM (survives this shell). Log: /tmp/qi-<mode>.log

set -euo pipefail

MODE="${1:-}"
[ "$MODE" = secure ] || [ "$MODE" = insecure ] || { echo "usage: $0 <secure|insecure>"; exit 1; }

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
QI_JAR="$REPO_ROOT/demo-car-with-events/demo-quality-inspection-service/target/demo-quality-inspection-service-4.4.0.2.jar"
CERTDIR="$REPO_ROOT/sos-examples-spring/demo-car-with-events/demo-quality-inspection-service/src/main/resources/certificates"
LOG="/tmp/qi-${MODE}.log"
[ -f "$QI_JAR" ] || { echo "missing $QI_JAR - build the demo services first"; exit 1; }

# stop any publisher already on 9895 (matches the jar, not this script)
pkill -f "demo-quality-inspection-service-4.4.0.2.jar" 2>/dev/null || true
for _ in $(seq 1 15); do ss -ltn 2>/dev/null | grep -q ":9895 " || break; sleep 1; done

ARGS=( --token.security.filter.enabled=false
       --server.address=127.0.0.1 --server.port=9895
       --sr_address=127.0.0.1 --sr_port=8443 )
if [ "$MODE" = secure ]; then
  ARGS+=( --server.ssl.enabled=true
          --server.ssl.key-store-type=PKCS12
          --server.ssl.key-store=file:$CERTDIR/qualityinspection.p12
          --server.ssl.key-store-password=123456
          --server.ssl.key-alias=qualityinspection
          --server.ssl.key-password=123456
          --server.ssl.client-auth=need
          --server.ssl.trust-store-type=PKCS12
          --server.ssl.trust-store=file:$CERTDIR/truststore.p12
          --server.ssl.trust-store-password=123456 )
else
  ARGS+=( --server.ssl.enabled=false )
fi

# clear the (reused) log path FIRST, so the readiness check below can only match THIS
# run's "Started" line and never a stale one from a previous launch, then launch detached
: > "$LOG"
( cd /tmp && setsid nohup java -jar "$QI_JAR" "${ARGS[@]}" >> "$LOG" 2>&1 & )

for _ in $(seq 1 30); do
  grep -qE "Started QualityInspection" "$LOG" 2>/dev/null && break
  grep -qE "APPLICATION FAILED|Error starting ApplicationContext" "$LOG" 2>/dev/null && { echo "publisher FAILED to start:"; tail -20 "$LOG"; exit 1; }
  sleep 1
done
grep -qE "Started QualityInspection" "$LOG" || { echo "publisher not ready in time (see $LOG)"; exit 1; }

SCHEME=http; [ "$MODE" = secure ] && SCHEME=https
echo "Quality Inspection publisher up in ${MODE} mode on ${SCHEME}://localhost:9895  (log: $LOG)"
grep -E "Security mode|Started Quality" "$LOG" | tail -2
