#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
TENANT_ID="${TENANT_ID:-}"
JWT_TOKEN="${JWT_TOKEN:-}"
CANVAS_ID="${CANVAS_ID:-}"
LARK_INSTANCE_CODE="${LARK_INSTANCE_CODE:-}"
ENV_FILE=""
EVIDENCE_DIR="${EVIDENCE_DIR:-}"
EXPECTED_APPROVERS="${EXPECTED_APPROVERS:-}"
SUBMITTER_USERNAME="${SUBMITTER_USERNAME:-}"
PREFLIGHT_FILE=""
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-${MYSQL_DATABASE:-canvas}}"
DB_USER="${DB_USER:-${MYSQL_USER:-root}}"
DB_PASSWORD="${DB_PASSWORD:-${MYSQL_PASSWORD:-}}"
SKIP_LARK_SCHEMA="${SKIP_LARK_SCHEMA:-false}"
VERIFY_LARK_INSTANCE="${VERIFY_LARK_INSTANCE:-false}"
SUBMIT_REVIEW="${SUBMIT_REVIEW:-false}"
SYNC_AFTER_SUBMIT="${SYNC_AFTER_SUBMIT:-false}"
APPROVE_TASK="${APPROVE_TASK:-false}"
REJECT_TASK="${REJECT_TASK:-false}"
TASK_ID="${TASK_ID:-}"
DECISION_COMMENT="${DECISION_COMMENT:-live verification}"

usage() {
  cat <<'USAGE'
Verify Canvas Feishu/Lark approval integration readiness and optional live flow.

Default behavior is non-destructive preflight:
  - validates lark-cli approval schemas unless SKIP_LARK_SCHEMA=true
  - validates Canvas DB approval definition and Lark user mappings

Required for preflight:
  TENANT_ID

DB mode environment:
  DB_HOST       MySQL host, default 127.0.0.1
  DB_PORT       MySQL port, default 3306
  DB_NAME       Database name, default canvas
  DB_USER       MySQL user, default root
  DB_PASSWORD   MySQL password, optional
  SUBMITTER_USERNAME  Canvas username that must have lark_open_id or lark_user_id; required when SUBMIT_REVIEW=true.
  EXPECTED_APPROVERS  Optional comma-separated Canvas usernames that must have lark_open_id.
                      When CANVAS_ID is set in DB mode, project admins are checked automatically.

Fixture mode:
  --preflight-file <json>

Environment file:
  --env-file <path>
    Sources a shell env file before validation. See scripts/lark-approval-live.env.example.

Evidence output:
  --evidence-dir <dir>
    Writes non-secret verification artifacts and summary JSON under a timestamped run directory.

JSON fixture shape:
  {
    "definitions": [
      {
        "tenant_id": 7,
        "definition_key": "CANVAS_PUBLISH_DEFAULT",
        "external_provider": "LARK",
        "external_definition_code": "approval-code"
      }
    ],
    "identities": [
      {
        "tenant_id": 7,
        "username": "alice",
        "lark_open_id": "ou_x",
        "lark_user_id": "u_x",
        "lark_department_id": "od_x"
      }
    ],
    "required_submitter": "alice",
    "required_approvers": ["bob", "tenant_admin"]
  }

Optional live HTTP checks:
  SUBMIT_REVIEW=true requires JWT_TOKEN, SUBMITTER_USERNAME, and CANVAS_ID, then POSTs /canvas/{id}/submit-review
    and verifies Lark external instance/task bindings in the response.
    In DB mode it also verifies persisted approval_instance/approval_task bindings.
  VERIFY_LARK_INSTANCE=true reads the submitted Lark instance with lark-cli, or LARK_INSTANCE_CODE if set.
  SYNC_AFTER_SUBMIT=true additionally POSTs /approvals/external/lark/sync?limit=20
  APPROVE_TASK=true POSTs /approvals/tasks/{taskId}/approve
  REJECT_TASK=true POSTs /approvals/tasks/{taskId}/reject
    TASK_ID is required unless SUBMIT_REVIEW=true returns a bound pending task.

Examples:
  TENANT_ID=7 scripts/verify-lark-approval-live.sh

  TENANT_ID=7 \
  JWT_TOKEN="$TOKEN" \
  SUBMITTER_USERNAME=alice \
  CANVAS_ID=62 \
  SUBMIT_REVIEW=true \
  SYNC_AFTER_SUBMIT=true \
  scripts/verify-lark-approval-live.sh
USAGE
}

