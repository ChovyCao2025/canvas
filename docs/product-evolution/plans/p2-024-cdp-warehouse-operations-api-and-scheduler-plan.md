# CDP Warehouse Operations API And Scheduler Implementation Plan

Spec: `../specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Add a production operations surface around the P2-022 CDP warehouse jobs with manual trigger APIs, status APIs, and gated incremental scheduling.

**Architecture:** Keep P2-022 backfill and aggregation as execution primitives. Add an operations service that computes bounded defaults from watermarks, a controller for operator use, and a scheduler guarded by configuration and in-process overlap protection.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Reactor, Spring `@Scheduled`, JUnit 5, Mockito, AssertJ.

## Current State

- `CdpWarehouseBackfillService` can replay accepted MySQL CDP rows into the warehouse sink.
- `CdpWarehouseAggregationService` can run bounded Doris DWD/DWS aggregation windows.
- `cdp_warehouse_sync_run` and `cdp_warehouse_watermark` exist, but only job code writes them.
- No API or scheduler exists for warehouse operators.

## Desired State

- Operators can inspect warehouse run/watermark state.
- Operators can manually trigger bounded backfill and aggregation.
- The service can run enabled incremental cycles using watermarks.
- The scheduler avoids duplicate overlapping execution in one JVM.

## Implementation Tasks

### Task 1: Register P2-024 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`
- `docs/product-evolution/plans/p2-024-cdp-warehouse-operations-api-and-scheduler-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Operations Service

- [x] **Step 1: Write failing tests**

Create `CdpWarehouseOperationsServiceTest` covering:
- status returns tenant, recent runs, and watermarks;
- manual backfill delegates with bounded positive limits;
- incremental backfill starts from the last event id watermark;
- incremental aggregation starts from the aggregate window watermark and caps the next window.

- [x] **Step 2: Implement service**

Create `CdpWarehouseOperationsService` with:
- `status(Long tenantId, int limit)`;
- `triggerBackfill(Long tenantId, Long lastId, int limit, String operator)`;
- `triggerAggregation(Long tenantId, LocalDateTime from, LocalDateTime to, String operator)`;
- `runIncrementalBackfill(Long tenantId, int limit)`;
- `runIncrementalAggregation(Long tenantId, LocalDateTime now, int windowMinutes)`.

### Task 3: Scheduler

- [x] **Step 1: Write failing scheduler tests**

Create `CdpWarehouseSchedulerTest` proving:
- disabled scheduler does not call operations;
- enabled scheduler calls both incremental jobs;
- overlap guard skips a concurrent second cycle.

- [x] **Step 2: Implement scheduler**

Create `CdpWarehouseScheduler`:
- `@Scheduled(fixedDelayString = "${canvas.warehouse.scheduler.fixed-delay-ms:60000}")`;
- gated by `${canvas.warehouse.scheduler.enabled:false}`;
- uses an `AtomicBoolean` to prevent overlapping local cycles;
- reads tenant, backfill limit, and aggregation window settings from properties.

### Task 4: Controller

- [x] **Step 1: Write failing controller tests**

Create `CdpWarehouseControllerTest` proving:
- `GET /warehouse/status` delegates with current tenant id;
- `POST /warehouse/backfill` delegates manual backfill;
- `POST /warehouse/aggregate` delegates manual aggregation.

- [x] **Step 2: Implement controller**

Create `CdpWarehouseController`:
- `GET /warehouse/status?limit=20`;
- `POST /warehouse/backfill`;
- `POST /warehouse/aggregate`;
- resolves tenant via `TenantContextResolver`;
- runs blocking work on bounded elastic.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseOperationsServiceTest,CdpWarehouseSchedulerTest,CdpWarehouseControllerTest,CdpWarehouseSchemaTest,CdpWarehouseBackfillServiceTest,CdpWarehouseAggregationServiceTest,DorisCdpEventStreamLoaderTest,CdpEventIngestionWarehouseSinkTest
```

- [x] **Step 2: Inspect status**

Check `git status --short` and keep unrelated dirty worktree changes untouched.

## Acceptance Checklist

- [x] Spec and plan are indexed as P2-024.
- [x] Warehouse status API exposes run and watermark state.
- [x] Manual backfill and aggregation APIs are bounded and tenant scoped.
- [x] Incremental jobs use watermarks instead of hard-coded windows.
- [x] Scheduler is disabled by default and overlap guarded.
- [x] Focused tests pass.
