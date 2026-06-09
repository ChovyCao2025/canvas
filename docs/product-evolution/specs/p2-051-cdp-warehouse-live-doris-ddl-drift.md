# P2-051 - CDP Warehouse Live Doris DDL Drift Spec

Priority: P2
Sequence: 051
Source: `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`
Implementation plan: `../plans/p2-051-cdp-warehouse-live-doris-ddl-drift-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add live Doris DDL drift detection so table contracts are not only checked against repository SQL assets, but also against the actual physical tables running in production.

## Current Baseline

- P2-031 persists physical table contracts and inspection rows.
- P2-031 checks checked-in DDL assets for partitioning, dynamic retention, replica count, bucket count, and distribution keys.
- P2-031 explicitly leaves live Doris `SHOW CREATE TABLE` drift detection out of scope.
- Operators cannot prove that a running Doris cluster still matches the saved CDP warehouse table contracts.

## In Scope

- Add live `SHOW CREATE TABLE` inspection for one table contract.
- Add live inspection for all active table contracts.
- Reuse the existing `cdp_warehouse_table_inspection` ledger.
- Reuse the existing table contract validation rules.
- Add tenant-scoped API endpoints and focused tests.

## Out Of Scope

- Automatic `ALTER TABLE` remediation.
- Dropping or modifying Doris partitions.
- New Flyway tables.
- UI.
- Cross-cluster comparison.

## Runtime Semantics

1. Live inspection is tenant scoped and uses the same tenant contract override behavior as asset inspection.
2. Only active table contracts are included in live all-table inspection.
3. Live inspection reads the contract physical table name and queries Doris `SHOW CREATE TABLE`.
4. If Doris is disabled or the table cannot be read, the inspection is recorded as `FAIL`.
5. If live DDL exists but violates non-fatal physical rules, the inspection is recorded as `WARN`.
6. If live DDL matches the contract, the inspection is recorded as `PASS`.
7. Live inspection rows set `ddl_asset_path` to a stable live source marker.
8. Contract latest inspection metadata is updated from live inspection results.

## Functional Requirements

1. Operators can inspect one live Doris table by table key.
2. Operators can inspect all active live Doris table contracts.
3. Live inspections return the same report shape as asset inspections.
4. Live inspections persist durable inspection evidence.
5. Missing Doris connectivity or missing live table DDL must fail closed.
6. Existing asset inspection APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-051 spec and plan are indexed.
- No Flyway migration is added.
- Single-table live inspection persists a PASS/WARN/FAIL inspection row.
- Live all-table inspection summarizes active table contract results.
- Doris-disabled or missing-live-DDL paths fail closed and are recorded.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy with live inspection API available but not scheduled.
2. Run live inspection in staging after Doris DDL deployment.
3. Compare live reports with P2-031 asset inspection results.
4. Use WARN/FAIL reports as release gates for warehouse jobs.
5. Add remediation automation in a later slice only after operators trust drift evidence.

## Rollback

- Stop calling live inspection endpoints.
- Existing table contracts and asset inspection APIs continue to work.
- Live inspection rows remain as audit evidence in the existing inspection ledger.
