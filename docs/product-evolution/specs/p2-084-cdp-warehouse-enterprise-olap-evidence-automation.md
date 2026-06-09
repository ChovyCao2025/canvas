# P2-084 - CDP Warehouse Enterprise OLAP Evidence Automation Spec

Priority: P2
Sequence: 084
Source: production OLAP completion audit after P2-083 operational evidence
Implementation plan: `../plans/p2-084-cdp-warehouse-enterprise-olap-evidence-automation-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Turn P2-083's on-demand enterprise OLAP evidence evaluation into an auditable automated collection loop that can run on a schedule, persist collection runs, and give operators a manual trigger plus history before release gates are evaluated.

## Current Baseline

- P2-081 creates the fail-closed enterprise OLAP readiness gate.
- P2-083 adds live Doris metric/workload evidence, operator drill evidence, synthetic ODS replay reuse, and proof-row normalization.
- The remaining production gap is operational cadence: without scheduled collection and run history, a production PASS can still depend on someone manually calling the proof endpoint at the right time and keeping external records elsewhere.

## External References

The automation model remains tied to official OLAP operations guidance:

- Apache Doris Monitoring Metrics: https://doris.apache.org/docs/4.x/admin-manual/maint-monitor/metrics/
  - FE and BE expose Prometheus-compatible metrics that should be scraped continuously rather than inspected only during release review.
- Apache Doris Monitoring and Alerting: https://doris.apache.org/docs/dev/admin-manual/maint-monitor/monitor-alert/
  - Doris documents a Prometheus, Grafana, and Alertmanager style monitoring deployment, which implies recurring collection and alert evidence.
- Apache Doris Workload Group: https://doris.apache.org/docs/4.x/admin-manual/workload-management/workload-group/
  - Workload Group resource controls must remain present over time, not just during an initial setup check.
- Apache Doris Backup and Restore: https://doris.apache.org/docs/3.x/admin-manual/data-admin/backup-restore/overview/
  - Backup and restore are separate recoverability operations whose drills need durable evidence.

## In Scope

- Persist a tenant-scoped enterprise OLAP evidence collection run for each manual or scheduled cycle.
- Add an automation service that records the current automated evidence rows from P2-083:
  - `doris_metrics`
  - `compaction_health`
  - `workload_isolation`
  - `query_slo`
  - synthetic or warehouse-driven `ingestion_replay`
- Add an `evidence_collection` proof row to enterprise OLAP readiness so stale or missing automated collection runs block production readiness.
- Keep operator-only gates (`backup_restore`, `runbook_drill`, manual `ingestion_replay`) in the existing append-only evidence ledger and do not overwrite them with scheduler guesses.
- Add a disabled-by-default scheduler with tenant id, fixed delay, lease TTL, actor, and source trigger configuration.
- Add operator APIs to manually run the collection loop and inspect recent collection runs.
- Keep production readiness proof side-effect free: proof may read evidence, but the scheduler or manual collection endpoint performs writes.

## Out Of Scope

- Provisioning Prometheus, Grafana, Alertmanager, object storage, or Doris clusters.
- Executing backup or restore operations automatically.
- Running synthetic ODS writes from the enterprise OLAP proof endpoint.
- Claiming enterprise OLAP is production-complete without a live configured scheduler, fresh PASS/WARN rows, and successful operator drills.

## Automation Semantics

1. Scheduler is disabled by default.
2. Only one collection cycle runs per node at a time.
3. If the warehouse lease service is available, scheduled collection uses a tenant-scoped lease to avoid duplicate distributed runs.
4. A collection run persists `RUNNING`, then finishes as `PASS`, `WARN`, or `FAIL`.
5. A failed collector still records a `FAIL` run with the error reason.
6. Automated evidence rows are appended; old evidence is not mutated.
7. Collection run status is the worst status of rows recorded during that cycle.
8. Manual collection uses the authenticated tenant and actor.

## API And Model Contract

Add `CdpWarehouseEnterpriseOlapEvidenceCollectionService` with:

- `run(Long tenantId, String trigger, String actor)`
- `recentRuns(Long tenantId, int limit)`

Each run returns:

- `id`
- `tenantId`
- `triggerType`
- `status`
- `startedAt`
- `finishedAt`
- `evidenceCount`
- `passCount`
- `warnCount`
- `failCount`
- `reason`
- `createdBy`

Extend `CdpWarehouseEnterpriseOlapEvidenceController` with:

- `POST /warehouse/enterprise-olap/evidence/collect`
- `GET /warehouse/enterprise-olap/evidence/collections`

## Acceptance Criteria

- P2-084 spec and plan are indexed after P2-083.
- A migration creates `cdp_warehouse_enterprise_olap_evidence_collection_run`.
- Evidence service exposes a collection method that persists automated evidence rows without duplicating operator-only rows.
- Collection service tests prove:
  - healthy live Doris query/metric/workload evidence plus synthetic replay records PASS run counts;
  - collector exceptions record a FAIL run;
  - recent run listing is tenant scoped and bounded;
  - missing or stale collection runs produce `enterprise_olap:evidence_collection` FAIL proof.
- Scheduler tests prove disabled scheduler is inert, enabled scheduler uses a lease when present, and overlapping local cycles are skipped.
- Controller tests prove manual collection and run history use the authenticated tenant and actor.
- Focused verification script runs P2-081, P2-083, and P2-084 tests with Java 21.

## Rollout

1. Deploy the run-history migration and services.
2. Configure `CANVAS_DORIS_FE_METRICS_URLS`, `CANVAS_DORIS_BE_METRICS_URLS`, and Doris JDBC.
3. Enable `canvas.warehouse.enterprise-olap-evidence-scheduler.enabled=true` only in environments where Doris metrics and JDBC are reachable.
4. Keep backup/restore and runbook drill evidence as explicit operator ledger entries after real drills.
5. Treat stale or failing collection runs as a release blocker until the next scheduled or manual cycle records fresh PASS/WARN evidence.