fail_usage() {
  echo "ERROR: $*" >&2
  usage >&2
  exit 2
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 2
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env-file)
      [[ $# -ge 2 ]] || fail_usage "--env-file requires a path"
      ENV_FILE="$2"
      [[ -f "$ENV_FILE" ]] || fail_usage "env file does not exist: $ENV_FILE"
      # shellcheck source=/dev/null
      source "$ENV_FILE"
      shift 2
      ;;
    --evidence-dir)
      [[ $# -ge 2 ]] || fail_usage "--evidence-dir requires a path"
      EVIDENCE_DIR="$2"
      shift 2
      ;;
    --preflight-file)
      [[ $# -ge 2 ]] || fail_usage "--preflight-file requires a path"
      PREFLIGHT_FILE="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail_usage "unknown argument: $1"
      ;;
  esac
done

[[ -n "$TENANT_ID" ]] || fail_usage "TENANT_ID is required"
if ! [[ "$TENANT_ID" =~ ^[0-9]+$ ]] || [[ "$TENANT_ID" -le 0 ]]; then
  fail_usage "TENANT_ID must be a positive integer"
fi

require_command node
require_command curl

if [[ "$SKIP_LARK_SCHEMA" != "true" ]]; then
  require_command lark-cli
  echo "Checking Lark approval schemas"
  lark-cli schema approval.instances.get --format json >/dev/null
  lark-cli schema approval.tasks.query --format json >/dev/null
  lark-cli schema approval.tasks.approve --format json >/dev/null
  lark-cli schema approval.tasks.reject --format json >/dev/null
fi

tmpdir="$(mktemp -d)"

evidence_run_dir=""
if [[ -n "$EVIDENCE_DIR" ]]; then
  mkdir -p "$EVIDENCE_DIR"
  evidence_run_dir="$EVIDENCE_DIR/lark-approval-$(date -u +%Y%m%dT%H%M%SZ)-$$"
  mkdir -p "$evidence_run_dir"
fi

copy_evidence() {
  local src="$1"
  local name="$2"
  if [[ -n "$evidence_run_dir" && -f "$src" ]]; then
    cp "$src" "$evidence_run_dir/$name"
  fi
}

write_evidence_summary() {
  local exit_code="${1:-0}"
  if [[ -z "$evidence_run_dir" ]]; then
    return
  fi
  node - "$evidence_run_dir/summary.json" \
    "$exit_code" \
    "$TENANT_ID" \
    "$API_BASE" \
    "$SUBMIT_REVIEW" \
    "$VERIFY_LARK_INSTANCE" \
    "$SYNC_AFTER_SUBMIT" \
    "$APPROVE_TASK" \
    "$REJECT_TASK" \
    "$CANVAS_ID" \
    "$LARK_INSTANCE_CODE" <<'NODE'
const fs = require('fs')
const pathModule = require('path')
const [
  outputPath,
  exitCodeRaw,
  tenantId,
  apiBase,
  submitReview,
  verifyLarkInstance,
  syncAfterSubmit,
  approveTask,
  rejectTask,
  canvasId,
  larkInstanceCode
] = process.argv.slice(2)

function enabled(value) {
  return value === 'true'
}

const exitCode = Number(exitCodeRaw)
const evidenceDir = pathModule.dirname(outputPath)

function readJson(name) {
  const file = pathModule.join(evidenceDir, name)
  if (!fs.existsSync(file)) return null
  return JSON.parse(fs.readFileSync(file, 'utf8'))
}

function readText(name) {
  const file = pathModule.join(evidenceDir, name)
  if (!fs.existsSync(file)) return ''
  return fs.readFileSync(file, 'utf8').trim()
}

function dataOf(payload) {
  if (!payload || typeof payload !== 'object') return payload
  return Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload
}

function larkTasks(data) {
  if (!data || typeof data !== 'object') return []
  if (Array.isArray(data.tasks)) return data.tasks
  if (Array.isArray(data.task_list)) return data.task_list
  return []
}

function summarizeSubmit() {
  const payload = dataOf(readJson('submit-response.json'))
  if (!payload || typeof payload !== 'object') return null
  const pendingTasks = Array.isArray(payload.pendingTasks) ? payload.pendingTasks : []
  return {
    approvalInstanceId: payload.id ?? null,
    status: payload.status ?? null,
    externalInstanceId: payload.externalInstanceId ?? null,
    pendingTaskCount: pendingTasks.length,
    externalTaskIds: pendingTasks
      .map((task) => task && task.externalTaskId)
      .filter(Boolean)
      .map(String)
  }
}

function summarizeSubmitDbBinding() {
  const line = readText('submit-db-binding.tsv')
  if (!line) return null
  const [externalInstanceId, taskCount, unboundTaskCount] = line.split('\t')
  return {
    externalInstanceId: externalInstanceId || null,
    taskCount: Number(taskCount),
    unboundTaskCount: Number(unboundTaskCount)
  }
}

function summarizeLarkInstance(name) {
  const payload = readJson(name)
  const data = dataOf(payload)
  if (!data || typeof data !== 'object') return null
  const tasks = larkTasks(data)
  return {
    instanceCode: data.instance_code || data.instanceCode || null,
    status: data.status || null,
    taskCount: tasks.length,
    taskIds: tasks
      .map((task) => task && (task.id || task.task_id))
      .filter(Boolean)
      .map(String),
    taskStatuses: tasks
      .map((task) => {
        const taskId = task && (task.id || task.task_id)
        return taskId ? { taskId: String(taskId), status: task.status || null } : null
      })
      .filter(Boolean)
  }
}

function summarizeSync() {
  const payload = readJson('sync-response.json')
  if (payload === null) return null
  return { changedCount: dataOf(payload) }
}

function summarizeDecision() {
  const payload = dataOf(readJson('decision-response.json'))
  if (!payload || typeof payload !== 'object') return null
  return {
    approvalInstanceId: payload.id ?? null,
    status: payload.status ?? null
  }
}

function summarizeDecisionDbBinding() {
  const line = readText('decision-db-binding.tsv')
  if (!line) return null
  const [taskStatus, externalTaskId, instanceStatus, externalInstanceId] = line.split('\t')
  return {
    taskStatus: taskStatus || null,
    externalTaskId: externalTaskId || null,
    instanceStatus: instanceStatus || null,
    externalInstanceId: externalInstanceId || null
  }
}

const preflight = readJson('preflight.json')

fs.writeFileSync(outputPath, JSON.stringify({
  verifiedAt: new Date().toISOString(),
  completed: exitCode === 0,
  exitCode,
  tenantId: Number(tenantId),
  apiBase,
  canvasId: canvasId || null,
  larkInstanceCode: larkInstanceCode || null,
  checks: {
    preflight: true,
    submitReview: enabled(submitReview),
    verifyLarkInstance: enabled(verifyLarkInstance),
    syncAfterSubmit: enabled(syncAfterSubmit),
    approveTask: enabled(approveTask),
    rejectTask: enabled(rejectTask)
  },
  artifacts: fs.readdirSync(evidenceDir)
    .filter((name) => name !== 'summary.json')
    .sort(),
  preflight: preflight
    ? {
        definitionCount: Array.isArray(preflight.definitions) ? preflight.definitions.length : 0,
        identityCount: Array.isArray(preflight.identities) ? preflight.identities.length : 0,
        requiredSubmitter: preflight.required_submitter || null,
        requiredApprovers: Array.isArray(preflight.required_approvers) ? preflight.required_approvers : []
      }
    : null,
  submit: summarizeSubmit(),
  submitDbBinding: summarizeSubmitDbBinding(),
  larkInstance: summarizeLarkInstance('lark-instance-get-response.json'),
  sync: summarizeSync(),
  decision: summarizeDecision(),
  decisionDbBinding: summarizeDecisionDbBinding(),
  decisionLarkInstance: summarizeLarkInstance('decision-lark-instance-get-response.json'),
  secretsRecorded: false
}, null, 2))
NODE
}

finish() {
  local exit_code=$?
  write_evidence_summary "$exit_code" || true
  if [[ -n "$evidence_run_dir" ]]; then
    echo "Evidence written to $evidence_run_dir"
  fi
  rm -rf "$tmpdir"
  if [[ "$exit_code" -eq 0 ]]; then
    echo "Lark approval verification completed"
  fi
  exit "$exit_code"
}

trap finish EXIT

preflight_json="$PREFLIGHT_FILE"

if [[ -z "$preflight_json" ]]; then
  require_command mysql
  definitions_tsv="$tmpdir/definitions.tsv"
  identities_tsv="$tmpdir/identities.tsv"
  required_approvers_tsv="$tmpdir/required-approvers.tsv"
  preflight_json="$tmpdir/preflight.json"

  MYSQL_PWD="$DB_PASSWORD" mysql \
    --batch \
    --raw \
    --skip-column-names \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    "$DB_NAME" \
    --execute "SELECT tenant_id, definition_key, external_provider, COALESCE(external_definition_code, '') FROM approval_definition WHERE tenant_id IN (0, ${TENANT_ID}) AND definition_key = 'CANVAS_PUBLISH_DEFAULT' ORDER BY tenant_id DESC;" \
    > "$definitions_tsv"

  MYSQL_PWD="$DB_PASSWORD" mysql \
    --batch \
    --raw \
    --skip-column-names \
    --host="$DB_HOST" \
    --port="$DB_PORT" \
    --user="$DB_USER" \
    "$DB_NAME" \
    --execute "SELECT tenant_id, username, COALESCE(lark_open_id, ''), COALESCE(lark_user_id, ''), COALESCE(lark_department_id, '') FROM approval_lark_user_identity WHERE tenant_id = ${TENANT_ID} ORDER BY username;" \
    > "$identities_tsv"

  : > "$required_approvers_tsv"
  if [[ -n "$CANVAS_ID" ]]; then
    if ! [[ "$CANVAS_ID" =~ ^[0-9]+$ ]] || [[ "$CANVAS_ID" -le 0 ]]; then
      fail_usage "CANVAS_ID must be a positive integer when used for DB preflight"
    fi
    MYSQL_PWD="$DB_PASSWORD" mysql \
      --batch \
      --raw \
      --skip-column-names \
      --host="$DB_HOST" \
      --port="$DB_PORT" \
      --user="$DB_USER" \
      "$DB_NAME" \
      --execute "SELECT DISTINCT pm.username FROM canvas_project_folder pf JOIN canvas_project_member pm ON pm.tenant_id = pf.tenant_id AND pm.project_id = pf.project_id WHERE pf.tenant_id = ${TENANT_ID} AND pf.canvas_id = ${CANVAS_ID} AND pm.role = 'PROJECT_ADMIN' AND COALESCE(pm.username, '') <> '' ORDER BY pm.username;" \
      > "$required_approvers_tsv"
  fi

  node - "$definitions_tsv" "$identities_tsv" "$required_approvers_tsv" "$EXPECTED_APPROVERS" "$SUBMITTER_USERNAME" "$CANVAS_ID" "$preflight_json" <<'NODE'
const fs = require('fs')
const [
  definitionsPath,
  identitiesPath,
  requiredApproversPath,
  expectedApproversRaw,
  submitterUsername,
  canvasId,
  outputPath
] = process.argv.slice(2)

const definitions = fs.readFileSync(definitionsPath, 'utf8')
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line) => {
    const [tenantId, definitionKey, externalProvider, externalDefinitionCode] = line.split('\t')
    return {
      tenant_id: Number(tenantId),
      definition_key: definitionKey,
      external_provider: externalProvider,
      external_definition_code: externalDefinitionCode
    }
  })

const identities = fs.readFileSync(identitiesPath, 'utf8')
  .split(/\r?\n/)
  .filter(Boolean)
  .map((line) => {
    const [tenantId, username, openId, userId, departmentId] = line.split('\t')
    return {
      tenant_id: Number(tenantId),
      username,
      lark_open_id: openId,
      lark_user_id: userId,
      lark_department_id: departmentId
    }
  })

const requiredApprovers = new Set()
for (const line of fs.readFileSync(requiredApproversPath, 'utf8').split(/\r?\n/).filter(Boolean)) {
  requiredApprovers.add(line.trim())
}
for (const value of String(expectedApproversRaw || '').split(',')) {
  const approver = value.trim()
  if (approver) requiredApprovers.add(approver)
}
if (requiredApprovers.size === 0 && String(canvasId || '').trim()) {
  requiredApprovers.add('tenant_admin')
}

fs.writeFileSync(outputPath, JSON.stringify({
  definitions,
  identities,
  required_submitter: String(submitterUsername || '').trim() || undefined,
  required_approvers: [...requiredApprovers]
}, null, 2))
NODE
fi

[[ -f "$preflight_json" ]] || fail_usage "preflight file does not exist: $preflight_json"
copy_evidence "$preflight_json" "preflight.json"

echo "Checking Canvas Lark approval preflight data"
node - "$preflight_json" "$TENANT_ID" "$EXPECTED_APPROVERS" "$SUBMITTER_USERNAME" <<'NODE'
const fs = require('fs')
const [path, tenantIdRaw, expectedApproversRaw, submitterUsernameRaw] = process.argv.slice(2)
const tenantId = Number(tenantIdRaw)
const payload = JSON.parse(fs.readFileSync(path, 'utf8'))
const definitions = Array.isArray(payload.definitions) ? payload.definitions : []
const identities = Array.isArray(payload.identities) ? payload.identities : []
const requiredSubmitter = text(submitterUsernameRaw) || text(payload.required_submitter)
const requiredApprovers = Array.isArray(payload.required_approvers) ? [...payload.required_approvers] : []
for (const value of String(expectedApproversRaw || '').split(',')) {
  const approver = text(value)
  if (approver) requiredApprovers.push(approver)
}

function text(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

const relevantDefinitions = definitions
  .filter((row) => Number(row.tenant_id) === tenantId || Number(row.tenant_id) === 0)
  .filter((row) => text(row.definition_key) === 'CANVAS_PUBLISH_DEFAULT')
  .sort((a, b) => Number(b.tenant_id) - Number(a.tenant_id))

if (relevantDefinitions.length === 0) {
  throw new Error(`missing CANVAS_PUBLISH_DEFAULT approval_definition for tenant ${tenantId} or fallback tenant 0`)
}

const activeDefinition = relevantDefinitions[0]
if (text(activeDefinition.external_provider).toUpperCase() !== 'LARK') {
  throw new Error(`CANVAS_PUBLISH_DEFAULT is not LARK-backed; got ${activeDefinition.external_provider || '<blank>'}`)
}
if (!text(activeDefinition.external_definition_code)) {
  throw new Error('CANVAS_PUBLISH_DEFAULT external_definition_code is required for Lark instance creation')
}

const tenantIdentities = identities.filter((row) => Number(row.tenant_id) === tenantId)
const identitiesWithSubmitterId = tenantIdentities.filter((row) => text(row.lark_open_id) || text(row.lark_user_id))
const identitiesWithOpenId = tenantIdentities.filter((row) => text(row.lark_open_id))

if (identitiesWithSubmitterId.length === 0) {
  throw new Error(`tenant ${tenantId} has no approval_lark_user_identity rows with lark_open_id or lark_user_id`)
}
if (identitiesWithOpenId.length === 0) {
  throw new Error(`tenant ${tenantId} has no approval_lark_user_identity rows with lark_open_id for task binding`)
}

const duplicateOpenIds = new Map()
for (const row of identitiesWithOpenId) {
  const openId = text(row.lark_open_id)
  duplicateOpenIds.set(openId, (duplicateOpenIds.get(openId) || 0) + 1)
}
const duplicates = [...duplicateOpenIds.entries()].filter(([, count]) => count > 1)
if (duplicates.length > 0) {
  throw new Error(`duplicate lark_open_id mappings are unsafe for task binding: ${duplicates.map(([openId]) => openId).join(', ')}`)
}

if (requiredSubmitter) {
  const submitterIdentity = identitiesWithSubmitterId
    .find((row) => text(row.username).toLowerCase() === requiredSubmitter.toLowerCase())
  if (!submitterIdentity) {
    throw new Error(`required submitter is missing lark_open_id or lark_user_id mapping: ${requiredSubmitter}`)
  }
}

const openIdsByUsername = new Map()
for (const row of identitiesWithOpenId) {
  openIdsByUsername.set(text(row.username).toLowerCase(), text(row.lark_open_id))
}
const missingApproverMappings = requiredApprovers
  .map(text)
  .filter(Boolean)
  .filter((approver) => !openIdsByUsername.has(approver.toLowerCase()))
if (missingApproverMappings.length > 0) {
  throw new Error(`required approvers are missing lark_open_id mappings: ${missingApproverMappings.join(', ')}`)
}

console.log(JSON.stringify({
  activeDefinitionTenantId: Number(activeDefinition.tenant_id),
  externalDefinitionCode: text(activeDefinition.external_definition_code),
  mappedUsers: tenantIdentities.length,
  taskBindableUsers: identitiesWithOpenId.length,
  checkedSubmitter: requiredSubmitter || null,
  checkedRequiredApprovers: requiredApprovers.length
}, null, 2))
NODE

submit_response="$tmpdir/submit-response.json"
submit_instance_id_file="$tmpdir/submit-instance-id.txt"
submit_external_instance_id_file="$tmpdir/submit-external-instance-id.txt"
submit_task_id_file="$tmpdir/submit-task-id.txt"
submit_external_task_ids_file="$tmpdir/submit-external-task-ids.json"
submit_external_task_ids_by_local_task_id_file="$tmpdir/submit-external-task-ids-by-local-task-id.json"
sync_response="$tmpdir/sync-response.json"
decision_response="$tmpdir/decision-response.json"
decision_external_task_id_file="$tmpdir/decision-external-task-id.txt"
decision_external_instance_id_file="$tmpdir/decision-external-instance-id.txt"

if [[ "$SUBMIT_REVIEW" == "true" ]]; then
  [[ -n "$JWT_TOKEN" ]] || fail_usage "JWT_TOKEN is required when SUBMIT_REVIEW=true"
  [[ -n "$SUBMITTER_USERNAME" ]] || fail_usage "SUBMITTER_USERNAME is required when SUBMIT_REVIEW=true so preflight can verify the JWT user's Lark identity"
  [[ -n "$CANVAS_ID" ]] || fail_usage "CANVAS_ID is required when SUBMIT_REVIEW=true"

  echo "Submitting Canvas publish review for canvas $CANVAS_ID"
  curl -fsS \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -o "$submit_response" \
    -d '{"reason":"Lark approval live verification"}' \
    "$API_BASE/canvas/$CANVAS_ID/submit-review"
  copy_evidence "$submit_response" "submit-response.json"

  node - "$submit_response" "$submit_instance_id_file" "$submit_external_instance_id_file" "$submit_task_id_file" "$submit_external_task_ids_file" "$submit_external_task_ids_by_local_task_id_file" <<'NODE'
const fs = require('fs')
const [
  responsePath,
  idPath,
  externalInstanceIdPath,
  taskIdPath,
  externalTaskIdsPath,
  externalTaskIdsByLocalTaskIdPath
] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(responsePath, 'utf8'))
const data = response.data || response
if (!data.id) throw new Error(`submit-review response missing approval instance id: ${JSON.stringify(response)}`)
if (!Number.isInteger(Number(data.id)) || Number(data.id) <= 0) {
  throw new Error(`submit-review response approval instance id must be a positive integer: ${data.id}`)
}
if (data.status !== 'PENDING') throw new Error(`submit-review expected PENDING status, got ${data.status}`)
if (!data.externalInstanceId) throw new Error(`submit-review response missing Lark externalInstanceId: ${JSON.stringify(response)}`)
const pendingTasks = Array.isArray(data.pendingTasks) ? data.pendingTasks : []
if (pendingTasks.length === 0) throw new Error(`submit-review response missing pendingTasks: ${JSON.stringify(response)}`)
const unboundTasks = pendingTasks.filter((task) => !task.externalTaskId)
if (unboundTasks.length > 0) {
  throw new Error(`submit-review response has pending tasks without externalTaskId: ${JSON.stringify(unboundTasks)}`)
}
const externalTaskIds = pendingTasks.map((task) => String(task.externalTaskId).trim()).filter(Boolean)
const externalTaskIdsByLocalTaskId = {}
for (const task of pendingTasks) {
  if (task.id && task.externalTaskId) {
    externalTaskIdsByLocalTaskId[String(task.id)] = String(task.externalTaskId).trim()
  }
}
const firstTaskId = pendingTasks[0] && pendingTasks[0].id
if (!Number.isInteger(Number(firstTaskId)) || Number(firstTaskId) <= 0) {
  throw new Error(`submit-review response first pending task id must be a positive integer: ${firstTaskId}`)
}
fs.writeFileSync(idPath, String(data.id))
fs.writeFileSync(externalInstanceIdPath, String(data.externalInstanceId))
fs.writeFileSync(taskIdPath, String(firstTaskId))
fs.writeFileSync(externalTaskIdsPath, JSON.stringify(externalTaskIds))
fs.writeFileSync(externalTaskIdsByLocalTaskIdPath, JSON.stringify(externalTaskIdsByLocalTaskId))
console.log(JSON.stringify({
  approvalInstanceId: data.id,
  firstPendingTaskId: firstTaskId,
  status: data.status,
  externalInstanceId: data.externalInstanceId,
  boundPendingTasks: pendingTasks.length,
  externalTaskIds,
  mappedExternalTaskIds: Object.keys(externalTaskIdsByLocalTaskId).length
}, null, 2))
NODE

  if [[ -z "$PREFLIGHT_FILE" ]]; then
    approval_instance_id="$(cat "$submit_instance_id_file")"
    binding_tsv="$tmpdir/submit-binding.tsv"
    MYSQL_PWD="$DB_PASSWORD" mysql \
      --batch \
      --raw \
      --skip-column-names \
      --host="$DB_HOST" \
      --port="$DB_PORT" \
      --user="$DB_USER" \
      "$DB_NAME" \
      --execute "SELECT COALESCE(i.external_instance_id, ''), COUNT(t.id), SUM(CASE WHEN COALESCE(t.external_task_id, '') = '' THEN 1 ELSE 0 END) FROM approval_instance i LEFT JOIN approval_task t ON t.instance_id = i.id AND t.tenant_id = i.tenant_id WHERE i.tenant_id = ${TENANT_ID} AND i.id = ${approval_instance_id} GROUP BY i.id, i.external_instance_id;" \
      > "$binding_tsv"
    copy_evidence "$binding_tsv" "submit-db-binding.tsv"
    node - "$binding_tsv" "$approval_instance_id" <<'NODE'
const fs = require('fs')
const [path, approvalInstanceId] = process.argv.slice(2)
const line = fs.readFileSync(path, 'utf8').trim()
if (!line) throw new Error(`approval instance ${approvalInstanceId} was not found in DB`)
const [externalInstanceId, taskCountRaw, unboundCountRaw] = line.split('\t')
const taskCount = Number(taskCountRaw)
const unboundCount = Number(unboundCountRaw)
if (!externalInstanceId) throw new Error(`approval instance ${approvalInstanceId} has no persisted external_instance_id`)
if (!Number.isFinite(taskCount) || taskCount <= 0) throw new Error(`approval instance ${approvalInstanceId} has no persisted tasks`)
if (!Number.isFinite(unboundCount) || unboundCount > 0) {
  throw new Error(`approval instance ${approvalInstanceId} has ${unboundCountRaw} persisted tasks without external_task_id`)
}
console.log(JSON.stringify({
  persistedExternalInstanceId: externalInstanceId,
  persistedBoundTasks: taskCount
}, null, 2))
NODE
  fi
fi

if [[ "$VERIFY_LARK_INSTANCE" == "true" ]]; then
  require_command lark-cli
  instance_code="$LARK_INSTANCE_CODE"
  if [[ -z "$instance_code" && -f "$submit_external_instance_id_file" ]]; then
    instance_code="$(cat "$submit_external_instance_id_file")"
  fi
  [[ -n "$instance_code" ]] || fail_usage "LARK_INSTANCE_CODE is required for VERIFY_LARK_INSTANCE=true unless SUBMIT_REVIEW=true returns externalInstanceId"

  echo "Reading Lark approval instance $instance_code"
  lark_instance_params="$tmpdir/lark-instance-params.json"
  lark_instance_response="$tmpdir/lark-instance-response.json"
  node - "$lark_instance_params" "$instance_code" <<'NODE'
const fs = require('fs')
const [path, instanceCode] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  instance_code: instanceCode,
  user_id_type: 'open_id'
}))
NODE
  lark-cli approval instances get \
    --as user \
    --params @"$lark_instance_params" \
    --format json \
    > "$lark_instance_response"
  copy_evidence "$lark_instance_params" "lark-instance-get-params.json"
  copy_evidence "$lark_instance_response" "lark-instance-get-response.json"
  external_task_ids_path=""
  if [[ -f "$submit_external_task_ids_file" ]]; then
    external_task_ids_path="$submit_external_task_ids_file"
  fi
  node - "$lark_instance_response" "$instance_code" "$external_task_ids_path" <<'NODE'
