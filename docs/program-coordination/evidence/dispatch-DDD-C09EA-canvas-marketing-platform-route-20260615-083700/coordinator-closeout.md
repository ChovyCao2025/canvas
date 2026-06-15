# DDD-C09EA Canvas Marketing Platform Route Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T08:44:00+08:00

## Scope

Ported the legacy marketing platform control-plane route into final modules:

- `GET /canvas/marketing-platform/control-plane`

The final controller preserves the compatibility envelope, defaults missing tenant scope to tenant `0`, accepts `X-Tenant-Id` for scoped summaries, and returns the legacy control-plane summary shape with capabilities, integration lanes, integration assets, readiness gate, and action items.

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Volta `019ec8b6-effe-7cd3-a7dc-9d61af45af95` before marking DDD-C09EA RUNNING. The coordinator continued local RED/GREEN work without idle waiting. Volta's same-scope test work added a useful tenant header contract but used reflective construction and a too-large `Map.of`; the coordinator retained the tenant-header coverage in a typed compatibility test. After one bounded wait timeout, coordinator closed Volta with previous_status `running`; a shutdown notification arrived after close.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingPlatformControlPlaneControllerCompatibilityTest test`
  - Failed before implementation because `MarketingPlatformControlPlaneFacade` and `MarketingPlatformControlPlaneController` were missing.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingPlatformControlPlaneControllerCompatibilityTest test`
  - Passed: 2 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 95 controllers / 797 endpoints; `route:/canvas/marketing-platform` removed; next top gap `route:/canvas/trigger`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09EA files passed.

## Accepted concerns

- The final module returns deterministic control-plane summary data rather than reading the platform evidence provider.
- No normal worker-return packet was produced before shutdown.
- Global cutover remains blocked by remaining controller/endpoint gaps.
