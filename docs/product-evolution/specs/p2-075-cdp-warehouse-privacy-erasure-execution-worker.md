# P2-075 - CDP Warehouse Privacy Erasure Execution Worker Spec

Priority: P2
Sequence: 075
Source: `docs/product-evolution/specs/p2-073-cdp-warehouse-privacy-erasure-propagation-proof.md`, `docs/product-evolution/specs/p2-074-cdp-warehouse-privacy-tombstone-ingestion-guard.md`
Implementation plan: `../plans/p2-075-cdp-warehouse-privacy-erasure-execution-worker-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Turn privacy erasure proof rows into executable deletion work for CDP operational state, CDP event logs, realtime retry buffers, and Doris warehouse assets, while keeping raw subject values out of the persistent privacy ledger.

## Current Baseline

- P2-073 tracks tenant-scoped erasure requests and per-asset proof, but explicitly excludes physical deletes.
- P2-074 prevents erased subjects from being recreated after proof, but does not delete existing rows.
- `DataDeletionService` can delete some operational tables by raw user id, but it is not connected to warehouse asset proof and does not cover CDP event log, realtime retry, or Doris evidence.

## In Scope

- Add an execution service that runs a bounded asset set for one P2-073 request.
- Accept raw `subjectValue` only as transient command input and verify it matches the stored `subject_hash`.
- Execute CDP MySQL deletes for profile, identity, tag, event log, and realtime retry assets.
- Record each asset result back through P2-073 proof rows with `PASS`, `WARN`, `FAIL`, or `SKIPPED`.
- Add a pluggable Doris erasure executor for ODS/DWD assets so production can execute live Doris deletes when configured.
- Record audience bitmap assets as non-pass evidence unless a later rebuild worker proves replacement.
- Add an operator API to execute one erasure request.
- Add focused service and controller tests.

## Out Of Scope

- Storing raw subject values.
- A persistent async queue for erasure execution.
- Rebuilding audience bitmap membership.
- Legal certification text.
- UI.

## Runtime Semantics

1. `subjectValue` is required for execution and is never returned in views or stored in request/proof rows.
2. The service recomputes `SHA-256(tenantId + ":" + subjectType + ":" + subjectValue)` and fails before mutation when it does not equal the request hash.
3. `dryRun=true` records matched counts without deleting rows.
4. `dryRun=false` deletes matching CDP rows and records affected counts.
5. Realtime retry rows are matched through event log ids before event rows are deleted.
6. Doris assets call a configured executor; missing executor produces `WARN` in dry run and `FAIL` in execution.
7. Audience bitmap assets produce `WARN` because current bitmap storage has no row-level physical delete contract.
8. Request rollup continues to use P2-073 semantics: `FAIL` beats `WARN`; all `PASS/SKIPPED` produces `PASS`.

## Functional Requirements

1. Operators can execute one request with tenant scope, request id, raw subject value, dry-run flag, actor, and optional asset filter.
2. The execution service rejects a subject value that does not match the stored request hash.
3. CDP profile, identity, and tag assets delete by resolved user id.
4. CDP event log deletes by subject type (`USER_ID`, `ANONYMOUS_ID`, `DEVICE_ID`, or identity-derived user ids).
5. Realtime retry deletes by matching event log ids captured before event deletion.
6. Doris ODS/DWD assets delegate to an optional executor and record executor output.
7. The controller binds the current tenant and returns the updated P2-073 request view.

## Technical Scope

- Add `CdpWarehousePrivacyErasureExecutionService`
- Add `CdpWarehouseDorisPrivacyErasureExecutor`
- Add optional endpoint to `CdpWarehousePrivacyErasureController`
- Add focused service and controller tests.
- Update product-evolution indexes.

## Acceptance Criteria

- P2-075 spec and plan are indexed.
- Service tests prove hash mismatch blocks before mutation.
- Service tests prove dry-run records matched counts without deleting.
- Service tests prove execution deletes CDP profile/identity/tag/event/retry rows and records proof.
- Service tests prove missing Doris executor records non-pass evidence.
- Controller tests prove tenant-scoped execute binding.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy execution service and API.
2. Run dry-run executions for recent P2-073 requests.
3. Configure Doris executor in staging and verify ODS/DWD delete proof.
4. Enable non-dry-run execution for approved erasure requests.
5. Keep P2-074 tombstone creation after PASS/WARN-reviewed execution to prevent recreation.

## Rollback

- Stop calling the execution API.
- P2-073 proof rows remain auditable.
- P2-074 tombstones can still block recreation independently.
