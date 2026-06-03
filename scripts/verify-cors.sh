#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ORIGIN="${1:-https://evil.example}"
TARGET_URL="${BASE_URL%/}/canvas/trigger/behavior"
headers_file="$(mktemp)"

cleanup() {
  rm -f "${headers_file}"
}
trap cleanup EXIT

curl -sS -D "${headers_file}" -o /dev/null \
  -H "Origin: ${ORIGIN}" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS "${TARGET_URL}"

tr -d '\r' < "${headers_file}"

if tr -d '\r' < "${headers_file}" | grep -Fqx "Access-Control-Allow-Origin: ${ORIGIN}"; then
  echo "ERROR: forbidden origin was allowed: ${ORIGIN}" >&2
  exit 1
fi

echo "OK: ${ORIGIN} was not echoed as an allowed CORS origin."
