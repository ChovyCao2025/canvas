# P2-035 - CDP Warehouse Audience Materialization Operations Spec

Priority: P2
Sequence: 035
Source: `docs/product-evolution/specs/p2-021-cdp-olap-audience-materialization.md`, `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`
Implementation plan: `../plans/p2-035-cdp-warehouse-audience-materialization-operations-plan.md`

## Goal

Add a production operations surface for OLAP-backed audience materialization so CDP behavior audiences can be manually materialized and their run history inspected without duplicating the P2-021 materialization engine.

## Current Baseline

- P2-021 provides `AudienceMaterializationService`, stable user indexes, versioned audience bitmaps, bounded behavior rule compilation, OLAP repository boundaries, and `audience_materialization_run`.
- Warehouse operations APIs exist for ingestion/backfill/aggregation, but there is no operations API for OLAP audience materialization.
- `AudienceController` supports existing batch audience compute, but it does not expose the OLAP materialization path.

## In Scope

- A small operations service that delegates materialization to the existing `AudienceMaterializationService`.
- Tenant-scoped run history listing from `audience_materialization_run`.
- Manual trigger API for one audience id.
- Controller tests proving tenant scoping and operator attribution.

## Out Of Scope

- New materialization algorithms.
- Replacing existing `/canvas/audiences/{id}/compute`.
- Scheduler fan-out for all enabled audiences.
- New warehouse schema.

## Runtime Semantics

1. Manual materialization runs through the existing P2-021 service.
2. Run listing is tenant-scoped and can filter by audience id and status.
3. Limit is bounded to prevent unbounded operational queries.
4. Operator attribution prefers explicit request operator, then current username, then `operator`.

## Functional Requirements

1. Operators can trigger materialization for one tenant audience through a warehouse operations API.
2. Operators can list recent materialization runs for the current tenant.
3. Run list responses include id, tenant, audience id, version, status, matched user count, bitmap key, error, timestamps, and creator.
4. The API must not implement a second materialization path.
5. Focused backend tests and warehouse/BI regression must pass.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationOperationsServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationControllerTest.java`

## Acceptance Criteria

- Service tests prove manual materialization delegates to the existing materialization service.
- Service tests prove recent run listing is tenant-scoped, bounded, and mapped to API views.
- Controller tests prove current tenant and operator attribution are passed through.
- No schema migration is added.
- Focused backend tests pass.
- Warehouse/BI regression passes.

## Rollout

1. Deploy the additive operations API.
2. Trigger one small CDP behavior audience materialization in staging.
3. Inspect `/warehouse/audiences/materialization-runs` for status, matched users, bitmap key, and errors.
4. Only then expose the trigger button in an operator UI.

## Rollback

- Stop calling the operations API.
- Existing P2-021 materialization data and bitmap versions remain intact.
