# DDD-C09CJ Coordinator Closeout

date: 2026-06-15
task id: DDD-C09CJ
dispatch id: dispatch-DDD-C09CJ-warehouse-metric-change-reviews-routes-20260615-024600
status: DONE_WITH_CONCERNS

## Scope

Migrated the legacy `/warehouse/metric-change-reviews` route family into final modules:

- `GET /warehouse/metric-change-reviews`
- `POST /warehouse/metric-change-reviews`
- `POST /warehouse/metric-change-reviews/{reviewId}/approve`
- `POST /warehouse/metric-change-reviews/{reviewId}/reject`
- `POST /warehouse/metric-change-reviews/{reviewId}/apply`

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseMetricChangeReviewApplicationServiceTest`
  - Result: 3 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseMetricChangeReviewControllerCompatibilityTest test`
  - Result: 3 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Result: reactor build success.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: `canvas-web` now has 55 controllers / 679 endpoints; `/warehouse/metric-change-reviews` is removed from reported top gaps. Global cutover remains blocked by route parity; next top gap is `route:/canvas/api-definitions`.
- Strict old-coupling scan over C09CJ production files
  - Result: no matches for old engine/domain/TenantContext coupling patterns.
- `git diff --check -- <C09CJ reserved files and coordination docs>`
  - Result: clean.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: `{ "ok": true }`.

## Notes

- The retained worker tests were corrected to match legacy behavior: dataset/metric filters are trim-only and case-sensitive, while status filter is uppercased.
- The current/proposed metric assertions were corrected: `currentMetric` remains the request-time snapshot and `proposedMetric` carries the new expression.
- No `backend/canvas-engine/**` or `pom.xml` files were edited.
