#!/usr/bin/env bash
# Verifies the live marketing content upload path against a running backend.
#
# The flow uploads a file, checks DAM status transitions, and leaves evidence that
# later release-loop verifiers can reference through ASSET_KEY.
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
TENANT_ID="${TENANT_ID:-}"
JWT_TOKEN="${JWT_TOKEN:-}"
ASSET_KEY="${ASSET_KEY:-live_upload_asset}"
ASSET_TYPE="${ASSET_TYPE:-FILE}"
MIME_TYPE="${MIME_TYPE:-application/pdf}"
FILE_PATH="${FILE_PATH:-}"
CALLBACK_SECRET="${CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET:-}"
CALLBACK_READY="${CALLBACK_READY:-false}"
CORS_ORIGIN="${CORS_ORIGIN:-${FRONTEND_ORIGIN:-}}"
VERIFY_CALLBACK_REJECTIONS="${VERIFY_CALLBACK_REJECTIONS:-true}"
CALLBACK_STALE_SECONDS="${CALLBACK_STALE_SECONDS:-3600}"

usage() {
  cat <<'USAGE'
Verify the marketing content direct-upload loop against a running backend.

Required environment:
  API_BASE       Backend base URL, default http://localhost:8080
  JWT_TOKEN      Bearer token for an authenticated content editor/admin
  TENANT_ID      Tenant id used by the public callback route
  FILE_PATH      Local file to upload

Optional environment:
  ASSET_KEY      Asset key, default live_upload_asset
  ASSET_TYPE     IMAGE | FILE | VIDEO | AUDIO, default FILE
  MIME_TYPE      MIME allowlisted by backend, default application/pdf
  CALLBACK_READY true to sign and send the public READY callback
  CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET required when CALLBACK_READY=true
  CORS_ORIGIN   Production frontend origin; when set, verifies S3/browser PUT preflight
  VERIFY_CALLBACK_REJECTIONS false to skip invalid/stale callback rejection probes
  CALLBACK_STALE_SECONDS seconds subtracted for the stale callback probe, default 3600

Example:
  API_BASE=http://localhost:8080 \
  JWT_TOKEN="$TOKEN" \
  TENANT_ID=1 \
  FILE_PATH=/tmp/proof.pdf \
  ASSET_KEY=proof_pdf \
  ASSET_TYPE=FILE \
  MIME_TYPE=application/pdf \
  CALLBACK_READY=true \
  CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET="$SECRET" \
  scripts/verify-marketing-content-upload-live.sh
USAGE
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 2
  fi
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command curl
require_command node
require_command shasum

if [[ -z "$JWT_TOKEN" || -z "$TENANT_ID" || -z "$FILE_PATH" ]]; then
  usage >&2
  exit 2
fi

if [[ ! -f "$FILE_PATH" ]]; then
  echo "FILE_PATH does not exist: $FILE_PATH" >&2
  exit 2
fi

if [[ "$CALLBACK_READY" == "true" && -z "$CALLBACK_SECRET" ]]; then
  echo "CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET is required when CALLBACK_READY=true" >&2
  exit 2
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

file_name="$(basename "$FILE_PATH")"
size_bytes="$(wc -c < "$FILE_PATH" | tr -d ' ')"
checksum="$(shasum -a 256 "$FILE_PATH" | awk '{print $1}')"
intent_request="$tmpdir/upload-intent-request.json"
intent_response="$tmpdir/upload-intent-response.json"
headers_file="$tmpdir/upload-headers.txt"
callback_body="$tmpdir/callback-body.json"
callback_response="$tmpdir/callback-response.json"
asset_response="$tmpdir/asset-response.json"
cors_headers="$tmpdir/cors-headers.txt"

node - "$intent_request" "$ASSET_KEY" "$ASSET_TYPE" "$MIME_TYPE" "$file_name" "$size_bytes" <<'NODE'
const fs = require('fs')
const [path, assetKey, assetType, mimeType, fileName, sizeBytes] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  assetKey,
  assetType,
  provider: 'S3',
  mimeType,
  fileName,
  sizeBytes: Number(sizeBytes),
  createdBy: 'live-upload-verifier'
}))
NODE

echo "Creating upload intent for $ASSET_KEY"
curl -fsS \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -o "$intent_response" \
  -d @"$intent_request" \
  "$API_BASE/marketing/content/assets/upload-intents"

node - "$intent_response" "$headers_file" <<'NODE'
const fs = require('fs')
const [responsePath, headersPath] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(responsePath, 'utf8'))
if (!response.data) {
  throw new Error(`upload intent response missing data: ${JSON.stringify(response)}`)
}
const intent = response.data
const params = intent.uploadParams || {}
if (params.handoffMode !== 'PRESIGNED_PUT') {
  throw new Error(`expected PRESIGNED_PUT handoff, got ${params.handoffMode}`)
}
const requiredHeaders = params.requiredHeaders || {}
const curlHeaders = Object.entries(requiredHeaders)
  .map(([key, value]) => ['-H', `${key}: ${value}`])
