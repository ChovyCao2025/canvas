# DDD-C09DM Coordinator Closeout

Status: DONE_WITH_CONCERNS

Scope:
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasPreferenceFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasPreferenceApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CanvasPreferenceCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasPreferenceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasPreferenceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasPreferenceControllerCompatibilityTest.java`

TDD:
- RED: `mvn test -pl canvas-context-canvas -Dtest=CanvasPreferenceApplicationServiceTest` failed before implementation because `CanvasPreferenceApplicationService` did not exist.
- GREEN: application test passed with 2 tests, 0 failures, 0 errors.

Verification:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasPreferenceApplicationServiceTest`
  - Passed: 2 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasPreferenceControllerCompatibilityTest test`
  - Passed: 3 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed: reactor BUILD SUCCESS.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed: current `canvas-web` 84 controllers / 773 endpoints.
  - `/canvas/preferences` removed from top route gaps.
  - Cutover still blocked globally by remaining route parity gaps.
- Strict old-coupling scan over DDD-C09DM files
  - Passed: no matches for old `R`, `PageResult`, `TenantContext`, old collaboration services, MyBatis, or `backend/canvas-engine` coupling.
- `git diff --check` over DDD-C09DM files and coordination state
  - Passed: no whitespace errors.

Accepted concerns:
- The new catalog is a compact deterministic in-memory final-module seed for route parity; durable preference persistence remains broader cutover work.
- The old controller also contains `GET /canvas/{canvasId}/collaboration/summary`; that endpoint is intentionally left for the separate `family:CanvasCollaboration` gap.
