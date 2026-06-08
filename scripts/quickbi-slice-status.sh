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

const root = process.argv[2];
const args = process.argv.slice(3);
const planPath = process.env.QUICKBI_PLAN_FILE || path.join(root, 'docs/superpowers/plans/2026-06-05-quickbi-platform.md');
const specPath = process.env.QUICKBI_SPEC_FILE || path.join(root, 'docs/superpowers/specs/2026-06-05-quickbi-platform-design.md');
const defaultClaimFile = path.join(root, 'tmp/quickbi-lane-claims.tsv');
const claimFile = process.env.QUICKBI_CLAIM_FILE || defaultClaimFile;
const gates = [
  { scope: 'normalQuickBiSlice', command: 'scripts/verify-quickbi-focus.sh' },
  { scope: 'apiDatasourceExtractOnly', command: 'scripts/verify-quickbi-focus.sh --api-extract-only' },
  { scope: 'broadBackendBiClaim', command: 'scripts/verify-quickbi-focus.sh --backend-all' },
];

let json = false;
let check = false;
let availableOnly = false;
let laneGate = null;
let scopeFilter = null;
let limit = null;
let dispatchOwnerPrefix = null;

function fail(message) {
  console.error(`ERROR: ${message}`);
  process.exit(1);
}

for (let i = 0; i < args.length; i += 1) {
  const arg = args[i];
  if (arg === '--json') json = true;
  else if (arg === '--check') check = true;
  else if (arg === '--available-only') availableOnly = true;
  else if (arg === '--lane-gate') laneGate = args[++i] || fail('--lane-gate requires text');
  else if (arg === '--scope') scopeFilter = args[++i] || fail('--scope requires a value');
  else if (arg === '--limit') limit = Number(args[++i] || fail('--limit requires a value'));
  else if (arg === '--dispatch-plan') dispatchOwnerPrefix = args[++i] || fail('--dispatch-plan requires an owner prefix');
  else if (arg === '--help' || arg === '-h') {
    console.log(`Usage: scripts/quickbi-slice-status.sh [--json] [--check] [--lane-gate TEXT] [--available-only] [--scope SCOPE] [--limit N] [--dispatch-plan OWNER_PREFIX]`);
    process.exit(0);
  } else fail(`unknown argument: ${arg}`);
}

if (!fs.existsSync(planPath)) fail(`QuickBI plan is missing: ${planPath}`);
if (!fs.existsSync(specPath)) fail(`QuickBI spec is missing: ${specPath}`);
if (!fs.existsSync(path.join(root, 'scripts/verify-quickbi-focus.sh'))) {
  fail('QuickBI focused verification gate is missing: scripts/verify-quickbi-focus.sh');
}
if (scopeFilter && !gates.some((gate) => gate.scope === scopeFilter)) fail(`unsupported scope: ${scopeFilter}`);
if (limit !== null && (!Number.isInteger(limit) || limit <= 0)) fail('--limit must be a positive integer');
if (limit !== null && !availableOnly && !dispatchOwnerPrefix) fail('--limit requires --available-only or --dispatch-plan');
if (availableOnly && dispatchOwnerPrefix) fail('--available-only cannot be combined with --dispatch-plan');
if (laneGate && (availableOnly || dispatchOwnerPrefix)) fail('--lane-gate cannot be combined with available or dispatch output');
if (dispatchOwnerPrefix && /[\t\r\n]/.test(dispatchOwnerPrefix)) fail('--dispatch-plan owner prefix cannot contain tabs or newlines');

const plan = fs.readFileSync(planPath, 'utf8');
const taskRegex = /^## Task (\d+):\s*(.+)$/gm;
const tasks = [];
let match;
while ((match = taskRegex.exec(plan)) !== null) {
  tasks.push({ id: Number(match[1]), title: match[2].trim(), index: match.index });
}
if (tasks.length === 0) fail('QuickBI plan has no task records');

const latestTask = tasks.reduce((latest, task) => task.id > latest.id ? task : latest, tasks[0]);
const sortedRecent = [...tasks].sort((a, b) => b.id - a.id).slice(0, 5)
  .map(({ id, title }) => ({ id, title }));
const nextTaskIndex = tasks
  .filter((task) => task.index > latestTask.index)
  .sort((a, b) => a.index - b.index)[0]?.index ?? plan.length;
const latestSection = plan.slice(latestTask.index, nextTaskIndex);
const remainingLine = latestSection.match(/Remaining production work after this task:\s*(.*)/)
  || [...plan.matchAll(/Remaining production work after this task:\s*(.*)/g)].at(-1);
const remainingText = remainingLine ? remainingLine[1].trim().replace(/\.$/, '') : '';
const remainingLanes = remainingText && remainingText.toLowerCase() !== 'none'
  ? remainingText.split(/,\s+(?=(?:and\s+)?[^,]+$)|,\s*/).map((lane) => lane.replace(/^and\s+/, '').trim()).filter(Boolean)
  : [];

function normalizeLane(lane) {
  return String(lane || '').trim().toLowerCase().replace(/\s+/g, ' ');
}

