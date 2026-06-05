# Canvas State And Data Consistency Integration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enforce and verify the P0-03 canvas state/data consistency implementation in the main worktree.

**Architecture:** The implementation is scoped to the P0-03 state-machine, runtime-policy, cleanup-safety, and Redis/DB reconciliation slices in `main`. It centralizes canvas lifecycle rules in `CanvasStateTransitionPolicy`, guards mutating services at the domain boundary, preserves runtime-referenced versions during cleanup, and adds explicit route/quota repair paths.

**Tech Stack:** Java 21, Spring Boot 3.2, WebFlux, MyBatis-Plus, Reactor, Redis, RocketMQ, JUnit 5, AssertJ, Mockito.

**Current Status:** Implemented and verified in `main` on 2026-06-04. Focused P0-03 verification and full `canvas-engine` tests both pass.

---

## Source Material

- Spec: `../specs/P0-03-canvas-state-data-consistency-spec.md`
- Source package: `../../../todo/p0/canvas-state-data-consistency/`
- Coverage matrix: `../../../todo/coverage-matrix.md`
- Evidence: `../evidence/P0-03-canvas-state-data-consistency.md`

## File Structure

**Production files changed:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicy.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java`

**Focused tests changed:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionServiceStateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceDraftUpdateStateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceStateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJobTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceQuotaReconciliationTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/MqRouteRefreshServiceTest.java`

**Documentation files:**
- Modify: `docs/architecture/archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md`
- Modify: `docs/architecture/archive/completed/plans/P0-03-canvas-state-data-consistency-plan.md`

No Flyway migration is part of this integration slice.

### Task 1: Verify Implementation Evidence

**Files:**
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicy.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicyTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionServiceStateTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceDraftUpdateStateTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceStateTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJobTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceQuotaReconciliationTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/MqRouteRefreshServiceTest.java`

- [x] **Step 1: Confirm `main` contains the P0-03 implementation**

Run:

```bash
rg -n "class CanvasStateTransitionPolicy|rebuildTriggerRoutes|reconcileInactiveCanvasQuotas" \
  backend/canvas-engine/src/main/java \
  backend/canvas-engine/src/test/java
```

Expected: output includes `CanvasStateTransitionPolicy.java`, `MqRouteRefreshService.rebuildTriggerRoutes`, and `TriggerPreCheckService.reconcileInactiveCanvasQuotas`.

- [x] **Step 2: Run focused P0-03 verification**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasStateTransitionPolicyTest,CanvasTransactionServiceStateTest,CanvasServiceDraftUpdateStateTest,CanvasOpsServiceStateTest,CanvasVersionCleanupJobTest,TriggerPreCheckServiceQuotaReconciliationTest,MqRouteRefreshServiceTest test
```

Result: PASS, 18 tests, 0 failures, 0 errors, 0 skipped.

### Task 2: Integrate The State Machine And Lifecycle Guards

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicy.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionServiceStateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceDraftUpdateStateTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasOpsServiceStateTest.java`

- [x] **Step 1: Implement lifecycle guards**

Result: `CanvasStateTransitionPolicy` was added and wired into `CanvasService`, `CanvasOpsService`, and `CanvasTransactionService`.

- [x] **Step 2: Run lifecycle-focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasStateTransitionPolicyTest,CanvasTransactionServiceStateTest,CanvasServiceDraftUpdateStateTest,CanvasOpsServiceStateTest test
```

Result: PASS, proving terminal KILLED state, publish/offline/archive guards, and published runtime-policy immutability are enforced.

### Task 3: Integrate Cleanup Safety And Redis/DB Reconciliation

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJob.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/MqRouteRefreshService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasVersionCleanupJobTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceQuotaReconciliationTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/MqRouteRefreshServiceTest.java`

- [x] **Step 1: Implement cleanup and reconciliation safeguards**

Result: `CanvasVersionCleanupJob` preserves runtime references, `TriggerPreCheckService` exposes quota reconciliation, and `MqRouteRefreshService` rebuilds all trigger routes.

- [x] **Step 2: Run cleanup and reconciliation tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasVersionCleanupJobTest,TriggerPreCheckServiceQuotaReconciliationTest,MqRouteRefreshServiceTest test
```

Result: PASS, proving cleanup preserves runtime-referenced versions and Redis route/quota repair paths are available.

### Task 4: Full P0-03 Verification And Documentation Update

**Files:**
- Modify: `docs/architecture/archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md`
- Modify: `docs/architecture/archive/completed/plans/P0-03-canvas-state-data-consistency-plan.md`

- [x] **Step 1: Run the full focused P0-03 command from the backend directory**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasStateTransitionPolicyTest,CanvasTransactionServiceStateTest,CanvasServiceDraftUpdateStateTest,CanvasOpsServiceStateTest,CanvasVersionCleanupJobTest,TriggerPreCheckServiceQuotaReconciliationTest,MqRouteRefreshServiceTest test
```

Result: PASS, 18 tests, 0 failures, 0 errors, 0 skipped.

- [x] **Step 2: Confirm `main` now contains the implementation evidence**

Run:

```bash
rg -n "class CanvasStateTransitionPolicy|rebuildTriggerRoutes|reconcileInactiveCanvasQuotas" \
  backend/canvas-engine/src/main/java \
  backend/canvas-engine/src/test/java
```

Expected: output includes `CanvasStateTransitionPolicy.java`, `MqRouteRefreshService.rebuildTriggerRoutes`, and `TriggerPreCheckService.reconcileInactiveCanvasQuotas` in the main worktree.

- [x] **Step 3: Update verification status after tests pass**

Result: `docs/architecture/archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md` now records implemented-in-main status and verification results.

- [x] **Step 4: Run full canvas-engine verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test
```

Result: PASS, 466 tests, 0 failures, 0 errors, 0 skipped.
