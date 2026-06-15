# DDD-C09CC Data Sources Routes Closeout

- Status: DONE_WITH_CONCERNS
- Worker: Sartre (`019ec72f-87f3-78a3-8e0a-8ceef03a4dc3`)
- Scope: `/canvas/data-sources` five-route legacy family

## Implemented

- Added final-module data-source facade, application service, and deterministic catalog.
- Added final `canvas-web` controller for:
  - `GET /canvas/data-sources`
  - `GET /canvas/data-sources/{id}/tables`
  - `POST /canvas/data-sources`
  - `PUT /canvas/data-sources/{id}`
  - `DELETE /canvas/data-sources/{id}`
- Preserved compatibility behaviors that matter for callers: `records` page shape, query forwarding, JDBC defaults, required field/type validation, tenant scoping, password masking, and `API_001` bad-request envelopes.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=DataSourceConfigApplicationServiceTest`
  - 4 tests, 0 failures
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=DataSourceConfigControllerCompatibilityTest test`
  - 3 tests, 0 failures
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor build success through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - current `canvas-web`: 48 controllers / 644 endpoints
  - `/canvas/data-sources` removed from reported top gaps
  - next top gap: `route:/canvas/marketing-preferences`
- Strict old-coupling scan over final DataSourceConfig production paths
  - no matches
- Scoped `git diff --check`
  - clean
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - passed

## Accepted Concerns

- Compact deterministic in-memory compatibility seed only.
- Durable persistence, real JDBC metadata probing, production secret encryption, audit writes, and complete old `TenantContext` semantics remain out of scope.
- Global cutover remains blocked by route parity.
