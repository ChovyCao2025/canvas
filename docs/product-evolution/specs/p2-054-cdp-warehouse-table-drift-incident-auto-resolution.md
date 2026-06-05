# P2-054 - CDP Warehouse Table Drift Incident Auto-Resolution Spec

Priority: P2
Sequence: 054
Source: `docs/product-evolution/specs/p2-052-cdp-warehouse-table-drift-incident-automation.md`, `docs/product-evolution/specs/p2-053-cdp-warehouse-table-drift-remediation-planning.md`
Implementation plan: `../plans/p2-054-cdp-warehouse-table-drift-incident-auto-resolution-plan.md`

## Goal

Close the table drift incident lifecycle by resolving stable `TABLE_DRIFT:{TABLE_KEY}` incidents when a later table inspection passes.

## Current Baseline

- P2-052 opens or refreshes table drift incidents for WARN/FAIL inspections.
- P2-052 explicitly leaves incident resolution after later PASS inspections out of scope.
- P2-053 helps operators prepare safe remediation steps.
- After operators remediate a table, the incident queue can remain noisy until someone manually resolves the incident.

## In Scope

- Add table-drift incident resolution by stable incident key.
- Resolve existing OPEN or ACKNOWLEDGED table drift incidents when a fresh scan report is PASS.
- Add a resolved count to table drift incident scan results.
- Keep WARN/FAIL incident opening behavior unchanged.
- Add focused tests.

## Out Of Scope

- Executing remediation SQL.
- Resolving non-table-drift incidents.
- New Flyway tables.
- UI.
- Cross-tenant fanout scheduling.

## Runtime Semantics

1. Scan remains tenant scoped.
2. WARN/FAIL reports continue to open or refresh table drift incidents.
3. PASS reports attempt to resolve the matching stable `TABLE_DRIFT:{TABLE_KEY}` incident.
4. Only incidents from `WAREHOUSE_TABLE_DRIFT` are auto-resolved.
5. Only `OPEN` and `ACKNOWLEDGED` incidents are affected.
6. A PASS report with no matching active incident is counted as skipped.
7. Resolution failures are counted without stopping the whole scan.

## Functional Requirements

1. Operators can run the same table drift incident scan API and receive `resolved` counts.
2. Scheduler behavior reuses the same scan entrypoint.
3. Table drift incidents are resolved only after fresh PASS evidence.
4. Stable incident keys are used rather than inspection ids.
5. Existing acknowledge and manual resolve APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseIncidentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableDriftIncidentService.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-054 spec and plan are indexed.
- No Flyway migration is added.
- PASS table drift reports resolve matching OPEN/ACKNOWLEDGED table drift incidents.
- PASS reports without active incidents are skipped.
- WARN/FAIL reports still open incidents.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy service and scan result changes.
2. Run table drift scans in staging after remediating a known drift incident.
3. Confirm the incident resolves only when inspection evidence is PASS.
4. Enable scheduler with the same cadence as P2-052 after staging evidence is clean.

## Rollback

- Stop calling the scan API or disable the scheduler.
- Operators can still manually resolve incidents through existing APIs.
