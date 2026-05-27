#!/bin/bash -e
#
# Copyright (c) 2024, 2026 Oracle and/or its affiliates.
#

DAYS=${DAYS:-9999}
CA_DAYS=${CA_DAYS:-$DAYS}
CERT_DAYS=${CERT_DAYS:-$DAYS}
CERT_MINUTES=${CERT_MINUTES:-}
CA_NOT_BEFORE=${CA_NOT_BEFORE:-}
CA_NOT_AFTER=${CA_NOT_AFTER:-}
CERT_NOT_BEFORE=${CERT_NOT_BEFORE:-}
CERT_NOT_AFTER=${CERT_NOT_AFTER:-}
RECREATE_CA=false
PKI_JSON_MASK='{"key": $key, "cert": $cert, "intermediates": [$ca]}'
CA_EXT=x509-ca.ext
CERT_EXT=x509.ext

usage() {
  cat <<EOT
Usage: $(basename "$0") [OPTIONS]

Generate test CA material, server-pki.json, and client-pki.json.

Options:
  --days DAYS              Set both CA and leaf certificate validity days.
  --ca-days DAYS           Set CA certificate validity days.
  --cert-days DAYS         Set server/client certificate validity days.
  --cert-minutes MINUTES   Set server/client validity from now UTC for MINUTES.
  --cert-valid-minutes MINUTES
                            Alias for --cert-minutes.
  --ca-not-before TIME     Set CA notBefore as [CC]YYMMDDHHMMSSZ.
  --ca-not-after TIME      Set CA notAfter as [CC]YYMMDDHHMMSSZ.
  --cert-not-before TIME   Set server/client notBefore as [CC]YYMMDDHHMMSSZ.
  --cert-not-after TIME    Set server/client notAfter as [CC]YYMMDDHHMMSSZ.
  -h, --help               Show this help.

Default validity is $DAYS days.
EOT
}

fail() {
  echo "$1" >&2
  echo "Run $(basename "$0") --help for usage." >&2
  exit 1
}

requireValue() {
  if [ $# -lt 2 ] || [ -z "$2" ] || [[ "$2" == --* ]]; then
    fail "$1 requires a value"
  fi
}

validateDays() {
  if [[ ! "$2" =~ ^[1-9][0-9]*$ ]]; then
    fail "$1 must be a positive integer"
  fi
}

validateMinutes() {
  if [[ ! "$2" =~ ^[1-9][0-9]*$ ]]; then
    fail "$1 must be a positive integer"
  fi
}

validateAsn1Time() {
  if [[ ! "$2" =~ ^([0-9]{12}|[0-9]{14})Z$ ]]; then
    fail "$1 must use [CC]YYMMDDHHMMSSZ, for example 20260515120000Z"
  fi
}

while [ $# -gt 0 ]; do
  case "$1" in
    --days)
      requireValue "$1" "$2"
      validateDays "$1" "$2"
      CA_DAYS=$2
      CERT_DAYS=$2
      RECREATE_CA=true
      shift 2
      ;;
    --ca-days)
      requireValue "$1" "$2"
      validateDays "$1" "$2"
      CA_DAYS=$2
      RECREATE_CA=true
      shift 2
      ;;
    --cert-days)
      requireValue "$1" "$2"
      validateDays "$1" "$2"
      CERT_DAYS=$2
      shift 2
      ;;
    --cert-minutes|--cert-valid-minutes)
      requireValue "$1" "$2"
      validateMinutes "$1" "$2"
      CERT_MINUTES=$2
      shift 2
      ;;
    --ca-not-before)
      requireValue "$1" "$2"
      validateAsn1Time "$1" "$2"
      CA_NOT_BEFORE=$2
      RECREATE_CA=true
      shift 2
      ;;
    --ca-not-after)
      requireValue "$1" "$2"
      validateAsn1Time "$1" "$2"
      CA_NOT_AFTER=$2
      RECREATE_CA=true
      shift 2
      ;;
    --cert-not-before)
      requireValue "$1" "$2"
      validateAsn1Time "$1" "$2"
      CERT_NOT_BEFORE=$2
      shift 2
      ;;
    --cert-not-after)
      requireValue "$1" "$2"
      validateAsn1Time "$1" "$2"
      CERT_NOT_AFTER=$2
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown option: $1"
      ;;
  esac
done

if [ -n "$CERT_MINUTES" ]; then
  validateMinutes "CERT_MINUTES" "$CERT_MINUTES"
  if [ -n "$CERT_NOT_BEFORE" ] || [ -n "$CERT_NOT_AFTER" ]; then
    fail "--cert-minutes cannot be combined with explicit certificate notBefore/notAfter values"
  fi
  if ! CERT_START_EPOCH=$(date -u +%s); then
    fail "Unable to compute certificate start time with date"
  fi
  CERT_END_EPOCH=$((CERT_START_EPOCH + CERT_MINUTES * 60))
  if ! CERT_NOT_BEFORE=$(date -u -d "@${CERT_START_EPOCH}" +%Y%m%d%H%M%SZ); then
    fail "Unable to compute certificate notBefore with GNU date"
  fi
  if ! CERT_NOT_AFTER=$(date -u -d "@${CERT_END_EPOCH}" +%Y%m%d%H%M%SZ); then
    fail "Unable to compute certificate notAfter with GNU date"
  fi
fi

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$SCRIPT_DIR"

KEY_PASSWORD=${KEY_PASSWORD:-}
PASS_IN=()
PASS_OUT=()
if [ -z "$KEY_PASSWORD" ]; then
  KEY_ENCRYPTION=(-nodes)
