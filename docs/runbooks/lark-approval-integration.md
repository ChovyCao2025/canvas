# Feishu/Lark Approval Integration Runbook

This runbook configures Canvas publish approvals to create and operate real Feishu/Lark approval instances through the Canvas approval core.

## Scope

This integration covers:

- Canvas publish approval submission.
- Lark approval instance creation.
- Local `approval_instance.external_instance_id` binding.
- Local `approval_task.external_task_id` binding after instance creation.
- Manual or scheduled Lark instance sync.
- Lark approve/reject task operations before local state changes.

It does not bypass Canvas local approval state. Canvas remains the local audit and publish gate. Lark is the external approval provider for definitions configured with `external_provider = 'LARK'`.

## Required Lark Permissions

Enable these scopes in the Feishu/Lark developer console and grant user authorization where user tokens are used:

| API path | Purpose | Token used by Canvas | Required scope |
| --- | --- | --- | --- |
| `POST /open-apis/approval/v4/instances` | Create Lark approval instance | Tenant token reference | Approval instance write permission for the app |
| `GET /open-apis/approval/v4/instances/{instance_code}` | Read instance status and task ids | User token reference | `approval:instance:read` |
| `POST /open-apis/approval/v4/tasks/approve` | Approve a task | User token reference | `approval:task:write` |
| `POST /open-apis/approval/v4/tasks/reject` | Reject a task | User token reference | `approval:task:write` |

The local implementation reads Lark instances with `user_id_type=open_id`, so returned `tasks[].user_id` must match the `lark_open_id` values stored in Canvas. The reader also accepts compatible `task_list[].task_id` and `task_list[].open_id` fields when returned by a Lark-compatible gateway.

Before changing request shapes, re-check the live CLI schema:

```bash
lark-cli schema approval.instances.get --format json
lark-cli schema approval.tasks.query --format json
lark-cli schema approval.tasks.approve --format json
lark-cli schema approval.tasks.reject --format json
```

Do not guess unlisted fields.

Canvas sends `uuid = canvas-approval-{approval_instance.id}` when creating a Lark approval instance. This keeps retries for the same local approval instance idempotent on the Lark side.

## Canvas Configuration

Set these application properties for the `canvas-engine` runtime:

```properties
canvas.approval.lark.base-url=https://open.feishu.cn
canvas.approval.lark.tenant-token-reference=<credential-reference-for-instance-create>
canvas.approval.lark.user-token-reference=<credential-reference-for-instance-read-and-task-actions>
canvas.approval.lark.sync.enabled=false
canvas.approval.lark.sync.tenant-id=<tenant-id>
canvas.approval.lark.sync.limit=100
canvas.approval.lark.sync.fixed-delay-ms=60000
```

Keep sync disabled until the tenant has valid approval definition and user identity mappings.

Token references are resolved through `MarketingMonitorProviderCredentialResolver`. Store real token material in the configured credential backend; do not commit tokens to source files.

## Database Setup

Enable Lark for the Canvas publish approval definition:

```sql
UPDATE approval_definition
SET external_provider = 'LARK',
    external_definition_code = '<lark-approval-definition-code>'
WHERE tenant_id = <tenant_id>
  AND definition_key = 'CANVAS_PUBLISH_DEFAULT';
```

If the tenant uses the seeded global definition (`tenant_id = 0`) as fallback, configure that row instead. Prefer a tenant-specific row when different tenants use different Lark approval definitions.

Add Canvas user to Lark identity mappings:

```sql
INSERT INTO approval_lark_user_identity (
  tenant_id,
  username,
  lark_open_id,
  lark_user_id,
  lark_department_id
) VALUES (
  <tenant_id>,
  '<canvas-username>',
  '<lark-open-id>',
  '<lark-user-id>',
  '<lark-department-id>'
)
ON DUPLICATE KEY UPDATE
  lark_open_id = VALUES(lark_open_id),
  lark_user_id = VALUES(lark_user_id),
  lark_department_id = VALUES(lark_department_id);
```

At minimum:

- The submitter must have `lark_open_id` or `lark_user_id` so Canvas can create the Lark instance.
- Each local approver that should be mapped to a Lark task must have `lark_open_id`. Canvas binds local tasks by matching this value to Lark `tasks[].user_id`.

## Lark Form Contract

Canvas publish approval generates `lark.create.form` as a JSON string with these stable field ids:

| Field id | Type | Source |
| --- | --- | --- |
| `canvas_name` | `input` | Canvas name |
| `canvas_id` | `input` | Canvas id |
| `draft_version` | `input` | Draft version number |
| `submit_reason` | `textarea` | Submit review reason |
| `project_key` | `input` | Canvas project key |
| `risk_level` | `input` | Canvas risk level |
| `risk_reasons` | `textarea` | Newline-separated risk reasons |
| `graph_json` | `textarea` | Draft graph JSON |

The Lark approval definition must contain compatible controls for the generated payload. If the Lark approval definition uses different control ids, either update the definition or change the Canvas form builder with tests.

## Verification Flow

1. Run the preflight verifier:

```bash
TENANT_ID=<tenant-id> scripts/verify-lark-approval-live.sh
```

The default mode is non-destructive. It checks Lark approval schemas, the active Canvas approval definition, and `approval_lark_user_identity` mappings. Use fixture mode when reviewing exported configuration without a live database:

`TENANT_ID` must be a positive integer because DB-backed verification uses it to query tenant-scoped approval configuration.

When `SUBMITTER_USERNAME` is set, the verifier checks that this Canvas user has `lark_open_id` or `lark_user_id`, because Lark instance creation requires one submitter identity. Set this to the username represented by the `JWT_TOKEN` used for `SUBMIT_REVIEW=true`; the live verifier requires it when `SUBMIT_REVIEW=true`.

When `CANVAS_ID` is set in DB mode, the verifier derives the Canvas publish approvers from the canvas project assignment. Project admins are checked first; if no project admins are found, `tenant_admin` is checked as the fallback approver. Every required approver must have a non-empty `lark_open_id`, because local task binding matches Lark `tasks[].user_id` returned with `user_id_type=open_id`.

When submitter or approvers are known ahead of time, or when using fixture mode, pass them explicitly:

```bash
TENANT_ID=<tenant-id> \
SUBMITTER_USERNAME=alice \
EXPECTED_APPROVERS=alice,bob \
scripts/verify-lark-approval-live.sh
```

For repeatable live checks, start from the environment template:

```bash
cp scripts/lark-approval-live.env.example /tmp/lark-approval-live.env
# edit /tmp/lark-approval-live.env with local tenant, DB, JWT, and Canvas values
scripts/verify-lark-approval-live.sh --env-file /tmp/lark-approval-live.env
```

For completion evidence, pass an evidence directory. The verifier creates a timestamped subdirectory containing non-secret artifacts such as `preflight.json`, submit/sync/decision responses, Lark instance reads, DB binding checks, and `summary.json`. The summary lists artifact filenames and extracts non-secret ids/statuses such as local approval instance id, Lark `externalInstanceId`, external task ids, Lark instance status, sync changed count, and post-decision task status when those checks are enabled. It also records `completed` and `exitCode`; on failure, the partial summary helps identify the last successful checkpoint.

```bash
scripts/verify-lark-approval-live.sh \
  --env-file /tmp/lark-approval-live.env \
  --evidence-dir /tmp/canvas-lark-approval-evidence
```

After a full live run, gate the evidence directory before treating the integration as complete:

```bash
scripts/verify-lark-approval-evidence.sh \
  --evidence-dir /tmp/canvas-lark-approval-evidence
```

This gate requires `secretsRecorded=false`, raw evidence artifact filenames for the live checkpoints, a successful submit, Lark instance read, DB binding check, manual sync, one approve/reject action, DB decision binding, and post-decision Lark task status evidence. It also verifies that the decision response belongs to the submitted local approval instance and that the decided external task id was returned by the same submit-review response.

Do not place real tokens or DB passwords in the evidence directory. The verifier does not write `JWT_TOKEN`, credential references, DB password values, or resolved Feishu/Lark tokens into its evidence summary.

