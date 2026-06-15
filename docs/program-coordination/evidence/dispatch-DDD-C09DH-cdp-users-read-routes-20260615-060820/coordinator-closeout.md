# DDD-C09DH Coordinator Closeout

Task: CDP Users read route parity
Status: DONE_WITH_CONCERNS
Closed at: 2026-06-15T06:10:20+08:00

## Files Changed

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpUserReadFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpUserReadApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpUserReadCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpUserReadApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpUserReadController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpUserReadControllerCompatibilityTest.java`

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpUserReadApplicationServiceTest`
  - Failed before implementation because `CdpUserReadFacade` and `CdpUserReadApplicationService` did not exist.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpUserReadApplicationServiceTest`
  - Passed: 2 tests, 0 failures, 0 errors.
- Web compatibility: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpUserReadControllerCompatibilityTest test`
  - Passed: 3 tests, 0 failures, 0 errors.
- Production compile: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed: reactor BUILD SUCCESS.
- Preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed: current canvas-web 79 controllers / 763 endpoints; `route:/cdp/users` removed from top gaps; next top gap `route:/canvas/async-tasks`; cutoverReady remains false.
- Strict old-coupling scan over DDD-C09DH files
  - Passed: no matches.
- Scoped whitespace check: `git diff --check -- <DDD-C09DH files and coordination docs>`
  - Passed: no whitespace errors.
- Dispatch state: `node tools/program-coordination/check-dispatch-state.mjs .`
  - Passed before closeout edits: `ok: true`.

## Accepted Concerns

- This is a compact deterministic final-module compatibility seed for the three missing CDP user read routes.
- Durable mapper-backed directory/insight parity and full legacy masking rules remain outside this batch.
- Global DDD-C09 cutover remains blocked by broader route parity.
