# DAG Side-Effect Idempotency And Context Bounds Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent retries, resumes, loops, and oversized context from creating duplicate side effects, inconsistent node outputs, or unbounded runtime resource use.

**Architecture:** Add a central node side-effect idempotency boundary, then change DAG output commit so handlers stage results and commit only after success. Enforce context size and node-scoped context keys in `ExecutionContext`, and move runtime guard validation into publish and dry-run paths.

**Tech Stack:** Java 21, Spring Boot, MyBatis, Flyway, Redis, JUnit 5, Mockito, existing DAG engine and handler contracts.

## Implementation Status

- Status: implemented and verified on 2026-06-05.
- Commit: not created in this session because the worktree contains many unrelated and parallel product-evolution changes.

---

## Spec Reference

- `docs/product-evolution/specs/p0-004-dag-side-effect-idempotency-and-context-bounds.md`
- Optimization sources: `docs/optimization/production-design-gaps.md`, `docs/optimization/bmad-product-review-2026-05.md`, `docs/optimization/todo/plan-review-findings.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectRecord.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

**Data And Config**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V93__node_side_effect_idempotency.sql`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineContextCommitTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextBoundsTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasValidationRuntimeGuardTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/WaitResumeQuotaBypassTest.java`

### Task 1: Failing Tests For Idempotency And Context Commit

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineContextCommitTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextBoundsTest.java`

- [x] **Step 1: Write idempotency service tests**

Create these idempotency service test methods:

```java
@Test void reserveCreatesRunningRecordForUniqueOperation()
@Test void reserveReturnsCompletedRecordForDuplicateOperation()
@Test void completeStoresStableOutputSnapshot()
@Test void failAllowsRetryUntilMaxAttempts()
@Test void duplicateOperationDoesNotReexecuteExternalSideEffect()
```

- [x] **Step 2: Write DAG commit tests**

Use a test handler that mutates a local counter and returns success or failure. Create these DAG commit test methods:

```java
@Test void failedHandlerDoesNotCommitNodeOutput()
@Test void successfulHandlerCommitsNodeOutputOnce()
@Test void duplicateCompletedSideEffectReturnsCachedOutput()
```

- [x] **Step 3: Write context bounds tests**

Create `ExecutionContextBoundsTest` methods named `calculatesSerializedContextSize`, `emitsWarningAtThreshold`, `rejectsAboveHardMax`, `storesNodeScopedFlatKeys`, and `readsLegacyNestedContextForCompatibility`.

- [x] **Step 4: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeSideEffectIdempotencyServiceTest,DagEngineContextCommitTest,ExecutionContextBoundsTest
```

Expected: FAIL because idempotency service, staged context commit, and hard context bounds are not implemented.

### Task 2: Idempotency Data And Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V93__node_side_effect_idempotency.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/idempotency/NodeSideEffectRecord.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/idempotency/NodeSideEffectIdempotencyServiceTest.java`

- [x] **Step 1: Add additive table**

Create `node_side_effect_idempotency` with tenant id, execution id, canvas id, node id, node type, operation key, idempotency key, status, attempt count, output JSON, error message, created at, updated at, and a unique key on tenant id plus idempotency key.

- [x] **Step 2: Implement service methods**

Implement:

```java
ReserveResult reserve(ExecutionContext ctx, String nodeId, String nodeType, String operationKey);
void complete(Long recordId, Map<String, Object> output);
void fail(Long recordId, String errorMessage);
Optional<Map<String, Object>> cachedOutput(String idempotencyKey);
String buildKey(ExecutionContext ctx, String nodeId, String nodeType, String operationKey);
```

Return a completed cached output without allowing another side effect when the key already completed.

- [x] **Step 3: Run idempotency tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeSideEffectIdempotencyServiceTest
```

Expected: PASS.

### Task 3: DAG Output Commit And Context Bounds

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineContextCommitTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextBoundsTest.java`

- [x] **Step 1: Add staged output commit**

In `DagEngine`, keep handler output in a local variable until `NodeResult` is successful. Commit through `ctx.putNodeOutput(nodeId, output)` after idempotency completion, and skip output commit for failed, timeout, suppressed, or pending results unless the result explicitly carries persisted wait metadata.

- [x] **Step 2: Enforce context size and namespaced flat keys**

Change `ExecutionContext.putNodeOutput` to serialize the candidate `nodeOutputs` or candidate output for sizing. Reject writes above `canvas.execution.context-max-bytes`. Write flat keys as `nodeId.fieldName` and retain legacy plain-field lookup only when no namespaced match exists.

- [x] **Step 3: Run DAG and context tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagEngineContextCommitTest,ExecutionContextBoundsTest
```

Expected: PASS.

### Task 4: Runtime Guards And Resume Quota Bypass

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasValidationRuntimeGuardTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/WaitResumeQuotaBypassTest.java`

- [x] **Step 1: Add validation guards**

Reject publish and dry-run validation when total node count exceeds `canvas.validation.max-node-count`, GOTO max jump count exceeds `canvas.validation.max-goto-jumps`, LOOP max iterations exceeds `canvas.validation.max-loop-iterations`, or transitive subflow dependencies contain a cycle.

- [x] **Step 2: Prove resume quota bypass**

Add tests where a WAIT resume payload enters `CanvasExecutionService.trigger` and verifies `TriggerPreCheckService.checkWithoutQuotaAccounting` and `consumeQuotaAndRecord` are not called for internal continuation trigger types.

- [x] **Step 3: Run guard and resume tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasValidationRuntimeGuardTest,WaitResumeQuotaBypassTest
```

Expected: PASS.

### Task 5: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p0-004-dag-side-effect-idempotency-and-context-bounds.md`
- Modify: `docs/product-evolution/plans/p0-004-dag-side-effect-idempotency-and-context-bounds-plan.md`

- [x] **Step 1: Run focused backend suite**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeSideEffectIdempotencyServiceTest,DagEngineContextCommitTest,ExecutionContextBoundsTest,CanvasValidationRuntimeGuardTest,WaitResumeQuotaBypassTest
```

Expected: PASS.

- [x] **Step 2: Run affected existing tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagEnginePendingTest,DagEngineDepthTest,WaitHandlerTest,CouponHandlerTest
```

Expected: PASS.

Note: `GoalCheckHandlerTest` and `CdpTagWriteHandlerTest` are not present in this repository snapshot, so the affected regression run uses the existing DAG, WAIT, and coupon tests.

- [ ] **Step 3: Commit implementation slice**

Run:

```bash
git add backend/canvas-engine/src docs/product-evolution/specs docs/product-evolution/plans
git commit -m "feat: add DAG idempotency and context guards"
```

Expected: commit contains only idempotency, DAG commit, context bounds, runtime guard, spec, and plan files.

### Verification Evidence

- BI publish approval compile unblock:

```bash
cd backend && mvn -pl canvas-engine clean test -Dtest=BiPublishApprovalServiceTest,BiPublishApprovalControllerTest
```

Result: 10 tests, 0 failures, 0 errors, 0 skipped.

- Focused P0-004 backend suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeSideEffectIdempotencyServiceTest,DagEngineContextCommitTest,ExecutionContextBoundsTest,CanvasValidationRuntimeGuardTest,WaitResumeQuotaBypassTest
```

Result: 21 tests, 0 failures, 0 errors, 0 skipped.

- Affected existing regression:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DagEnginePendingTest,DagEngineDepthTest,WaitHandlerTest,CouponHandlerTest
```

Result: 16 tests, 0 failures, 0 errors, 0 skipped.
