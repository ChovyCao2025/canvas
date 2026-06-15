# DDD-C09CY Coordinator Closeout

Task: DDD-C09CY `/cdp/realtime-audiences` and `/cdp/audiences` route compatibility seed

Status: DONE_WITH_CONCERNS

Worker:
- Jason `019ec7e3-3f0f-7011-a074-cd3c57735ea0`
- Returned sidecar contract review at `docs/program-coordination/evidence/dispatch-DDD-C09CY-realtime-audience-routes-20260615-sidecar/realtime-audience-compatibility-sidecar.md`.

Verification:
- RED: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=RealtimeAudienceApplicationServiceTest`
  - Failed as expected because `RealtimeAudienceFacade` and `RealtimeAudienceApplicationService` were missing.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=RealtimeAudienceApplicationServiceTest`
  - 4 tests run, 0 failures, 0 errors.
- Web compatibility: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=RealtimeAudienceControllerCompatibilityTest test`
  - 3 tests run, 0 failures, 0 errors.
- Production compile: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Reactor build success.
- Cutover preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Current `canvas-web`: 70 controllers / 736 endpoints.
  - `/cdp/audiences` and `/cdp/realtime-audiences` removed from top route gaps.
  - Next top gap: `route:/cdp/write-keys`.
- Strict old-coupling scan over DDD-C09CY production files:
  - Clean for old realtime audience service, tenant context, old CDP domain package, old common envelope, and old engine coupling strings.
- Scoped diff whitespace check:
  - Clean.
- Dispatch state check:
  - `ok: true`.

Accepted concerns:
- Global cutover remains blocked by route parity: old `canvas-engine` web has 142 controllers / 806 endpoints; final `canvas-web` has 70 controllers / 736 endpoints.
- Realtime audience implementation is a compact deterministic seed. Durable event de-duplication, bitmap persistence, safe-size blocking, and full legacy snapshot row shape remain out of scope for this route batch.
