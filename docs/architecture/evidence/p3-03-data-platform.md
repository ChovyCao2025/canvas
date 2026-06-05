# P3-03 Data Platform Evidence

Date: 2026-06-05

## Verdict

P3-03 is implemented as a thin proof-of-concept definition for audience compute history. It does not approve a full data platform, a separate data service, Flink, ClickHouse, lakehouse layers, or BI semantic serving.

The proof starts from source ownership, retention, PII classification, replay, deletion propagation, and operating evidence. Online canvas execution and audience membership checks must not depend on the data platform.

## Created Documents

- `docs/architecture/work-products/p3-03-data-platform/data-platform-source-inventory.md`
- `docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md`
- `docs/architecture/work-products/p3-03-data-platform/data-platform-contract-governance.md`

## Inventory Summary

The source inventory covers:

- canvas;
- execution;
- trace;
- DLQ;
- CDP profile/identity/tag;
- audience;
- notification/reach;
- consent/suppression;
- event log data.

The first POC includes `audience_compute_run`, `audience_definition`, `audience_stat`, and aggregate `perf_run_id` links to `event_log` or `canvas_execution` when needed.

The first POC excludes raw traces, raw identity values, message payloads, provider payloads, full BI/warehouse governance tables, and external datasource row data.

## Selected Slice

Selected slice: audience compute history.

Success metric: 99 percent of completed `audience_compute_run` rows visible in the serving model within 5 minutes, with zero raw PII fields and zero impact on online audience membership checks.

Stop criteria are recorded in `docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md`.

## Verification Commands

```bash
rg "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration
rg "EventLogDO|RocketMQ|MqTriggerMessage|MessageSendRecordDO|CdpUserIdentityDO" backend/canvas-engine/src/main/java/org/chovy/canvas
test -f docs/architecture/work-products/p3-03-data-platform/data-platform-source-inventory.md
rg "canvas|execution|trace|CDP|audience|notification|consent|PII|freshness|retention" docs/architecture/work-products/p3-03-data-platform/data-platform-source-inventory.md
test -f docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md
rg "Input sources|Transform|Storage|Serving API|SLA|Retention|PII|Cost|Rollback|Success metric|full data platform is deferred" docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md
test -f docs/architecture/work-products/p3-03-data-platform/data-platform-contract-governance.md
rg "schema versioning|compatibility|replay|ordering|backfill|deletion propagation|lineage|CDC|event contract" docs/architecture/work-products/p3-03-data-platform/data-platform-contract-governance.md
rg "contract test|freshness|dropped records|backfill duration|query latency|storage growth|stop criteria" docs/architecture/work-products/p3-03-data-platform/data-platform-poc-plan.md docs/architecture/evidence/p3-03-data-platform.md
```

Result: all documentation checks passed.

Backend schema command:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,AudienceComputeRunTrackingSchemaTest
```

Result: 4 tests run, 0 failures, 0 errors, 0 skipped.

The first schema-test run failed because Flyway version `V250` was duplicated by `V250__operator_visibility_and_testability.sql` and `V250__cdp_warehouse_synthetic_ods_data_path_probe.sql`. The warehouse synthetic probe migration was renumbered to `V251__cdp_warehouse_synthetic_ods_data_path_probe.sql`; the schema tests then passed.

No files were staged or committed.
