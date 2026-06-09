#!/usr/bin/env bash
# Runs a live end-to-end check for the marketing content release loop.
#
# The flow verifies a READY DAM asset, creates/updates a content template,
# validates variables, publishes twice, resolves runtime content, rolls back,
# and checks audit evidence against a running backend.
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
JWT_TOKEN="${JWT_TOKEN:-}"
ASSET_KEY="${ASSET_KEY:-}"
TEMPLATE_KEY="${TEMPLATE_KEY:-live-release-$(date +%s)}"
OPERATOR="${OPERATOR:-live-release-verifier}"
FIRST_NAME="${FIRST_NAME:-Alice}"

usage() {
  cat <<'USAGE'
Verify the marketing content release loop against a running backend.

Required environment:
  API_BASE       Backend base URL, default http://localhost:8080
  JWT_TOKEN      Bearer token for an authenticated content editor/admin
  ASSET_KEY      Existing READY DAM asset key, ideally produced by verify-marketing-content-upload-live.sh

Optional environment:
  TEMPLATE_KEY   Unique template key for the probe, default live-release-<epoch>
  OPERATOR       Audit actor label, default live-release-verifier
  FIRST_NAME     Runtime merge value used by resolve, default Alice

Example:
  API_BASE=https://canvas.example.com \
  JWT_TOKEN="$TOKEN" \
  ASSET_KEY=proof_pdf \
  TEMPLATE_KEY=release-proof-$(date +%s) \
  scripts/verify-marketing-content-release-live.sh
USAGE
}

# Ensure required CLI tools are available before creating remote state.
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

if [[ -z "$JWT_TOKEN" || -z "$ASSET_KEY" ]]; then
  usage >&2
  exit 2
fi

tmpdir="$(mktemp -d)"
trap 'rm -rf "$tmpdir"' EXIT

auth_headers=(-H "Authorization: Bearer $JWT_TOKEN")

# POST a JSON file with the configured bearer token and capture the response body.
post_json() {
  local body="$1"
  local path="$2"
  local output="$3"
  curl -fsS \
    "${auth_headers[@]}" \
    -H "Content-Type: application/json" \
    -o "$output" \
    -d @"$body" \
    "$API_BASE$path"
}

# GET an authenticated JSON endpoint and capture the response body.
get_json() {
  local path="$1"
  local output="$2"
  curl -fsS \
    "${auth_headers[@]}" \
    -o "$output" \
    "$API_BASE$path"
}

asset_response="$tmpdir/asset-response.json"
template_draft="$tmpdir/template-draft.json"
template_update="$tmpdir/template-update.json"
template_status="$tmpdir/template-status.json"
validation_request="$tmpdir/validation-request.json"
validation_response="$tmpdir/validation-response.json"
publish_request="$tmpdir/publish-request.json"
publish_response="$tmpdir/publish-response.json"
resolve_request="$tmpdir/resolve-request.json"
resolve_response="$tmpdir/resolve-response.json"
publish2_response="$tmpdir/publish2-response.json"
active_releases="$tmpdir/active-releases.json"
rollback_request="$tmpdir/rollback-request.json"
rollback_response="$tmpdir/rollback-response.json"
resolve_after_rollback="$tmpdir/resolve-after-rollback.json"
audit_response="$tmpdir/audit-response.json"

echo "Verifying existing READY DAM asset $ASSET_KEY"
# Asset readiness is the release precondition; publishing must not reference a missing DAM asset.
asset_query="$(node - "$ASSET_KEY" <<'NODE'
const [assetKey] = process.argv.slice(2)
const params = new URLSearchParams({ keyword: assetKey, status: 'READY' })
console.log(params.toString())
NODE
)"
get_json "/marketing/content/assets?$asset_query" "$asset_response"
node - "$asset_response" "$ASSET_KEY" <<'NODE'
const fs = require('fs')
const [path, assetKey] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const assets = Array.isArray(response.data) ? response.data : []
const asset = assets.find((candidate) => candidate.assetKey === assetKey)
if (!asset) {
  throw new Error(`READY DAM asset not found for ${assetKey}: ${JSON.stringify(response)}`)
}
if (asset.status !== 'READY') {
  throw new Error(`DAM asset ${assetKey} is not READY: ${asset.status}`)
}
console.log(JSON.stringify(asset, null, 2))
NODE

