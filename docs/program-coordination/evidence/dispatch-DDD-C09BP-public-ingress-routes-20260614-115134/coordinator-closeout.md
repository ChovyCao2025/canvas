# DDD-C09BP Coordinator Closeout

Date: 2026-06-14T12:25:00+08:00

Status: DONE_WITH_CONCERNS

Worker: Herschel `019ec449-327a-7142-960b-87dd888bb8da`

## Result

Added final-module public ingress compatibility coverage for `route:/public`.
The new `PublicIngressController` exposes all 8 legacy public endpoints through
`canvas-web`, backed by compact deterministic `canvas-platform` API,
application, and domain classes.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=PublicIngressApplicationServiceTest`
  passed: 2 tests, BUILD SUCCESS.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=PublicIngressControllerCompatibilityTest test`
  passed: 3 tests, reactor BUILD SUCCESS.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reported `canvas-web` 35 controllers / 559 endpoints, with `route:/public`
  removed from the reported top gaps. The next top gap is
  `route:/warehouse/availability`.
- Strict old-coupling scan over the final PublicIngress paths returned no
  matches.
- Scoped `git diff --check` returned no findings.

## Accepted Concerns

- This is a compact deterministic compatibility seed only.
- Durable marketing form persistence, WhatsApp verification/signature semantics,
  asset upload callback handling, monitoring ingestion, and external provider
  behavior remain out of scope for this route parity batch.
- Global route parity still blocks DDD-C09 final cutover.

## Rollback

Remove the six PublicIngress code/test files and revert only the DDD-C09BP
coordination/evidence entries.
