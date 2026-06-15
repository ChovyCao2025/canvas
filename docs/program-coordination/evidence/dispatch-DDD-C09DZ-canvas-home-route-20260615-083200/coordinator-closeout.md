# DDD-C09DZ Canvas Home Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:36:00+08:00

## Scope

Ported the legacy home overview route into final modules:

- `GET /canvas/home/overview`

The final controller preserves the compatibility envelope, normalizes `days` to the legacy 1..30 range with fallback 7, and returns the legacy overview shape: `range`, `summary`, `trend`, `topCanvases`, and `attentionItems`.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker McClintock `019ec8b1-fd72-7800-ae1a-eb290bfda2d8` before marking DDD-C09DZ RUNNING. The coordinator continued local RED/GREEN work without idle waiting. After one bounded wait timeout, coordinator closed McClintock with previous_status `running`; a shutdown notification arrived after close.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=HomeOverviewControllerCompatibilityTest test`
  - Failed before implementation because `HomeOverviewFacade` and `HomeOverviewController` were missing.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=HomeOverviewControllerCompatibilityTest test`
  - Passed: 1 test, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 94 controllers / 796 endpoints; `route:/canvas/home` removed; next top gap `route:/canvas/marketing-platform`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09DZ files passed.

## Accepted concerns

- The final module uses deterministic overview data rather than legacy mapper/Doris aggregation.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
