# DDD-C09CH Coordinator Closeout

date: 2026-06-15
task id: DDD-C09CH
dispatch id: dispatch-DDD-C09CH-warehouse-catalog-routes-20260615-022850
status: DONE_WITH_CONCERNS

## Scope

Migrated the legacy `/warehouse/catalog` route family into final modules:

- `GET /warehouse/catalog/datasets`
- `POST /warehouse/catalog/datasets`
- `POST /warehouse/catalog/lineage`
- `GET /warehouse/catalog/datasets/{datasetKey}/lineage`
- `GET /warehouse/catalog/datasets/{datasetKey}/lineage/transitive`

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseCatalogApplicationServiceTest`
  - Result: 4 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseCatalogControllerCompatibilityTest test`
  - Result: 3 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Result: reactor build success.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: `canvas-web` now has 53 controllers / 669 endpoints; `/warehouse/catalog` is removed from the reported top gaps. Global cutover remains blocked by route parity; next top gap is `route:/warehouse/enterprise-olap`.
- Strict old-coupling scan over C09CH production files
  - Result: no matches for old engine/domain/TenantContext coupling patterns.
- `git diff --check -- <C09CH reserved files and coordination docs>`
  - Result: clean.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: `{ "ok": true }`.

## Notes

- The first context test run failed at test compile time because AssertJ inferred `ObjectAssert<Map<String,Object>>` for a `singleElement()` chain. The coordinator fixed the assertion to explicitly check size and then assert on the single map, preserving the same behavior contract.
- No `backend/canvas-engine/**` or `pom.xml` files were edited.
- Tests are limited to meaningful behavior and compatibility risks: tenant defaulting, dataset upsert/filter normalization, lineage direction/depth, route aliases, `X-Tenant-Id`, and error envelope behavior.
