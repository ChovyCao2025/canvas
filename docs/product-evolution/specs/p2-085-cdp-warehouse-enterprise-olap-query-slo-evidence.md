# P2-085 - CDP Warehouse Enterprise OLAP Query SLO Evidence Spec

Priority: P2
Sequence: 085
Source: production OLAP completion audit after P2-084 evidence automation
Implementation plan: `../plans/p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence-plan.md`

## Goal

Add first-class enterprise OLAP query SLO and capacity evidence so production readiness proves representative BI, audience, and ad-hoc analytical query paths are fresh, successful, and within latency, error, queueing, and memory policy.

## Current Baseline

- P2-081 makes enterprise OLAP readiness fail closed when operational evidence is absent.
- P2-083 collects and persists live Doris metrics, workload isolation, compaction, ingestion replay, and operator drill evidence.
- P2-084 schedules and records automated collection runs.
- The remaining production gap is query-path proof: global FE/BE metrics can be healthy while the actual dashboards, audience materialization lookups, and ad-hoc segmentation queries miss business SLOs under concurrency or queue pressure.

## External References

- Apache Doris Query Profile: https://doris.apache.org/docs/4.x/query-acceleration/query-profile/
  - Query Profile records per-query execution detail such as elapsed time, row count, and memory usage, which makes it the correct basis for representative query SLO evidence.
- Apache Doris Diagnostic Tools: https://doris.apache.org/docs/query-acceleration/performance-tuning-overview/diagnostic-tools/
  - Doris diagnostic tooling includes audit logs and query execution statistics for slow SQL and systematic performance diagnosis.
- Apache Doris Concurrency Control and Queuing: https://doris.apache.org/docs/4.x/admin-manual/workload-management/concurrency-control-and-queuing/
  - Workload groups can set maximum concurrency, queue length, and queue timeout; production evidence must verify queue wait and rejection behavior, not just static workload group existence.
- Apache Pinot Query Stages: https://docs.pinot.apache.org/build-with-pinot/querying-and-sql/multi-stage-query/understanding-stages
  - Pinot breaks distributed analytical queries into stages, reinforcing that production OLAP readiness must validate query execution behavior instead of only cluster liveness.

## In Scope

- Add a required `query_slo` enterprise OLAP gate.
- Extend the Doris evidence client contract with query SLO observations for representative profiles:
  - `bi_dashboard`
  - `audience_materialization`
  - `ad_hoc_segment`
- Evaluate query SLO evidence with fail-closed defaults:
  - all required profiles must be present;
  - each profile must have enough samples;
  - p95 and p99 latency must stay under policy;
  - error rate must stay under policy;
  - queue wait must stay under policy;
  - peak memory must stay under policy;
  - evidence must be fresh.
- Persist `query_slo` rows during automated evidence collection.
- Add `query_slo` to production readiness proof and enterprise readiness summary.
- Make JDBC-backed query SLO collection configurable through SQL so production deployments can point at Doris audit/profile tables or a curated observability view without hard-coding an environment-specific system table.

## Out Of Scope

- Running destructive load tests from production readiness proof.
- Inventing synthetic query success when no Doris query SLO SQL is configured.
- Provisioning Pinot, Druid, Prometheus, Grafana, or Doris.
- Claiming enterprise OLAP is fully production-complete without current PASS/WARN query SLO evidence from a real workload window.

## Query SLO Semantics

1. Missing query SLO client/configuration returns `FAIL`.
2. Missing required profile evidence returns `FAIL`.
3. Stale profile evidence returns `FAIL`.
4. Fewer than the minimum samples for any profile returns `FAIL`.
5. Any profile error rate at or above the fail threshold returns `FAIL`.
6. Any p95 or p99 latency at or above fail thresholds returns `FAIL`.
7. Any maximum queue wait at or above the queue threshold returns `FAIL`.
8. Any peak memory at or above the memory threshold returns `FAIL`.
9. p95 latency in the warning band returns `WARN` when no fail condition exists.
10. Only all required profiles within policy returns `PASS`.

## Default Policy

| Policy | Default |
| --- | --- |
| Required profiles | `bi_dashboard`, `audience_materialization`, `ad_hoc_segment` |
| Evidence freshness | 15 minutes |
| Minimum samples per profile | 5 |
| p95 fail threshold | 3000 ms |
| p95 warn threshold | 2000 ms |
| p99 fail threshold | 8000 ms |
| Error rate fail threshold | 1% |
| Queue wait fail threshold | 3000 ms |
| Peak memory fail threshold | 4 GiB |

## JDBC Query Contract

`canvas.doris.query-slo-sql` must return rows with these aliases:

- `profile_key`
- `workload_group`
- `sample_count`
- `error_count`
- `p95_latency_ms`
- `p99_latency_ms`
- `max_queue_wait_ms`
- `max_peak_memory_bytes`
- `measured_at`

The SQL may read from Doris audit logs, profile-derived summary tables, or a curated observability view. A blank SQL property means query SLO collection is intentionally unconfigured and therefore fails closed.

## Acceptance Criteria

- P2-085 spec and plan are indexed after P2-084.
- `CdpWarehouseEnterpriseOlapReadinessService` requires `query_slo`.
- `CdpWarehouseEnterpriseOlapEvidenceService` emits `enterprise_olap:query_slo` proof evidence.
- Automated collection persists `query_slo` rows with the other automated evidence.
- `HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient` reads configured query SLO SQL and maps rows into typed evidence.
- Tests prove:
  - all required query profiles within policy produce PASS;
  - missing profiles fail closed;
  - high p95 latency produces WARN before fail;
  - high error rate, high p99, high queue wait, high memory, or stale samples fail;
  - blank query SLO SQL fails closed in the JDBC client;
  - production readiness includes query SLO in enterprise gates.
- Focused and warehouse verification commands pass with Java 21.

## Rollout

1. Deploy the code with `canvas.doris.query-slo-sql` blank so environments fail closed until configured.
2. Build a Doris audit/profile summary SQL or view that emits the P2-085 JDBC query contract.
3. Enable automated evidence collection and confirm fresh `query_slo` PASS/WARN rows appear.
4. Treat `enterprise_olap:query_slo` FAIL as a release blocker until representative workloads have current evidence under policy.
