#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-lark-approval-evidence.sh"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

valid_dir="$TMP_DIR/valid"
invalid_dir="$TMP_DIR/invalid"
missing_artifact_dir="$TMP_DIR/missing-artifact"
listed_but_missing_dir="$TMP_DIR/listed-but-missing"
mismatched_decision_dir="$TMP_DIR/mismatched-decision"
mkdir -p "$valid_dir" "$invalid_dir" "$missing_artifact_dir" "$listed_but_missing_dir" "$mismatched_decision_dir"

cat > "$valid_dir/summary.json" <<'JSON'
{
  "completed": true,
  "exitCode": 0,
  "tenantId": 7,
  "artifacts": [
    "preflight.json",
    "submit-response.json",
    "submit-db-binding.tsv",
    "lark-instance-get-response.json",
    "sync-response.json",
    "decision-response.json",
    "decision-db-binding.tsv",
    "decision-lark-instance-get-response.json"
  ],
  "checks": {
    "submitReview": true,
    "verifyLarkInstance": true,
    "syncAfterSubmit": true,
    "approveTask": true,
    "rejectTask": false
  },
  "submit": {
    "approvalInstanceId": 101,
    "status": "PENDING",
    "externalInstanceId": "lark-instance-101",
    "pendingTaskCount": 1,
    "externalTaskIds": ["lark-task-201"]
  },
  "submitDbBinding": {
    "externalInstanceId": "lark-instance-101",
    "taskCount": 1,
    "unboundTaskCount": 0
  },
  "larkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "PENDING",
    "taskIds": ["lark-task-201"],
    "taskStatuses": [
      { "taskId": "lark-task-201", "status": "PENDING" }
    ]
  },
  "sync": {
    "changedCount": 0
  },
  "decision": {
    "approvalInstanceId": 101,
    "status": "APPROVED"
  },
  "decisionDbBinding": {
    "taskStatus": "APPROVED",
    "externalTaskId": "lark-task-201",
    "instanceStatus": "APPROVED",
    "externalInstanceId": "lark-instance-101"
  },
  "decisionLarkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "APPROVED",
    "taskStatuses": [
      { "taskId": "lark-task-201", "status": "DONE" }
    ]
  },
  "secretsRecorded": false
}
JSON

for artifact in \
  preflight.json \
  submit-response.json \
  submit-db-binding.tsv \
  lark-instance-get-response.json \
  sync-response.json \
  decision-response.json \
  decision-db-binding.tsv \
  decision-lark-instance-get-response.json
do
  touch "$valid_dir/$artifact"
done

cat > "$invalid_dir/summary.json" <<'JSON'
{
  "completed": true,
  "exitCode": 0,
  "tenantId": 7,
  "artifacts": [
    "preflight.json",
    "submit-response.json",
    "submit-db-binding.tsv",
    "lark-instance-get-response.json",
    "sync-response.json",
    "decision-response.json",
    "decision-db-binding.tsv",
    "decision-lark-instance-get-response.json"
  ],
  "checks": {
    "submitReview": true,
    "verifyLarkInstance": true,
    "syncAfterSubmit": true,
    "approveTask": true,
    "rejectTask": false
  },
  "submit": {
    "approvalInstanceId": 101,
    "status": "PENDING",
    "externalInstanceId": "lark-instance-101",
    "pendingTaskCount": 1,
    "externalTaskIds": ["lark-task-201"]
  },
  "submitDbBinding": {
    "externalInstanceId": "lark-instance-101",
    "taskCount": 1,
    "unboundTaskCount": 0
  },
  "larkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "PENDING",
    "taskIds": ["lark-task-201"]
  },
  "sync": {
    "changedCount": 0
  },
  "decision": {
    "approvalInstanceId": 101,
    "status": "APPROVED"
  },
  "decisionDbBinding": {
    "taskStatus": "APPROVED",
    "externalTaskId": "lark-task-201",
    "instanceStatus": "APPROVED",
    "externalInstanceId": "lark-instance-101"
  },
  "decisionLarkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "APPROVED",
    "taskStatuses": [
      { "taskId": "lark-task-201", "status": "PENDING" }
    ]
  },
  "secretsRecorded": false
}
JSON

cat > "$missing_artifact_dir/summary.json" <<'JSON'
{
  "completed": true,
  "exitCode": 0,
  "tenantId": 7,
  "artifacts": [
    "preflight.json",
    "submit-response.json"
  ],
  "checks": {
    "submitReview": true,
    "verifyLarkInstance": true,
    "syncAfterSubmit": true,
    "approveTask": true,
    "rejectTask": false
  },
  "submit": {
    "approvalInstanceId": 101,
    "status": "PENDING",
    "externalInstanceId": "lark-instance-101",
    "pendingTaskCount": 1,
    "externalTaskIds": ["lark-task-201"]
  },
  "submitDbBinding": {
    "externalInstanceId": "lark-instance-101",
    "taskCount": 1,
    "unboundTaskCount": 0
  },
  "larkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "PENDING",
    "taskIds": ["lark-task-201"]
  },
  "sync": {
    "changedCount": 0
  },
  "decision": {
    "approvalInstanceId": 101,
    "status": "APPROVED"
  },
  "decisionDbBinding": {
    "taskStatus": "APPROVED",
    "externalTaskId": "lark-task-201",
    "instanceStatus": "APPROVED",
    "externalInstanceId": "lark-instance-101"
  },
  "decisionLarkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "APPROVED",
    "taskStatuses": [
      { "taskId": "lark-task-201", "status": "DONE" }
    ]
  },
  "secretsRecorded": false
}
JSON

