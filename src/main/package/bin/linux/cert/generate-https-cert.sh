#!/usr/bin/env bash
# Generates a local CA and a current-IP HTTPS certificate for Ratel FM.
set -euo pipefail

BASE_DIR=""
KEYTOOL=""
HTTPS_PORT="38443"
CONTEXT_PATH="/ratel/fm"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-dir)
      BASE_DIR="$2"
      shift 2
      ;;
    --keytool)
      KEYTOOL="$2"
      shift 2
      ;;
    --https-port)
      HTTPS_PORT="$2"
      shift 2
      ;;
    --context-path)
      CONTEXT_PATH="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

if [[ -z "$BASE_DIR" || -z "$KEYTOOL" ]]; then
  echo "--base-dir and --keytool are required"
  exit 1
fi
if [[ ! -x "$KEYTOOL" ]]; then
  echo "keytool not found or not executable: $KEYTOOL"
  exit 1
fi

CERT_DIR="$BASE_DIR/certs"
WORK_DIR="$CERT_DIR/work"
CA_STORE="$CERT_DIR/ratel-local-ca.p12"
CA_CERT="$CERT_DIR/ratel-local-ca.cer"
CA_PASSWORD_FILE="$CERT_DIR/ratel-local-ca.password"
SERVER_STORE="$CERT_DIR/ratel-fm-server.p12"
SERVER_PASSWORD_FILE="$CERT_DIR/ratel-fm-server.password"
SERVER_CSR="$WORK_DIR/ratel-fm-server.csr"
SERVER_CERT="$WORK_DIR/ratel-fm-server.crt"
INFO_FILE="$CERT_DIR/https-info.txt"

mkdir -p "$CERT_DIR" "$WORK_DIR"

password_file() {
  local path="$1"
  if [[ -f "$path" ]]; then
    tr -d '[:space:]' < "$path"
    return
  fi
  local password
  password="$(date +%s%N | sha256sum | awk '{print $1}')"
  printf '%s' "$password" > "$path"
  printf '%s' "$password"
}

CA_PASSWORD="$(password_file "$CA_PASSWORD_FILE")"
SERVER_PASSWORD="$(password_file "$SERVER_PASSWORD_FILE")"

if [[ ! -f "$CA_STORE" ]]; then
  "$KEYTOOL" -genkeypair \
    -alias ratel-local-ca \
    -keyalg RSA \
    -keysize 4096 \
    -validity 3650 \
    -dname "CN=Ratel FM Local CA, OU=Ratel FM, O=ratel, L=Chengdu, ST=Sichuan, C=CN" \
    -ext bc=ca:true \
    -ext KeyUsage=keyCertSign,cRLSign \
    -storetype PKCS12 \
    -keystore "$CA_STORE" \
    -storepass "$CA_PASSWORD" \
    -keypass "$CA_PASSWORD"
fi

if [[ ! -f "$CA_CERT" ]]; then
  "$KEYTOOL" -exportcert \
    -alias ratel-local-ca \
    -keystore "$CA_STORE" \
    -storepass "$CA_PASSWORD" \
    -rfc \
    -file "$CA_CERT"
fi

HOST_NAMES=("localhost")
HOSTNAME_VALUE="$(hostname 2>/dev/null || true)"
if [[ -n "$HOSTNAME_VALUE" ]]; then
  HOST_NAMES+=("$HOSTNAME_VALUE")
fi

IP_ADDRESSES=("127.0.0.1")
if command -v hostname >/dev/null 2>&1; then
  for ip in $(hostname -I 2>/dev/null || true); do
    if [[ "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ && "$ip" != 169.254.* && "$ip" != "127.0.0.1" ]]; then
      IP_ADDRESSES+=("$ip")
    fi
  done
fi

SAN_PARTS=()
for name in "${HOST_NAMES[@]}"; do
  SAN_PARTS+=("dns:${name,,}")
done
for ip in "${IP_ADDRESSES[@]}"; do
  SAN_PARTS+=("ip:$ip")
done
SAN="SAN=$(IFS=,; echo "${SAN_PARTS[*]}")"
PRIMARY_NAME="${HOST_NAMES[0]}"

rm -f "$SERVER_STORE" "$SERVER_CSR" "$SERVER_CERT"

"$KEYTOOL" -genkeypair \
  -alias ratel-fm-server \
  -keyalg RSA \
  -keysize 2048 \
  -validity 825 \
  -dname "CN=$PRIMARY_NAME, OU=Ratel FM, O=ratel, L=Chengdu, ST=Sichuan, C=CN" \
  -storetype PKCS12 \
  -keystore "$SERVER_STORE" \
  -storepass "$SERVER_PASSWORD" \
  -keypass "$SERVER_PASSWORD"
"$KEYTOOL" -certreq \
  -alias ratel-fm-server \
  -keystore "$SERVER_STORE" \
  -storepass "$SERVER_PASSWORD" \
  -file "$SERVER_CSR" \
  -ext "$SAN" \
  -ext EKU=serverAuth
"$KEYTOOL" -gencert \
  -alias ratel-local-ca \
  -keystore "$CA_STORE" \
  -storepass "$CA_PASSWORD" \
  -infile "$SERVER_CSR" \
  -outfile "$SERVER_CERT" \
  -rfc \
  -validity 825 \
  -ext "$SAN" \
  -ext EKU=serverAuth \
  -ext KeyUsage=digitalSignature,keyEncipherment
"$KEYTOOL" -importcert \
  -alias ratel-local-ca \
  -keystore "$SERVER_STORE" \
  -storepass "$SERVER_PASSWORD" \
  -file "$CA_CERT" \
  -noprompt
"$KEYTOOL" -importcert \
  -alias ratel-fm-server \
  -keystore "$SERVER_STORE" \
  -storepass "$SERVER_PASSWORD" \
  -file "$SERVER_CERT"

{
  echo "Ratel FM HTTPS certificate generated at $(date '+%Y-%m-%d %H:%M:%S')"
  echo "CA certificate: $CA_CERT"
  echo "Server keystore: $SERVER_STORE"
  echo "Host names: ${HOST_NAMES[*]}"
  echo "IP addresses: ${IP_ADDRESSES[*]}"
  echo "HTTPS URLs:"
  for ip in "${IP_ADDRESSES[@]}"; do
    echo "https://$ip:$HTTPS_PORT$CONTEXT_PATH"
  done
} > "$INFO_FILE"

echo "HTTPS certificate generated."
echo "CA certificate for client trust: $CA_CERT"
echo "Server keystore: $SERVER_STORE"
for ip in "${IP_ADDRESSES[@]}"; do
  echo "HTTPS URL: https://$ip:$HTTPS_PORT$CONTEXT_PATH"
done
