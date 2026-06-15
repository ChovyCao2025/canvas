# DDD-C09DX Canvas Event Attributes Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:22:00+08:00

## Scope

Ported the legacy event attribute discovery route into final modules:

- `GET /canvas/event-attributes/discovered`

The final controller preserves the compatibility envelope, delegates status filtering to a final CDP facade, and returns the legacy discovered attribute fields: `id`, `eventCode`, `attrName`, `attrType`, `status`, `sampleValue`, `firstSeenAt`, and `lastSeenAt`.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Planck `019ec89e-61b0-7e73-a5cf-485e229f65a8` before marking DDD-C09DX RUNNING. The coordinator continued local RED/GREEN work without idle waiting and integrated useful same-scope worker changes for the final CDP facade contract. After one bounded wait timeout, coordinator closed Planck with previous_status `running`; a later shutdown notification arrived.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=EventAttributeDiscoveryControllerCompatibilityTest test`
  - Failed before implementation because `EventAttributeDiscoveryController` was missing; a later RED also caught the missing facade constructor contract.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=EventAttributeDiscoveryControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 92 controllers / 794 endpoints; `route:/canvas/event-attributes` removed; next top gap `route:/canvas/events`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09DX files passed.

## Accepted concerns

- The route uses deterministic final-module seed data rather than durable legacy discovery persistence.
- DDD-C09DX also recovered the prior contactability facade/controller mismatch discovered during focused test compilation.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
