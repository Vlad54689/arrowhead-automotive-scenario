#!/bin/bash -e
#
# gen_food_certs.sh - generate coherent system certificates for the 3 food-chain services,
# signed with the testcloud1 chain so they are trusted by the Arrowhead core.
#
# Same logic as scripts/mtls/gen_service_certs.sh (the automotive cert generator), but:
#   * for the 3 food systems (coldchainmonitor, riskscoring, supplychaintrust);
#   * writing into the MAIN repo next to the real code (demo-car-with-events/demo-food-chain/<svc>/
#     src/main/resources/certificates), NOT the submodule.
#
# Chain produced per keystore (length 3):
#   <system>.testcloud1.aitia.arrowhead.eu -> testcloud1.aitia.arrowhead.eu -> arrowhead.eu
# SAN: DNSName=localhost, IPAddress=127.0.0.1   (so 127.0.0.1 mTLS stays STRICT, hostname-verified)
# Format: PKCS12, password 123456, alias = system name.
#
# Run from anywhere: bash scripts/food/gen_food_certs.sh

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

# food-chain services live in the MAIN repo (lesson from automotive: real code is here, not submodule)
SERVICES_DIR="${ROOT}/demo-car-with-events/demo-food-chain"

# service_dir : system_name : keystore_filename
SERVICES=(
  "demo-cold-chain-monitor-service:coldchainmonitor:coldchainmonitor.p12"
  "demo-risk-scoring-service:riskscoring:riskscoring.p12"
  "demo-supply-chain-trust-service:supplychaintrust:supplychaintrust.p12"
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
