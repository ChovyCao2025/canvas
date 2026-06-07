#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPT="$ROOT_DIR/scripts/verify-lark-approval-live.sh"
ENV_EXAMPLE="$ROOT_DIR/scripts/lark-approval-live.env.example"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

require_pattern() {
  local pattern="$1"
  local message="$2"
  if ! grep -Eq -- "$pattern" "$SCRIPT"; then
    echo "Missing Lark approval verifier behavior: $message" >&2
    exit 1
  fi
}

run_ok() {
  local name="$1"
  local file="$2"
  if ! TENANT_ID=7 SKIP_LARK_SCHEMA=true "$SCRIPT" --preflight-file "$file" >"$TMP_DIR/${name}.out" 2>"$TMP_DIR/${name}.err"; then
    echo "Expected success for $name" >&2
    cat "$TMP_DIR/${name}.out" "$TMP_DIR/${name}.err" >&2
    exit 1
  fi
}

run_fail() {
  local name="$1"
  local file="$2"
  if TENANT_ID=7 SKIP_LARK_SCHEMA=true "$SCRIPT" --preflight-file "$file" >"$TMP_DIR/${name}.out" 2>"$TMP_DIR/${name}.err"; then
    echo "Expected failure for $name" >&2
    cat "$TMP_DIR/${name}.out" "$TMP_DIR/${name}.err" >&2
    exit 1
  fi
}

valid="$TMP_DIR/valid.json"
local_provider="$TMP_DIR/local-provider.json"
missing_code="$TMP_DIR/missing-code.json"
missing_identity="$TMP_DIR/missing-identity.json"
duplicate_open_id="$TMP_DIR/duplicate-open-id.json"
missing_required_approver="$TMP_DIR/missing-required-approver.json"
missing_required_submitter="$TMP_DIR/missing-required-submitter.json"

cat > "$valid" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LARK",
      "external_definition_code": "approval-code"
    }
  ],
  "required_submitter": "alice",
  "required_approvers": ["alice", "bob"],
  "identities": [
    {
      "tenant_id": 7,
      "username": "alice",
      "lark_open_id": "ou_alice",
      "lark_user_id": "u_alice",
      "lark_department_id": "od_growth"
    },
    {
      "tenant_id": 7,
      "username": "bob",
      "lark_open_id": "ou_bob",
      "lark_user_id": "",
      "lark_department_id": ""
    }
  ]
}
JSON

cat > "$local_provider" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LOCAL",
      "external_definition_code": "approval-code"
    }
  ],
  "identities": [
    {
      "tenant_id": 7,
      "username": "alice",
      "lark_open_id": "ou_alice"
    }
  ]
}
JSON

cat > "$missing_code" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LARK",
      "external_definition_code": ""
    }
  ],
  "identities": [
    {
      "tenant_id": 7,
      "username": "alice",
      "lark_open_id": "ou_alice"
    }
  ]
}
JSON

cat > "$missing_identity" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LARK",
      "external_definition_code": "approval-code"
    }
  ],
  "identities": []
}
JSON

cat > "$duplicate_open_id" <<'JSON'
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
      "lark_open_id": "ou_same"
    },
    {
      "tenant_id": 7,
      "username": "bob",
      "lark_open_id": "ou_same"
    }
  ]
}
JSON

cat > "$missing_required_approver" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LARK",
      "external_definition_code": "approval-code"
    }
  ],
  "required_approvers": ["bob"],
  "identities": [
    {
      "tenant_id": 7,
      "username": "alice",
      "lark_open_id": "ou_alice"
    }
  ]
}
JSON

cat > "$missing_required_submitter" <<'JSON'
{
  "definitions": [
    {
      "tenant_id": 7,
      "definition_key": "CANVAS_PUBLISH_DEFAULT",
      "external_provider": "LARK",
      "external_definition_code": "approval-code"
    }
  ],
  "required_submitter": "alice",
  "identities": [
    {
      "tenant_id": 7,
      "username": "bob",
      "lark_open_id": "ou_bob"
    }
  ]
}
JSON

run_ok "valid" "$valid"
run_fail "local-provider" "$local_provider"
run_fail "missing-code" "$missing_code"
run_fail "missing-identity" "$missing_identity"
run_fail "duplicate-open-id" "$duplicate_open_id"
run_fail "missing-required-approver" "$missing_required_approver"
run_fail "missing-required-submitter" "$missing_required_submitter"