const fs = require('fs')
const [path, expectedInstanceCode, externalTaskIdsPath] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const data = response.data || response
const instanceCode = data.instance_code || data.instanceCode
if (instanceCode !== expectedInstanceCode) {
  throw new Error(`Lark instance get expected instance_code ${expectedInstanceCode}, got ${instanceCode || '<blank>'}`)
}
if (!data.status) throw new Error(`Lark instance ${expectedInstanceCode} response missing status`)
const tasks = Array.isArray(data.tasks) ? data.tasks : (Array.isArray(data.task_list) ? data.task_list : [])
let checkedExternalTaskIds = 0
if (externalTaskIdsPath) {
  const expectedTaskIds = JSON.parse(fs.readFileSync(externalTaskIdsPath, 'utf8'))
  checkedExternalTaskIds = expectedTaskIds.length
  const remoteTaskIds = new Set(tasks.map((task) => task.id || task.task_id).filter(Boolean).map(String))
  const missingTaskIds = expectedTaskIds.filter((taskId) => !remoteTaskIds.has(String(taskId)))
  if (missingTaskIds.length > 0) {
    throw new Error(`Lark instance ${expectedInstanceCode} is missing bound external task ids: ${missingTaskIds.join(', ')}`)
  }
}
console.log(JSON.stringify({
  larkInstanceCode: instanceCode,
  larkInstanceStatus: data.status,
  larkTaskCount: tasks.length,
  checkedExternalTaskIds
}, null, 2))
NODE
fi

