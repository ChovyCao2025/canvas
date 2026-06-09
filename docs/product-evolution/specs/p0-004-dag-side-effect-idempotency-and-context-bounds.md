# P0-004 - DAG Side-Effect Idempotency And Context Bounds Spec

Priority: P0
Sequence: 004
Source: `docs/optimization/archive/production-design-gaps.md`, `docs/optimization/archive/bmad-product-review-2026-05.md`, `docs/optimization/todo/plan-review-findings.md`
Implementation plan: `../plans/p0-004-dag-side-effect-idempotency-and-context-bounds-plan.md`

## Implementation Status

Implemented and verified on 2026-06-05. Verification evidence is recorded in `../plans/p0-004-dag-side-effect-idempotency-and-context-bounds-plan.md`.

## Goal

Prevent retries, resumes, loops, and oversized context from creating duplicate side effects, inconsistent node outputs, or unbounded runtime resource use.

## User And Business Value

Marketing actions such as coupons, points, messages, API calls, and MQ outputs can be retried or resumed without accidental double-spend or silently corrupted execution state.

## Evidence From Optimization

- `ExecutionContext.putNodeOutput` still flattens all node output keys into one map and only exposes `isOversized()` for callers to check.
- `DagEngine` persists context at terminal and waiting boundaries, with some special timeout saves, but not as a uniform after-node lifecycle contract.
- Optimization sources flag handler idempotency, deferred context write, context size enforcement, GOTO/loop/subflow limits, profile snapshotting, and version safety.

## In Scope

- Introduce a central node side-effect idempotency service keyed by execution id, node id, node type, attempt, and operation key.
- Make side-effect handlers reserve, complete, and return cached outcomes through the idempotency service.
- Buffer handler output and commit it to `ExecutionContext` only after the handler succeeds.
- Enforce serialized context size limits and namespaced flattened keys such as `nodeId.field`.
- Add hard guards for GOTO jumps, loop iterations, total node count, and transitive subflow cycles.
- Snapshot profile fields used by one execution so later CDP writes cannot change decisions inside the same execution.
- Add tests proving WAIT/GOAL resume bypasses new-trigger quota and cooldown paths.

## Out Of Scope

- Rewriting the entire DAG engine to an imperative scheduler.
- Provider-specific delivery outbox behavior, covered by P0-003.
- OLAP trace storage, covered by analytics and architecture migration specs.

## Functional Requirements

1. A handler retry must not execute the same external side effect twice when the first attempt already completed.
2. Handler output must not be visible to downstream nodes until the handler reports success.
3. Context writes that exceed the configured serialized size limit must fail the node with a typed reason.
4. Flat context key collisions must be observable and eliminated through node-scoped namespacing.
5. Infinite or excessive GOTO, loop, and subflow chains must fail during validation or before consuming the full global timeout.
6. Resume triggers must not spend user quota or fail due to normal trigger cooldown.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V93__node_side_effect_idempotency.sql`
- `backend/canvas-engine/src/main/resources/application.yml`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineContextCommitTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextBoundsTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasValidationRuntimeGuardTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/WaitResumeQuotaBypassTest.java`

## Dependencies

- Side-effect handlers must expose enough operation metadata to build stable operation keys.
- Validation must run before publish and before dry-run so failures are caught consistently.

## Risks And Controls

- Interface churn risk: introduce a default compatibility adapter for handlers, then migrate side-effect handlers first.
- False duplicate risk: include node type and operation key in the idempotency key, not only node id.
- Context compatibility risk: preserve old `getContextValue(field)` reads during migration while preferring `getContextValue(nodeId, field)`.

## Acceptance Criteria

- Coupon, points, API call, MQ send, and delivery-like handlers have idempotency tests for retry and resume.
- Failed handlers do not leave partial `nodeOutputs` or `flatContext` entries.
- Oversized context fails predictably and records a typed trace reason.
- Publish validation rejects excessive node count, excessive GOTO bound, and transitive subflow cycle.
- WAIT/GOAL resume tests prove quota and cooldown are bypassed for internal continuations.
