# DDD-C09DP Coordinator Closeout

Status: `DONE_WITH_CONCERNS`

Routes covered:
- `GET /warehouse/offline-cycle/plan`
- `POST /warehouse/offline-cycle/run`
- `GET /warehouse/retention/plan`
- `POST /warehouse/retention/run`

Verification:
- RED: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseOfflineRetentionApplicationServiceTest` failed before implementation on missing facade/service types.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseOfflineRetentionApplicationServiceTest` passed, 2 tests / 0 failures.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseOfflineRetentionControllerCompatibilityTest test` passed, 2 tests / 0 failures.
- Compile: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests` passed.
- Preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported current canvas-web 87 controllers / 785 endpoints; `/warehouse/offline-cycle` and `/warehouse/retention` removed; next top gap `family:Canvas`.
- Strict old-coupling scan over DDD-C09DP files returned no matches.
- `git diff --check` over DDD-C09DP files and coordination docs passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed while RUNNING and again before closeout.

Accepted concerns:
- Implementation is deterministic final-module seed behavior, not the old MyBatis-backed warehouse operations/retention services.
- Sagan did not return a normal final packet; coordinator closed it after one bounded wait and no target file changes.
- Global cutover remains blocked by route parity.
