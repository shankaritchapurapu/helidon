#!/bin/bash -e
#
# Copyright (c) 2024 Oracle and/or its affiliates.
#

DAYS=9999
PKI_JSON_MASK='{"key": $key, "cert": $cert, "intermediates": [$ca]}'

KEY_PASSWORD=
if [ -z "$KEY_PASSWORD" ]; then NO_DES="-nodes"; else NO_DES="-aes128"; fi
PASS_IN=${KEY_PASSWORD:+-passin pass:}${KEY_PASSWORD}
PASS_OUT=${KEY_PASSWORD:+-passout pass:}${KEY_PASSWORD}

cat <<EOT > x509.ext
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
subjectAltName = @alt_names
[alt_names]
DNS.1 = localhost
DNS.2 = test.oci-helidon.io
EOT

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
  openssl req ${NO_DES} -new -sha256 -newkey rsa:2048 -extensions v3_req \
  	-subj "$SUBJECT" \
  	-keyout ${NAME}.key ${PASS_OUT}\
  	-out ${NAME}.csr
  # Sing with CA
  openssl x509 -req -sha256 -days $DAYS -extfile x509.ext \
  	-CA ca.pem -CAkey ca.key ${PASS_IN}\
  	-in ${NAME}.csr -out ${NAME}.pem
  # Create JSON files with PKI format
  jq --null-input \
  --arg key "$(cat ${NAME}.key)" --arg cert "$(cat ${NAME}.pem)" --arg ca "$(cat ca.pem)" \
  "$PKI_JSON_MASK" > ${NAME}-pki.json
}

# Create self-signed CA
# Add -nodes in case no password is set to avoid key encryption
openssl req ${NO_DES} -new -sha256 -newkey rsa:2048 -keyout ca.key ${PASS_OUT} -out ca.csr -subj "/CN=Helidon-Test-CA"
openssl x509 -req -days $DAYS -in ca.csr -signkey ca.key ${PASS_IN} -out ca.pem

# Create mTls participants in PKI format, server-pki.json and client-pki.json
createKeyCert "server" "/CN=Helidon-Test-Server"
createKeyCert "client" "/CN=Helidon-Test-Client"

# Cleanup
rm ./client.*
rm ./server.*
rm ./*.csr
rm ./*.ext