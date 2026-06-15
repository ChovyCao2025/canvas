# DDD-C09EB Canvas Trigger Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:50:00+08:00

## Scope

Ported the legacy behavior trigger route into final modules:

- `POST /canvas/trigger/behavior`

The final controller preserves the compatibility envelope, parses the legacy raw JSON body, validates required `canvasId`, `userId`, `eventCode`, and `eventId` fields, and delegates a typed behavior trigger command to a final canvas facade.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Copernicus `019ec8be-5a43-72d2-b7a6-8dbfb5d8ef38` before marking DDD-C09EB RUNNING. The coordinator continued local RED/GREEN work without idle waiting. After one bounded wait timeout, coordinator closed Copernicus with previous_status `running`; a shutdown notification arrived after close.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasTriggerControllerCompatibilityTest test`
  - Failed before implementation because `CanvasTriggerFacade` and `CanvasTriggerController` were missing.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasTriggerControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 96 controllers / 798 endpoints; `route:/canvas/trigger` removed; next top gap `route:/cdp/events`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09EB files passed.

## Accepted concerns

- The final module records deterministic acceptance data rather than publishing to the legacy Disruptor.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
