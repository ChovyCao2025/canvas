# P2-050 - CDP Warehouse Operational Retention And Cleanup Spec

Priority: P2
Sequence: 050
Source: `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`, `docs/product-evolution/specs/p2-025-cdp-warehouse-realtime-retry-buffer.md`, `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`, `docs/product-evolution/specs/p2-049-cdp-warehouse-offline-cycle-orchestration.md`
Implementation plan: `../plans/p2-050-cdp-warehouse-operational-retention-and-cleanup-plan.md`

## Goal

Add tenant-scoped retention planning and cleanup for warehouse operational ledgers so offline runs, realtime retry history, and resolved incidents stay bounded in production without deleting active operational state.

## Current Baseline

- Offline warehouse backfill, aggregation, and P2-049 cycle orchestration write durable rows to `cdp_warehouse_sync_run`.
- Realtime retry processing writes terminal and non-terminal rows to `cdp_warehouse_realtime_retry`.
- Warehouse quality, readiness, realtime pipeline, and realtime job automation write incidents to `cdp_warehouse_incident`.
- Operators can list and inspect these records, but there is no retention plan, cleanup API, or scheduled cleanup path for operational history.

## In Scope

- Add a retention plan that counts rows eligible for cleanup by tenant and cutoff.
- Add a cleanup run that deletes only eligible terminal history.
- Add a disabled-by-default scheduler protected by the existing warehouse lease pattern.
- Add tenant-scoped API endpoints for plan and run.
- Add focused service, scheduler, and controller tests.

## Out Of Scope

- Deleting warehouse fact tables, Doris partitions, audience bitmap versions, or BI data.
- Archival export to object storage.
- Multi-tenant fanout scheduling.
- UI.
- New Flyway tables.

## Retention Semantics

1. Cleanup is tenant scoped.
2. Retention days must be positive and bounded.
3. Sync run cleanup deletes rows with `finished_at` older than the sync-run cutoff.
4. Realtime retry cleanup deletes only terminal rows with status `SUCCESS` or `DEAD` and `finished_at` older than the retry cutoff.
5. Incident cleanup deletes only `RESOLVED` rows with `resolved_at` older than the incident cutoff.
6. Rows without terminal timestamps are never eligible.
7. Open or acknowledged incidents are never eligible.
8. Pending, retrying, or sending realtime retry rows are never eligible.
9. Plan and run use the same eligibility rules.
10. Scheduler uses the same cleanup entrypoint as manual run.

## Functional Requirements

1. Operators can preview eligible cleanup counts before deleting rows.
2. Operators can run cleanup manually through a tenant-scoped API.
3. Cleanup returns deleted counts per ledger.
4. Cleanup defaults are conservative and can be overridden per request or scheduler config.
5. Scheduler is disabled by default and guarded against overlapping cycles.
6. Existing warehouse status, retry, incident, readiness, and offline-cycle APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRetentionScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-050 spec and plan are indexed.
- No Flyway migration is added.
- Retention plan counts eligible sync runs, terminal retry rows, and resolved incidents by tenant.
- Cleanup deletes only eligible rows and returns per-ledger counts.
- Scheduler delegates to retention cleanup behind the lease and disabled/overlap gates.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service and API changes with scheduler disabled.
2. Run retention plan in staging and compare counts with direct SQL.
3. Run manual cleanup for one tenant with high retention days first.
4. Lower retention days to the intended production defaults after confirming deletion scope.
5. Enable scheduler only after manual runs are verified.

## Rollback

- Disable the retention scheduler.
- Stop calling the retention run API.
- Existing ledgers remain readable; deleted rows require external backups if restoration is needed.