if [[ "$SYNC_AFTER_SUBMIT" == "true" ]]; then
  [[ -n "$JWT_TOKEN" ]] || fail_usage "JWT_TOKEN is required when SYNC_AFTER_SUBMIT=true"
  echo "Triggering manual Lark approval sync"
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -o "$sync_response" \
    "$API_BASE/approvals/external/lark/sync?limit=20"
  copy_evidence "$sync_response" "sync-response.json"
  node - "$sync_response" <<'NODE'
const fs = require('fs')
const response = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'))
if (!Object.prototype.hasOwnProperty.call(response, 'data') && typeof response !== 'number') {
  throw new Error(`sync response missing data: ${JSON.stringify(response)}`)
}
console.log(JSON.stringify({ syncChangedCount: response.data ?? response }, null, 2))
NODE
fi

if [[ "$APPROVE_TASK" == "true" || "$REJECT_TASK" == "true" ]]; then
  [[ -n "$JWT_TOKEN" ]] || fail_usage "JWT_TOKEN is required for task decision verification"
  if [[ -z "$TASK_ID" && -f "$submit_task_id_file" ]]; then
    TASK_ID="$(cat "$submit_task_id_file")"
  fi
  [[ -n "$TASK_ID" ]] || fail_usage "TASK_ID is required for task decision verification unless SUBMIT_REVIEW=true returns a pending task"
  if ! [[ "$TASK_ID" =~ ^[0-9]+$ ]] || [[ "$TASK_ID" -le 0 ]]; then
    fail_usage "TASK_ID must be a positive integer"
  fi
  if [[ "$APPROVE_TASK" == "true" && "$REJECT_TASK" == "true" ]]; then
    fail_usage "APPROVE_TASK and REJECT_TASK cannot both be true"
  fi

  action="reject"
  expected_task_status="REJECTED"
  if [[ "$APPROVE_TASK" == "true" ]]; then
    action="approve"
    expected_task_status="APPROVED"
  fi

  echo "Verifying Lark-backed task $action for task $TASK_ID"
  node - "$tmpdir/decision-body.json" "$DECISION_COMMENT" <<'NODE'
