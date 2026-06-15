# DDD-C09EG Coordinator Closeout

Task: port legacy `GET /platform/workstreams` into final platform web module.

Worker:
- Singer `019ec8ec-c5f6-7850-90c4-75d5f9fa37aa`
- Timed out once with no evidence file and was closed with previous status `running`.
- Coordinator retained the useful RED test and completed the missing final controller locally.

Changes:
- Added final `PlatformWorkstreamController` at `GET /platform/workstreams`.
- Reused existing `PlatformWorkstreamFacade` / `WorkstreamStatusView`.
- Added focused compatibility coverage for old success envelope and stable workstream status fields.

Verification:
- RED/current failure: focused Maven initially failed because `PlatformWorkstreamController` was missing.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=PlatformWorkstreamControllerCompatibilityTest test`
  - Passed: 1 test, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile`
  - Passed with reactor `BUILD SUCCESS`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 100 controllers / 818 endpoints after DDD-C09EG and DDD-C09EH.
  - `route:/platform` removed from the top gap list.
- Strict old-coupling scan over touched final files:
  - No `org.chovy.canvas.engine`, old service/mapper/entity/common imports, `canvas-engine`, `PlatformWorkstreamService`, or `org.chovy.canvas.common.R` matches.
- `git diff --check` over touched files passed.

Accepted concerns:
- Worker did not produce a normal return packet.
- Global cutover remains blocked by controller count parity.
