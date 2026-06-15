# DDD-C09BH Audience Routes Closeout

Date: 2026-06-14

## Scope

Added final-module compatibility coverage for the legacy `/canvas/audiences`
route family:

- `GET /canvas/audiences`
- `GET /canvas/audiences/source-fields`
- `POST /canvas/audiences/preview`
- `GET /canvas/audiences/{id}`
- `GET /canvas/audiences/ready`
- `POST /canvas/audiences`
- `PUT /canvas/audiences/{id}`
- `DELETE /canvas/audiences/{id}`
- `POST /canvas/audiences/{id}/compute`
- `GET /canvas/audiences/{id}/stat`

## Worker Handling

Turing `019ec392-b466-7330-b3bd-42e88eeaa730` was spawned as a bounded
sidecar worker. The coordinator kept the critical path local, wrote RED tests,
implemented the reserved files, and ran verification without idle polling. A
single short harvest returned a matching worker packet; the worker was then
closed.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=AudienceApplicationServiceTest`
  - passed, 2 tests
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AudienceControllerCompatibilityTest test`
  - passed, 3 tests
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - passed
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - passed; `canvas-web` advanced to 27 controllers / 487 endpoints
  - `route:/canvas/audiences` no longer appears in the reported top gaps
- strict old-coupling `rg` scan over the reserved Audience files
  - no matches

## Accepted Concerns

- Audience behavior is a deterministic compatibility seed, not durable
  persistence-backed production logic.
- Global cutover remains blocked by broader controller and endpoint parity
  gaps outside this dispatch.
