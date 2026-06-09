# P2-064 - CDP Warehouse Scheduled Audience Contract Gates Spec

Priority: P2
Sequence: 064
Source: `docs/product-evolution/specs/p2-045-cdp-warehouse-audience-materialization-scheduler-and-rollback.md`, `docs/product-evolution/specs/p2-058-cdp-warehouse-scheduled-audience-availability-gate.md`, `docs/product-evolution/specs/p2-060-cdp-warehouse-consumer-availability-contracts.md`, `docs/product-evolution/specs/p2-061-cdp-warehouse-contract-gated-consumers.md`
Implementation plan: `../plans/p2-064-cdp-warehouse-scheduled-audience-contract-gates-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Wire P2-060 consumer availability contracts into scheduled OLAP audience refreshes so each due audience is evaluated against its own table, dataset, or metric dependencies before materialization starts.

## Current Baseline

- P2-045 adds scheduled audience materialization and rollback.
- P2-058 gates scheduled refreshes with the coarse P2-055 warehouse window-level availability decision.
- P2-060 stores downstream consumer availability contracts and evaluates asset-level availability.
- P2-061 uses those contracts for BI queries and manual audience materialization.
- Scheduled audience refresh still cannot bind a due audience to a consumer availability contract.

## In Scope

- Add a scheduled refresh path that evaluates one consumer availability contract per due audience.
- Derive a stable default contract key from the audience id: `audience_{audienceId}`.
- Allow `data_source_config` JSON to override the contract key with `warehouseAvailabilityContractKey` or `consumerAvailabilityContractKey`.
- Block only the audience whose contract is not allowed; other due audiences continue.
- Count scanned, due, succeeded, failed, blocked, and skipped audiences.
- Add disabled-by-default scheduler configuration for the consumer contract gate.
- Preserve existing ungated and window-level gated scheduled refresh behavior.
- Add focused service and scheduler tests.

## Out Of Scope

- New Flyway schema.
- Automatic creation of consumer contracts.
- Replacing manual contract-gated materialization.
- BI subscription or dashboard scheduler gates.
- UI.

## Runtime Semantics

1. The scheduler keeps the existing tenant, limit, operator, lease, and overlap behavior.
2. When the consumer contract gate is disabled, existing P2-058 behavior is unchanged.
3. When the consumer contract gate is enabled, it takes precedence over the window-level scheduled availability gate because P2-060 contract evaluation already includes the window-level availability decision.
4. Candidate audiences are selected with the existing materialization candidate query.
5. Non-due audiences are counted as skipped and are not evaluated.
6. Due audiences evaluate a contract key derived from `data_source_config` override or the default `audience_{audienceId}`.
7. Contract `allowed=false` blocks that audience and does not call materialization.
8. Contract evaluation exceptions fail that audience and do not call materialization.
9. Contract `allowed=true` runs the existing materialization service.
10. Contract policy decides whether WARN is allowed; the scheduler does not accept a request-level allow-warn override.

## Functional Requirements

1. Scheduled audience refresh can run through consumer availability contracts.
2. A due audience with an allowed contract is materialized.
3. A due audience with a blocked contract is counted as blocked and is not materialized.
4. Contract key override in `data_source_config` is honored.
5. Invalid or missing `data_source_config` falls back to the default key.
6. Scheduler configuration can enable the consumer contract gate without changing existing defaults.
7. Existing ungated and window-level gated scheduled refresh tests remain compatible.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationScheduler.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/CdpWarehouseAudienceMaterializationSchedulerTest.java`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

## Acceptance Criteria

- P2-064 spec and plan are indexed.
- No Flyway migration is added.
- Service tests prove allowed contract materializes a due audience with default key `audience_{id}`.
- Service tests prove blocked contract does not invoke materialization.
- Service tests prove `data_source_config` contract key override is honored.
- Service tests prove missing consumer availability service fails closed.
- Scheduler tests prove consumer contract gate precedence over window-level availability gate.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy with `canvas.warehouse.audience-materialization-scheduler.consumer-contract-gate.enabled=false`.
2. Register consumer availability contracts for critical scheduled audiences using default `audience_{id}` keys or explicit config overrides.
3. Enable the gate in staging for one tenant.
4. Compare blocked counts with P2-063 consumer availability incidents.
5. Enable production tenants after contract inventory and asset evidence are stable.

## Rollback

- Disable `canvas.warehouse.audience-materialization-scheduler.consumer-contract-gate.enabled`.
- Existing ungated and P2-058 window-level gated scheduled refresh behavior remains available.
