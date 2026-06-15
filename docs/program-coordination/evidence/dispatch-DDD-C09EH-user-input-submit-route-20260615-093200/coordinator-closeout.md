# DDD-C09EH Coordinator Closeout

Task: port legacy `POST /user-input/responses/{responseId}/submit` into final canvas web module.

Worker:
- Dirac `019ec8ed-229c-7800-a047-1b36c333db66`
- Timed out once with no evidence file and was closed with previous status `running`.
- Coordinator retained useful partial implementation and fixed the compile issue in the compatibility test.

Changes:
- Added final `UserInputFacade`.
- Adapted `UserInputApplicationService` to implement the facade while preserving its existing submit behavior.
- Added final `UserInputController` for `POST /user-input/responses/{responseId}/submit`.
- Added focused compatibility coverage for path `responseId`, request body response/operator mapping, old success envelope, and API_001 bad-request envelope when final service rejects the response.

Verification:
- RED/current failure: focused Maven initially failed during test compile because the route/controller work was incomplete and the nested map assertion needed a type-safe assertion.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=UserInputControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed with reactor `BUILD SUCCESS`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 100 controllers / 818 endpoints after DDD-C09EG and DDD-C09EH.
  - `route:/user-input` removed from the top gap list.
- Strict old-coupling scan over touched final files:
  - No `org.chovy.canvas.engine`, old service/mapper/entity/common imports, `canvas-engine`, `UserInputService`, or `org.chovy.canvas.common.R` matches.
- `git diff --check` over touched files passed.

Accepted concerns:
- Worker did not produce a normal return packet.
- The final route uses existing `UserInputApplicationService` persistence semantics; broader runtime coverage remains with existing service/persistence tests.
- Global cutover remains blocked by controller count parity.