const fs = require('fs')
const [path, comment] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({ comment }))
NODE
  curl -fsS \
    -X POST \
    -H "Authorization: Bearer $JWT_TOKEN" \
    -H "Content-Type: application/json" \
    -o "$decision_response" \
    -d @"$tmpdir/decision-body.json" \
    "$API_BASE/approvals/tasks/$TASK_ID/$action"
  copy_evidence "$tmpdir/decision-body.json" "decision-request.json"
  copy_evidence "$decision_response" "decision-response.json"
  node - "$decision_response" "$action" <<'NODE'
const fs = require('fs')
const [path, action] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const data = response.data || response
if (!data.id) throw new Error(`task ${action} response missing approval instance id: ${JSON.stringify(response)}`)
console.log(JSON.stringify({ approvalInstanceId: data.id, status: data.status }, null, 2))
NODE

  if [[ -z "$PREFLIGHT_FILE" ]]; then
    decision_tsv="$tmpdir/decision-binding.tsv"
    MYSQL_PWD="$DB_PASSWORD" mysql \
      --batch \
      --raw \
      --skip-column-names \
      --host="$DB_HOST" \
      --port="$DB_PORT" \
      --user="$DB_USER" \
      "$DB_NAME" \
      --execute "SELECT t.status, COALESCE(t.external_task_id, ''), i.status, COALESCE(i.external_instance_id, '') FROM approval_task t JOIN approval_instance i ON i.id = t.instance_id AND i.tenant_id = t.tenant_id WHERE t.tenant_id = ${TENANT_ID} AND t.id = ${TASK_ID} LIMIT 1;" \
      > "$decision_tsv"
    copy_evidence "$decision_tsv" "decision-db-binding.tsv"
    node - "$decision_tsv" "$TASK_ID" "$expected_task_status" "$decision_external_task_id_file" "$decision_external_instance_id_file" <<'NODE'
