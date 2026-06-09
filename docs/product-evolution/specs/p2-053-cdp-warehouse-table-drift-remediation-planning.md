# P2-053 - CDP Warehouse Table Drift Remediation Planning Spec

Priority: P2
Sequence: 053
Source: `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`, `docs/product-evolution/specs/p2-051-cdp-warehouse-live-doris-ddl-drift.md`, `docs/product-evolution/specs/p2-052-cdp-warehouse-table-drift-incident-automation.md`
Implementation plan: `../plans/p2-053-cdp-warehouse-table-drift-remediation-planning-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Generate safe, reviewable remediation plans for warehouse table drift so operators can move from drift evidence and incidents to explicit corrective actions without allowing the service to mutate Doris tables automatically.

## Current Baseline

- P2-031 records physical table contracts and asset DDL inspection evidence.
- P2-051 records live Doris `SHOW CREATE TABLE` drift evidence.
- P2-052 opens stable incidents for table contract drift.
- Operators still have to translate violations into manual repair actions outside the platform.

## In Scope

- Add remediation plan generation for a single table contract.
- Add remediation plan generation for all active table contracts.
- Base every plan on a fresh asset or live inspection report.
- Generate conservative remediation steps with risk level, executable flag, reason, operator action, and SQL text only when the action is safe to review.
- Add tenant-scoped API endpoints and focused tests.

## Out Of Scope

- Executing Doris `ALTER TABLE`.
- Creating repair jobs, approval workflows, or rollout windows.
- New Flyway tables.
- Resolving incidents after remediation.
- UI.

## Runtime Semantics

1. Remediation planning is tenant scoped.
2. Live mode runs live inspection before producing a plan.
3. Asset mode runs asset inspection before producing a plan.
4. PASS reports produce an empty remediation step list.
5. Safe table property drift can produce reviewable `ALTER TABLE ... SET (...)` SQL.
6. Partition, distribution, bucket, missing table, and unreadable DDL issues are marked non-executable and require manual rebuild, migration, or connectivity repair.
7. Generated SQL is only emitted for valid Doris identifier shapes.
8. The API returns the inspection id and source used to build the plan.

## Functional Requirements

1. Operators can generate a single-table remediation plan.
2. Operators can generate remediation plans for all active table contracts.
3. Plans distinguish executable review SQL from manual remediation work.
4. Plans classify risk as `LOW`, `MEDIUM`, or `HIGH`.
5. Plans never execute DDL.
6. Existing inspection and incident APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-053 spec and plan are indexed.
- No Flyway migration is added.
- PASS inspections return no remediation steps.
- Property drift returns executable review SQL when identifiers are valid.
- Partition, distribution, and bucket drift return non-executable manual steps.
- Controller endpoints are tenant scoped.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy remediation plan APIs.
2. Generate plans in staging for P2-052 table drift incidents.
3. Review generated SQL and manual steps with data platform operators.
4. Use plans as runbook inputs; do not wire automatic execution.
5. Add approval-backed execution only in a later slice if operational evidence supports it.

## Rollback

- Stop calling remediation plan endpoints.
- Existing table inspection and incident workflows continue independently.
