# P1-002 - Operator Visibility And Testability Spec

Priority: P1
Sequence: 002
Source: `todo/p1/operator-visibility-and-testability.md`
Implementation plan: `../plans/p1-002-operator-visibility-and-testability-plan.md`

## Implementation Status

Status: Implemented and focused-verified on 2026-06-05; full module regressions are not recorded as run/passing.

Verification evidence is recorded in `../plans/p1-002-operator-visibility-and-testability-plan.md`.

## Goal

Expose backend execution, send-record, policy, dry-run, trace, and table-operation capabilities through operator-facing APIs and UI helpers that can be tested without reading logs.

## User And Business Value

Operators can inspect queued and failed executions, replay safe failures, search delivery records, verify policy state, test a draft, and export bounded operational tables without backend-only access.

## Scope Split

This spec implements operator visibility and testability surfaces. Production runtime gates, alert routing, dashboards, and degradation switches remain in `p0-005-production-operability-and-runtime-gates`. Provider receipt reconciliation remains in `p0-003-delivery-outbox-receipts-and-reconciliation`.

## In Scope

- Execution request list filters, status summary, single replay, and bounded batch replay UI wiring for the existing `/canvas/execution-requests` controller.
- DLQ list and replay visibility for existing `/canvas/dlq` behavior.
- Message send record search and detail endpoint for `message_send_record`.
- Marketing policy read/write endpoints for consent, suppression, and channel availability records.
- Dry-run result presentation that maps execution output and trace status into node colors and operator messages.
- Reusable table helper behavior: filter query serialization, fixed operation-column metadata, row selection, and CSV export row limits.
- Frontend service wrappers and pure presentation helpers for execution requests, message records, policies, dry-run visualization, and operator tables.

## Out Of Scope

- Full report builder, chart drill-down, real-time collaborative editing, and CRDT.
- Large asynchronous exports beyond a bounded synchronous CSV export.
- User preference persistence for per-user column layouts; this can follow in P2 after usage is proven.
- Adding new execution engine behavior beyond the minimal fields needed to explain dry-run and trace results.

## Functional Requirements

1. Operators must be able to filter execution requests by `canvasId`, `status`, `userId`, and `sourceMsgId`.
2. Batch replay must default to replayable statuses only and must cap `limit` at 500.
3. Message send record search must support `canvasId`, `executionId`, `userId`, `channel`, `status`, and time range filters.
4. Message send detail must show request payload, status, external message id, and error message without requiring database access.
5. Policy management endpoints must let admins inspect and update consent, suppression, and customer channel records for a user and channel.
6. Dry-run UI helpers must classify success, failure, skipped, and running node traces into stable colors and summary counts.
7. Operator table helpers must block CSV export above the configured synchronous row limit.
8. All new backend list endpoints must use bounded pagination with a maximum page size of 100.
9. All new frontend display helpers must have pure Vitest coverage before page wiring.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageSendRecordController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPolicyAdminController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MessageSendRecordDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingConsentDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSuppressionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerChannelDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MessageSendRecordMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingConsentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingSuppressionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CustomerChannelMapper.java`
- `backend/canvas-engine/src/main/resources/db/migration/V250__operator_visibility_and_testability.sql`

### Frontend Touchpoints

- `frontend/src/services/operatorApi.ts`
- `frontend/src/services/api.ts`
- `frontend/src/components/canvas/ExecutionTracePanel.tsx`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-editor/dryRunVisualization.ts`
- `frontend/src/pages/canvas-stats/index.tsx`
- `frontend/src/pages/canvas-stats/operatorTables.ts`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasExecutionRequestManagementControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MessageSendRecordControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingPolicyAdminControllerTest.java`
- `frontend/src/pages/canvas-editor/dryRunVisualization.test.ts`
- `frontend/src/pages/canvas-stats/operatorTables.test.ts`

## Dependencies

- P0-001 should provide tenant scoping for send records and marketing policy tables before production rollout.
- Existing `ExecutionTracePanel` and dry-run endpoint provide the first visualization surface.
- Existing `CanvasExecutionRequestManagementController` already owns replay semantics and rate limiting; this spec exposes and tests the operator workflow around it.

## Risks And Controls

- Replay can duplicate side effects. Keep default replay restricted to `FAILED` and `RETRY`; require `force=true` for any other state.
- Message payloads can contain personal data. Display payloads through bounded detail endpoints and rely on P0-001/P0-002 masking and authorization before broad rollout.
- CSV export can lock the browser. Block synchronous export above 5,000 rows and instruct users to narrow filters.
- Policy writes can affect real sends. Restrict the policy admin route to admin roles and record `updatedBy` when P0-001 audit fields exist.

## Acceptance Criteria

- Backend tests prove execution request listing clamps pagination and batch replay caps limits at 500.
- Backend tests prove message send record list/detail endpoints filter by canvas/user/status and return bounded pages.
- Backend tests prove marketing policy admin endpoints upsert consent, suppression, and channel records with normalized channel codes.
- Frontend dry-run helper tests prove trace colors and summary counts are stable for success, failure, skipped, and running nodes.
- Frontend table helper tests prove filter serialization, export row-limit blocking, and fixed operation column metadata.
- Focused backend and frontend commands in the implementation plan pass.
