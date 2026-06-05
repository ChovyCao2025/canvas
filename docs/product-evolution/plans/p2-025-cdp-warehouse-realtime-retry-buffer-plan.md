# CDP Warehouse Realtime Retry Buffer Implementation Plan

**Goal:** Persist failed near-real-time warehouse sink attempts and retry them with bounded, observable semantics.

**Architecture:** Keep MySQL `cdp_event_log` authoritative. On inline sink failure, enqueue a retry row keyed by tenant and event log id. A retry service claims due rows, reuses `CdpWarehouseEventSink`, and marks rows `SUCCESS`, `RETRY`, or `DEAD`. A scheduler runs the retry service only when enabled.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, Spring `@Scheduled`, JUnit 5, Mockito, AssertJ.

## Current State

- P2-022 inline warehouse mirroring is best-effort only.
- P2-024 can manually or automatically backfill by watermark.
- Failed realtime sink attempts are not durable or queryable.

## Desired State

- Failed realtime warehouse deliveries become durable retry rows.
- Retry cycles are bounded and idempotent.
- Exhausted rows are marked dead instead of disappearing into logs.
- CDP ingestion remains successful even when Doris or the sink fails.

## Implementation Tasks

### Task 1: Register P2-025 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-025-cdp-warehouse-realtime-retry-buffer.md`
- `docs/product-evolution/plans/p2-025-cdp-warehouse-realtime-retry-buffer-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Retry Schema

- [x] **Step 1: Write failing schema test**

Create `CdpWarehouseRealtimeRetrySchemaTest` for:
- `cdp_warehouse_realtime_retry`;
- unique `(tenant_id, event_log_id)`;
- due retry index;
- status index.

- [x] **Step 2: Add migration and DAL objects**

Create:
- `V192__cdp_warehouse_realtime_retry.sql`;
- `CdpWarehouseRealtimeRetryDO`;
- `CdpWarehouseRealtimeRetryMapper`.

### Task 3: Retry Service

- [x] **Step 1: Write failing service tests**

Create `CdpWarehouseRealtimeRetryServiceTest` for:
- idempotent enqueue;
- retry success;
- retry failure schedules `RETRY`;
- max attempts mark `DEAD`.

- [x] **Step 2: Implement retry service**

Create `CdpWarehouseRealtimeRetryService` with:
- `enqueueFailure(CdpEventLogDO row, String errorMessage)`;
- `retryDue(LocalDateTime now, int limit, int maxAttempts)`;
- bounded error messages and next retry calculation.

### Task 4: Ingestion Hook And Scheduler

- [x] **Step 1: Write failing ingestion and scheduler tests**

Create:
- `CdpEventIngestionWarehouseRetryTest`;
- `CdpWarehouseRealtimeRetrySchedulerTest`.

- [x] **Step 2: Implement hook and scheduler**

Modify `CdpEventIngestionService` to enqueue retry rows when the optional warehouse sink throws.

Create `CdpWarehouseRealtimeRetryScheduler`:
- disabled by default;
- fixed delay property;
- cycle limit and max attempts properties;
- local overlap guard.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseRealtimeRetrySchemaTest,CdpWarehouseRealtimeRetryServiceTest,CdpWarehouseRealtimeRetrySchedulerTest,CdpEventIngestionWarehouseRetryTest,CdpEventIngestionWarehouseSinkTest,CdpWarehouseOperationsServiceTest,CdpWarehouseSchedulerTest,CdpWarehouseControllerTest,CdpWarehouseBackfillServiceTest,CdpWarehouseAggregationServiceTest,DorisCdpEventStreamLoaderTest
```

- [x] **Step 2: Inspect changed files**

Check `git status --short` and keep unrelated dirty files untouched.

## Acceptance Checklist

- [x] P2-025 spec and plan are indexed.
- [x] Retry table is additive and idempotent by tenant/event log id.
- [x] Sink failures from ingestion enqueue retry rows without rejecting events.
- [x] Retry worker reuses the shared warehouse sink.
- [x] Exhausted retries are marked `DEAD`.
- [x] Scheduler is disabled by default and overlap guarded.
- [x] Focused tests pass.
