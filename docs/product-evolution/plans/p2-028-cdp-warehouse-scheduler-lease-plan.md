# CDP Warehouse Scheduler Lease Implementation Plan

**Goal:** Add a tenant-scoped database lease for warehouse schedulers so multi-instance deployments do not run the same warehouse job concurrently.

**Architecture:** Add an additive MySQL lease table and a small service around MyBatis lease acquisition. Existing schedulers keep their local `AtomicBoolean` guard and call the lease service before invoking warehouse work.

**Tech Stack:** Java 21, Spring Boot WebFlux scheduler components, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

## Current State

- Warehouse schedulers are disabled by default.
- When enabled, they are protected from same-JVM overlap only.
- Multi-pod deployments can still race on backfill, aggregation, retry, and quality checks.

## Desired State

- A scheduler cycle must acquire a tenant-scoped lease before doing work.
- A denied lease must skip the cycle without invoking downstream services.
- Crash recovery is handled by lease expiry.

## Implementation Tasks

### Task 1: Register P2-028 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-028-cdp-warehouse-scheduler-lease.md`
- `docs/product-evolution/plans/p2-028-cdp-warehouse-scheduler-lease-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Lease Schema

- [x] **Step 1: Write schema test**

Create `CdpWarehouseJobLeaseSchemaTest` proving:
- `cdp_warehouse_job_lease` exists;
- `(tenant_id, lease_key)` is unique;
- `lease_until` is indexed for expiry visibility.

- [x] **Step 2: Add migration and DAL**

Create:
- `V195__cdp_warehouse_scheduler_lease.sql`;
- `CdpWarehouseJobLeaseDO`;
- `CdpWarehouseJobLeaseMapper`.

### Task 3: Lease Service

- [x] **Step 1: Write service tests**

Create `CdpWarehouseJobLeaseServiceTest` covering:
- acquired lease executes and releases;
- denied lease skips work;
- failed work still releases the lease.

- [x] **Step 2: Implement service**

Create `CdpWarehouseJobLeaseService` with:
- `runWithLease(Long tenantId, String leaseKey, Duration ttl, Supplier<Boolean> work)`;
- owner id normalization;
- positive TTL validation.

### Task 4: Scheduler Integration

- [x] **Step 1: Update scheduler tests**

Extend scheduler tests so denied leases skip:
- `CdpWarehouseScheduler`;
- `CdpWarehouseRealtimeRetryScheduler`;
- `CdpWarehouseQualityScheduler`.

- [x] **Step 2: Update schedulers**

Inject `CdpWarehouseJobLeaseService` and use stable lease keys:
- `CDP_WAREHOUSE_MAIN`;
- `CDP_WAREHOUSE_REALTIME_RETRY`;
- `CDP_WAREHOUSE_QUALITY`.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseJobLeaseSchemaTest,CdpWarehouseJobLeaseServiceTest,CdpWarehouseSchedulerTest,CdpWarehouseRealtimeRetrySchedulerTest,CdpWarehouseQualitySchedulerTest,CdpWarehouseCatalogSchemaTest,CdpWarehouseCatalogServiceTest,CdpWarehouseCatalogControllerTest
```

- [x] **Step 2: Run warehouse regression**

Run P2-022 through P2-028 focused warehouse tests.

## Acceptance Checklist

- [x] P2-028 spec and plan are indexed.
- [x] Lease table is additive and tenant scoped.
- [x] Lease acquisition is atomic through a unique key upsert.
- [x] Schedulers skip cycles when leases are denied.
- [x] Existing disabled-by-default scheduler behavior remains intact.
- [x] Focused warehouse tests pass.
