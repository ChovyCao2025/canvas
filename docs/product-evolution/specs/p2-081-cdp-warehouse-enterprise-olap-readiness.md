# P2-081 - CDP Warehouse Enterprise OLAP Readiness Spec

Priority: P2
Sequence: 081
Source: production OLAP gap review after P2-021 through P2-080
Implementation plan: `../plans/p2-081-cdp-warehouse-enterprise-olap-readiness-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Turn the existing Doris-backed CDP warehouse work into an explicit enterprise OLAP readiness gate that fails closed when production operating evidence is missing.

## Current Baseline

- P2-022 through P2-080 already cover major warehouse capabilities: ingestion, backfill, quality, catalog, lineage, scheduler leases, SLO policy, semantic metrics, availability gates, physical E2E certification, realtime job probes, synthetic ODS data-path proof, privacy erasure propagation, and audience bitmap rebuild automation.
- `CdpWarehouseProductionReadinessProofService` already aggregates warehouse readiness, window availability, consumer availability contracts, and optional privacy erasure backlog.
- The current proof still does not make enterprise OLAP operating evidence first-class. A PASS can be interpreted too broadly even when Doris observability, workload isolation, backup/restore, compaction/tablet health, ingestion replay, and operational drill evidence are not present.

## External References

The gate model is based on official OLAP operations docs, with Apache Doris as the primary target because the repository already uses Doris terminology and services.

- Apache Doris Monitoring Metrics: https://doris.apache.org/docs/4.x/admin-manual/maint-monitor/metrics/
  - Doris exposes FE/BE Prometheus-compatible metrics and calls out QPS, error rate, query latency, memory, thread pool queueing, disk usage, tablet count, connection count, and compaction score as monitoring and alerting inputs.
- Apache Doris Workload Group: https://doris.apache.org/docs/4.x/admin-manual/workload-management/workload-group/
  - Workload Group is Doris' in-process resource isolation mechanism for CPU, memory, IO, queueing, and concurrency controls.
- Apache Doris Backup and Restore: https://doris.apache.org/docs/3.x/admin-manual/data-admin/backup-restore/overview/
  - Doris supports backup and restore to remote storage; restore is operationally distinct from backup and must be evidenced.
- Apache Druid Compaction: https://druid.apache.org/docs/latest/data-management/compaction/
  - Druid treats compaction as a production segment optimization concern with conflict handling against ingestion.
- Apache Druid Clustered Deployment: https://druid.apache.org/docs/latest/tutorials/cluster/
  - Druid's production guidance separates master, data, and query responsibilities and recommends multiple master/query servers, dedicated metadata storage, distributed deep storage, and separate ZooKeeper in production.
- Apache Pinot Architecture: https://docs.pinot.apache.org/architecture-and-concepts/concepts/architecture
  - Pinot's architecture separates controllers, brokers, servers, minions, and segment storage, reinforcing that production OLAP is a multi-component operational system.

## In Scope

- Add an enterprise OLAP readiness domain service that normalizes required gates and computes PASS/WARN/FAIL.
- Require existing canvas warehouse evidence:
  - warehouse readiness
  - window availability
  - consumer contract evaluations
  - privacy erasure backlog evidence
- Require enterprise OLAP operating evidence:
  - Doris FE/BE metrics freshness
  - workload group or equivalent resource isolation policy
  - representative query SLO and capacity evidence
  - successful backup and restore drill evidence
  - compaction/tablet health evidence
  - ingestion replay or synthetic data-path proof
  - operator runbook or disaster recovery drill evidence
- Integrate the enterprise OLAP summary into production readiness proof without deleting the existing readiness, availability, contract, or privacy detail.
- Preserve focused tests that can run without a live Doris cluster by treating live Doris checks as explicit evidence contracts.

## Out Of Scope

- Provisioning a Doris, Druid, Pinot, ClickHouse, Kafka, or Flink cluster.
- Polling live Prometheus, Doris HTTP APIs, object storage, or backup repositories in this slice.
- Replacing existing P2-022 through P2-080 warehouse services.
- Changing real BI query execution or audience materialization behavior beyond exposing a stricter production proof.

## Gate Semantics

The enterprise OLAP gate is fail-closed:

1. Every required gate must have explicit evidence.
2. Missing critical evidence is `FAIL`.
3. Invalid or unknown evidence status is `FAIL`.
4. Any critical gate with `FAIL` makes the summary `FAIL`.
5. Any critical gate with `WARN` and no `FAIL` makes the summary `WARN`.
6. Only all critical gates at `PASS` returns `PASS`.
7. Existing production proof status must include the enterprise OLAP summary status when enterprise evaluation is enabled.

## Required Gates

| Gate | Critical | Source | Production meaning |
| --- | --- | --- | --- |
| `warehouse_readiness` | yes | existing readiness service | Offline, realtime, incident, BI, and audience materialization readiness is current. |
| `window_availability` | yes | existing availability service | Requested offline/realtime/hybrid window is available for consumers. |
| `consumer_contracts` | yes | existing consumer contract evidence | BI/audience consumers have evaluated contracts for the requested window. |
| `privacy_erasure_backlog` | yes | existing privacy backlog service | CDP privacy erasure has no failed or overdue warehouse propagation backlog. |
| `doris_metrics` | yes | operator/Doris evidence | FE/BE metrics are fresh enough for alerting and diagnosis. |
| `evidence_collection` | yes | warehouse automation | A recent automated enterprise OLAP evidence collection run completed with PASS or WARN evidence and is not stale. |
| `workload_isolation` | yes | operator/Doris evidence | Workload groups or equivalent controls isolate BI, ingestion, and audience workloads. |
| `query_slo` | yes | Doris query profile or audit evidence | Representative BI, audience, and ad-hoc analytical query profiles are fresh and within latency, error, queueing, and memory policy. |
| `backup_restore` | yes | operator/Doris evidence | Recent backup and restore drill proves recoverability. |
| `compaction_health` | yes | operator/Doris evidence | Tablet count, compaction score, disk, and tablet health are under policy. |
| `ingestion_replay` | yes | existing synthetic data-path or replay evidence | Ingestion can replay or prove visibility through ODS/Doris. |
| `runbook_drill` | yes | operator evidence | Production runbook or DR drill has recent successful evidence. |

## API And Model Contract

`CdpWarehouseEnterpriseOlapReadinessService` returns:

- `tenantId`
- `status`
- `evaluatedAt`
- `summary`
- `gates`
- `missingCriticalGates`

Each gate contains:

- `key`
- `status`
- `reason`
- `critical`
- `source`

`CdpWarehouseProductionReadinessProofService.ProductionReadinessProof` gains an optional `enterpriseOlapReadiness` field. Its existing constructor compatibility must be preserved for existing tests and callers.

## Acceptance Criteria

- P2-081 spec and plan are indexed.
- Enterprise readiness service tests prove:
  - all required gates PASS gives PASS,
  - missing Doris/operational gates gives FAIL,
  - unknown statuses normalize to FAIL,
  - WARN propagates when no FAIL exists,
  - production proof evidence can be normalized into enterprise gates.
- Production readiness proof tests prove enterprise OLAP summary is included and affects the overall proof status.
- Focused verification script runs P2-081 tests with Java 21 and avoids unrelated dirty-test compilation blockers.
- Documentation clearly states that this slice does not make the product fully enterprise-grade by itself; it creates the measurable gate that exposes remaining production evidence gaps.

## Rollout

1. Deploy the enterprise OLAP readiness service and proof integration.
2. Let current environments surface missing production evidence as `FAIL` rather than interpreting earlier P2 slices as full production readiness.
3. Add live evidence collectors in later slices for Doris metrics, workload groups, query SLOs, backup/restore, compaction health, and runbook/drill ledgers.
