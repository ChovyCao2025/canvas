# P2-083 - CDP Warehouse Enterprise OLAP Operational Evidence Spec

Priority: P2
Sequence: 083
Source: user-requested production-grade OLAP completion after P2-081 readiness gate
Implementation plan: `../plans/p2-083-cdp-warehouse-enterprise-olap-operational-evidence-plan.md`

## Goal

Turn the P2-081 enterprise OLAP readiness gate into a production evidence loop that can collect, persist, and evaluate live Doris and operator evidence instead of relying on hand-assembled proof rows.

## Current Baseline

- P2-021 through P2-080 provide CDP warehouse ingestion, materialization, BI gates, availability contracts, physical E2E certification, synthetic ODS proof, privacy erasure propagation, and audience bitmap rebuild automation.
- P2-081 adds a fail-closed enterprise OLAP readiness gate with required gates for warehouse readiness, window availability, consumer contracts, privacy erasure backlog, Doris metrics, workload isolation, backup/restore, compaction health, ingestion replay, and runbook drills.
- P2-081 still leaves the most important production gap open: Doris and operator evidence must be supplied externally, so a PASS is only credible when another system has already produced proof rows.

## External References

The production gates map to official OLAP operations references:

- Apache Doris Monitoring Metrics: https://doris.apache.org/docs/4.x/admin-manual/maint-monitor/metrics/
  - Doris exposes FE/BE metrics in Prometheus-compatible format and treats QPS, query error rate, query latency, connection count, thread pool queueing, disk, tablet count, and compaction score as monitoring and alerting inputs.
- Apache Doris Monitoring and Alerting: https://doris.apache.org/docs/dev/admin-manual/maint-monitor/monitor-alert/
  - Doris documents a Prometheus, Grafana, and Alertmanager oriented monitoring system for FE/BE metrics.
- Apache Doris Workload Group: https://doris.apache.org/docs/4.x/admin-manual/workload-management/workload-group/
  - Workload Group provides Doris in-process CPU, memory, IO, queueing, and concurrency isolation, with limitations that must be explicit in production evidence.
- Apache Doris Backup and Restore: https://doris.apache.org/docs/3.x/admin-manual/data-admin/backup-restore/overview/
  - Doris backup writes snapshots to remote repositories and restore is a separate recoverability operation that must be drilled.
- Apache Druid Compaction: https://druid.apache.org/docs/latest/data-management/compaction/
  - Druid frames compaction as a production segment optimization and ingestion conflict concern, reinforcing that compaction health is an OLAP production gate.
- Apache Pinot Architecture: https://docs.pinot.apache.org/architecture-and-concepts/concepts/architecture
  - Pinot separates controller, broker, server, minion, and segment storage roles, reinforcing that enterprise OLAP readiness is multi-component operational readiness.

## In Scope

- Persist tenant-scoped enterprise OLAP operational evidence for:
  - `backup_restore`
  - `runbook_drill`
  - manually certified `ingestion_replay` evidence when a synthetic probe is not used
- Collect live Doris evidence for:
  - `doris_metrics`
  - `compaction_health`
  - `workload_isolation`
  - `query_slo`
- Reuse recent synthetic ODS data-path probe runs as `ingestion_replay` evidence.
- Normalize all evidence into P2-081 `enterprise_olap:<gate>` proof rows.
- Expose operator APIs to record backup/restore, runbook, and manual ingestion replay evidence and to inspect latest proof rows.
- Fail closed when a live collector is missing, Doris metrics endpoints are not configured, workload group reads fail, or operator evidence is missing/stale.
- Keep readiness proof side-effect free by default: it reads latest evidence and recent probes, but does not trigger synthetic writes during proof generation.

## Out Of Scope

- Provisioning a Doris, Druid, Pinot, ClickHouse, Kafka, or Flink cluster.
- Implementing a full Prometheus, Grafana, or Alertmanager deployment.
- Running destructive backup/restore operations from readiness proof.
- Replacing P2-021 audience materialization or BI query engines.
- Claiming the product is fully enterprise deployed without live environment configuration and a current PASS evidence set.

## Evidence Semantics

