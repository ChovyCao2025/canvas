# DDD-C09CW Coordinator Closeout

Task: DDD-C09CW `/canvas/execution-requests` route compatibility seed

Status: DONE_WITH_CONCERNS

Worker:
- Hegel `019ec7d3-24bf-7b30-883b-8b7d8facedf4`
- Returned sidecar contract review at `docs/program-coordination/evidence/dispatch-DDD-C09CW-execution-requests-routes-20260615-sidecar/execution-requests-compatibility-sidecar.md`.

Verification:
- RED: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=ExecutionRequestApplicationServiceTest`
  - Failed as expected because `ExecutionRequestFacade` and `ExecutionRequestApplicationService` were missing.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=ExecutionRequestApplicationServiceTest`
  - 6 tests run, 0 failures, 0 errors.
- Web compatibility: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ExecutionRequestControllerCompatibilityTest test`
  - 3 tests run, 0 failures, 0 errors.
- Production compile: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Reactor build success.
- Cutover preflight: `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Current `canvas-web`: 68 controllers / 727 endpoints.
  - `/canvas/execution-requests` removed from top route gaps.
  - Next top gap: `route:/canvas/mq-trigger-rejected`.
- Strict old-coupling scan over DDD-C09CW production files:
  - Clean for old mapper/DO, Disruptor service, rate limiter, tenant/security context, MyBatis Plus, and old engine coupling strings.
- Scoped diff whitespace check:
  - Clean.
- Dispatch state check:
  - `ok: true`.

Accepted concerns:
- Global cutover remains blocked by route parity: old `canvas-engine` web has 142 controllers / 806 endpoints; final `canvas-web` has 68 controllers / 727 endpoints.
- Execution request implementation is a compact deterministic seed. Durable persistence, replay rate limiting, security-principal operator extraction, and real immediate dispatch remain out of scope for this route batch.
