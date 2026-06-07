# CDP Warehouse Enterprise OLAP Query SLO Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add enterprise OLAP query SLO evidence so production readiness validates representative query-path latency, error, queueing, memory, and freshness policy.

**Architecture:** Extend the existing Doris evidence client with typed query SLO observations. Evaluate the new `query_slo` gate inside `CdpWarehouseEnterpriseOlapEvidenceService`, persist it during automated collection, and add it to the required enterprise readiness gates. Keep proof endpoints side-effect free and make live Doris query SLO collection configurable through a SQL property.

**Tech Stack:** Java 21, Spring Boot configuration, JdbcTemplate, MyBatis evidence ledger, JUnit 5, Mockito, AssertJ, shell verification script.

---

## Files

- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapDorisEvidenceClient.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessService.java`.
- Modify `backend/canvas-engine/src/main/resources/application.yml`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationYamlTest.java`.
- Modify `scripts/verify-enterprise-olap-focus.sh`.

## Tasks

### Task 1: Index P2-085 Docs

- [x] Add P2-085 rows after P2-084 in product evolution indexes.
- [x] Run:

```bash
rg -n "P2-085|p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence.md docs/product-evolution/plans/p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence-plan.md
```

Expected: all five files contain the P2-085 slug or row.

### Task 2: Add Failing Query SLO Tests

- [x] Add evidence service tests for PASS, WARN p95, missing profile FAIL, stale FAIL, and hard policy FAIL cases.
- [x] Add automated collection test proving `query_slo` is persisted.
- [x] Add readiness and production proof tests proving `query_slo` is required and included.
- [x] Add JDBC client tests proving blank SQL fails closed and configured SQL rows are mapped.
- [x] Add application YAML test proving `canvas.doris.query-slo-sql` is configurable.
- [x] Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest='ApplicationYamlTest,CdpWarehouseEnterpriseOlapEvidenceServiceTest,CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest,CdpWarehouseEnterpriseOlapReadinessServiceTest,CdpWarehouseProductionReadinessProofServiceTest,HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest' test
```

Expected before implementation: FAIL because the `query_slo` client method, gate, readiness requirement, and application property do not exist.

### Task 3: Implement Doris Query SLO Client Contract

- [x] Add `querySlo()` to `CdpWarehouseEnterpriseOlapDorisEvidenceClient`.
- [x] Add `QuerySloEvidence` record with `profileKey`, `workloadGroup`, `sampleCount`, `errorCount`, `p95LatencyMs`, `p99LatencyMs`, `maxQueueWaitMs`, `maxPeakMemoryBytes`, and `measuredAt`.
- [x] Add `canvas.doris.query-slo-sql` property to `application.yml`.
- [x] Implement `HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.querySlo()` by running configured SQL through the Doris `JdbcTemplate` and mapping required aliases.

### Task 4: Implement Query SLO Evidence Gate

- [x] Add `query_slo` to proof order and automated collection order.
- [x] Evaluate required profile keys, sample count, freshness, p95, p99, error rate, queue wait, and peak memory.
- [x] Return `FAIL` for missing client/config/rows and `WARN` for p95 warning band.
- [x] Persist `query_slo` during automated collection.

### Task 5: Integrate Readiness And Verification

- [x] Add `query_slo` to `CdpWarehouseEnterpriseOlapReadinessService` required gates.
- [x] Extend production readiness proof tests to require the new row.
- [x] Add focused script selector/source entries.
- [x] Run:

```bash
bash -n scripts/verify-enterprise-olap-focus.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) scripts/verify-enterprise-olap-focus.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClientTest,ApplicationYamlTest' test
JAVA_HOME=$(/usr/libexec/java_home -v 21) scripts/verify-olap2-focus.sh
git diff --check
rg -n "TO[D]O|TB[D]" docs/product-evolution/specs/p2-081-cdp-warehouse-enterprise-olap-readiness.md docs/product-evolution/specs/p2-083-cdp-warehouse-enterprise-olap-operational-evidence.md docs/product-evolution/specs/p2-084-cdp-warehouse-enterprise-olap-evidence-automation.md docs/product-evolution/specs/p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence.md docs/product-evolution/plans/p2-085-cdp-warehouse-enterprise-olap-query-slo-evidence-plan.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.java
```

Expected: shell syntax passes, focused and warehouse tests pass, whitespace check passes, and placeholder scan returns no matches.

## Self-Review Checklist

- Query SLO evidence is required for enterprise OLAP readiness.
- Query SLO collection fails closed when SQL is not configured.
- Proof endpoints remain side-effect free.
- Automated collection appends `query_slo` evidence but does not fabricate operator-only evidence.
- The spec states the rollout boundary and does not claim production completeness without live evidence.
