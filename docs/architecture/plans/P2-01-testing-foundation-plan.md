# Testing Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an executable testing foundation for the canvas engine and editor so P0/P1 fixes are protected by backend integration tests, frontend behavior tests, and CI gates.

**Architecture:** Keep the current monorepo and module layout. Backend tests stay under `backend/canvas-engine/src/test/java/org/chovy/canvas`, integration harness code lives under `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport`, and frontend tests stay beside feature helpers in `frontend/src`. Testcontainers covers MySQL, Redis, and RocketMQ when available; any local substitute must be documented in `docs/architecture/testing/manual-verification.md` with owner and expiration date.

**Tech Stack:** Java 21, Spring Boot 3.2, JUnit 5, AssertJ, Reactor Test, Testcontainers, MySQL 8, Redis 7, RocketMQ 5.3.1, React 18, TypeScript, Vitest, Vite, GitHub Actions.

---

## Source Material

- Spec: `../specs/P2-01-testing-foundation-spec.md`
- Source package: `../todo/p2/testing-foundation/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/specs/P2-01-testing-foundation-spec.md`
- Read: `docs/architecture/todo/p2/testing-foundation/plan.md`
- Read: `docs/architecture/todo/coverage-matrix.md`
- Modify: `backend/canvas-engine/pom.xml`
- Modify: `.github/workflows/canvas-ci.yml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasIntegrationTestBase.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasRocketMqTestSupport.java`
- Create: `docs/architecture/testing/test-layer-map.md`
- Create: `docs/architecture/testing/manual-verification.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGateTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyIntegrationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Test: `frontend/src/pages/canvas-editor/canvasEditorAutosave.test.tsx`
- Test: `frontend/src/components/config-panel/formValues.test.ts`

### Task 1: Define test layers

**Files:**
- Create: `docs/architecture/testing/test-layer-map.md`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ExecutionLifecycleGateTest.java`
- Read: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Read: `frontend/src/components/config-panel/formValues.test.ts`

- [x] List backend unit, backend integration, migration/schema, controller, and frontend behavior test layers with one current file and one owner per layer.
- [x] Map the critical missing coverage from the spec to concrete test classes: canvas state machine, Redis/DB side effects, trigger admission, scheduler races, circuit breaker races, direct execution auth, and end-to-end canvas execution.
- [x] Add a "promotion rule" table that says when a unit test must become an integration test.

**Run:**
```bash
test -f docs/architecture/testing/test-layer-map.md
rg "canvas state machine|Redis/DB side effects|direct execution auth|end-to-end canvas execution" docs/architecture/testing/test-layer-map.md
cd backend && mvn test -pl canvas-engine -Dtest=CanvasTransactionAnnotationTest,ExecutionLifecycleGateTest,ExecutionControllerMachineAuthTest
cd frontend && npm test -- graphHydration formValues
```

**Expected:** The layer map names every critical coverage area from the spec, backend focused tests pass, and frontend focused tests pass.

### Task 2: Add Testcontainers or equivalent integration harness

**Files:**
- Modify: `backend/canvas-engine/pom.xml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasIntegrationTestBase.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasRocketMqTestSupport.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyIntegrationTest.java`

- [x] Add test-scoped Testcontainers dependencies for MySQL, Redis, and RocketMQ support in `backend/canvas-engine/pom.xml`.
- [x] Create `CanvasIntegrationTestBase` that starts MySQL and Redis containers, wires Spring datasource/Redis properties, and applies Flyway migrations.
- [x] Create `CanvasRocketMqTestSupport` that either starts RocketMQ or exposes a documented local substitute for MQ-dependent tests.
- [x] Convert one Redis/DB side-effect test to use the integration base.