fs.writeFileSync(headersPath, JSON.stringify(curlHeaders))
console.log(JSON.stringify({
  intentKey: intent.intentKey,
  uploadToken: intent.uploadToken,
  uploadUrl: intent.uploadUrl,
  storageUrl: params.storageUrl,
  requiredHeaders
}, null, 2))
NODE

upload_url="$(node - "$intent_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
console.log(response.data.uploadUrl)
NODE
)"
storage_url="$(node - "$intent_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
console.log(response.data.uploadParams.storageUrl || '')
NODE
)"
upload_token="$(node - "$intent_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
console.log(response.data.uploadToken)
NODE
)"

echo "Uploading file bytes to presigned URL"
curl_header_args=()
while IFS= read -r arg; do
  curl_header_args+=("$arg")
done < <(node - "$headers_file" <<'NODE'
const fs = require('fs')
const args = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
for (const arg of args.flat()) console.log(arg)
NODE
)

if [[ -n "$CORS_ORIGIN" ]]; then
  echo "Verifying browser CORS preflight for presigned PUT from $CORS_ORIGIN"
  cors_request_headers="$(node - "$intent_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const requiredHeaders = response.data.uploadParams.requiredHeaders || {}
console.log(Object.keys(requiredHeaders).join(', '))
NODE
)"
  cors_status="$(curl -sS -o /dev/null -D "$cors_headers" -w "%{http_code}" \
    -X OPTIONS \
    -H "Origin: $CORS_ORIGIN" \
    -H "Access-Control-Request-Method: PUT" \
    -H "Access-Control-Request-Headers: $cors_request_headers" \
    "$upload_url" || true)"
  node - "$cors_headers" "$cors_status" "$CORS_ORIGIN" "$cors_request_headers" <<'NODE'
const fs = require('fs')
const [headersPath, status, origin, requestedHeaders] = process.argv.slice(2)
if (!['200', '204'].includes(status)) {
  throw new Error(`S3 CORS preflight failed with HTTP ${status}`)
}
const raw = fs.readFileSync(headersPath, 'utf8')
const headers = new Map()
for (const line of raw.split(/\r?\n/)) {
  const index = line.indexOf(':')
  if (index <= 0) continue
  const key = line.slice(0, index).trim().toLowerCase()
  const value = line.slice(index + 1).trim()
  headers.set(key, value)
}
const allowOrigin = headers.get('access-control-allow-origin')
if (allowOrigin !== origin) {
  throw new Error(`S3 CORS preflight did not echo production origin ${origin}; got ${allowOrigin || '<missing>'}`)
}
const methods = (headers.get('access-control-allow-methods') || '')
  .split(',')
  .map((value) => value.trim().toUpperCase())
if (!methods.includes('PUT')) {
  throw new Error(`S3 CORS preflight does not allow PUT: ${headers.get('access-control-allow-methods') || '<missing>'}`)
}
const allowHeadersValue = headers.get('access-control-allow-headers') || ''
const allowHeaders = allowHeadersValue
  .split(',')
  .map((value) => value.trim().toLowerCase())
const requested = requestedHeaders
  .split(',')
  .map((value) => value.trim().toLowerCase())
  .filter(Boolean)
if (!allowHeaders.includes('*')) {
  for (const header of requested) {
    if (!allowHeaders.includes(header)) {
      throw new Error(`S3 CORS preflight does not allow required header ${header}: ${allowHeadersValue || '<missing>'}`)
    }
  }
}
NODE
fi

curl -fsS -X PUT "${curl_header_args[@]}" --data-binary @"$FILE_PATH" "$upload_url" >/dev/null

echo "Upload PUT completed"

if [[ "$CALLBACK_READY" != "true" ]]; then
  echo "Skipping READY callback. Set CALLBACK_READY=true to verify signed callback and asset READY gate."
  exit 0
fi

node - "$callback_body" "$ASSET_KEY" "$ASSET_TYPE" "$MIME_TYPE" "$storage_url" "$upload_token" "$size_bytes" "$checksum" <<'NODE'
const fs = require('fs')
const [path, assetKey, assetType, mimeType, storageUrl, uploadToken, sizeBytes, checksum] = process.argv.slice(2)
const body = {
  provider: 'S3',
  uploadToken,
  assetKey,
  assetType,
  mimeType,
  storageUrl,
  status: 'READY',
  sizeBytes: Number(sizeBytes),
  checksumSha256: checksum,
  scanStatus: 'PASSED',
  metadata: {
    verifier: 'scripts/verify-marketing-content-upload-live.sh'
  }
}
if (assetType === 'VIDEO') {
  body.transcodeStatus = 'READY'
  body.durationMs = Number(process.env.DURATION_MS || 1)
}
fs.writeFileSync(path, JSON.stringify(body))
NODE