else
  KEY_ENCRYPTION=(-aes128)
  PASS_IN=(-passin "pass:${KEY_PASSWORD}")
  PASS_OUT=(-passout "pass:${KEY_PASSWORD}")
fi

x509ValidityArgs() {
  VALIDITY_ARGS=()
  if [ -n "$2" ]; then
    VALIDITY_ARGS+=("-not_before" "$2")
  fi
  if [ -n "$3" ]; then
    VALIDITY_ARGS+=("-not_after" "$3")
  else
    VALIDITY_ARGS+=("-days" "$1")
  fi
}

##
# Create certificate and private key for mTls participant,
# package key, cert and ca cert in PKI JSON structure.
#
# {
#   "key": "-----BEGIN PRIVATE KEY-----\nMIIEvAIBAD...",
#   "cert": "-----BEGIN CERTIFICATE-----\nMIIDRjCCA...",
#   "intermediates": [
#     "-----BEGIN CERTIFICATE-----\nMIICvTCCAaUCFEZ..."
#   ]
# }
#
createKeyCert() {
  NAME=$1
  SUBJECT=${2}

  # Create private key and certificate signing request
  # Add -nodes in case no password is set to avoid key encryption
  openssl req "${KEY_ENCRYPTION[@]}" -new -sha256 -newkey rsa:2048 -extensions v3_req \
    -subj "$SUBJECT" \
    -keyout "${NAME}.key" "${PASS_OUT[@]}" \
    -out "${NAME}.csr"
  # Sign with CA
  x509ValidityArgs "$CERT_DAYS" "$CERT_NOT_BEFORE" "$CERT_NOT_AFTER"
  openssl x509 -req -sha256 "${VALIDITY_ARGS[@]}" -extfile "$CERT_EXT" \
    -CA ca.pem -CAkey ca.key "${PASS_IN[@]}" \
    -in "${NAME}.csr" -out "${NAME}.pem"
  # Create JSON files with PKI format
  jq --null-input \
  --arg key "$(cat "${NAME}.key")" --arg cert "$(cat "${NAME}.pem")" --arg ca "$(cat ca.pem)" \
  "$PKI_JSON_MASK" > "${NAME}-pki.json"
}

createCaExt() {
  cat <<EOT > $CA_EXT
[v3_ca]
subjectKeyIdentifier=hash
authorityKeyIdentifier=keyid:always,issuer
basicConstraints=critical,CA:TRUE
keyUsage=critical,keyCertSign,cRLSign
EOT
}

createCa() {
  # Create self-signed CA
  # Add -nodes in case no password is set to avoid key encryption
  createCaExt
  if [ -f ca.key ]; then
    openssl req -new -sha256 -key ca.key "${PASS_IN[@]}" -out ca.csr -subj "/CN=Helidon-Test-CA"
  else
    openssl req "${KEY_ENCRYPTION[@]}" -new -sha256 -newkey rsa:2048 -keyout ca.key "${PASS_OUT[@]}" \
      -out ca.csr -subj "/CN=Helidon-Test-CA"
  fi
  x509ValidityArgs "$CA_DAYS" "$CA_NOT_BEFORE" "$CA_NOT_AFTER"
  openssl x509 -req -sha256 "${VALIDITY_ARGS[@]}" -in ca.csr -signkey ca.key "${PASS_IN[@]}" \
    -extfile "$CA_EXT" -extensions v3_ca -out ca.pem
}

caIsValid() {
  openssl x509 -in ca.pem -noout -text 2>/dev/null | grep -q "CA:TRUE"
}

if [ -f ca.pem ]; then
  if caIsValid; then
    if [ -f ca.key ]; then
      if [ "$RECREATE_CA" = "true" ]; then
        echo "Regenerating existing ca.pem with requested validity"
        createCa
      else
        echo "Reusing existing ca.pem"
      fi
    elif [ -f server-pki.json ] && [ -f client-pki.json ]; then
      if [ "$RECREATE_CA" = "true" ]; then
        echo "ca.pem exists and is a CA certificate, but ca.key is missing; cannot regenerate CA material" >&2
        echo "Restore ca.key for this CA, or remove ca.pem to generate a new CA" >&2
        exit 1
      fi
      echo "Reusing existing ca.pem, server-pki.json and client-pki.json"
      exit 0
    else
      echo "ca.pem exists and is a CA certificate, but ca.key is missing; cannot sign new PKI material" >&2
      echo "Restore ca.key for this CA, or remove ca.pem to generate a new CA" >&2
      exit 1
    fi
  elif [ -f ca.key ]; then
    echo "Existing ca.pem is not a CA certificate, regenerating it with CA extensions"
    createCa
  elif [ -f server-pki.json ] && [ -f client-pki.json ]; then
    echo "ca.pem exists but is not a CA certificate; cannot reuse existing PKI material" >&2
    echo "Restore ca.key and rerun this script, or remove ca.pem and the PKI JSON files to generate new material" >&2
    exit 1
  else
    echo "ca.pem exists but ca.key is missing; cannot regenerate a valid CA certificate" >&2
    echo "Remove ca.pem to generate a new CA, or restore ca.key for this CA" >&2
    exit 1
  fi
else
  createCa
fi

cat <<EOT > $CERT_EXT
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage=critical,digitalSignature,keyEncipherment
extendedKeyUsage=serverAuth,clientAuth
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
DNS.2 = test.oci-helidon.io
EOT

# Create mTls participants in PKI format, server-pki.json and client-pki.json
createKeyCert "server" "/CN=Helidon-Test-Server"
createKeyCert "client" "/CN=Helidon-Test-Client"

# Cleanup
rm -f ./client.*
rm -f ./server.*
rm -f ./*.csr
rm -f ./*.ext
