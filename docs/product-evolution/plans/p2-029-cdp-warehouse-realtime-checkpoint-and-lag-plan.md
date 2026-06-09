# CDP Warehouse Realtime Checkpoint And Lag Implementation Plan

**Goal:** Add durable realtime checkpoint and backlog visibility for CDP warehouse ODS delivery.

**Architecture:** Keep P2-025 retry buffer as the recovery mechanism. Add a checkpoint service that is called by direct ingestion sink delivery and retry delivery. Expose status through a small WebFlux controller.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Current State

- Realtime warehouse writes are best-effort and recoverable through retry.
- Operators can see quality checks only after reconciliation windows.
- There is no realtime ODS delivery checkpoint.

## Desired State

- Operators can inspect the latest delivered event id and event-time/received-time/delivered-time freshness.
- Direct sink failures and retry failures are reflected in a durable checkpoint.
- Retry backlog is visible together with the checkpoint.

## Implementation Tasks

### Task 1: Register P2-029 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-029-cdp-warehouse-realtime-checkpoint-and-lag.md`
- `docs/product-evolution/plans/p2-029-cdp-warehouse-realtime-checkpoint-and-lag-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Checkpoint Schema

- [x] **Step 1: Write schema test**

Create `CdpWarehouseRealtimeCheckpointSchemaTest` proving:
- `cdp_warehouse_realtime_checkpoint` exists;
- `(tenant_id, stream_key)` is unique;
- `last_delivered_at` is indexed.

- [x] **Step 2: Add migration and DAL**

Create:
- `V196__cdp_warehouse_realtime_checkpoint.sql`;
- `CdpWarehouseRealtimeCheckpointDO`;
- `CdpWarehouseRealtimeCheckpointMapper`.

### Task 3: Checkpoint Service

- [x] **Step 1: Write service tests**

Create `CdpWarehouseRealtimeCheckpointServiceTest` covering:
- delivered checkpoint upsert;
- failure checkpoint upsert;
- status returns checkpoint rows and retry backlog counts.

- [x] **Step 2: Implement service**

Create `CdpWarehouseRealtimeCheckpointService` with:
- `recordDelivered(CdpEventLogDO row, String deliverySource)`;
- `recordFailure(CdpEventLogDO row, String errorMessage)`;
- `status(Long tenantId)`.

### Task 4: Runtime Integration

- [x] **Step 1: Update ingestion tests and implementation**

Record checkpoint after direct warehouse sink success and failure.

- [x] **Step 2: Update retry tests and implementation**

Record checkpoint after retry sink success and failure.

### Task 5: Controller And Verification

- [x] **Step 1: Add controller tests and implementation**

Expose:
- `GET /warehouse/realtime/status`.

- [x] **Step 2: Run focused warehouse tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseRealtimeCheckpointSchemaTest,CdpWarehouseRealtimeCheckpointServiceTest,CdpWarehouseRealtimeControllerTest,CdpEventIngestionWarehouseCheckpointTest,CdpWarehouseRealtimeRetryServiceTest
```

## Acceptance Checklist

- [x] P2-029 spec and plan are indexed.
- [x] Realtime checkpoint table is additive and tenant scoped.
- [x] Direct sink success and failure update checkpoint state.
- [x] Retry success and failure update checkpoint state.
- [x] Realtime status exposes checkpoint and retry backlog.
- [x] Focused warehouse tests pass.
