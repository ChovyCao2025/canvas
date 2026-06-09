# CDP Warehouse Enterprise OLAP Operational Evidence Implementation Plan

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a production evidence loop for enterprise OLAP readiness by collecting live Doris evidence, persisting operator drill evidence, and feeding normalized proof rows into P2-081.

**Architecture:** Keep P2-081 as the final readiness gate. Add a focused evidence ledger and service that produces `enterprise_olap:<gate>` proof rows from live Doris metrics, Doris workload groups, recent synthetic ODS probes, and operator backup/runbook evidence. Readiness proof remains side-effect free and fails closed when evidence is missing.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, shell verification script.

---

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V302__enterprise_olap_operational_evidence.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParser.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapDorisEvidenceClient.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpJdbcCdpWarehouseEnterpriseOlapDorisEvidenceClient.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceController.java`.
- Create tests:
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrometheusMetricsParserTest.java`
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java`
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceControllerTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java`.
- Modify `scripts/verify-enterprise-olap-focus.sh`.
- Modify:
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md`
  - `docs/product-evolution/specs/INDEX.md`
  - `docs/product-evolution/plans/INDEX.md`

## Tasks

### Task 1: Index P2-083 Docs

- [x] Add P2-083 rows after P2-082 in the three product evolution indexes.
- [x] Run:

```bash
rg -n "P2-083|p2-083-cdp-warehouse-enterprise-olap-operational-evidence" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-083-cdp-warehouse-enterprise-olap-operational-evidence.md docs/product-evolution/plans/p2-083-cdp-warehouse-enterprise-olap-operational-evidence-plan.md
```

Expected: all five files contain the P2-083 slug or row.

### Task 2: Add Failing Evidence Service Tests

- [x] Write tests for metrics parsing, happy path evidence aggregation, fail-closed missing/stale evidence, workload isolation controls, synthetic ODS replay reuse, and operator key validation.
- [x] Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpWarehouseDorisPrometheusMetricsParserTest,CdpWarehouseEnterpriseOlapEvidenceServiceTest
```

Expected before implementation: FAIL because the parser and evidence service classes do not exist.

### Task 3: Implement Evidence Persistence

- [x] Add Flyway migration `V302__enterprise_olap_operational_evidence.sql`.
- [x] Add DO and mapper for append-only evidence rows.
- [x] Keep latest-row selection tenant scoped and bounded.

### Task 4: Implement Live Doris Evidence

- [x] Implement Prometheus metric parsing for numeric Doris metrics.
- [x] Implement the Doris evidence client contract.
- [x] Implement an HTTP metrics plus JDBC workload group client that fails closed when metrics URLs or Doris JDBC are missing.

### Task 5: Implement Evidence Aggregation

- [x] Add `CdpWarehouseEnterpriseOlapEvidenceService`.
- [x] Convert live Doris metrics to `doris_metrics` and `compaction_health`.
- [x] Convert workload group rows to `workload_isolation`.
- [x] Convert backup/restore and runbook drill ledger rows to their gates with expiry checks.
- [x] Convert recent synthetic ODS probe PASS/WARN/FAIL to `ingestion_replay`, falling back to operator ledger evidence.

### Task 6: Integrate Production Readiness

- [x] Inject optional `CdpWarehouseEnterpriseOlapEvidenceService` into `CdpWarehouseProductionReadinessProofService`.
- [x] Add normalized `enterprise_olap:<gate>` proof evidence before calling `CdpWarehouseEnterpriseOlapReadinessService`.
- [x] Preserve existing constructors and record compatibility.

### Task 7: Focused Verification

- [x] Add `CdpWarehouseEnterpriseOlapEvidenceController` for `POST /warehouse/enterprise-olap/evidence`, `GET /warehouse/enterprise-olap/evidence/latest`, and `GET /warehouse/enterprise-olap/evidence/proof`.
- [x] Add controller tests proving tenant and actor are read from `TenantContextResolver.currentOrError()`.
- [x] Extend `scripts/verify-enterprise-olap-focus.sh` to compile and run P2-083 focused tests.
- [x] Run:

```bash
bash -n scripts/verify-enterprise-olap-focus.sh
scripts/verify-enterprise-olap-focus.sh
git diff --check
rg -n "TO[D]O|TB[D]" docs/product-evolution/specs/p2-083-cdp-warehouse-enterprise-olap-operational-evidence.md docs/product-evolution/plans/p2-083-cdp-warehouse-enterprise-olap-operational-evidence-plan.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java
```

Expected: shell syntax passes, focused tests pass, whitespace check passes, and placeholder scan returns no matches.

## Self-Review Checklist

- The implementation must not trigger synthetic writes during production readiness proof generation.
- Missing live Doris configuration must fail enterprise OLAP gates instead of silently passing.
- Operator drill evidence must expire.
- P2-081 remains the final readiness decision point.
- No existing applied Flyway migration is edited.