const fs = require('fs')
const [path, taskId, expectedTaskStatus, externalTaskIdPath, externalInstanceIdPath] = process.argv.slice(2)
const line = fs.readFileSync(path, 'utf8').trim()
if (!line) throw new Error(`approval task ${taskId} was not found in DB`)
const [taskStatus, externalTaskId, instanceStatus, externalInstanceId] = line.split('\t')
if (taskStatus !== expectedTaskStatus) {
  throw new Error(`approval task ${taskId} expected status ${expectedTaskStatus}, got ${taskStatus}`)
}
if (!externalTaskId) throw new Error(`approval task ${taskId} has no persisted external_task_id after decision`)
if (!externalInstanceId) throw new Error(`approval task ${taskId} instance has no persisted external_instance_id after decision`)
fs.writeFileSync(externalTaskIdPath, externalTaskId)
fs.writeFileSync(externalInstanceIdPath, externalInstanceId)
console.log(JSON.stringify({
  persistedTaskStatus: taskStatus,
  persistedExternalTaskId: externalTaskId,
  persistedInstanceStatus: instanceStatus,
  persistedExternalInstanceId: externalInstanceId
}, null, 2))
NODE
  fi

  if [[ "$VERIFY_LARK_INSTANCE" == "true" && ! -f "$decision_external_task_id_file" && -f "$submit_external_task_ids_by_local_task_id_file" ]]; then
    node - "$submit_external_task_ids_by_local_task_id_file" "$TASK_ID" "$decision_external_task_id_file" <<'NODE'