cp "$valid_dir/summary.json" "$listed_but_missing_dir/summary.json"

cat > "$mismatched_decision_dir/summary.json" <<'JSON'
{
  "completed": true,
  "exitCode": 0,
  "tenantId": 7,
  "artifacts": [
    "preflight.json",
    "submit-response.json",
    "submit-db-binding.tsv",
    "lark-instance-get-response.json",
    "sync-response.json",
    "decision-response.json",
    "decision-db-binding.tsv",
    "decision-lark-instance-get-response.json"
  ],
  "checks": {
    "submitReview": true,
    "verifyLarkInstance": true,
    "syncAfterSubmit": true,
    "approveTask": true,
    "rejectTask": false
  },
  "submit": {
    "approvalInstanceId": 101,
    "status": "PENDING",
    "externalInstanceId": "lark-instance-101",
    "pendingTaskCount": 1,
    "externalTaskIds": ["lark-task-201"]
  },
  "submitDbBinding": {
    "externalInstanceId": "lark-instance-101",
    "taskCount": 1,
    "unboundTaskCount": 0
  },
  "larkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "PENDING",
    "taskIds": ["lark-task-201"],
    "taskStatuses": [
      { "taskId": "lark-task-201", "status": "PENDING" }
    ]
  },
  "sync": {
    "changedCount": 0
  },
  "decision": {
    "approvalInstanceId": 202,
    "status": "APPROVED"
  },
  "decisionDbBinding": {
    "taskStatus": "APPROVED",
    "externalTaskId": "lark-task-999",
    "instanceStatus": "APPROVED",
    "externalInstanceId": "lark-instance-101"
  },
  "decisionLarkInstance": {
    "instanceCode": "lark-instance-101",
    "status": "APPROVED",
    "taskStatuses": [
      { "taskId": "lark-task-999", "status": "DONE" }
    ]
  },
  "secretsRecorded": false
}
JSON

for artifact in \
  preflight.json \
  submit-response.json \
  submit-db-binding.tsv \
  lark-instance-get-response.json \
  sync-response.json \
  decision-response.json \
  decision-db-binding.tsv \
  decision-lark-instance-get-response.json
do
  touch "$mismatched_decision_dir/$artifact"
done

"$SCRIPT" --summary "$valid_dir/summary.json" > "$TMP_DIR/valid.out"
"$SCRIPT" --evidence-dir "$valid_dir" > "$TMP_DIR/valid-dir.out"

if "$SCRIPT" --summary "$invalid_dir/summary.json" > "$TMP_DIR/invalid.out" 2> "$TMP_DIR/invalid.err"; then
  echo "Expected invalid evidence to fail" >&2
  cat "$TMP_DIR/invalid.out" "$TMP_DIR/invalid.err" >&2
  exit 1
fi

grep -q 'post-decision Lark task lark-task-201 is not APPROVED/DONE' "$TMP_DIR/invalid.err"

if "$SCRIPT" --summary "$missing_artifact_dir/summary.json" > "$TMP_DIR/missing-artifact.out" 2> "$TMP_DIR/missing-artifact.err"; then
  echo "Expected missing artifact evidence to fail" >&2
  cat "$TMP_DIR/missing-artifact.out" "$TMP_DIR/missing-artifact.err" >&2
  exit 1
fi

grep -q 'required evidence artifact is missing: submit-db-binding.tsv' "$TMP_DIR/missing-artifact.err"

if "$SCRIPT" --summary "$listed_but_missing_dir/summary.json" > "$TMP_DIR/listed-but-missing.out" 2> "$TMP_DIR/listed-but-missing.err"; then
  echo "Expected listed but missing artifact evidence to fail" >&2
  cat "$TMP_DIR/listed-but-missing.out" "$TMP_DIR/listed-but-missing.err" >&2
  exit 1
fi

grep -q 'required evidence artifact file is missing: preflight.json' "$TMP_DIR/listed-but-missing.err"

if "$SCRIPT" --summary "$mismatched_decision_dir/summary.json" > "$TMP_DIR/mismatched-decision.out" 2> "$TMP_DIR/mismatched-decision.err"; then
  echo "Expected mismatched decision evidence to fail" >&2
  cat "$TMP_DIR/mismatched-decision.out" "$TMP_DIR/mismatched-decision.err" >&2
  exit 1
fi

grep -q 'decision approvalInstanceId does not match submit approvalInstanceId' "$TMP_DIR/mismatched-decision.err"
grep -q 'decision DB externalTaskId was not returned by submit-review' "$TMP_DIR/mismatched-decision.err"
echo "verify-lark-approval-evidence tests passed"
