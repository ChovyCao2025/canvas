# P2-076 - CDP Warehouse Privacy Audience Bitmap Rebuild Proof Spec

Priority: P2
Sequence: 076
Source: `docs/product-evolution/specs/p2-073-cdp-warehouse-privacy-erasure-propagation-proof.md`, `docs/product-evolution/specs/p2-075-cdp-warehouse-privacy-erasure-execution-worker.md`
Implementation plan: `../plans/p2-076-cdp-warehouse-privacy-audience-bitmap-rebuild-proof-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Close the remaining privacy erasure gap for CDP warehouse audience materialization by rebuilding affected offline and hybrid audience bitmap versions after upstream erasure assets pass, then recording `AUDIENCE_BITMAP_VERSION` proof through the existing P2-073 ledger.

## Current Baseline

- P2-073 creates an `AUDIENCE_BITMAP_VERSION` asset plan for each erasure request.
- P2-075 deletes CDP operational, realtime retry, and Doris assets, but records `AUDIENCE_BITMAP_VERSION` as `WARN` because bitmap membership has no row-level delete contract.
- Audience materialization already supports manual rebuilds, run history, scheduler refresh, availability gates, contract gates, and rollback.
- There is no bridge from a privacy erasure request to audience bitmap rebuild proof.

## In Scope

- Add a tenant-scoped rebuild service for one privacy erasure request.
- Block rebuild proof when non-audience erasure assets are not `PASS` or `SKIPPED`.
- Rebuild enabled `OFFLINE_BATCH` and `HYBRID` audience definitions through the existing materialization operations service.
- Support an optional explicit audience id list for targeted proof runs.
- Record `AUDIENCE_BITMAP_VERSION` proof as `PASS`, `WARN`, `FAIL`, or `SKIPPED` through P2-073.
- Add an operator endpoint to trigger rebuild proof for one erasure request.
- Add service and controller tests.

## Out Of Scope

- Storing or accepting raw subject values.
- New bitmap storage format or row-level bitmap delete.
- Async queueing for very large audience fleets.
- UI.
- Legal certification text.

## Runtime Semantics

1. The rebuild command never requires raw subject values. It proves replacement by rebuilding materialized audience outputs from already-erased source data.
2. Before rebuild, the service loads the P2-073 request and checks all non-`AUDIENCE_BITMAP_VERSION` asset proofs in that request.
3. If any upstream proof is not `PASS` or `SKIPPED`, the service records `AUDIENCE_BITMAP_VERSION` as `WARN` with a blocked reason and performs no materialization.
4. If explicit `audienceIds` are provided, only enabled `OFFLINE_BATCH` and `HYBRID` definitions for those ids are rebuilt.
5. If no explicit ids are provided, enabled `OFFLINE_BATCH` and `HYBRID` materialization candidates are selected up to the bounded command limit.
6. If no candidates are found, the service records `SKIPPED`.
7. If all selected candidates rebuild with `SUCCESS`, the service records `PASS`.
8. If any selected candidate fails or throws, the service records `FAIL`.
9. If candidate selection is truncated by the command limit, the service records `WARN` after rebuilding the selected candidates, because more bitmap versions require proof.

## Functional Requirements

1. Operators can trigger audience bitmap rebuild proof with tenant scope, request id, actor, limit, and optional audience ids.
2. Rebuild proof must not expose or persist raw subject values.
3. Upstream incomplete, failed, or warning erasure proof must block materialization and record non-pass audience evidence.
4. Successful rebuilds must call existing audience materialization operations and record matched and affected audience counts.
5. Targeted rebuilds must ignore disabled, online-only, or cross-tenant audience definitions.
6. Controller endpoints must use the current tenant context.

## Technical Scope

- Add `CdpWarehousePrivacyAudienceBitmapRebuildService`
- Extend `CdpWarehousePrivacyErasureController`
- Add service and controller tests.
- Update product-evolution indexes.

## Acceptance Criteria

- P2-076 spec and plan are indexed.
- Service tests prove upstream non-pass proof blocks materialization and records `WARN`.
- Service tests prove successful rebuilds record `PASS` for `AUDIENCE_BITMAP_VERSION`.
- Service tests prove no candidates records `SKIPPED`.
- Service tests prove failed materialization records `FAIL`.
- Controller tests prove tenant-scoped rebuild binding.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy the service and endpoint.
2. Run P2-075 erasure execution until CDP, realtime, and Doris assets pass or skip.
3. Trigger audience bitmap rebuild proof for the request.
4. Create tombstones only after the full request proof is acceptable for the operator policy.

## Rollback

- Stop calling the rebuild endpoint.
- Existing P2-073 proof rows remain auditable.
- P2-075 continues to record `AUDIENCE_BITMAP_VERSION` as `WARN` until rebuild proof is restored.