node - "$template_draft" "$TEMPLATE_KEY" "$ASSET_KEY" "$OPERATOR" <<'NODE'
const fs = require('fs')
const [path, templateKey, assetKey, operator] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  templateKey,
  displayName: `Release verifier ${templateKey}`,
  channel: 'EMAIL',
  subject: 'Hello {{ firstName }}',
  body: 'Stable body for {{ firstName }} using asset.',
  designJson: JSON.stringify({ blocks: [{ type: 'text', text: 'stable' }] }),
  assetRefsJson: JSON.stringify([assetKey]),
  status: 'DRAFT',
  reviewNotes: 'release verifier draft',
  createdBy: operator
}))
NODE

echo "Creating draft template $TEMPLATE_KEY"
# The draft includes both a DAM asset reference and a runtime variable for later validation.
post_json "$template_draft" "/marketing/content/templates" "$tmpdir/template-create-response.json"

node - "$template_status" "$OPERATOR" <<'NODE'
const fs = require('fs')
const [path, operator] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  status: 'APPROVED',
  reviewNotes: `approved by ${operator}`
}))
NODE

echo "Approving template $TEMPLATE_KEY"
# Approval status is required before the release validation and publish gates can pass.
post_json "$template_status" "/marketing/content/templates/$TEMPLATE_KEY/status" "$tmpdir/template-approve-response.json"

node - "$validation_request" "$TEMPLATE_KEY" <<'NODE'
const fs = require('fs')
const [path, templateKey] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({ sourceType: 'TEMPLATE', sourceKey: templateKey }))
NODE

echo "Validating release gate"
# Validation checks readiness without creating or changing an active release.
post_json "$validation_request" "/marketing/content/releases/validate" "$validation_response"
node - "$validation_response" "$ASSET_KEY" <<'NODE'
const fs = require('fs')
const [path, assetKey] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const validation = response.data
if (!validation || validation.ready !== true) {
  throw new Error(`release validation is not ready: ${JSON.stringify(response)}`)
}
if (!Array.isArray(validation.assetRefs) || !validation.assetRefs.includes(assetKey)) {
  throw new Error(`release validation does not include asset ${assetKey}: ${JSON.stringify(validation)}`)
}
NODE

node - "$publish_request" "$TEMPLATE_KEY" "$OPERATOR" <<'NODE'
const fs = require('fs')
const [path, templateKey, operator] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  sourceType: 'TEMPLATE',
  sourceKey: templateKey,
  createdBy: operator,
  note: 'release verifier version 1'
}))
NODE

echo "Publishing first active release"
# First publish creates the active release that downstream resolve calls should serve.
post_json "$publish_request" "/marketing/content/releases/publish" "$publish_response"
release_key="$(node - "$publish_response" "$TEMPLATE_KEY" <<'NODE'
const fs = require('fs')
const [path, templateKey] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const release = response.data
if (!release || release.releaseKey !== `template-${templateKey}` || release.status !== 'ACTIVE' || release.sourceVersion !== 1) {
  throw new Error(`unexpected first release: ${JSON.stringify(response)}`)
}
if (!release.checksumSha256) {
  throw new Error(`first release checksum is missing: ${JSON.stringify(release)}`)
}
console.log(release.releaseKey)
NODE
)"

node - "$resolve_request" "$FIRST_NAME" <<'NODE'
const fs = require('fs')
const [path, firstName] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({ firstName }))
NODE

echo "Resolving first release $release_key"
# Resolve proves runtime merge values and DAM asset expansion work on the active release.
post_json "$resolve_request" "/marketing/content/releases/$release_key/resolve" "$resolve_response"
node - "$resolve_response" "$FIRST_NAME" "$ASSET_KEY" <<'NODE'
const fs = require('fs')
const [path, firstName, assetKey] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const resolved = response.data
if (!resolved || resolved.status !== 'ACTIVE' || resolved.sourceVersion !== 1) {
  throw new Error(`unexpected first resolved release: ${JSON.stringify(response)}`)
}
if (resolved.renderedSubject !== `Hello ${firstName}`) {
  throw new Error(`first release subject did not render: ${resolved.renderedSubject}`)
}
if (!String(resolved.renderedBody || '').includes(`Stable body for ${firstName}`)) {
  throw new Error(`first release body did not render stable copy: ${resolved.renderedBody}`)
}
const assets = Array.isArray(resolved.assets) ? resolved.assets : []
if (!assets.some((asset) => asset.assetKey === assetKey && asset.status === 'READY')) {
  throw new Error(`resolved release missing READY asset ${assetKey}: ${JSON.stringify(resolved)}`)
}
NODE

