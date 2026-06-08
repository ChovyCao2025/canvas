#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NODE_BIN="${NODE_BIN:-/opt/homebrew/bin/node}"

if [[ ! -x "$NODE_BIN" ]]; then
  NODE_BIN="$(command -v node)"
fi

"$NODE_BIN" - "$ROOT_DIR" "$@" <<'NODE'
const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const root = process.argv[2];
const args = process.argv.slice(3);
const statusScript = path.join(root, 'scripts/quickbi-slice-status.sh');
const claimFile = process.env.QUICKBI_CLAIM_FILE || path.join(root, 'tmp/quickbi-lane-claims.tsv');
const gateCommands = {
  normalQuickBiSlice: 'scripts/verify-quickbi-focus.sh',
  apiDatasourceExtractOnly: 'scripts/verify-quickbi-focus.sh --api-extract-only',
  broadBackendBiClaim: 'scripts/verify-quickbi-focus.sh --backend-all',
};

let mode = null;
let lane = null;
let owner = null;
let note = '';
let json = false;
let dryRun = false;
let scope = null;

function fail(message) {
  console.error(`ERROR: ${message}`);
  process.exit(1);
}

for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === '--claim') { mode = 'claim'; lane = args[++i] || fail('--claim requires lane text'); }
  else if (arg === '--release') { mode = 'release'; lane = args[++i] || fail('--release requires lane text'); }
  else if (arg === '--claim-next') mode = 'claimNext';
  else if (arg === '--release-orphaned') mode = 'releaseOrphaned';
  else if (arg === '--list') mode = 'list';
  else if (arg === '--owner') owner = args[++i] || fail('--owner requires a value');
  else if (arg === '--note') note = args[++i] || '';
  else if (arg === '--json') json = true;
  else if (arg === '--dry-run') dryRun = true;
  else if (arg === '--scope') scope = args[++i] || fail('--scope requires a value');
  else if (arg === '--stale-minutes') { i += 1; }
  else if (arg === '--help' || arg === '-h') {
    console.log('Usage: scripts/quickbi-claim-lane.sh --claim LANE|--claim-next|--release LANE|--release-orphaned|--list --owner OWNER [--json] [--dry-run]');
    process.exit(0);
  } else fail(`unknown argument: ${arg}`);
}

if (!mode) fail('claim mode is required');
if (scope && !Object.hasOwn(gateCommands, scope)) fail(`unsupported scope: ${scope}`);
if (scope && mode !== 'claimNext') fail('--scope is only supported with --claim-next');
if (['claim', 'release', 'claimNext'].includes(mode) && !owner) fail('--owner is required');
if (owner && /[\t\r\n]/.test(owner)) fail('--owner cannot contain tabs or newlines');
if (note && /[\t\r\n]/.test(note)) fail('--note cannot contain tabs or newlines');

function normalizeLane(value) {
  return String(value || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

function readStatus(extraArgs = []) {
  const result = spawnSync(statusScript, ['--json', ...extraArgs], {
    cwd: root,
    env: process.env,
    encoding: 'utf8',
  });
  if (result.status !== 0) fail((result.stderr || result.stdout || 'failed to read QuickBI status').trim());
  return JSON.parse(result.stdout);
}

function readEntries() {
  if (!fs.existsSync(claimFile)) return [];
  return fs.readFileSync(claimFile, 'utf8').split(/\r?\n/).filter(Boolean).map((line) => {
    const [status, timestamp, owner, lane, scope, command, note = ''] = line.split('\t');
    return { status, timestamp, owner, lane, scope, command, note };
  });
}

function activeClaims() {
  const latest = new Map();
  for (const entry of readEntries()) latest.set(normalizeLane(entry.lane), entry);
  return [...latest.values()].filter((entry) => entry.status === 'active');
}

function appendEntry(entry) {
  if (dryRun) return;
  fs.mkdirSync(path.dirname(claimFile), { recursive: true });
  fs.appendFileSync(claimFile, [
    entry.status,
    entry.timestamp,
    entry.owner,
    entry.lane,
    entry.scope,
    entry.command,
    entry.note || '',
  ].join('\t') + '\n');
}

function fallbackGate(targetLane) {
  const normalized = normalizeLane(targetLane);
  const inferredScope = normalized.includes('quick engine') || normalized.includes('queue') || normalized.includes('worker wakeup')
    ? 'broadBackendBiClaim'
    : normalized.includes('api extract') && normalized.includes('materialization')
      ? 'apiDatasourceExtractOnly'
      : 'normalQuickBiSlice';
  return { lane: targetLane, scope: inferredScope, command: gateCommands[inferredScope] };
}

function resolveGate(targetLane) {
  const status = readStatus();
  const found = status.laneGateHints.find((hint) => normalizeLane(hint.lane) === normalizeLane(targetLane))
    || status.laneGateHints.find((hint) => normalizeLane(hint.lane).includes(normalizeLane(targetLane)));
  return found || fallbackGate(targetLane);
}

function claimLane(targetLane, targetOwner, targetNote, statusValue = 'active') {
  const resolved = resolveGate(targetLane);
  const active = activeClaims().find((claim) => normalizeLane(claim.lane) === normalizeLane(resolved.lane));
  if (statusValue === 'active' && active && active.owner !== targetOwner) {
    fail(`QuickBI lane is already claimed by ${active.owner}: ${resolved.lane}`);
  }
  const entry = {
    status: dryRun ? 'preview' : statusValue,
    timestamp: new Date().toISOString(),
    owner: targetOwner,
    lane: resolved.lane,
    scope: resolved.scope,
    command: resolved.command,
    note: targetNote,
  };
  if (statusValue === 'active') appendEntry({ ...entry, status: 'active' });
  return entry;
}

function releaseLane(targetLane, targetOwner) {
  const active = activeClaims().find((claim) => normalizeLane(claim.lane) === normalizeLane(targetLane));
  if (!active) fail(`QuickBI lane is not actively claimed: ${targetLane}`);
  if (active.owner !== targetOwner) fail(`QuickBI lane is claimed by ${active.owner}, not ${targetOwner}`);
  const entry = { ...active, status: 'released', timestamp: new Date().toISOString() };
  appendEntry(entry);
  return entry;
}

function output(value) {
  if (json) console.log(JSON.stringify(value, null, 2));
  else if (Array.isArray(value)) value.forEach((entry) => console.log(`${entry.status}\t${entry.owner}\t${entry.lane}`));
  else console.log(`${value.status}\t${value.owner}\t${value.lane}\t${value.command}`);
}

if (mode === 'list') {
  output(activeClaims());
} else if (mode === 'claim') {
  output(claimLane(lane, owner, note));
} else if (mode === 'release') {
  output(releaseLane(lane, owner));
} else if (mode === 'claimNext') {
  const status = readStatus();
  let lanes = status.availableLanes;
  if (scope) lanes = lanes.filter((candidate) => candidate.scope === scope);
  if (lanes.length === 0) fail('no unclaimed QuickBI remaining-work lanes are available');
  output(claimLane(lanes[0].lane, owner, note));
} else if (mode === 'releaseOrphaned') {
  const status = readStatus();
  const released = status.orphanedActiveClaims.map((claim) => releaseLane(claim.lane, claim.owner));
  output(released);
}
NODE
