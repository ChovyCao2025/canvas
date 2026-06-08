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
EMPTY_PLAN_FILE="$(mktemp)"
trap 'rm -f "$CLAIM_FILE" "$LANE_PLAN_FILE" "$EMPTY_PLAN_FILE"' EXIT

cat >"$LANE_PLAN_FILE" <<'PLAN'
## Task 1: Open Lane

Remaining production work after this task: spreadsheet/big-screen mobile layout variants and formula/pivot advanced editing.
PLAN

STATUS_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$STATUS_SCRIPT" --json)"
STATUS_JSON="$STATUS_JSON" "$NODE_BIN" <<'NODE'
const status = JSON.parse(process.env.STATUS_JSON);
if (!status.latestTask || typeof status.latestTask.id !== 'number') {
  throw new Error('latestTask with numeric id is required');
}
if (!Array.isArray(status.remainingLanes) || status.remainingLanes.length === 0) {
  throw new Error('remainingLanes must include current QuickBI lanes');
}
if (!Array.isArray(status.availableLanes) || status.availableLanes.length !== status.remainingLanes.length) {
  throw new Error('availableLanes must mirror remaining lanes when no claims exist');
}
for (const lane of status.availableLanes) {
  if (!lane.lane || !lane.scope || !lane.command) {
    throw new Error('available lane must expose lane, scope, and command');
  }
}
NODE

FIRST_LANE="$(STATUS_JSON="$STATUS_JSON" "$NODE_BIN" -e 'const s = JSON.parse(process.env.STATUS_JSON); process.stdout.write(s.remainingLanes[0]);')"
QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --claim "$FIRST_LANE" --owner status-owner --json >/dev/null

CLAIMED_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$STATUS_SCRIPT" --json)"
CLAIMED_JSON="$CLAIMED_JSON" "$NODE_BIN" <<'NODE'
const status = JSON.parse(process.env.CLAIMED_JSON);
if (!status.activeClaims.some((claim) => claim.owner === 'status-owner')) {
  throw new Error('active claim should be visible in status JSON');
}
if (status.availableLanes.some((lane) => lane.lane === status.activeClaims[0].lane)) {
  throw new Error('claimed lane should be excluded from available lanes');
}
NODE

QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$CLAIM_SCRIPT" --release "$FIRST_LANE" --owner status-owner --json >/dev/null

DISPATCH_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$LANE_PLAN_FILE" "$STATUS_SCRIPT" --dispatch-plan dispatch-owner --limit 1 --json)"
DISPATCH_JSON="$DISPATCH_JSON" "$NODE_BIN" <<'NODE'
const plan = JSON.parse(process.env.DISPATCH_JSON);
if (!Array.isArray(plan) || plan.length !== 1) {
  throw new Error('dispatch plan should return one entry');
}
if (plan[0].owner !== 'dispatch-owner-1') {
  throw new Error(`unexpected dispatch owner: ${plan[0].owner}`);
}
if (!Array.isArray(plan[0].claimArgs) || !plan[0].claimArgs.includes('--claim')) {
  throw new Error('dispatch entry must include claimArgs');
}
NODE

cat >"$EMPTY_PLAN_FILE" <<'PLAN'
## Task 1: Previous Lane

Remaining production work after this task: stale lane.

## Task 2: Close Lanes

Remaining production work after this task: None.
PLAN

EMPTY_JSON="$(QUICKBI_CLAIM_FILE="$CLAIM_FILE" QUICKBI_PLAN_FILE="$EMPTY_PLAN_FILE" "$STATUS_SCRIPT" --json)"
EMPTY_JSON="$EMPTY_JSON" "$NODE_BIN" <<'NODE'
const status = JSON.parse(process.env.EMPTY_JSON);
if (status.latestTask.id !== 2) {
  throw new Error(`expected latest task 2, got ${status.latestTask.id}`);
}
if (status.remainingLanes.length !== 0) {
  throw new Error(`expected no remaining lanes, got ${status.remainingLanes.join(', ')}`);
}
if (status.availableLanes.length !== 0) {
  throw new Error('available lanes should be empty when remaining work is none');
}
NODE

echo "quickbi slice status script test passed"