timestamp="$(date +%s)"
signature="$(node - "$CALLBACK_SECRET" "$timestamp" "$callback_body" <<'NODE'
const crypto = require('crypto')
const fs = require('fs')
const [secret, timestamp, bodyPath] = process.argv.slice(2)
const body = fs.readFileSync(bodyPath, 'utf8')
const digest = crypto
  .createHmac('sha256', secret)
  .update(`${timestamp}\n${body}`)
  .digest('hex')
console.log(`sha256=${digest}`)
NODE
)"

echo "Sending signed READY callback"
if [[ "$VERIFY_CALLBACK_REJECTIONS" != "false" ]]; then
  echo "Verifying invalid READY callback signature is rejected"
  invalid_response="$tmpdir/invalid-signature-response.json"
  invalid_status="$(curl -sS -o "$invalid_response" -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -H "X-Canvas-Asset-Timestamp: $timestamp" \
    -H "X-Canvas-Asset-Signature: sha256=bad" \
    -d @"$callback_body" \
    "$API_BASE/public/marketing/content/assets/upload-callbacks/$TENANT_ID/S3" || true)"
  if [[ "$invalid_status" != "401" && "$invalid_status" != "403" ]]; then
    echo "Expected invalid READY callback signature to be rejected with 401/403, got $invalid_status" >&2
    cat "$invalid_response" >&2 || true
    exit 1
  fi

  echo "Verifying stale READY callback timestamp is rejected"
  stale_response="$tmpdir/stale-timestamp-response.json"
  stale_timestamp="$((timestamp - CALLBACK_STALE_SECONDS))"
  stale_signature="$(node - "$CALLBACK_SECRET" "$stale_timestamp" "$callback_body" <<'NODE'
const crypto = require('crypto')
const fs = require('fs')
const [secret, timestamp, bodyPath] = process.argv.slice(2)
const body = fs.readFileSync(bodyPath, 'utf8')
const digest = crypto
  .createHmac('sha256', secret)
  .update(`${timestamp}\n${body}`)
  .digest('hex')
console.log(`sha256=${digest}`)
NODE
)"
  stale_status="$(curl -sS -o "$stale_response" -w "%{http_code}" \
    -H "Content-Type: application/json" \
    -H "X-Canvas-Asset-Timestamp: $stale_timestamp" \
    -H "X-Canvas-Asset-Signature: $stale_signature" \
    -d @"$callback_body" \
    "$API_BASE/public/marketing/content/assets/upload-callbacks/$TENANT_ID/S3" || true)"
  if [[ "$stale_status" != "401" && "$stale_status" != "403" ]]; then
    echo "Expected stale READY callback timestamp to be rejected with 401/403, got $stale_status" >&2
    cat "$stale_response" >&2 || true
    exit 1
  fi
fi

curl -fsS \
  -H "Content-Type: application/json" \
  -H "X-Canvas-Asset-Timestamp: $timestamp" \
  -H "X-Canvas-Asset-Signature: $signature" \
  -o "$callback_response" \
  -d @"$callback_body" \
  "$API_BASE/public/marketing/content/assets/upload-callbacks/$TENANT_ID/S3"

node - "$callback_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
if (!response.data || response.data.status !== 'COMPLETED') {
  throw new Error(`callback did not complete upload intent: ${JSON.stringify(response)}`)
}
console.log(JSON.stringify(response.data, null, 2))
NODE

echo "Verifying READY DAM asset row"
asset_query="$(node - "$ASSET_KEY" "$ASSET_TYPE" <<'NODE'
const [assetKey, assetType] = process.argv.slice(2)
const params = new URLSearchParams({
  keyword: assetKey,
  assetType,
  status: 'READY',
})
console.log(params.toString())
NODE
)"
curl -fsS \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -o "$asset_response" \
  "$API_BASE/marketing/content/assets?$asset_query"

node - "$asset_response" "$ASSET_KEY" "$storage_url" "$checksum" <<'NODE'
const fs = require('fs')
const [responsePath, assetKey, storageUrl, checksum] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(responsePath, 'utf8'))
const assets = Array.isArray(response.data) ? response.data : []
const asset = assets.find((candidate) => candidate.assetKey === assetKey)
if (!asset) {
  throw new Error(`READY DAM asset not found for ${assetKey}: ${JSON.stringify(response)}`)
}
if (asset.status !== 'READY') {
  throw new Error(`DAM asset ${assetKey} is not READY: ${asset.status}`)
}
if (asset.storageUrl !== storageUrl) {
  throw new Error(`DAM asset ${assetKey} storageUrl mismatch: ${asset.storageUrl} !== ${storageUrl}`)
}
if (!asset.checksumSha256) {
  throw new Error(`DAM asset ${assetKey} checksumSha256 is missing`)
}
if (asset.checksumSha256.toLowerCase() !== checksum.toLowerCase()) {
  throw new Error(`DAM asset ${assetKey} checksum mismatch: ${asset.checksumSha256} !== ${checksum}`)
}
console.log(JSON.stringify(asset, null, 2))
NODE

echo "Marketing content upload live verifier completed"
