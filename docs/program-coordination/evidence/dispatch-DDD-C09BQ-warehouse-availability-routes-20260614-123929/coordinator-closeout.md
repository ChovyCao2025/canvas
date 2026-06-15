# DDD-C09BQ Coordinator Closeout

Date: 2026-06-14T13:02:00+08:00

Status: DONE_WITH_CONCERNS

Worker: Avicenna `019ec476-c5aa-7932-982f-8622f8032a88`

## Result

Added final-module warehouse availability compatibility coverage for
`route:/warehouse/availability`. The new `CdpWarehouseAvailabilityController`
exposes all 8 legacy warehouse availability endpoints through `canvas-web`,
backed by compact deterministic `canvas-context-cdp` API, application, and
domain classes.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseAvailabilityApplicationServiceTest`
  passed: 3 tests, BUILD SUCCESS.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseAvailabilityControllerCompatibilityTest test`
  passed: 2 tests, reactor BUILD SUCCESS.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 36 controllers / 567 endpoints, with
  `route:/warehouse/availability` removed from the reported top gaps. The next
  top gap is `route:/canvas/tag-definitions`.
- Strict old-coupling scan over the final Warehouse Availability paths returned
  no matches.
- Scoped `git diff --check` returned no findings.

## Accepted Concerns

- This is a compact deterministic compatibility seed only.
- Durable warehouse availability persistence, incident scan semantics,
  freshness-window evaluation, tenant context resolution, and legacy service
  parity remain out of scope for this route parity batch.
- Global route parity still blocks DDD-C09 final cutover.

## Rollback

Remove the six CdpWarehouseAvailability code/test files and revert only the
DDD-C09BQ coordination/evidence entries.
