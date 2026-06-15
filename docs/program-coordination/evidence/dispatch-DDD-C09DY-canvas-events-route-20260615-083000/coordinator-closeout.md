# DDD-C09DY Canvas Events Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:31:00+08:00

## Scope

Ported the legacy canvas event report route into final modules:

- `POST /canvas/events/report`

The final controller preserves the compatibility envelope and delegates the raw JSON request body to a final canvas facade. The application service validates required `eventCode` and `userId` fields, returns deterministic acceptance data, and preserves `idempotencyKey` when present.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Bohr `019ec8a8-d6d8-7700-915d-5235fd3d6a03` before marking DDD-C09DY RUNNING. The coordinator continued local verification without idle waiting. After one bounded wait timeout, coordinator closed Bohr with previous_status `running`; a shutdown notification arrived after close.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasEventReportControllerCompatibilityTest test`
  - Failed before implementation because `CanvasEventReportController` was missing.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasEventReportControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 93 controllers / 795 endpoints; `route:/canvas/events` removed; next top gap `route:/canvas/home`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09DY files passed.

## Accepted concerns

- The route uses deterministic final-module reporting behavior rather than the legacy engine event-processing side effects.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
