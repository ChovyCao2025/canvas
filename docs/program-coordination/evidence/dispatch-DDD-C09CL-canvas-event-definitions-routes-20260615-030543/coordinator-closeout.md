# DDD-C09CL Coordinator Closeout

Status: `DONE_WITH_CONCERNS`

## Scope Closed

Closed route gap: `route:/canvas/event-definitions`

Implemented final-module route seed for:

- `GET /canvas/event-definitions`
- `POST /canvas/event-definitions`
- `PUT /canvas/event-definitions/{id}`
- `DELETE /canvas/event-definitions/{id}`

Out of scope:

- `POST /canvas/events/report`

## Compatibility Protected

- List supports `page`, `size`, and optional `enabled`.
- List preserves legacy `id ASC` ordering.
- List response uses legacy `total + list` page shape.
- Create/update/delete invalidate meaningful event codes in the in-memory compatibility catalog.
- Controller maps `IllegalArgumentException` to the standard `API_001` bad-request envelope.

No ceremonial route-only test class was added; tests cover behavior and response compatibility.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=EventDefinitionApplicationServiceTest`
  passed: 2 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=EventDefinitionControllerCompatibilityTest test`
  passed: 2 tests, 0 failures.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  passed; current `canvas-web` is 57 controllers / 687 endpoints, and `route:/canvas/event-definitions` is no longer a top gap. Global `cutoverReady` remains `false`; next top gap is `route:/canvas/identity-types`.
- Strict old-coupling scan over DDD-C09CL production files returned no matches.
- Scoped `git diff --check` passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before closeout state update.

## Accepted Concerns

- Compact deterministic in-memory compatibility seed only; durable `event_definition` persistence and real tiered cache eviction are out of scope for this route-parity batch.
- No normal worker-return packet; the coordinator closed `Kierkegaard` after one bounded timeout and finished the exact reserved scope.
- Global route parity remains blocked by remaining gaps.
