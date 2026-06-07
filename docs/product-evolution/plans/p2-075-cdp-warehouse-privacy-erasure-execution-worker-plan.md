# CDP Warehouse Privacy Erasure Execution Worker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute P2-073 privacy erasure requests against CDP operational state, event logs, retry buffers, and Doris warehouse assets without persisting raw subject values.

**Architecture:** Add a domain execution service that verifies transient raw subject values against stored request hashes, runs asset-specific delete handlers, and records proof through the existing P2-073 service. Doris deletion is behind a small executor interface so local tests do not need Doris and production can wire live JDBC execution.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-075 spec, plan, and index rows.
- Add failing execution service and controller tests first.
- Implement a transient-subject execution service.
- Add optional Doris executor abstraction.
- Add execute endpoint to the existing privacy erasure controller.
- Verify focused P2-075 tests and warehouse/CDP regression.

## Files

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseDorisPrivacyErasureExecutor.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureExecutionService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyErasureExecutionServiceTest.java`
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java`
- Modify `docs/product-evolution/specs/INDEX.md`
- Modify `docs/product-evolution/plans/INDEX.md`
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create P2-075 docs and insert index rows after P2-074.

- [x] **Step 2: Write execution service tests**

Cover hash mismatch, CDP dry-run, CDP execution, and missing Doris executor proof behavior.

- [x] **Step 3: Write controller binding test**

Cover `POST /warehouse/privacy/erasure/requests/{id}/execute` tenant binding and request body forwarding.

- [x] **Step 4: Implement Doris executor abstraction**

Add a small interface returning asset execution counts and status.

- [x] **Step 5: Implement execution service**

Verify subject hash, prefetch matching event ids, run selected asset handlers, and record proof through P2-073.

- [x] **Step 6: Wire controller endpoint**

Inject execution service optionally and expose the execute endpoint.

- [x] **Step 7: Verify**

Run focused tests:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyErasureExecutionServiceTest,CdpWarehousePrivacyErasureControllerTest
```

Run warehouse/CDP regression:

```bash
cd backend
mvn -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest'
```

## Verification

- Focused P2-075 tests passed on 2026-06-05:
  `CdpWarehousePrivacyErasureExecutionServiceTest,CdpWarehousePrivacyErasureControllerTest`
- Warehouse/CDP regression passed on 2026-06-05:
  `CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest`
  - Result: 402 tests, 0 failures, 1 skipped (`DorisConnectionTest`).
- P2-075 privacy-ledger redaction audit updated on 2026-06-05:
  `CdpWarehousePrivacyErasureExecutionServiceTest.dorisExecutorOutputDoesNotPersistRawSubjectValue`
  proves Doris executor proof/error output is redacted before being returned or recorded through P2-073 proof rows.
- Focused OLAP privacy slice passed on 2026-06-05:
  `CdpWarehousePrivacyErasureExecutionServiceTest,CdpWarehousePrivacyErasureServiceTest,CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyTombstoneServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest,CdpWarehouseProductionReadinessProofServiceTest`
  - Result: 49 tests, 0 failures.
