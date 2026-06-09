# P2-052 - CDP Warehouse Table Drift Incident Automation Spec

Priority: P2
Sequence: 052
Source: `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`, `docs/product-evolution/specs/p2-051-cdp-warehouse-live-doris-ddl-drift.md`, `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`
Implementation plan: `../plans/p2-052-cdp-warehouse-table-drift-incident-automation-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Route warehouse physical table drift into the existing incident queue so WARN/FAIL contract inspections become actionable production operations work instead of passive evidence.

## Current Baseline

- P2-031 records DDL asset inspection evidence in `cdp_warehouse_table_inspection`.
- P2-051 records live Doris `SHOW CREATE TABLE` drift evidence using the same inspection ledger.
- The warehouse incident loop handles quality, readiness, realtime pipeline, and realtime job failures.
- Table drift WARN/FAIL reports do not automatically open or refresh incidents.

## In Scope

- Add a table drift incident input to `CdpWarehouseIncidentService`.
- Add a scanner that runs table inspection and records incidents for non-PASS reports.
- Support live inspection scans by default and asset inspection scans when requested.
- Add a tenant-scoped scan API.
- Add a disabled-by-default scheduler protected by the existing warehouse lease pattern.
- Add focused tests.

## Out Of Scope

- New incident table or schema changes.
- Automatic Doris `ALTER TABLE` remediation.
- Resolving table drift incidents when later inspections pass.
- UI.
- Multi-tenant fanout scheduling.

## Runtime Semantics

1. Scan is tenant scoped.
2. Live mode runs P2-051 live all-table inspection.
3. Asset mode runs P2-031 asset all-table inspection.
4. PASS reports are skipped.
5. WARN/FAIL reports open or refresh `TABLE_DRIFT:{TABLE_KEY}` incidents.
6. Incident source id is the inspection row id when available.
7. Incident description includes table key, physical name, status, source marker, violation count, violations, and inspected time.
8. Scheduler is disabled by default and uses the warehouse lease service when available.
9. Inspection failures are counted without stopping the whole scan.

## Functional Requirements

1. Operators can scan table drift incidents through a tenant-scoped API.
2. Operators can choose live or asset inspection mode.
3. Non-PASS table reports become stable incidents.
4. PASS table reports do not open incidents.
5. Scheduler uses the same scan entrypoint as the API.
6. Existing incident acknowledge/resolve APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentScheduler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-052 spec and plan are indexed.
- No Flyway migration is added.
- Table drift incidents use stable keys and existing incident upsert behavior.
- Scan skips PASS reports and records WARN/FAIL reports.
- Scheduler honors disabled, lease, and overlap gates.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service and API changes with scheduler disabled.
2. Run live drift incident scan in staging after P2-051 live inspection is validated.
3. Review opened table drift incidents and match them with inspection evidence.
4. Enable scheduler only after incident volume is understood.
5. Keep remediation manual until later slices add explicit ALTER planning.

## Rollback

- Disable the table drift incident scheduler.
- Stop calling the scan API.
- Existing table inspections and incident rows remain auditable.