node - "$template_update" "$TEMPLATE_KEY" "$ASSET_KEY" "$OPERATOR" <<'NODE'
const fs = require('fs')
const [path, templateKey, assetKey, operator] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  templateKey,
  displayName: `Release verifier ${templateKey}`,
  channel: 'EMAIL',
  subject: 'Hello {{ firstName }}',
  body: 'Changed body for {{ firstName }} using asset.',
  designJson: JSON.stringify({ blocks: [{ type: 'text', text: 'changed' }] }),
  assetRefsJson: JSON.stringify([assetKey]),
  status: 'DRAFT',
  reviewNotes: 'release verifier changed draft',
  createdBy: operator
}))
NODE

echo "Publishing second release version"
# Second publish verifies supersede behavior: only the newest release should remain ACTIVE.
post_json "$template_update" "/marketing/content/templates" "$tmpdir/template-update-response.json"
post_json "$template_status" "/marketing/content/templates/$TEMPLATE_KEY/status" "$tmpdir/template-approve2-response.json"
post_json "$publish_request" "/marketing/content/releases/publish" "$publish2_response"
node - "$publish2_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const release = response.data
if (!release || release.status !== 'ACTIVE' || release.sourceVersion !== 2) {
  throw new Error(`unexpected second release: ${JSON.stringify(response)}`)
}
if (!release.checksumSha256) {
  throw new Error(`second release checksum is missing: ${JSON.stringify(release)}`)
}
NODE

echo "Verifying exactly one ACTIVE release"
# Active release cardinality guards against split-brain content serving.
active_query="$(node - "$TEMPLATE_KEY" <<'NODE'
const [templateKey] = process.argv.slice(2)
const params = new URLSearchParams({ sourceType: 'TEMPLATE', sourceKey: templateKey, status: 'ACTIVE' })
console.log(params.toString())
NODE
)"
get_json "/marketing/content/releases?$active_query" "$active_releases"
node - "$active_releases" "$release_key" <<'NODE'
const fs = require('fs')
const [path, releaseKey] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const releases = Array.isArray(response.data) ? response.data : []
const active = releases.filter((release) => release.releaseKey === releaseKey && release.status === 'ACTIVE')
if (active.length !== 1) {
  throw new Error(`expected exactly one active release for ${releaseKey}, got ${active.length}: ${JSON.stringify(response)}`)
}
if (active[0].sourceVersion !== 2) {
  throw new Error(`expected active release version 2 before rollback: ${JSON.stringify(active[0])}`)
}
NODE

node - "$rollback_request" "$OPERATOR" <<'NODE'
const fs = require('fs')
const [path, operator] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  actor: operator,
  reason: 'release verifier rollback'
}))
NODE

echo "Rolling back to previous release"
# Rollback verifies operational recovery and restores the previous release body.
post_json "$rollback_request" "/marketing/content/releases/$release_key/rollback" "$rollback_response"
node - "$rollback_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const release = response.data
if (!release || release.status !== 'ACTIVE' || release.sourceVersion !== 1) {
  throw new Error(`rollback did not restore version 1: ${JSON.stringify(response)}`)
}
NODE

post_json "$resolve_request" "/marketing/content/releases/$release_key/resolve" "$resolve_after_rollback"
node - "$resolve_after_rollback" "$FIRST_NAME" <<'NODE'
const fs = require('fs')
const [path, firstName] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const resolved = response.data
if (!resolved || resolved.sourceVersion !== 1 || resolved.status !== 'ACTIVE') {
  throw new Error(`unexpected resolved release after rollback: ${JSON.stringify(response)}`)
}
if (!String(resolved.renderedBody || '').includes(`Stable body for ${firstName}`)) {
  throw new Error(`rollback did not restore stable body: ${resolved.renderedBody}`)
}
NODE

echo "Verifying release audit events"
# Audit evidence closes the release loop for publish, supersede, rollback, and restore actions.
audit_query="$(node - "$release_key" <<'NODE'
const [releaseKey] = process.argv.slice(2)
const params = new URLSearchParams({ targetType: 'RELEASE', targetKey: releaseKey, limit: '20' })
console.log(params.toString())
NODE
)"
get_json "/marketing/content/audit-events?$audit_query" "$audit_response"
node - "$audit_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
const events = Array.isArray(response.data) ? response.data.map((event) => event.eventType) : []
for (const required of ['RELEASE_PUBLISHED', 'RELEASE_SUPERSEDED', 'RELEASE_ROLLED_BACK', 'RELEASE_RESTORED']) {
  if (!events.includes(required)) {
    throw new Error(`missing audit event ${required}: ${JSON.stringify(response)}`)
  }
}
NODE

echo "Marketing content release live verifier completed for $release_key"
