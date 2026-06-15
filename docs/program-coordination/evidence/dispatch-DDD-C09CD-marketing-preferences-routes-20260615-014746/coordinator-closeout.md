# DDD-C09CD Marketing Preferences Routes Closeout

- Status: DONE_WITH_CONCERNS
- Worker: Parfit (`019ec73e-e8cf-76f0-940d-cddd2883b002`)
- Scope: `/canvas/marketing-preferences` five-route legacy family

## Implemented

- Added final-module marketing preference facade, application service, and deterministic catalog.
- Added final `canvas-web` controller for:
  - `GET /canvas/marketing-preferences/users/{userId}`
  - `PUT /canvas/marketing-preferences/users/{userId}/consents/{channel}`
  - `PUT /canvas/marketing-preferences/users/{userId}/channels/{channel}`
  - `POST /canvas/marketing-preferences/users/{userId}/suppressions`
  - `PUT /canvas/marketing-preferences/suppressions/{id}/deactivate`
- Preserved compatibility behaviors that matter for callers: default tenant `0`, channel/status normalization, suppression state, report summary shape, tenant isolation, and `API_001` bad-request envelopes.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingPreferenceApplicationServiceTest`
  - 2 tests, 0 failures
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingPreferenceControllerCompatibilityTest test`
  - 3 tests, 0 failures
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor build success through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - current `canvas-web`: 49 controllers / 649 endpoints
  - `/canvas/marketing-preferences` removed from reported top gaps
  - next top gap: `route:/canvas/tag-import-sources`
- Strict old-coupling scan over final MarketingPreference production paths
  - no matches
- Scoped `git diff --check`
  - clean
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - passed

## Accepted Concerns

- Compact deterministic in-memory compatibility seed only.
- Durable `marketing_consent`, `customer_channel`, and `marketing_suppression` persistence remains out of scope.
- Global cutover remains blocked by route parity.
