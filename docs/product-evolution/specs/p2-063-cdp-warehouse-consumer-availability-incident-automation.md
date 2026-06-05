# P2-063 - CDP Warehouse Consumer Availability Incident Automation Spec

Priority: P2
Sequence: 063
Source: `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-059-cdp-warehouse-availability-incident-automation.md`, `docs/product-evolution/specs/p2-062-cdp-warehouse-asset-availability-automation.md`
Implementation plan: `../plans/p2-063-cdp-warehouse-consumer-availability-incident-automation-plan.md`

## Goal

Route P2-060 consumer availability contract WARN/FAIL evidence into the existing warehouse incident loop, and automatically resolve matching contract incidents when later contract evaluations pass.

## Current Baseline

- P2-059 opens and resolves incidents for window-level warehouse availability gates.
- P2-060 evaluates consumer availability contracts that combine window-level and table/dataset/metric asset evidence.
- P2-061 gates BI and manual audience operations with consumer contract decisions.
- P2-062 lets offline aggregation and realtime checkpoint jobs publish automated asset evidence.
- Consumer contract WARN/FAIL/PASS results still do not create or resolve operational incidents.

## In Scope

- Add a stable `WAREHOUSE_CONSUMER_AVAILABILITY` incident source for consumer contract evaluations.
- Open incidents when a contract scan returns `WARN` or `FAIL`.
- Resolve matching open/acknowledged incidents when a contract scan returns `PASS`.
- Add a manual API to scan one contract or active contracts by consumer type.
- Add a disabled-by-default scheduler that scans active contracts over a rolling window with the existing warehouse lease.
- Add focused incident, service, scheduler, and controller tests.

## Out Of Scope

- New Flyway schema.
- Replacing P2-059 availability incidents.
- Replacing P2-061 execution gates.
- UI.
- Auto-discovering missing consumer contracts.

## Runtime Semantics

1. A consumer contract incident key is stable by contract key: `CONSUMER_AVAILABILITY:{CONTRACT_KEY}`.
2. The incident source type is `WAREHOUSE_CONSUMER_AVAILABILITY`.
3. `FAIL` creates a `CRITICAL` incident; `WARN` creates a `WARN` incident.
4. `PASS` attempts to resolve the matching active incident.
5. Manual scans can target a single `contractKey` or all active contracts matching an optional `consumerType`.
6. Scheduler scans all active contracts for one tenant, optional consumer type, and a rolling window.
7. Incident writes are best effort per contract; one failed incident write does not abort remaining contract scans.
8. Contract evaluation failures are counted as failed scan items and do not open misleading incidents.

## Functional Requirements

1. Operators can scan a single consumer availability contract and open/resolve its incident.
2. Operators can scan active consumer contracts by tenant and optional consumer type.
3. WARN/FAIL scans open stable incidents containing consumer ref, mode, policy, requested window, message, and asset gates.
4. PASS scans resolve matching active incidents.
5. The scheduler can run contract scans with lease protection and overlap protection.
6. Existing P2-059 availability incident behavior remains unchanged.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseIncidentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseConsumerAvailabilityIncidentScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseConsumerAvailabilityIncidentController.java`
- Focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-063 spec and plan are indexed.
- No Flyway migration is added.
- Incident service tests prove consumer contract WARN/FAIL opens stable incidents and PASS is ignored for open.
- Incident service tests prove consumer contract PASS resolution uses a source-scoped stable key.
- Consumer availability incident scan tests prove single-contract and multi-contract scan counts.
- Scheduler tests prove disabled, lease-protected, and overlap behavior.
- Controller tests prove tenant-scoped manual scan delegation.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy after P2-060 through P2-062.
2. Use manual scans for critical BI and audience contracts in staging.
3. Enable the scheduler for one tenant and one consumer type at a time.
4. Monitor `WAREHOUSE_CONSUMER_AVAILABILITY` incidents and compare with P2-059 availability incidents.
5. Expand to all active contracts after contract inventory is stable.

## Rollback

- Disable `canvas.warehouse.consumer-availability-incident-scheduler.enabled`.
- Stop calling the contract incident scan endpoint.
- Existing P2-059 incidents, P2-060 contract evaluation, and P2-061 execution gates continue independently.
