# CDP Warehouse Quality And Reconciliation Implementation Plan

**Goal:** Add durable, tenant-scoped data-quality checks for CDP warehouse ODS count reconciliation and aggregate watermark lag.

**Architecture:** Keep P2-022/P2-024/P2-025 execution paths unchanged. Add a quality service above MySQL, Doris JDBC, and warehouse watermarks. Each check records a durable ledger row and returns a compact DTO for APIs and schedulers.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, MySQL, Doris JDBC, Spring `@Scheduled`, JUnit 5, Mockito, AssertJ.

## Current State

- CDP warehouse ingestion, retry, backfill, aggregation, and operations APIs exist.
- There is no durable warehouse quality ledger.
- Operators cannot compare MySQL accepted event count with Doris ODS count without ad hoc SQL.

## Desired State

- Operators can run bounded ODS count reconciliation.
- Operators can inspect aggregate watermark lag.
- Quality checks are persisted for audit and trend review.
- A scheduler can run conservative checks when explicitly enabled.

## Implementation Tasks

### Task 1: Register P2-026 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`
- `docs/product-evolution/plans/p2-026-cdp-warehouse-quality-and-reconciliation-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Quality Schema

- [x] **Step 1: Write failing schema test**

Create `CdpWarehouseQualitySchemaTest` for:
- `cdp_warehouse_quality_check`;
- status/check-type indexes;
- tenant/window index.

- [x] **Step 2: Add migration and DAL objects**

Create:
- `V193__cdp_warehouse_quality_checks.sql`;
- `CdpWarehouseQualityCheckDO`;
- `CdpWarehouseQualityCheckMapper`.

### Task 3: Quality Service

- [x] **Step 1: Write failing service tests**

Create `CdpWarehouseQualityServiceTest` for:
- ODS count reconciliation `PASS`;
- ODS count reconciliation `WARN`;
- Doris-disabled reconciliation `SKIPPED`;
- aggregate watermark lag `PASS` and missing watermark `WARN`;
- recent checks are bounded.

- [x] **Step 2: Implement service**

Create `CdpWarehouseQualityService` with:
- `reconcileOds(Long tenantId, LocalDateTime from, LocalDateTime to, long tolerance, String operator)`;
- `checkAggregateLag(Long tenantId, LocalDateTime now, long maxLagMinutes, String operator)`;
- `recentChecks(Long tenantId, int limit)`.

### Task 4: Controller And Scheduler

- [x] **Step 1: Write failing controller and scheduler tests**

Create:
- `CdpWarehouseQualityControllerTest`;
- `CdpWarehouseQualitySchedulerTest`.

- [x] **Step 2: Implement controller and scheduler**

Create `CdpWarehouseQualityController`:
- `GET /warehouse/quality/checks`;
- `POST /warehouse/quality/reconcile-ods`;
- `POST /warehouse/quality/aggregate-lag`.

Create `CdpWarehouseQualityScheduler`:
- disabled by default;
- fixed delay property;
- tenant, window, tolerance, and lag threshold properties;
- overlap guard.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseQualitySchemaTest,CdpWarehouseQualityServiceTest,CdpWarehouseQualitySchedulerTest,CdpWarehouseQualityControllerTest,CdpWarehouseRealtimeRetrySchemaTest,CdpWarehouseRealtimeRetryServiceTest,CdpWarehouseRealtimeRetrySchedulerTest,CdpEventIngestionWarehouseRetryTest,CdpEventIngestionWarehouseSinkTest,CdpWarehouseOperationsServiceTest,CdpWarehouseSchedulerTest,CdpWarehouseControllerTest,CdpWarehouseBackfillServiceTest,CdpWarehouseAggregationServiceTest,DorisCdpEventStreamLoaderTest
```

- [x] **Step 2: Inspect changed files**

Check `git status --short` and leave unrelated dirty files untouched.

## Acceptance Checklist

- [x] P2-026 spec and plan are indexed.
- [x] Quality check table is additive and tenant scoped.
- [x] ODS reconciliation records `PASS`, `WARN`, and `SKIPPED`.
- [x] Aggregate lag checks use warehouse watermarks.
- [x] Recent check APIs are tenant scoped and bounded.
- [x] Scheduler is disabled by default and overlap guarded.
- [x] Focused tests pass.
