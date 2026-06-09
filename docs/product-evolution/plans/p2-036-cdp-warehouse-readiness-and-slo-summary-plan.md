# CDP Warehouse Readiness And SLO Summary Implementation Plan

Spec: `../specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`

**Goal:** Add a read-only warehouse readiness summary that aggregates existing offline, realtime, BI, incident, and audience materialization facts.

**Architecture:** Keep all existing warehouse services as sources of truth. `CdpWarehouseReadinessService` only calls them, converts to section summaries, and derives PASS/WARN/FAIL.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add `CdpWarehouseReadinessService`.
- Add `/warehouse/readiness`.
- Add service/controller tests.
- No Flyway migration.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-036 docs.

- [x] **Step 2: Add readiness service**

Aggregate warehouse status, realtime pipeline status, open incidents, BI datasource health, and materialization run history.

- [x] **Step 3: Add readiness controller**

Expose tenant-scoped readiness under `/warehouse/readiness`.

- [x] **Step 4: Add tests and verify**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CdpWarehouseReadinessServiceTest,CdpWarehouseReadinessControllerTest test
```

Run compile and warehouse/BI/audience regression.

## Verification

- [x] P2-036 focused tests passed: `6 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `209 tests, 0 failures, 1 skipped`.
