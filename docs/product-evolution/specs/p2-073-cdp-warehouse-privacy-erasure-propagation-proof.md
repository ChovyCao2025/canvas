# P2-073 - CDP Warehouse Privacy Erasure Propagation Proof Spec

Priority: P2
Sequence: 073
Source: `docs/product-evolution/specs/p2-034-cdp-warehouse-field-governance-and-bi-policy.md`, `docs/product-evolution/specs/p3-010-advanced-privacy-and-compliance.md`
Implementation plan: `../plans/p2-073-cdp-warehouse-privacy-erasure-propagation-proof-plan.md`

## Goal

Add an auditable CDP warehouse privacy erasure propagation proof so data-subject deletion work can be tracked across MySQL CDP state, Doris ODS/DWD assets, realtime buffers, and audience materialization outputs before production readiness is allowed to pass.

## Current Baseline

- P2-034 governs field access and BI policy, but explicitly excludes data-subject request handling.
- P3-010 gates advanced privacy candidates, but explicitly excludes runtime deletion/export behavior.
- Warehouse readiness, availability, and E2E certification do not know whether erasure requests are pending, overdue, failed, or proven across warehouse assets.

## In Scope

- Tenant-scoped erasure request ledger using hashed subject references instead of storing raw user identifiers.
- Per-asset propagation proof rows for CDP operational tables, Doris warehouse layers, realtime buffers, and audience materialization artifacts.
- Service APIs to create a request, create default asset plans, record asset proof, list request history, and summarize backlog status.
- Operator controller endpoints for request creation, proof recording, request lookup, and backlog summary.
- Production readiness evidence key `privacy_erasure_backlog` when the proof service is available.
- Tests that prove schema, hashing/masking, default asset plans, proof status rollup, controller binding, and readiness evidence.

## Out Of Scope

- Executing physical deletes in Doris or MySQL.
- Exporting data-subject data.
- Legal interpretation or compliance certification.
- UI.

## Runtime Semantics

1. A request stores `subject_hash` and `subject_ref_masked`; raw subject values are not persisted in the warehouse privacy ledger.
2. Creating a request inserts default asset proof plans unless explicit target assets are supplied.
3. Asset proof can be recorded independently by external deletion jobs or manual operator runs.
4. Request status is derived from asset proofs: `FAIL` beats `WARN`, incomplete plans keep the request `RUNNING`, and all terminal PASS/SKIPPED proofs produce `PASS`.
5. Backlog summary is `FAIL` when any request is overdue or failed, `WARN` when active requests are pending/running/warned, and `PASS` when there is no active backlog.
6. Production readiness includes `privacy_erasure_backlog` evidence when the service is configured.

## Functional Requirements

1. Operators can create an erasure request with request key, subject type, subject value, reason, SLA due time, target assets, and requester.
2. The service must hash and mask the subject and never expose the raw subject in returned views.
3. Default plans must include CDP profile/identity/tag state, CDP event log, Doris ODS/DWD assets, realtime retry buffer, and audience materialization artifacts.
4. Operators can record asset proof with status, matched count, affected count, proof message, error, executor, and executed time.
5. Request status must roll up from proof statuses.
6. Readiness proof must fail or warn when erasure backlog is failed, overdue, or active.
7. Controller endpoints must be tenant-scoped.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V254__cdp_warehouse_privacy_erasure_proof.sql`
- Add `CdpWarehousePrivacyErasureRequestDO`
- Add `CdpWarehousePrivacyErasureAssetProofDO`
- Add `CdpWarehousePrivacyErasureRequestMapper`
- Add `CdpWarehousePrivacyErasureAssetProofMapper`
- Add `CdpWarehousePrivacyErasureService`
- Add `CdpWarehousePrivacyErasureController`
- Extend `CdpWarehouseProductionReadinessProofService` with optional privacy erasure evidence.
- Add focused schema, service, readiness, and controller tests.

## Acceptance Criteria

- P2-073 spec and plan are indexed.
- Migration test proves both request and asset proof tables exist with tenant, request, subject hash, asset key, status, and SLA indexes.
- Service tests prove raw subject values are not returned, default plans are created, asset proof rolls request status, and backlog summary handles PASS/WARN/FAIL.
- Readiness tests prove `privacy_erasure_backlog` evidence affects production proof status when configured.
- Controller tests prove tenant-scoped create, proof record, summary, and recent endpoints.
- Focused backend tests pass.
- Warehouse regression passes.

## Rollout

1. Deploy migration and service with no automatic deletion workers.
2. Register erasure requests from compliance operations or deletion workflow integrations.
3. Wire external deletion jobs to record per-asset proof.
4. Monitor readiness evidence and incidents for overdue or failed propagation.
5. Only after stable proof collection, add executable delete workers as a child spec.

## Rollback

- Stop creating new erasure requests.
- Production readiness can be run with the service disabled if emergency rollback is needed.
- Ledger rows are additive audit records and do not affect ingestion or query execution directly.
