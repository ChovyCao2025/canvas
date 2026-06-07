# Enterprise Approval Workflow Design

## Context

The current repository has several unrelated review mechanisms:

- `canvas_manual_approval` backs runtime manual nodes and `/canvas/pending-reviews`.
- `bi_publish_approval` backs BI resource publishing.
- `require_review_before_publish` exists on projects but is not enforced by canvas publishing.
- Other worktrees contain only roadmap notes or domain-specific approval tables; no unified `approval_instance` / `approval_task` / `ApprovalWorkflowService` implementation exists.

The new approval system replaces scattered approval entry points with a reusable local workflow core. External products such as Feishu/Lark Approval, ServiceNow, Jira Service Management approvals, Power Automate approvals, Camunda user tasks, and Salesforce approval processes all share the same durable primitives: definition, instance, assigned task, decision, escalation/reminder hooks, and audit trail. This implementation uses those primitives locally and adds a provider boundary for Feishu/Lark task decisions without making Lark a hard runtime dependency.

## Goals

- Add a tenant-scoped approval domain with definitions, instances, tasks, and immutable audit events.
- Enforce canvas publish approval as a server-side gate, including project-level `require_review_before_publish` and high-risk graph/runtime signals.
- Make final canvas approval automatically run the approved publish action.
- Keep existing BI publish approval endpoints compatible while backing them with the unified approval system.
- Replace operational pending-review reads with unified approval tasks while preserving runtime manual approval compatibility.
- Provide frontend API/types and a practical approval inbox entry.
- Support a `LARK` external provider boundary where local approval instances/tasks bind to Feishu/Lark instance and task identifiers, and approve/reject calls execute against Lark before local state changes.

## Non-Goals

- Do not remove legacy tables in this pass.
- Do not make Feishu/Lark Approval a hard runtime dependency.
- Do not guess the Feishu/Lark approval instance creation payload before the exact OpenAPI schema and form contract are confirmed.
- Do not silently fall back to local approval behavior when a `LARK` approval is missing its external instance or task id.
- Do not implement a visual workflow designer for approval definitions.
- Do not change unrelated project governance, BI resource, or execution behavior.

## Domain Model

`approval_definition` stores reusable approval templates:

- `definition_key`, `domain`, `target_type`, `mode`, `min_approvals`, `default_due_hours`.
- `external_provider` selects the provider. `LOCAL` stays fully local. `LARK` routes external actions through the Lark provider. `external_definition_code` stores the upstream approval definition code when available.

`approval_instance` stores one submitted approval:

- Target identity: `domain`, `target_type`, `target_id`, `target_version_id`.
- State: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `EXPIRED`.
- Submit metadata: submitter, reason, risk level/reasons, snapshot.
- Auto action metadata: `PUBLISH_CANVAS`, `auto_action_status`, and error text.
- External binding: `external_instance_id` stores the Feishu/Lark `instance_code` for Lark-backed approvals.

`approval_task` stores work assigned to an approver:

- `approver` may be a username or a role token such as `tenant_admin`.
- State: `PENDING`, `APPROVED`, `REJECTED`, `TRANSFERRED`, `CANCELLED`, `EXPIRED`.
- External binding: `external_task_id` stores the Feishu/Lark task id for Lark-backed approvals.

`approval_audit_event` stores immutable transitions:

- submit, approve, reject, cancel, auto-action success/failure.

## Core Services

`ApprovalWorkflowService` owns generic lifecycle behavior:

- Submit creates an instance, one or more pending tasks, and a submit audit event.
- Approve/reject validates task ownership, updates the task, rolls up instance status, records audit, and triggers a registered auto-action handler after final approval.
- Latest-approved lookup gates business actions by target identity and version.
- Task listing returns tasks visible to the current user or role.

`ApprovalAutoActionHandler` allows business domains to execute approved actions without hard-coding domain dependencies into the workflow service.

`ApprovalExternalProvider` allows non-local providers to participate in the workflow without moving business approval policy out of the local core:

- `LOCAL` definitions use only local state.
- `LARK` definitions call the provider during submit/decision.
- Submit can bind pre-created Lark `instance_code` and task ids from the approval snapshot.
- Submit can create a Lark instance when the snapshot explicitly supplies `lark.create.form` plus `openId` or `userId`; the provider does not infer or synthesize Lark form controls.
- After creating a Lark instance, the provider fetches `instances.get` and binds local pending tasks to returned Lark task ids when a local approver's mapped Lark `open_id` uniquely matches `tasks[].user_id`.
- Canvas publish submissions generate a default `lark.create.form` from Canvas business data. The generated controls use stable ids for `canvas_name`, `canvas_id`, `draft_version`, `submit_reason`, `project_key`, `risk_level`, `risk_reasons`, and `graph_json`.
- Canvas publish submissions include Lark submitter identity from explicit request fields first (`larkOpenId`, `larkUserId`, `larkDepartmentId`), then fall back to the durable `approval_lark_user_identity` mapping keyed by `tenant_id + username`; usernames are not treated as Lark ids.
- Approve/reject calls the Lark task action first and only updates local state after that call succeeds.
- Missing Lark external ids are hard failures, not silent local approvals.

`HttpLarkApprovalClient` calls Feishu/Lark approval task APIs:

- `POST /open-apis/approval/v4/instances`
- `GET /open-apis/approval/v4/instances/{instance_code}`
- `POST /open-apis/approval/v4/tasks/approve`
- `POST /open-apis/approval/v4/tasks/reject`

The client resolves tokens through deployment-side credential references. `canvas.approval.lark.tenant-token-reference` is used for instance creation. `canvas.approval.lark.user-token-reference` is used for instance reads and task decisions. Tokens and secrets are not hard-coded or logged.

Operational setup and live verification are documented in `docs/runbooks/lark-approval-integration.md`.

`ApprovalWorkflowService.syncExternalInstance` synchronizes a single Lark-backed local approval instance:

- Fetches current Lark instance state through the provider.
- Maps Lark instance statuses: `APPROVED` -> `APPROVED`, `REJECTED` -> `REJECTED`, `CANCELED` / `DELETED` -> `CANCELLED`, `PENDING` -> `PENDING`.
- Maps Lark task statuses by external task id, including `DONE` -> local `APPROVED`.
- Records external sync audit events for task and instance transitions.
- Runs approved auto actions when external sync moves an instance into `APPROVED`.

`ApprovalWorkflowService.syncPendingExternalInstances` provides the bounded batch primitive for orchestration:

- Selects local pending approval instances that already have an external Lark instance binding.
- Applies the same provider sync path as single-instance sync.
- Bounds each run by a caller-provided limit, capped internally.
- Returns the number of local instances whose status changed during the run.

`POST /approvals/external/lark/sync?limit=...` exposes a secured manual sync trigger:

- Uses the current tenant context.
- Requires `TENANT_ADMIN`, `ADMIN`, or `SUPER_ADMIN`.
- Calls `syncPendingExternalInstances` with the requested limit.
- Returns the number of local approval instances changed by the sync run.

`LarkApprovalSyncScheduler` provides automatic sync orchestration:

- Disabled by default via `canvas.approval.lark.sync.enabled=false`.
- Uses `canvas.approval.lark.sync.tenant-id`, `canvas.approval.lark.sync.limit`, and `canvas.approval.lark.sync.fixed-delay-ms`.
- Calls `syncPendingExternalInstances` for the configured tenant.
- Uses an overlap guard so a slow sync run does not run concurrently with the next tick.

`CanvasPublishApprovalService` owns canvas-specific policy:

- Requires approval when the project has `require_review_before_publish=1`.
- Requires approval for high-risk drafts: Groovy/custom script signals, coupon/benefit nodes, or unlimited global execution cap.
- Submission snapshots the current draft version and risk reasons.
- Submission adds a Lark create payload with the generated Canvas form and Lark submitter identity from explicit request fields or the local username mapping table.
- Direct publish calls are blocked until a fresh approved instance covers the current draft version.

`CanvasPublishApprovalAutoActionHandler` executes `PUBLISH_CANVAS` after final approval. It refuses to publish if the current draft version differs from the approved version.

`BiPublishApprovalService` keeps existing API contracts and legacy table writes, but also mirrors submit/review/require-approved operations to the unified workflow when the core service is available.

## API

New generic approval API:

- `GET /approvals/tasks?status=PENDING`
- `GET /approvals/instances?targetType=CANVAS&targetId=123`
- `POST /approvals/tasks/{taskId}/approve`
- `POST /approvals/tasks/{taskId}/reject`

Canvas approval API:

- `POST /canvas/{id}/submit-review`
- `GET /canvas/{id}/approval-status`
- `POST /canvas/{id}/publish` keeps its route but enforces approval before publishing.

Compatibility:

- Existing BI publish approval endpoints remain unchanged.
- Existing runtime manual approval endpoints remain available.
- `/canvas/pending-reviews` becomes a compatibility view over unified approval tasks when the approval core is wired.

## Testing

Backend tests:

- Migration contains all approval tables and seed definitions.
- Workflow submit creates instance, task, and audit event.
- Approval completes the instance and rejects unauthorized approvers.
- Lark provider creates an instance only from explicit snapshot create payload and external definition code.
- Lark provider stores external bindings and invokes the Lark task action before local approve/reject.
- Lark provider rejects missing external instance/task ids.
- Lark provider maps `instances.get` status payloads and workflow sync persists the mapped state locally.
- Lark provider maps newly-created instance tasks by unique approver `open_id` when `instances.get` returns `tasks[].user_id`.
- Canvas publish is blocked without an approved current-draft approval.
- Final canvas approval runs the publish auto action.
- BI publish approval mirrors requests into unified approvals while preserving legacy behavior.

Frontend tests:

- API service builds approval task, instance, canvas submit-review, and status calls.
- Approval presentation helpers classify pending/approved/rejected states.

## Rollout

The first migration is additive. Existing data remains valid. Existing BI approval rows still work as fallback while new rows also create unified instances. Runtime manual approval remains on `canvas_manual_approval` until the execution engine is migrated, but operator inboxes use the new unified task API.

## Remaining Lark Scope

This pass does not claim full Feishu/Lark approval parity until a real Feishu/Lark tenant has been verified end to end. The operational configuration and live verification procedure is captured in `docs/runbooks/lark-approval-integration.md`.