const fs = require('fs')
const [mappingPath, localTaskId, outputPath] = process.argv.slice(2)
const mapping = JSON.parse(fs.readFileSync(mappingPath, 'utf8'))
const externalTaskId = mapping[String(localTaskId)]
if (externalTaskId) {
  fs.writeFileSync(outputPath, externalTaskId)
}
NODE
  fi
  if [[ "$VERIFY_LARK_INSTANCE" == "true" && ! -f "$decision_external_instance_id_file" && -f "$submit_external_instance_id_file" ]]; then
    cp "$submit_external_instance_id_file" "$decision_external_instance_id_file"
  fi

  if [[ "$VERIFY_LARK_INSTANCE" == "true" && -f "$decision_external_task_id_file" && -f "$decision_external_instance_id_file" ]]; then
    require_command lark-cli
    decision_instance_code="$(cat "$decision_external_instance_id_file")"
    decision_external_task_id="$(cat "$decision_external_task_id_file")"
    echo "Reading Lark approval instance $decision_instance_code after task $action"
    decision_lark_instance_params="$tmpdir/decision-lark-instance-params.json"
    decision_lark_instance_response="$tmpdir/decision-lark-instance-response.json"
    node - "$decision_lark_instance_params" "$decision_instance_code" <<'NODE'
