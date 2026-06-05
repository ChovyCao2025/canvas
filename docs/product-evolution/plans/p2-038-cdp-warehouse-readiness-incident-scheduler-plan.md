# CDP Warehouse Readiness Incident Scheduler Implementation Plan

**Goal:** Add a disabled-by-default scheduler that runs readiness incident scans with the existing warehouse lease and overlap guard pattern.

**Architecture:** Mirror existing warehouse scheduler classes. `CdpWarehouseReadinessIncidentScheduler` owns only scheduling and guards; the scanner remains `CdpWarehouseReadinessIncidentService`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-038 spec, plan, and indexes.
- Add scheduler service.
- Add focused scheduler tests.
- No Flyway migration.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-038 docs.

- [x] **Step 2: Add scheduler**

Add disabled-by-default scheduled cycle with tenant id, lease TTL, fixed delay, and overlap guard.

- [x] **Step 3: Add tests and verify**

Run focused scheduler tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `4 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `225 tests, 0 failures, 1 skipped`.