```bash
TENANT_ID=<tenant-id> \
SKIP_LARK_SCHEMA=true \
scripts/verify-lark-approval-live.sh --preflight-file /path/to/preflight.json
```

2. Confirm the approval definition is Lark-backed:

```sql
SELECT tenant_id, definition_key, external_provider, external_definition_code
FROM approval_definition
WHERE tenant_id IN (0, <tenant_id>)
  AND definition_key = 'CANVAS_PUBLISH_DEFAULT';
```

3. Confirm submitter and approver identity mappings:

```sql
SELECT tenant_id, username, lark_open_id, lark_user_id, lark_department_id
FROM approval_lark_user_identity
WHERE tenant_id = <tenant_id>;
```

4. Submit Canvas publish review through the application:

```http
POST /canvas/{canvasId}/submit-review
Content-Type: application/json

{
  "reason": "publish approval live verification"
}
```

Or use the live verifier explicitly:

```bash
TENANT_ID=<tenant-id> \
JWT_TOKEN="$TOKEN" \
SUBMITTER_USERNAME=<canvas-username> \
CANVAS_ID=<canvas-id> \
SUBMIT_REVIEW=true \
scripts/verify-lark-approval-live.sh
```

With `SUBMIT_REVIEW=true`, the verifier fails unless the submit response contains a Lark `externalInstanceId` and every returned pending task contains an `externalTaskId`. When running in DB mode, it also re-queries `approval_instance` and `approval_task` to prove those bindings were persisted.

The verifier stores the first returned pending task id from `SUBMIT_REVIEW=true`. In the same run, `APPROVE_TASK=true` or `REJECT_TASK=true` can use that task automatically when `TASK_ID` is omitted.

To additionally prove the created `externalInstanceId` is readable from Feishu/Lark, enable CLI-side instance verification:

```bash
TENANT_ID=<tenant-id> \
JWT_TOKEN="$TOKEN" \
SUBMITTER_USERNAME=<canvas-username> \
CANVAS_ID=<canvas-id> \
SUBMIT_REVIEW=true \
VERIFY_LARK_INSTANCE=true \
scripts/verify-lark-approval-live.sh
```

`VERIFY_LARK_INSTANCE=true` calls `lark-cli approval instances get --as user` with `user_id_type=open_id`. The CLI user must have `approval:instance:read` authorization. When used together with `SUBMIT_REVIEW=true`, the verifier also checks that every locally returned `externalTaskId` appears in the remote Lark instance task list. To verify an already-created Lark instance without submitting a new Canvas review, pass `LARK_INSTANCE_CODE=<instance-code>`.

5. Check local instance and task bindings:

```sql
SELECT id, status, external_instance_id, snapshot_json
FROM approval_instance
WHERE tenant_id = <tenant_id>
  AND target_type = 'CANVAS'
  AND target_id = '<canvas-id>'
ORDER BY id DESC
LIMIT 1;

SELECT id, approver, status, external_task_id
FROM approval_task
WHERE tenant_id = <tenant_id>
  AND instance_id = <approval-instance-id>
ORDER BY step_no, id;
```

Expected:

- `approval_instance.external_instance_id` is the Lark `instance_code`.
- Each mapped approver has `approval_task.external_task_id` populated.

6. Trigger manual sync as an admin:

```http
POST /approvals/external/lark/sync?limit=20
```

Or include sync in the live verifier:

```bash
TENANT_ID=<tenant-id> \
JWT_TOKEN="$TOKEN" \
SUBMITTER_USERNAME=<canvas-username> \
CANVAS_ID=<canvas-id> \
SUBMIT_REVIEW=true \
SYNC_AFTER_SUBMIT=true \
scripts/verify-lark-approval-live.sh
```

Expected:

- The endpoint returns the number of locally changed instances.
- Lark terminal statuses update local instance/task status.
- Approved Canvas publish approvals run the `PUBLISH_CANVAS` auto action when the approved draft is still current.

7. Verify approve/reject through Canvas:

```http
POST /approvals/tasks/{taskId}/approve
Content-Type: application/json

{
  "comment": "approved in live verification"
}
```

Or use:

```bash
TENANT_ID=<tenant-id> \
JWT_TOKEN="$TOKEN" \
TASK_ID=<approval-task-id> \
APPROVE_TASK=true \
scripts/verify-lark-approval-live.sh
```

Or run a single-submit decision check when the JWT user is allowed to approve the first returned task:

```bash
TENANT_ID=<tenant-id> \
JWT_TOKEN="$TOKEN" \
SUBMITTER_USERNAME=<canvas-username> \
CANVAS_ID=<canvas-id> \
SUBMIT_REVIEW=true \
VERIFY_LARK_INSTANCE=true \
SYNC_AFTER_SUBMIT=true \
APPROVE_TASK=true \
scripts/verify-lark-approval-live.sh
```

For `LARK` approvals, Canvas must call Lark first. If the local task lacks `external_task_id`, the operation fails instead of silently approving locally.

When running in DB mode, the verifier also re-queries the selected task after approve/reject. It fails unless the task status is `APPROVED` or `REJECTED` for the requested action and both the task `external_task_id` and parent instance `external_instance_id` are still persisted.

When `VERIFY_LARK_INSTANCE=true` is also enabled, the verifier reads the Lark instance again after approve/reject and checks the selected remote task status. For approve, Lark `APPROVED` and `DONE` are accepted as successful task terminal states; for reject, Lark `REJECTED` is required.

In DB mode, the post-decision remote check uses persisted `external_instance_id` and `external_task_id`. In fixture/non-DB mode, it can use the task mapping returned by the same `SUBMIT_REVIEW=true` response.

## Scheduled Sync

After manual verification succeeds, enable scheduled sync:

```properties
canvas.approval.lark.sync.enabled=true
canvas.approval.lark.sync.tenant-id=<tenant-id>
canvas.approval.lark.sync.limit=100
canvas.approval.lark.sync.fixed-delay-ms=60000
```

Use one scheduler configuration per running deployment. The scheduler has an overlap guard, but duplicate app instances with the same tenant setting may still add unnecessary Lark reads.

## Troubleshooting

| Symptom | Likely cause | Fix |
| --- | --- | --- |
| `Lark approval external definition code is required before creating an instance` | `approval_definition.external_definition_code` is empty | Set the Lark approval definition code on the active tenant definition |
| `Lark approval create payload requires openId or userId` | Submitter has no explicit request identity and no `approval_lark_user_identity` mapping | Insert submitter mapping or pass `larkOpenId` / `larkUserId` explicitly |
| Local task has no `external_task_id` after submit | Approver has no `lark_open_id`, Lark instance did not return a matching `tasks[].user_id`, or multiple remote tasks share the same `user_id` | Add/repair approver mapping and inspect the Lark instance task list |
| Approve/reject fails with missing external task id | Local task was not bound to a Lark task | Fix mapping and resubmit or sync after task ids are available |
| Instance sync fails with user token error | `canvas.approval.lark.user-token-reference` missing or invalid | Configure a user token with `approval:instance:read` and `approval:task:write` |
| Instance creation fails with tenant token error | `canvas.approval.lark.tenant-token-reference` missing or invalid | Configure tenant token reference and required instance creation permission |

## Local Regression Commands

The current worktree may contain unrelated compile blockers outside approval. Use focused verification for approval changes:

```bash
scripts/verify-lark-approval-local.sh
```

If Maven's normal `test` lifecycle is blocked by unrelated dirty-tree test sources outside approval, rerun the same focused set with targeted approval test compilation:

```bash
ALLOW_TARGETED_TEST_COMPILE=true scripts/verify-lark-approval-local.sh
```

Live verifier script self-test:

```bash
scripts/verify-lark-approval-live.test.sh
scripts/verify-lark-approval-evidence.test.sh
scripts/verify-lark-approval-local.test.sh
```

Frontend approval API surface:

```bash
cd frontend
npm run test -- src/services/approvalApi.test.ts
```