const fs = require('fs')
const [path, instanceCode] = process.argv.slice(2)
fs.writeFileSync(path, JSON.stringify({
  instance_code: instanceCode,
  user_id_type: 'open_id'
}))
NODE
    lark-cli approval instances get \
      --as user \
      --params @"$decision_lark_instance_params" \
      --format json \
      > "$decision_lark_instance_response"
    copy_evidence "$decision_lark_instance_params" "decision-lark-instance-get-params.json"
    copy_evidence "$decision_lark_instance_response" "decision-lark-instance-get-response.json"
    node - "$decision_lark_instance_response" "$decision_instance_code" "$decision_external_task_id" "$expected_task_status" <<'NODE'
const fs = require('fs')
const [path, expectedInstanceCode, expectedTaskId, expectedTaskStatus] = process.argv.slice(2)
const response = JSON.parse(fs.readFileSync(path, 'utf8'))
const data = response.data || response
const instanceCode = data.instance_code || data.instanceCode
if (instanceCode !== expectedInstanceCode) {
  throw new Error(`Lark decision instance get expected instance_code ${expectedInstanceCode}, got ${instanceCode || '<blank>'}`)
}
const tasks = Array.isArray(data.tasks) ? data.tasks : (Array.isArray(data.task_list) ? data.task_list : [])
const remoteTask = tasks.find((task) => String(task.id || task.task_id) === String(expectedTaskId))
if (!remoteTask) throw new Error(`Lark decision instance ${expectedInstanceCode} missing task ${expectedTaskId}`)
const remoteStatus = String(remoteTask.status || '').toUpperCase()
const allowedStatuses = expectedTaskStatus === 'APPROVED' ? ['APPROVED', 'DONE'] : [expectedTaskStatus]
if (!allowedStatuses.includes(remoteStatus)) {
  throw new Error(`Lark task ${expectedTaskId} expected remote status ${allowedStatuses.join('/')} after decision, got ${remoteStatus || '<blank>'}`)
}
console.log(JSON.stringify({
  larkDecisionInstanceCode: instanceCode,
  larkDecisionTaskId: expectedTaskId,
  larkDecisionTaskStatus: remoteStatus
}, null, 2))
NODE
  fi
fi
