# CDP Warehouse Enterprise OLAP Evidence Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add scheduled and manual enterprise OLAP evidence collection with auditable run history, so P2-083 evidence is refreshed continuously instead of only being evaluated on demand.

**Architecture:** Keep `CdpWarehouseEnterpriseOlapEvidenceService` as the evidence normalizer. Add a collection service that calls a new automated collection method, persists a run record, and exposes recent runs. Add a disabled-by-default scheduler that follows existing warehouse scheduler patterns: tenant id, fixed delay, optional lease, actor, and local overlap guard.

**Tech Stack:** Java 21, Spring Boot scheduling, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, shell verification script.

---

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V303__enterprise_olap_evidence_collection_run.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseEnterpriseOlapEvidenceCollectionRunMapper.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceScheduler.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceController.java`.
- Modify `backend/canvas-engine/src/main/resources/application.yml`.
- Create tests:
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest.java`
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceSchedulerTest.java`
- Modify tests:
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceServiceTest.java`
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseEnterpriseOlapEvidenceControllerTest.java`
  - `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ApplicationYamlTest.java`
- Modify `scripts/verify-enterprise-olap-focus.sh`.
- Modify:
  - `docs/product-evolution/IMPLEMENTATION_ORDER.md`
  - `docs/product-evolution/specs/INDEX.md`
  - `docs/product-evolution/plans/INDEX.md`

## Tasks

### Task 1: Index P2-084 Docs

- [x] Add P2-084 rows after P2-083 in product evolution indexes.
- [x] Run:

```bash
rg -n "P2-084|p2-084-cdp-warehouse-enterprise-olap-evidence-automation" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-084-cdp-warehouse-enterprise-olap-evidence-automation.md docs/product-evolution/plans/p2-084-cdp-warehouse-enterprise-olap-evidence-automation-plan.md
```

Expected: all five files contain the P2-084 slug or row.

### Task 2: Add Failing Evidence Collection Tests

- [x] Add a test proving `collectAutomatedEvidence` persists only automated gates and skips operator-only duplicates.
- [x] Add collection service tests for PASS counts, FAIL run persistence on exception, tenant-scoped recent run listing, and stale collection proof failures.
- [x] Add scheduler tests for disabled, lease-backed, and overlap behavior.
- [x] Add controller tests for manual collection and collection history endpoints.
- [x] Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest='CdpWarehouseEnterpriseOlapEvidenceServiceTest,CdpWarehouseEnterpriseOlapEvidenceCollectionServiceTest,CdpWarehouseEnterpriseOlapEvidenceSchedulerTest,CdpWarehouseEnterpriseOlapEvidenceControllerTest' test
```

Expected before implementation: FAIL because collection service, scheduler, run DO, mapper, and controller methods do not exist.

### Task 3: Implement Run Persistence

- [x] Add `V303__enterprise_olap_evidence_collection_run.sql` with tenant, trigger, status, counts, timestamps, reason, actor, and indexes.
- [x] Add `CdpWarehouseEnterpriseOlapEvidenceCollectionRunDO`.
- [x] Add mapper methods `insert`, `updateFinished`, and `listRecent`.

### Task 4: Implement Automated Evidence Collection

- [x] Add `collectAutomatedEvidence(Long tenantId, String actor)` to `CdpWarehouseEnterpriseOlapEvidenceService`.
- [x] Reuse the existing Doris and ingestion replay gate logic.
- [x] Persist rows for `doris_metrics`, `workload_isolation`, `query_slo`, `compaction_health`, and non-operator `ingestion_replay`.
- [x] Return an `EvidenceBundle` for the rows written in that cycle.

### Task 5: Implement Collection Service And Scheduler

- [x] Add `CdpWarehouseEnterpriseOlapEvidenceCollectionService` that creates a RUNNING row, calls automated evidence collection, and updates final counts/status.
- [x] Add `enterprise_olap:evidence_collection` proof evidence from recent collection runs into production readiness.
- [x] Add `CdpWarehouseEnterpriseOlapEvidenceScheduler` with disabled-by-default config, local overlap guard, optional warehouse lease, and actor/trigger values.
- [x] Add application config keys for scheduler enabled, tenant id, fixed delay, actor, trigger type, and lease TTL.

### Task 6: Extend Operations API

- [x] Add `POST /warehouse/enterprise-olap/evidence/collect`.
- [x] Add `GET /warehouse/enterprise-olap/evidence/collections`.
- [x] Ensure both endpoints use `TenantContextResolver.currentOrError()`.

### Task 7: Focused Verification

- [x] Extend `scripts/verify-enterprise-olap-focus.sh` for P2-084 classes and tests.
- [x] Run:

```bash
bash -n scripts/verify-enterprise-olap-focus.sh
scripts/verify-enterprise-olap-focus.sh
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest='CdpWarehouse*Test' test
scripts/verify-olap2-focus.sh
git diff --check
rg -n "TO[D]O|TB[D]" docs/product-evolution/specs/p2-081-cdp-warehouse-enterprise-olap-readiness.md docs/product-evolution/specs/p2-083-cdp-warehouse-enterprise-olap-operational-evidence.md docs/product-evolution/specs/p2-084-cdp-warehouse-enterprise-olap-evidence-automation.md docs/product-evolution/plans/p2-084-cdp-warehouse-enterprise-olap-evidence-automation-plan.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceCollectionService.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapEvidenceScheduler.java
```

Expected: shell syntax passes, focused and warehouse tests pass, whitespace check passes, and placeholder scan returns no matches.

## Self-Review Checklist

- Scheduler remains disabled by default.
- Proof endpoints stay side-effect free.
- Automated collection does not overwrite or fabricate operator backup/restore or runbook drill evidence.
- Collection run history is append/update only and tenant scoped.
- Missing Doris collector/config records FAIL rather than being silently skipped.