**Run:**
```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

**Expected:** Tests start containerized MySQL and Redis or fail with one named documented substitute in `docs/architecture/testing/manual-verification.md`; no test depends on a developer's existing local database state.

### Task 3: Add tests for P0 package acceptance criteria before code changes

**Files:**
- Modify: `docs/architecture/testing/test-layer-map.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasTransactionAnnotationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryConcurrencyTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyTest.java`
- Test: `frontend/src/pages/canvas-editor/localDraft.test.ts`

- [x] For each P0 plan, add at least one test class or test file to the layer map before implementation work starts.
- [x] Cover security hardening with route/auth tests, state consistency with canvas transaction/state tests, reactive safety with blocking-boundary tests, execution concurrency with registry/request tests, resilience with DLQ/retry tests, and tenant isolation with tenant-scoped repository tests.
- [x] Mark any missing test as "blocked by harness" only when Task 2 cannot run that dependency locally and the manual verification file names the command owner.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=SecurityConfigRouteTest,CanvasTransactionAnnotationTest,InFlightExecutionRegistryConcurrencyTest,CanvasExecutionRequestServiceIdempotencyTest
cd frontend && npm test -- localDraft
```

**Expected:** The P0 rows in `docs/architecture/testing/test-layer-map.md` name a concrete test file, and the focused backend/frontend commands pass.

### Task 4: Add CI commands for backend and frontend tests

**Files:**
- Modify: `.github/workflows/canvas-ci.yml`
- Modify: `backend/canvas-engine/pom.xml`
- Modify: `frontend/package.json`
- Read: `docker-compose.local.yml`

- [x] Keep CI backend execution on `cd backend && mvn test`.
- [x] Keep CI frontend execution on `cd frontend && npm test` and `cd frontend && npm run build`.
- [x] Add a separate integration-test job or profile only after Task 2 is stable in CI.
- [x] Document required Docker services for local integration runs in the workflow comments or `docs/architecture/testing/test-layer-map.md`.

**Run:**
```bash
cd backend && mvn test
cd frontend && npm test
cd frontend && npm run build
```

**Expected:** CI-equivalent backend tests, frontend tests, and frontend build complete from clean commands using the required `cd backend && mvn ...` and `cd frontend && npm ...` forms.

### Task 5: Track residual manual verification where infrastructure cannot be containerized locally

**Files:**
- Create: `docs/architecture/testing/manual-verification.md`
- Read: `docker-compose.local.yml`
- Read: `docs/stressTest/local-capacity-runbook.md`
- Read: `docs/stressTest/distributed-capacity-runbook.md`

- [x] Create a table with columns: dependency, command, environment, owner, evidence file, expiration date, and automated replacement path.
- [x] Add one row for any RocketMQ scenario that is not containerized in Task 2.
- [x] Add one row for any capacity or distributed run that requires the local Docker stack.
- [x] Remove a row when an automated test replaces it.

**Run:**
```bash
test -f docs/architecture/testing/manual-verification.md
rg "dependency|owner|expiration date|automated replacement path|RocketMQ" docs/architecture/testing/manual-verification.md
```

**Expected:** Every manual verification item has a command, owner, evidence destination, and removal condition.

### Task 6: Review and hand off scoped testing foundation changes

**Files:**
- Modify: `docs/architecture/plans/P2-01-testing-foundation-plan.md`
- Modify: `.github/workflows/canvas-ci.yml`
- Modify: `backend/canvas-engine/pom.xml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasIntegrationTestBase.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasRocketMqTestSupport.java`
- Create: `docs/architecture/testing/test-layer-map.md`
- Create: `docs/architecture/testing/manual-verification.md`
- Create: `docs/architecture/evidence/P2-01-testing-foundation.md`

- [x] Review testing foundation files and evidence for this plan.
- [x] Record verification results in `docs/architecture/evidence/P2-01-testing-foundation.md`.
- [x] Leave commit/staging to the user because this working tree contains unrelated active changes.

**Run:**
```bash
git diff -- docs/architecture/plans/P2-01-testing-foundation-plan.md docs/architecture/specs/P2-01-testing-foundation-spec.md .github/workflows/canvas-ci.yml backend/canvas-engine/pom.xml backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport docs/architecture/testing docs/architecture/evidence/P2-01-testing-foundation.md
```

**Expected:** The handoff evidence names the files changed, commands run, and remaining manual substitutes; no commit is created unless the user explicitly requests one.
