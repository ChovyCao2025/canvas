# DDD-C09DI Coordinator Closeout

Task: Canvas Async Tasks route parity
Status: DONE_WITH_CONCERNS
Closed at: 2026-06-15T06:17:00+08:00

## Files Changed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/AsyncTaskFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/AsyncTaskApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/AsyncTaskCatalog.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/AsyncTaskApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/AsyncTaskController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/AsyncTaskControllerCompatibilityTest.java`

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=AsyncTaskApplicationServiceTest`
  - Failed before implementation because `AsyncTaskFacade` and `AsyncTaskApplicationService` did not exist.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=AsyncTaskApplicationServiceTest`
  - Passed: 2 tests, 0 failures, 0 errors.
- Web compatibility: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AsyncTaskControllerCompatibilityTest test`
  - Passed: 3 tests, 0 failures, 0 errors.
- Production compile: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed: reactor BUILD SUCCESS.
- Preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed: current canvas-web 80 controllers / 765 endpoints; `route:/canvas/async-tasks` removed from top gaps; next top gap `route:/canvas/execution`; cutoverReady remains false.
- Strict old-coupling scan over DDD-C09DI files
  - Passed: no matches.
- Scoped whitespace check: `git diff --check -- <DDD-C09DI files and coordination docs>`
  - Passed: no whitespace errors.
- Dispatch state: `node tools/program-coordination/check-dispatch-state.mjs .`
  - Passed before closeout edits: `ok: true`.

## Accepted Concerns

- This is a compact deterministic final-module compatibility seed for the two public async task read routes.
- Darwin recommended platform ownership; coordinator kept the already reserved execution-domain scope because the cutover target is execution/task status read parity and adjacent route seeds live under `web/execution`.
- Durable mapper-backed async task/subscription persistence and producer-facing lifecycle mutation parity remain outside this batch.
- Global DDD-C09 cutover remains blocked by broader route parity.
