#!/usr/bin/env bash
set -euo pipefail

SUMMARY_PATH=""

usage() {
  cat <<'USAGE'
Verify that a Feishu/Lark approval live evidence directory proves the full Canvas approval integration flow.

Usage:
  scripts/verify-lark-approval-evidence.sh --summary /path/to/summary.json
  scripts/verify-lark-approval-evidence.sh --evidence-dir /path/to/evidence-root-or-run-dir

The gate requires:
  - live verifier completed successfully
  - summary explicitly reports that secrets were not recorded
  - raw evidence artifacts are present for submit, DB bindings, Lark reads, sync, and decision
  - submit-review ran and returned externalInstanceId plus bound externalTaskIds
  - DB submit binding has no unbound tasks
  - Lark instance read succeeded and contains the submitted external task ids
  - manual sync endpoint ran
  - approve or reject ran through Canvas
  - DB decision binding and post-decision Lark task status match the requested action
USAGE
}

fail_usage() {
  echo "ERROR: $*" >&2
  usage >&2
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --summary)
      [[ $# -ge 2 ]] || fail_usage "--summary requires a path"
      SUMMARY_PATH="$2"
      shift 2
      ;;
    --evidence-dir)
      [[ $# -ge 2 ]] || fail_usage "--evidence-dir requires a path"
      [[ -d "$2" ]] || fail_usage "evidence dir does not exist: $2"
      SUMMARY_PATH="$(find "$2" -name summary.json -type f -print | sort | tail -n 1)"
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

[[ -n "$SUMMARY_PATH" ]] || fail_usage "--summary or --evidence-dir is required"
[[ -f "$SUMMARY_PATH" ]] || fail_usage "summary file does not exist: $SUMMARY_PATH"

node - "$SUMMARY_PATH" <<'NODE'
const fs = require('fs')
const path = require('path')
const [summaryPath] = process.argv.slice(2)
const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf8'))
const summaryDir = path.dirname(summaryPath)
const failures = []

function requireCondition(condition, message) {
  if (!condition) failures.push(message)
}

function array(value) {
  return Array.isArray(value) ? value : []
}

function text(value) {
  if (value === null || value === undefined) return ''
  return String(value).trim()
}

function hasRemoteTaskStatus(instance, taskId, allowedStatuses) {
  const statuses = array(instance && instance.taskStatuses)
  return statuses.some((task) =>
    text(task.taskId) === text(taskId)
      && allowedStatuses.includes(text(task.status).toUpperCase()))
}

const checks = summary.checks || {}
const submit = summary.submit || {}
const submitDb = summary.submitDbBinding || {}
const larkInstance = summary.larkInstance || {}
const sync = summary.sync || {}
const decision = summary.decision || {}
const decisionDb = summary.decisionDbBinding || {}
const decisionLark = summary.decisionLarkInstance || {}
const artifacts = new Set(array(summary.artifacts).map(text))
const submittedExternalTaskIds = new Set(array(submit.externalTaskIds).map(text))

function requireArtifact(name) {
  requireCondition(artifacts.has(name), `required evidence artifact is missing: ${name}`)
  requireCondition(fs.existsSync(path.join(summaryDir, name)), `required evidence artifact file is missing: ${name}`)
}

requireCondition(summary.completed === true, 'live verifier did not complete successfully')
requireCondition(Number(summary.exitCode) === 0, `live verifier exitCode is not 0: ${summary.exitCode}`)
requireCondition(summary.secretsRecorded === false, 'summary must report secretsRecorded=false')
requireCondition(checks.submitReview === true, 'SUBMIT_REVIEW=true evidence is required')
requireCondition(checks.verifyLarkInstance === true, 'VERIFY_LARK_INSTANCE=true evidence is required')
requireCondition(checks.syncAfterSubmit === true, 'SYNC_AFTER_SUBMIT=true evidence is required')
requireCondition(checks.approveTask === true || checks.rejectTask === true, 'APPROVE_TASK=true or REJECT_TASK=true evidence is required')
requireCondition(!(checks.approveTask === true && checks.rejectTask === true), 'approve and reject evidence cannot both be true')

requireArtifact('preflight.json')
requireArtifact('submit-response.json')
requireArtifact('submit-db-binding.tsv')
requireArtifact('lark-instance-get-response.json')
requireArtifact('sync-response.json')
requireArtifact('decision-response.json')
requireArtifact('decision-db-binding.tsv')
requireArtifact('decision-lark-instance-get-response.json')

requireCondition(Boolean(submit.approvalInstanceId), 'submit approvalInstanceId is missing')
requireCondition(text(submit.status) === 'PENDING', `submit status must be PENDING, got ${submit.status}`)
requireCondition(Boolean(text(submit.externalInstanceId)), 'submit externalInstanceId is missing')
requireCondition(Number(submit.pendingTaskCount) > 0, 'submit pendingTaskCount must be positive')
requireCondition(array(submit.externalTaskIds).length === Number(submit.pendingTaskCount), 'submit externalTaskIds must cover every pending task')

requireCondition(Boolean(text(submitDb.externalInstanceId)), 'submit DB externalInstanceId is missing')
requireCondition(Number(submitDb.taskCount) > 0, 'submit DB taskCount must be positive')
requireCondition(Number(submitDb.unboundTaskCount) === 0, `submit DB has unbound tasks: ${submitDb.unboundTaskCount}`)
requireCondition(text(submitDb.externalInstanceId) === text(submit.externalInstanceId), 'submit DB externalInstanceId does not match submit response')

requireCondition(text(larkInstance.instanceCode) === text(submit.externalInstanceId), 'Lark instance code does not match submit externalInstanceId')
requireCondition(Boolean(text(larkInstance.status)), 'Lark instance status is missing')
const remoteTaskIds = new Set(array(larkInstance.taskIds).map(text))
for (const taskId of array(submit.externalTaskIds)) {
  requireCondition(remoteTaskIds.has(text(taskId)), `Lark instance is missing submitted external task id ${taskId}`)
}

requireCondition(Object.prototype.hasOwnProperty.call(sync, 'changedCount'), 'sync changedCount evidence is missing')

requireCondition(Boolean(decision.approvalInstanceId), 'decision approvalInstanceId is missing')
requireCondition(text(decision.approvalInstanceId) === text(submit.approvalInstanceId), 'decision approvalInstanceId does not match submit approvalInstanceId')
requireCondition(Boolean(text(decision.status)), 'decision status is missing')
requireCondition(Boolean(text(decisionDb.taskStatus)), 'decision DB taskStatus is missing')
requireCondition(Boolean(text(decisionDb.externalTaskId)), 'decision DB externalTaskId is missing')
requireCondition(submittedExternalTaskIds.has(text(decisionDb.externalTaskId)), 'decision DB externalTaskId was not returned by submit-review')
requireCondition(Boolean(text(decisionDb.externalInstanceId)), 'decision DB externalInstanceId is missing')
requireCondition(text(decisionDb.externalInstanceId) === text(submit.externalInstanceId), 'decision DB externalInstanceId does not match submit externalInstanceId')
requireCondition(text(decisionLark.instanceCode) === text(submit.externalInstanceId), 'post-decision Lark instance code does not match submit externalInstanceId')

if (checks.approveTask === true) {
  requireCondition(text(decisionDb.taskStatus) === 'APPROVED', `decision DB taskStatus must be APPROVED, got ${decisionDb.taskStatus}`)
  requireCondition(hasRemoteTaskStatus(decisionLark, decisionDb.externalTaskId, ['APPROVED', 'DONE']),
    `post-decision Lark task ${decisionDb.externalTaskId} is not APPROVED/DONE`)
}
if (checks.rejectTask === true) {
  requireCondition(text(decisionDb.taskStatus) === 'REJECTED', `decision DB taskStatus must be REJECTED, got ${decisionDb.taskStatus}`)
  requireCondition(hasRemoteTaskStatus(decisionLark, decisionDb.externalTaskId, ['REJECTED']),
    `post-decision Lark task ${decisionDb.externalTaskId} is not REJECTED`)
}

if (failures.length > 0) {
  console.error(JSON.stringify({
    ok: false,
    summaryPath,
    failures
  }, null, 2))
  process.exit(1)
}

console.log(JSON.stringify({
  ok: true,
  summaryPath,
  tenantId: summary.tenantId,
  approvalInstanceId: submit.approvalInstanceId,
  externalInstanceId: submit.externalInstanceId,
  externalTaskIds: submit.externalTaskIds,
  decisionTaskStatus: decisionDb.taskStatus,
  decisionRemoteInstanceStatus: decisionLark.status
}, null, 2))
NODE
