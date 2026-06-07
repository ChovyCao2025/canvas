# Enterprise Approval Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable approval workflow core and enforce it for canvas publishing while adapting existing BI approval flows.

**Architecture:** Add approval definition/instance/task/audit tables and a generic `ApprovalWorkflowService`. Domain services submit target snapshots and register auto-action handlers; canvas publishing uses the core as a hard server-side gate.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, AssertJ, Mockito, React/Vite/Vitest.

---

### Task 1: Core Approval Persistence And Workflow

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V350__enterprise_approval_workflow.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalDefinitionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalInstanceDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalTaskDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApprovalAuditEventDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalDefinitionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalInstanceMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalTaskMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApprovalAuditEventMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalSubmitCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalDecisionCommand.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalInstanceView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalTaskView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalAutoActionHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalWorkflowService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/ApprovalWorkflowServiceTest.java`

- [ ] Write failing tests for migration shape, submit lifecycle, approve lifecycle, reject lifecycle, and unauthorized approver rejection.
- [ ] Run `cd backend && mvn -pl canvas-engine -Dtest=ApprovalWorkflowServiceTest test` and verify RED.
- [ ] Add the migration, data objects, mappers, records, and service.
- [ ] Run the same command and verify GREEN.

### Task 2: Canvas Publish Approval Gate

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalStatusView.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalAutoActionHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/CanvasPublishApprovalServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CanvasControllerApprovalTest.java`

- [ ] Write failing tests for submit-review, required-review publish block, approved current draft publish allow, stale approval rejection, and final approval auto publish.
- [ ] Run `cd backend && mvn -pl canvas-engine -Dtest=CanvasPublishApprovalServiceTest,CanvasControllerApprovalTest test` and verify RED.
- [ ] Implement canvas risk evaluation, approver resolution, submit/status methods, auto-action handler, and controller endpoints.
- [ ] Change `POST /canvas/{id}/publish` to call the approval gate before `CanvasService.publish`.
- [ ] Run the same command and verify GREEN.

### Task 3: Approval API And Pending Review Replacement

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalDecisionRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApprovalController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ApprovalControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerApprovalTaskTest.java`

- [ ] Write failing tests for task inbox listing, approve/reject routes, and `/canvas/pending-reviews` returning unified tasks when the workflow service is present.
- [ ] Run targeted controller tests and verify RED.
- [ ] Implement the generic controller and Ops compatibility branch.
- [ ] Run targeted controller tests and verify GREEN.

### Task 4: BI Approval Adapter

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/resource/BiPublishApprovalServiceTest.java`

- [ ] Add failing tests proving `requestApproval`, `reviewApproval`, and `requireApprovedApproval` interact with `ApprovalWorkflowService` when configured.
- [ ] Run `cd backend && mvn -pl canvas-engine -Dtest=BiPublishApprovalServiceTest test` and verify RED.
- [ ] Add optional workflow-service integration without breaking legacy table behavior.
- [ ] Run the same command and verify GREEN.

### Task 5: Frontend Approval API And Inbox Entry

**Files:**
- Modify: `frontend/src/services/api.ts`
- Create: `frontend/src/services/approvalApi.test.ts`
- Create: `frontend/src/pages/approvals/approvalPresentation.ts`
- Create: `frontend/src/pages/approvals/approvalPresentation.test.ts`
- Create: `frontend/src/pages/approvals/index.tsx`
- Modify: app routing/navigation files as needed by existing frontend structure.
- Modify: `frontend/src/pages/canvas-editor/useCanvasPublishWorkflow.ts`

- [ ] Write failing Vitest coverage for approval API calls and state presentation helpers.
- [ ] Run targeted frontend tests and verify RED.
- [ ] Add approval service functions, inbox page, and submit-review path in the canvas publish workflow when backend reports approval required.
- [ ] Run targeted frontend tests and verify GREEN.

### Task 6: Verification

- [ ] Run backend targeted approval tests.
- [ ] Run frontend targeted approval tests.
- [ ] Run a compile/build command for touched modules where feasible.
- [ ] Audit that no unrelated dirty files were reverted or staged.

