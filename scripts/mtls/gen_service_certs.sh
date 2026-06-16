#!/bin/bash -e
#
# Phase 1 (mTLS experiment): generate coherent system certificates for the 4
# demo services, signed with the testcloud1 chain so they are trusted by the
# Arrowhead core.
#
# Replicates the official `create_system_keystore` logic from
# core-java-spring/scripts/certificate_generation/lib_certs.sh, but uses a short
# keystore alias equal to the Arrowhead system name (matching the structure of
# the existing core keystores, e.g. serviceregistry.p12).
#
# Chain produced per keystore (length 3):
#   <system>.testcloud1.aitia.arrowhead.eu -> testcloud1.aitia.arrowhead.eu -> arrowhead.eu
# SAN: DNSName=localhost, IPAddress=127.0.0.1
# Format: PKCS12, password 123456
#
# Run from the project root: bash scripts/mtls/gen_service_certs.sh

export PASSWORD="123456"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CERTS="${ROOT}/core-java-spring/certificates/testcloud1"

ROOT_KEYSTORE="${CERTS}/master.p12"
ROOT_ALIAS="arrowhead.eu"
ROOT_CRT="${CERTS}/master.crt"

CLOUD_KEYSTORE="${CERTS}/testcloud1.p12"
CLOUD_ALIAS="testcloud1.aitia.arrowhead.eu"
CLOUD_CRT="${CERTS}/testcloud1.crt"

SAN="dns:localhost,ip:127.0.0.1"

SERVICES_DIR="${ROOT}/sos-examples-spring/demo-car-with-events"

# service_dir : system_name : keystore_filename
SERVICES=(
  "demo-quality-inspection-service:qualityinspection:qualityinspection.p12"
  "demo-maintenance-recommendation-service:maintenancerecommendation:maintenancerecommendation.p12"
  "demo-traceability-log-service:traceabilitylog:traceabilitylog.p12"
  "demo-car-service:carservice:carservice.p12"
)

gen_system_keystore() {
  local SYSTEM_KEYSTORE=$1 ALIAS=$2 CN=$3

  rm -f "${SYSTEM_KEYSTORE}"
  mkdir -p "$(dirname "${SYSTEM_KEYSTORE}")"

  # 1. system key pair with SAN
  keytool -genkeypair -v \
    -keystore "${SYSTEM_KEYSTORE}" -storepass:env PASSWORD \
    -keyalg RSA -keysize 2048 -validity 3650 \
    -alias "${ALIAS}" -keypass:env PASSWORD \
    -dname "CN=${CN}" \
    -ext "SubjectAlternativeName=${SAN}"

  # 2. import root (trust anchor)
  keytool -importcert -v \
    -keystore "${SYSTEM_KEYSTORE}" -storepass:env PASSWORD \
    -alias "${ROOT_ALIAS}" -file "${ROOT_CRT}" \
    -trustcacerts -noprompt

  # 3. import cloud (intermediate CA)
  keytool -importcert -v \
    -keystore "${SYSTEM_KEYSTORE}" -storepass:env PASSWORD \
    -alias "${CLOUD_ALIAS}" -file "${CLOUD_CRT}" \
    -trustcacerts -noprompt

  # 4. CSR -> sign with cloud key -> import full chain back under system alias
  keytool -certreq -v \
    -keystore "${SYSTEM_KEYSTORE}" -storepass:env PASSWORD \
    -alias "${ALIAS}" -keypass:env PASSWORD |
  keytool -gencert -v \
    -keystore "${CLOUD_KEYSTORE}" -storepass:env PASSWORD \
    -validity 3650 \
    -alias "${CLOUD_ALIAS}" -keypass:env PASSWORD \
    -ext "SubjectAlternativeName=${SAN}" -rfc |
  keytool -importcert \
    -keystore "${SYSTEM_KEYSTORE}" -storepass:env PASSWORD \
    -alias "${ALIAS}" -keypass:env PASSWORD \
    -trustcacerts -noprompt
}

gen_truststore() {
  local TRUSTSTORE=$1
  rm -f "${TRUSTSTORE}"
  # trust the local cloud CA (matches core's testcloud1/truststore.p12) ...
  keytool -importcert -v \
    -keystore "${TRUSTSTORE}" -storepass:env PASSWORD \
    -alias "${CLOUD_ALIAS}" -file "${CLOUD_CRT}" \
    -trustcacerts -noprompt
  # ... and the root, so the whole chain validates
  keytool -importcert -v \
    -keystore "${TRUSTSTORE}" -storepass:env PASSWORD \
    -alias "${ROOT_ALIAS}" -file "${ROOT_CRT}" \
    -trustcacerts -noprompt
}

for entry in "${SERVICES[@]}"; do
  IFS=":" read -r SVC_DIR SYS_NAME KS_FILE <<<"${entry}"
  DEST="${SERVICES_DIR}/${SVC_DIR}/src/main/resources/certificates"
  mkdir -p "${DEST}"

  # back up any existing keystores/truststore (never delete) -> .p12.bak
  for f in "${DEST}"/*.p12; do
    [ -e "${f}" ] || continue
    case "${f}" in
      *.bak) continue ;;
    esac
    mv -f "${f}" "${f}.bak"
    echo "backed up: ${f} -> ${f}.bak"
  done

  echo "=== generating ${SYS_NAME} -> ${DEST}/${KS_FILE} ==="
  gen_system_keystore "${DEST}/${KS_FILE}" "${SYS_NAME}" \
    "${SYS_NAME}.testcloud1.aitia.arrowhead.eu"
  gen_truststore "${DEST}/truststore.p12"
done

echo "ALL DONE"
