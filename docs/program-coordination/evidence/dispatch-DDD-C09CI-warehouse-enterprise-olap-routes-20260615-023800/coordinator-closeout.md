# DDD-C09CI Coordinator Closeout

date: 2026-06-15
task id: DDD-C09CI
dispatch id: dispatch-DDD-C09CI-warehouse-enterprise-olap-routes-20260615-023800
status: DONE_WITH_CONCERNS

## Scope

Migrated the legacy `/warehouse/enterprise-olap/evidence` route family into final modules:

- `POST /warehouse/enterprise-olap/evidence`
- `GET /warehouse/enterprise-olap/evidence/latest`
- `GET /warehouse/enterprise-olap/evidence/proof`
- `POST /warehouse/enterprise-olap/evidence/collect`
- `GET /warehouse/enterprise-olap/evidence/collections`

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseEnterpriseOlapEvidenceApplicationServiceTest`
  - Result: 2 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseEnterpriseOlapEvidenceControllerCompatibilityTest test`
  - Result: 3 tests run, 0 failures, 0 errors.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Result: reactor build success.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: `canvas-web` now has 54 controllers / 674 endpoints; `/warehouse/enterprise-olap` is removed from reported top gaps. Global cutover remains blocked by route parity; next top gap is `route:/warehouse/metric-change-reviews`.
- Strict old-coupling scan over C09CI production files
  - Result: no matches for old engine/domain/TenantContext coupling patterns.
- `git diff --check -- <C09CI reserved files and coordination docs>`
  - Result: clean.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: `{ "ok": true }`.

## Notes

- The first test expectations from the sidecar used a non-legacy `operator_gate` key and expected `collections(0)` to fail. The coordinator corrected this to the old contract: operator keys are `backup_restore`, `ingestion_replay`, `runbook_drill`; non-positive collection limits fall back to 20.
- The first application/web test failures also exposed proof-order semantics: `latest` and `proof` include missing automated evidence before operator evidence, so overall status is `FAIL` until automated evidence exists.
- No `backend/canvas-engine/**` or `pom.xml` files were edited.
