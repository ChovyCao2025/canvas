# CDP Warehouse Audience Materialization Operations Implementation Plan

Spec: `../specs/p2-035-cdp-warehouse-audience-materialization-operations.md`

**Goal:** Add a production operations API for triggering and inspecting P2-021 OLAP audience materialization.

**Architecture:** Keep the existing `AudienceMaterializationService` as the only materialization engine. Add a thin operations service for run listing and controller delegation.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add `AudienceMaterializationOperationsService`.
- Add `/warehouse/audiences/{audienceId}/materialize`.
- Add `/warehouse/audiences/materialization-runs`.
- Add service/controller tests.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-035 docs.

- [x] **Step 2: Add operations service**

Delegate manual materialization to `AudienceMaterializationService` and list recent `audience_materialization_run` rows.

- [x] **Step 3: Add controller APIs**

Expose tenant-scoped manual trigger and run listing under `/warehouse/audiences`.

- [x] **Step 4: Add tests and verify**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=AudienceMaterializationOperationsServiceTest,CdpWarehouseAudienceMaterializationControllerTest test
```

Run compile and warehouse/BI regression.

## Verification

- [x] P2-035 focused tests passed: `6 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI regression passed: `196 tests, 0 failures, 1 skipped`.
