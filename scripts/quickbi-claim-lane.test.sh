#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
STATUS_SCRIPT="$ROOT_DIR/scripts/quickbi-slice-status.sh"
CLAIM_SCRIPT="$ROOT_DIR/scripts/quickbi-claim-lane.sh"
NODE_BIN="${NODE_BIN:-/opt/homebrew/bin/node}"

if [[ ! -x "$NODE_BIN" ]]; then
  NODE_BIN="$(command -v node)"
fi

CLAIM_FILE="$(mktemp)"
LANE_PLAN_FILE="$(mktemp)"
trap 'rm -f "$CLAIM_FILE" "$LANE_PLAN_FILE"' EXIT

cat >"$LANE_PLAN_FILE" <<'PLAN'
## Task 1: Open Lane

Remaining production work after this task: spreadsheet/big-screen mobile layout variants and formula/pivot advanced editing.
PLAN

PREVIEW_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --claim-next --owner preview-owner --dry-run --json)"
PREVIEW_JSON="$PREVIEW_JSON" "$NODE_BIN" <<'NODE'
const preview = JSON.parse(process.env.PREVIEW_JSON);
if (preview.status !== 'preview') {
  throw new Error(`expected preview status, got ${preview.status}`);
}
if (preview.owner !== 'preview-owner' || !preview.lane || !preview.command) {
  throw new Error('preview must include owner, lane, and command');
}
NODE

EMPTY_AFTER_PREVIEW="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$STATUS_SCRIPT" --json)"
EMPTY_AFTER_PREVIEW="$EMPTY_AFTER_PREVIEW" "$NODE_BIN" <<'NODE'
const status = JSON.parse(process.env.EMPTY_AFTER_PREVIEW);
if (status.activeClaims.length !== 0) {
  throw new Error('dry-run claim-next must not persist a claim');
}
NODE

CLAIM_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --claim-next --owner claim-owner --note "next lane" --json)"
CLAIM_JSON="$CLAIM_JSON" "$NODE_BIN" <<'NODE'
const claim = JSON.parse(process.env.CLAIM_JSON);
if (claim.status !== 'active' || claim.owner !== 'claim-owner') {
  throw new Error('claim-next should persist an active claim for claim-owner');
}
if (!claim.lane || !claim.scope || !claim.command) {
  throw new Error('claim-next result must include lane, scope, and command');
}
NODE

QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --claim "already completed QuickBI lane" --owner orphan-owner --json >/dev/null
ORPHAN_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$STATUS_SCRIPT" --json)"
ORPHAN_JSON="$ORPHAN_JSON" "$NODE_BIN" <<'NODE'
const status = JSON.parse(process.env.ORPHAN_JSON);
if (!status.orphanedActiveClaims.some((claim) => claim.owner === 'orphan-owner')) {
  throw new Error('orphaned active claim should be exposed');
}
NODE

RELEASED_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --release-orphaned --json)"
RELEASED_JSON="$RELEASED_JSON" "$NODE_BIN" <<'NODE'
const released = JSON.parse(process.env.RELEASED_JSON);
if (!Array.isArray(released) || !released.some((claim) => claim.owner === 'orphan-owner')) {
  throw new Error('release-orphaned should release the orphan owner claim');
}
NODE

echo "quickbi claim lane script test passed"