### Task 7: Lark Approval Provider Boundary

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalExternalProvider.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalExternalSubmissionResult.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalTaskActionRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/LarkApprovalProvider.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/HttpLarkApprovalClient.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/approval/ApprovalWorkflowService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/ApprovalWorkflowServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/approval/LarkApprovalProviderTest.java`

- [x] Add provider boundary so business code depends on the local approval core while external providers are selected by `approval_definition.external_provider`.
- [x] Persist external instance/task bindings returned by a provider into `approval_instance.external_instance_id` and `approval_task.external_task_id`.
- [x] For `LARK` approvals, call the external provider before updating the local task on approve/reject.
- [x] Add a Lark provider that supports pre-created Lark binding data in the approval snapshot and refuses to approve/reject when the external instance or task id is missing.
- [x] Add conservative Lark instance creation via `POST /open-apis/approval/v4/instances` when the approval snapshot explicitly provides `lark.create.form` and `openId` or `userId`; do not infer form fields.
- [x] Add an HTTP Lark client for `approval/v4/tasks/approve` and `approval/v4/tasks/reject`, using deployment-side user-token credential resolution via `canvas.approval.lark.user-token-reference`.
- [x] Add tenant-token credential resolution via `canvas.approval.lark.tenant-token-reference` for Lark approval instance creation.
- [x] Add Lark instance status sync through `approval/v4/instances.get`, mapping Lark `APPROVED`, `REJECTED`, `CANCELED`, `DELETED`, and task `DONE` statuses into local approval statuses.
- [x] Add `ApprovalWorkflowService.syncExternalInstance` so Lark terminal status updates local instance/task state, records sync audit events, and triggers approved auto actions.
- [x] Add `ApprovalWorkflowService.syncPendingExternalInstances` to batch-sync pending local approvals that already have external Lark instance bindings, bounded by a caller-provided limit.
- [x] Add secured admin-only `POST /approvals/external/lark/sync?limit=...` endpoint that invokes bounded pending Lark approval sync for the current tenant.
- [x] Add disabled-by-default `LarkApprovalSyncScheduler` for automatic pending Lark approval sync, with tenant id, limit, fixed delay, and overlap guard.
- [x] Add Canvas publish generation of Lark `form` payloads from Canvas business data, with stable field ids for canvas name/id, draft version, submit reason, project key, risk level, risk reasons, and graph JSON.
- [x] Allow Canvas publish review requests to carry explicit Lark submitter identity (`larkOpenId`, `larkUserId`, `larkDepartmentId`) without inferring those ids from Canvas usernames.
- [x] Add durable `approval_lark_user_identity` mapping keyed by `tenant_id + username`, with resolver fallback so Canvas publish review requests do not need to pass Lark ids manually when a local mapping exists.
- [x] Add external task-id mapping after Lark instance creation by reading `instances.get` and binding local tasks when mapped approver `open_id` uniquely matches returned `tasks[].user_id`.
- [x] Add `docs/runbooks/lark-approval-integration.md` covering Lark scopes, token references, approval definition setup, identity mappings, generated form contract, manual sync, scheduled sync, and live verification steps.
- [x] Add `scripts/verify-lark-approval-live.sh` and `scripts/verify-lark-approval-live.test.sh` so real tenant validation starts with repeatable preflight checks before optional live submit/sync/decision calls.
- [x] Verify with `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=ApprovalWorkflowServiceTest,LarkApprovalProviderTest,CanvasPublishApprovalServiceTest,ApprovalControllerTest,CanvasControllerApprovalTest,OpsControllerApprovalTaskTest,BiPublishApprovalServiceTest test`.
- [x] Verify the Lark sync increment with isolated approval compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=ApprovalWorkflowServiceTest,LarkApprovalProviderTest`, because the current dirty worktree has unrelated main/test compile blockers outside approval.
- [x] Verify the secured sync endpoint increment with isolated approval/controller compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=ApprovalWorkflowServiceTest,LarkApprovalProviderTest,ApprovalControllerTest`.
- [x] Verify scheduler increment with isolated test compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=ApprovalWorkflowServiceTest,LarkApprovalProviderTest,LarkApprovalSyncSchedulerTest,ApprovalControllerTest`.
- [x] Verify Canvas Lark form increment with isolated approval compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=LarkApprovalProviderTest,ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,CanvasPublishApprovalServiceTest`.
- [x] Verify frontend approval API surface with `cd frontend && npm run test -- src/services/approvalApi.test.ts`.
- [x] Verify Lark user identity mapping increment with isolated approval compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=ApprovalLarkUserIdentityResolverTest,LarkApprovalProviderTest,ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,CanvasPublishApprovalServiceTest`.
- [x] Verify Lark created-instance task binding increment with isolated approval compilation and `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine surefire:test -Dtest=ApprovalLarkUserIdentityResolverTest,LarkApprovalProviderTest,ApprovalWorkflowServiceTest,ApprovalControllerTest,LarkApprovalSyncSchedulerTest,CanvasPublishApprovalServiceTest`.
- [x] Verify runbook references with `rg -n "lark-approval-integration|Remaining Lark Work|full submit/sync/approve/reject" docs/runbooks/lark-approval-integration.md docs/superpowers/specs/2026-06-06-enterprise-approval-workflow-design.md docs/superpowers/plans/2026-06-06-enterprise-approval-workflow.md`.
- [x] Verify live verifier script fixtures with `scripts/verify-lark-approval-live.test.sh`.

**Remaining Lark Work:**
- [ ] Validate the full submit/sync/approve/reject flow against a real Feishu/Lark tenant with configured approval definition code, form controls, token references, scopes, and `approval_lark_user_identity` rows.