Evidence status uses the same `PASS`, `WARN`, `FAIL` semantics as P2-081:

1. Missing required live or operator evidence is `FAIL`.
2. Unsupported or unknown status normalizes to `FAIL`.
3. Expired evidence is `FAIL`.
4. Live collector exceptions are `FAIL`.
5. A WARN gate makes the enterprise OLAP readiness `WARN` when there is no FAIL.
6. Production readiness proof includes every normalized enterprise evidence row before computing the P2-081 readiness summary.

## Required Production Evidence

| Gate | Evidence source | Required proof |
| --- | --- | --- |
| `doris_metrics` | Doris FE/BE Prometheus metrics | At least one fresh FE sample and one fresh BE sample with query errors, query latency, thread queueing, disk, tablet, and load metrics under policy. |
| `compaction_health` | Doris FE/BE Prometheus metrics | Compaction score, tablet health, and disk pressure are below fail thresholds. |
| `workload_isolation` | Doris `information_schema.workload_groups` or equivalent | BI, ingestion, and audience workloads have explicit CPU, memory, IO, concurrency, or queue controls. |
| `query_slo` | Doris query profile, audit log, or curated observability view | Representative BI, audience materialization, and ad-hoc segment query profiles satisfy freshness, sample count, latency, error, queueing, and memory policy. |
| `backup_restore` | Operator ledger | Recent backup and restore drill succeeded against remote repository evidence. |
| `ingestion_replay` | Synthetic ODS probe or operator ledger | Recent synthetic probe or replay drill proves accepted events become visible in Doris ODS. |
| `runbook_drill` | Operator ledger | Recent operational runbook or DR drill passed with owner and evidence payload. |

## API And Model Contract

Add `CdpWarehouseEnterpriseOlapEvidenceService` with:

- `recordOperatorEvidence(Long tenantId, EvidenceCommand command, String actor)`
- `latestEvidence(Long tenantId)`
- `proofEvidence(Long tenantId)`

Each evidence row returns:

- `id`
- `tenantId`
- `evidenceKey`
- `source`
- `status`
- `reason`
- `measuredAt`
- `expiresAt`
- `evidenceJson`
- `createdBy`

`CdpWarehouseProductionReadinessProofService` must read `proofEvidence` before invoking `CdpWarehouseEnterpriseOlapReadinessService`, so the existing fail-closed P2-081 gate evaluates current operational evidence.

`CdpWarehouseEnterpriseOlapEvidenceController` exposes:

- `POST /warehouse/enterprise-olap/evidence`
- `GET /warehouse/enterprise-olap/evidence/latest`
- `GET /warehouse/enterprise-olap/evidence/proof`

## Acceptance Criteria

- P2-083 spec and plan are indexed after P2-082.
- A migration creates `cdp_warehouse_enterprise_olap_evidence` as an append-only evidence ledger.
- Live Doris metrics parsing handles Prometheus text and missing samples fail closed.
- Evidence service tests prove:
  - live Doris metrics plus workload groups plus operator ledger rows can produce PASS enterprise proof rows;
  - missing metrics collector or stale operator evidence returns FAIL rows;
  - workload groups without explicit controls fail `workload_isolation`;
  - recent synthetic ODS probe PASS can satisfy `ingestion_replay`;
  - unsupported operator evidence keys are rejected.
- Production readiness proof tests prove enterprise evidence rows are added before P2-081 readiness evaluation.
- Controller tests prove operator evidence recording and latest evidence reads use the authenticated tenant and actor.
- Focused verification script runs P2-081 and P2-083 tests with Java 21 while avoiding unrelated dirty test compilation blockers.

## Rollout

1. Deploy the evidence ledger and service.
2. Configure Doris FE/BE metrics URLs with `CANVAS_DORIS_FE_METRICS_URLS` and
   `CANVAS_DORIS_BE_METRICS_URLS`, and configure Doris JDBC in environments that expect production PASS.
3. Record backup/restore and runbook drill evidence after successful drills.
4. Schedule or manually run synthetic ODS probe evidence for ingestion replay.
5. Treat enterprise OLAP readiness FAIL as a release blocker until all gates have fresh PASS/WARN evidence under policy.