require_pattern 'lark-cli schema approval\.instances\.get' 'approval instance schema check'
require_pattern '--env-file' 'env file support'
require_pattern '--evidence-dir' 'evidence directory support'
require_pattern 'lark-approval-live\.env\.example' 'env example documented in usage'
require_pattern 'secretsRecorded' 'evidence summary must state whether secrets are recorded'
require_pattern 'completed: exitCode === 0' 'evidence summary must report completion status'
require_pattern 'exitCode' 'evidence summary must report exit code'
require_pattern 'trap finish EXIT' 'evidence summary must be attempted on failures'
require_pattern 'artifacts: fs\.readdirSync' 'evidence summary must list artifact files'
require_pattern 'summarizeSubmit' 'evidence summary must extract submit result ids'
require_pattern 'summarizeLarkInstance' 'evidence summary must extract Lark instance status'
require_pattern 'submitDbBinding' 'evidence summary must extract DB submit bindings'
require_pattern 'decisionLarkInstance' 'evidence summary must extract post-decision Lark status'
require_pattern 'copy_evidence "\$submit_response" "submit-response\.json"' 'submit response evidence'
require_pattern 'copy_evidence "\$lark_instance_response" "lark-instance-get-response\.json"' 'Lark instance response evidence'
require_pattern 'copy_evidence "\$decision_lark_instance_response" "decision-lark-instance-get-response\.json"' 'post-decision Lark evidence'
require_pattern 'lark-cli schema approval\.tasks\.query' 'approval task schema check'
require_pattern 'lark-cli schema approval\.tasks\.approve' 'approval approve schema check'
require_pattern 'lark-cli schema approval\.tasks\.reject' 'approval reject schema check'
require_pattern 'TENANT_ID must be a positive integer' 'tenant id must be SQL-safe'
require_pattern 'SUBMITTER_USERNAME' 'explicit submitter preflight'
require_pattern 'required submitter is missing lark_open_id or lark_user_id mapping' 'required submitter mapping guard'
require_pattern 'checkedSubmitter' 'preflight reports checked submitter mapping'
require_pattern 'SUBMITTER_USERNAME is required when SUBMIT_REVIEW=true' 'live submit requires submitter identity preflight'
require_pattern 'SUBMITTER_USERNAME=alice' 'live submit examples must include submitter identity'
require_pattern 'EXPECTED_APPROVERS' 'explicit expected approver preflight'
require_pattern 'canvas_project_member' 'DB preflight derives Canvas project approvers'
require_pattern 'required approvers are missing lark_open_id mappings' 'required approver mapping guard'
require_pattern 'checkedRequiredApprovers' 'preflight reports checked approver mappings'
require_pattern '/canvas/\$CANVAS_ID/submit-review' 'optional Canvas submit-review live check'
require_pattern 'missing Lark externalInstanceId' 'submit-review must prove Lark instance binding'
require_pattern 'pending tasks without externalTaskId' 'submit-review must prove Lark task bindings'
require_pattern 'mappedExternalTaskIds' 'submit-review must persist local-to-external task mapping'
require_pattern 'first pending task id must be a positive integer' 'submit-review must expose a usable task id'
require_pattern 'TASK_ID is required for task decision verification unless SUBMIT_REVIEW=true returns a pending task' 'decision can reuse submitted task id'
require_pattern 'VERIFY_LARK_INSTANCE=true' 'optional Lark instance read verification'
require_pattern 'lark-cli approval instances get' 'live verifier must read the created Lark instance'
require_pattern 'user_id_type.*open_id' 'Lark instance read must request open_id task users'
require_pattern 'response missing status' 'Lark instance read must prove remote status exists'
require_pattern 'missing bound external task ids' 'Lark instance read must prove bound task ids exist remotely'
require_pattern 'checkedExternalTaskIds' 'Lark instance read must report checked remote task bindings'
require_pattern 'persisted external_instance_id' 'DB verification must prove persisted Lark instance binding'
require_pattern 'persisted tasks without external_task_id' 'DB verification must prove persisted Lark task bindings'
require_pattern '/approvals/external/lark/sync[?]limit=20' 'manual Lark sync live check'
require_pattern '/approvals/tasks/\$TASK_ID/\$action' 'approve/reject live check'
require_pattern 'TASK_ID must be a positive integer' 'decision task id must be SQL-safe'
require_pattern 'expected status [$][{]expectedTaskStatus[}]' 'decision DB verification must prove task status'
require_pattern 'persisted external_task_id after decision' 'decision DB verification must prove task external binding'
require_pattern 'persisted external_instance_id after decision' 'decision DB verification must prove instance external binding'
require_pattern 'after task [$]action' 'decision verification must reread Lark after task action'
require_pattern 'submit_external_task_ids_by_local_task_id_file' 'decision remote verification can use submit response mapping without DB'
require_pattern 'expected remote status' 'decision verification must prove remote task status'
require_pattern 'larkDecisionTaskStatus' 'decision verification must report remote task status'
require_pattern 'external_definition_code is required' 'definition code preflight failure'
require_pattern 'duplicate lark_open_id' 'duplicate identity guard'

if ! grep -Eq 'SUBMIT_REVIEW=false' "$ENV_EXAMPLE"; then
  echo "Missing non-destructive submit default in env example" >&2
  exit 1
fi
if ! grep -Eq 'VERIFY_LARK_INSTANCE=false' "$ENV_EXAMPLE"; then
  echo "Missing Lark instance verification toggle in env example" >&2
  exit 1
fi
if ! grep -Eq 'APPROVE_TASK=false' "$ENV_EXAMPLE"; then
  echo "Missing approve safety default in env example" >&2
  exit 1
fi
if ! grep -Eq 'EVIDENCE_DIR=' "$ENV_EXAMPLE"; then
  echo "Missing evidence directory option in env example" >&2
  exit 1
fi
if ! grep -Eq 'SUBMITTER_USERNAME=' "$ENV_EXAMPLE"; then
  echo "Missing submitter username option in env example" >&2
  exit 1
fi

echo "verify-lark-approval-live tests passed"
