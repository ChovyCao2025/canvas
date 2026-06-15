# DDD-C09EC CDP Events Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:58:00+08:00

## Scope

Ported the legacy CDP event ingestion route into final modules:

- `POST /cdp/events/track`

The final controller preserves the compatibility envelope, maps `X-Cdp-Write-Key` and optional `X-Tenant-Id` into a final `CdpWriteKeyView`, defaults a missing body to an empty batch, and delegates the typed batch command to the existing final `CdpEventIngestionFacade`.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Carver `019ec8c4-c96d-7e51-80b5-c42d532faced` before marking DDD-C09EC RUNNING. The coordinator continued local RED/GREEN work without idle waiting. After one bounded wait timeout, coordinator closed Carver with previous_status `running`; a shutdown notification arrived after close.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpEventIngestionControllerCompatibilityTest test`
  - Failed before implementation because `CdpEventIngestionController` was missing.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpEventIngestionControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 97 controllers / 799 endpoints; `route:/cdp/events` removed; next top gap `route:/delivery`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09EC files passed.

## Accepted concerns

- The final controller currently maps the write key header into a lightweight final view rather than using the old write-key auth service.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
