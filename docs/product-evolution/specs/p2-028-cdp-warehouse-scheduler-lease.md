# P2-028 - CDP Warehouse Scheduler Lease Spec

Priority: P2
Sequence: 028
Source: `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`, `docs/product-evolution/specs/p2-025-cdp-warehouse-realtime-retry-buffer.md`, `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`
Implementation plan: `../plans/p2-028-cdp-warehouse-scheduler-lease-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a tenant-scoped warehouse scheduler lease so multi-instance deployments do not run backfill, aggregation, realtime retry, or quality checks concurrently for the same warehouse job.

## Current Baseline

- P2-024, P2-025, and P2-026 schedulers use in-process `AtomicBoolean` guards.
- The in-process guard prevents reentry inside one JVM only.
- In production, multiple pods can still execute the same warehouse scheduler cycle at the same time.

## In Scope

- A MySQL lease table keyed by tenant and lease key.
- A service that acquires, extends, and releases warehouse job leases.
- Scheduler integration for:
  - warehouse backfill and aggregation;
  - realtime retry buffer;
  - warehouse quality checks.
- Tests proving acquired leases execute work and denied leases skip work.

## Out Of Scope

- External lock systems such as Redis Redlock or ZooKeeper.
- Full workflow orchestration.
- Lock-at-least-for cadence throttling.
- Cross-service DAG scheduling.

## Runtime Semantics

1. Each scheduler has a stable lease key.
2. Lease rows are tenant scoped.
3. A scheduler cycle runs only after the lease is acquired.
4. A lease can be reacquired when the current lease has expired or when the same owner extends it.
5. The scheduler releases the lease after the cycle completes; lease expiry is the crash recovery path.
6. If no lease is acquired, the scheduler returns `false` and does not invoke the underlying warehouse operation.

## Functional Requirements

1. The lease table must have a unique key on `(tenant_id, lease_key)`.
2. The lease service must not require a live scheduler to be unit-tested.
3. The warehouse scheduler must use lease key `CDP_WAREHOUSE_MAIN`.
4. The realtime retry scheduler must use lease key `CDP_WAREHOUSE_REALTIME_RETRY`.
5. The quality scheduler must use lease key `CDP_WAREHOUSE_QUALITY`.
6. Default scheduler behavior remains disabled unless the existing scheduler enabled flags are set.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V195__cdp_warehouse_scheduler_lease.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseJobLeaseDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseJobLeaseMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseJobLeaseService.java`
- Scheduler constructor and run-cycle integration.
- Unit tests for schema, service, and scheduler lease behavior.

## Acceptance Criteria

- Schema test proves lease table, uniqueness, and lease expiry index exist.
- Service tests prove acquired leases run work, denied leases skip work, and acquired leases are released.
- Scheduler tests prove each warehouse scheduler skips work when the lease is denied.
- Focused warehouse backend tests pass.

## Rollout

1. Deploy the additive lease table migration.
2. Enable warehouse schedulers in one environment with multiple backend instances.
3. Verify only one instance logs execution for each lease key at a time.

## Rollback

- Disable warehouse schedulers with existing feature flags.
- Leave the lease table in place; it has no effect if schedulers are disabled.