function laneScope(lane) {
  const normalized = normalizeLane(lane);
  if (normalized.includes('api extract') && normalized.includes('materialization')) return 'apiDatasourceExtractOnly';
  if (normalized.includes('quick engine') || normalized.includes('queue') || normalized.includes('worker wakeup')) return 'broadBackendBiClaim';
  return 'normalQuickBiSlice';
}

function gateForLane(lane) {
  const scope = laneScope(lane);
  return { lane, scope, command: gates.find((gate) => gate.scope === scope).command };
}

const laneGateHints = remainingLanes.map(gateForLane);

function readActiveClaims() {
  if (!fs.existsSync(claimFile)) return [];
  const latestByLane = new Map();
  for (const line of fs.readFileSync(claimFile, 'utf8').split(/\r?\n/)) {
    if (!line.trim()) continue;
    const [status, timestamp, owner, lane, scope, command, note = ''] = line.split('\t');
    const entry = { status, timestamp, owner, lane, scope, command, note };
    latestByLane.set(normalizeLane(lane), entry);
  }
  return [...latestByLane.values()].filter((entry) => entry.status === 'active');
}

const activeClaims = readActiveClaims();
const activeLaneSet = new Set(activeClaims.map((claim) => normalizeLane(claim.lane)));
const availableLanes = laneGateHints.filter((lane) => !activeLaneSet.has(normalizeLane(lane.lane)));
const orphanedActiveClaims = activeClaims
  .filter((claim) => !remainingLanes.some((lane) => normalizeLane(lane) === normalizeLane(claim.lane)))
  .map((claim) => ({
    ...claim,
    releaseArgs: ['--release', claim.lane, '--owner', claim.owner],
  }));

function filteredAvailable() {
  let lanes = [...availableLanes];
  if (scopeFilter) lanes = lanes.filter((lane) => lane.scope === scopeFilter);
  if (limit !== null) lanes = lanes.slice(0, limit);
  return lanes;
}

function resolveLaneGate(text) {
  const needle = normalizeLane(text);
  const exact = laneGateHints.find((lane) => normalizeLane(lane.lane) === needle);
  const partial = exact || laneGateHints.find((lane) => normalizeLane(lane.lane).includes(needle));
  if (!partial) fail(`no QuickBI remaining lane matches: ${text}`);
  return partial;
}

if (check) {
  if (remainingLanes.length === 0) fail('QuickBI remaining lanes are empty');
  if (json) {
    console.log(JSON.stringify({ status: 'ok', latestTask: { id: latestTask.id, title: latestTask.title } }));
  } else {
    console.log(`QuickBI status check passed: Task ${latestTask.id}`);
  }
  process.exit(0);
}

if (laneGate) {
  const resolved = resolveLaneGate(laneGate);
  if (json) console.log(JSON.stringify(resolved, null, 2));
  else {
    console.log(`lane=${resolved.lane}`);
    console.log(`scope=${resolved.scope}`);
    console.log(`command=${resolved.command}`);
  }
  process.exit(0);
}

if (dispatchOwnerPrefix) {
  const planRows = filteredAvailable().map((lane, index) => ({
    owner: `${dispatchOwnerPrefix}-${index + 1}`,
    lane: lane.lane,
    scope: lane.scope,
    command: lane.command,
    claimArgs: ['--claim', lane.lane, '--owner', `${dispatchOwnerPrefix}-${index + 1}`],
  }));
  if (json) console.log(JSON.stringify(planRows, null, 2));
  else planRows.forEach((row) => console.log(`${row.owner}\t${row.scope}\t${row.lane}\t${row.command}`));
  process.exit(0);
}

if (availableOnly) {
  const lanes = filteredAvailable();
  if (json) console.log(JSON.stringify(lanes, null, 2));
  else lanes.forEach((lane) => console.log(`${lane.scope}\t${lane.lane}\t${lane.command}`));
  process.exit(0);
}

const status = {
  planPath: path.relative(root, planPath),
  specPath: path.relative(root, specPath),
  latestTask: { id: latestTask.id, title: latestTask.title },
  gates,
  recentTasks: sortedRecent,
  remainingLanes,
  laneGateHints,
  activeClaims,
  availableLanes,
  orphanedActiveClaims,
};

if (json) {
  console.log(JSON.stringify(status, null, 2));
} else {
  console.log(`Latest QuickBI task: ${latestTask.id} ${latestTask.title}`);
  console.log('Remaining lanes:');
  remainingLanes.forEach((lane) => console.log(`- ${lane}`));
  console.log('Active local lane claims:');
  activeClaims.length ? activeClaims.forEach((claim) => console.log(`- ${claim.owner}: ${claim.lane}`)) : console.log('- none');
  console.log('Available unclaimed lanes:');
  availableLanes.length ? availableLanes.forEach((lane) => console.log(`- [${lane.scope}] ${lane.lane}`)) : console.log('- none');
  if (orphanedActiveClaims.length) {
    console.log('Active claims outside remaining lanes:');
    orphanedActiveClaims.forEach((claim) => console.log(`- ${claim.owner}: ${claim.lane}`));
  }
}
NODE
