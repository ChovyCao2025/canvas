# P2-074 - CDP Warehouse Privacy Tombstone Ingestion Guard Spec

Priority: P2
Sequence: 074
Source: `docs/product-evolution/specs/p2-073-cdp-warehouse-privacy-erasure-propagation-proof.md`, `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/DataDeletionService.java`
Implementation plan: `../plans/p2-074-cdp-warehouse-privacy-tombstone-ingestion-guard-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Prevent erased CDP subjects from being recreated by future events or mirrored back into warehouse OLAP storage by adding tenant-scoped privacy tombstones and enforcing them before CDP profile creation, event persistence, realtime warehouse mirror, and downstream backfill eligibility.

## Current Baseline

- `DataDeletionService` can delete selected CDP and marketing rows.
- P2-073 tracks erasure propagation proof and readiness backlog.
- There is no durable tombstone that prevents a deleted user from being recreated by later CDP event ingestion, identity import, canvas execution, or warehouse mirror.

## In Scope

- Add a privacy subject tombstone table keyed by tenant, subject type, and subject hash.
- Add service APIs to create, revoke, list, inspect, and enforce tombstones.
- Use the same tenant/type/value hash semantics as P2-073.
- Block `CdpEventIngestionService` before user/profile creation, MySQL event insert, event publish, retry enqueue, checkpoint, and warehouse sink mirror.
- Block `CdpUserService.ensureUser` and `ensureUserByIdentity` from recreating tombstoned subjects.
- Record blocked attempts count and last blocked timestamp for audit.
- Add focused tests and warehouse regression verification.

## Out Of Scope

- Replacing `DataDeletionService`.
- Executing physical Doris deletes.
- UI.
- Legal interpretation of erasure requirements.

## Runtime Semantics

1. Tombstones store subject hash and masked subject reference, never raw subject values.
2. Active tombstones block matching tenant + subject type + subject value.
3. Ingestion of blocked user events fails before `cdp_event_log` insert and before `CdpWarehouseEventSink`.
4. User ensure calls for blocked subjects fail before profile/identity insert or last-seen update.
5. Blocked attempts increment `blocked_event_count` and set `last_blocked_at`.
6. Revoked tombstones stop blocking but retain audit history.

## Functional Requirements

1. Operators can create tombstones from raw subject values or from P2-073 request evidence.
2. Operators can revoke a tombstone with reviewer/operator evidence.
3. The service can return a `TombstoneDecision` for ingestion/CDP callers.
4. CDP event ingestion rejects tombstoned user events.
5. CDP user ensure rejects tombstoned user and external identity values.
6. Tombstone list and decision views never expose raw subject values.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V256__cdp_warehouse_privacy_tombstone_guard.sql`
- Add `CdpWarehousePrivacySubjectTombstoneDO`
- Add `CdpWarehousePrivacySubjectTombstoneMapper`
- Add `CdpWarehousePrivacyTombstoneService`
- Add `CdpWarehousePrivacyTombstoneController`
- Modify `CdpEventIngestionService`
- Modify `CdpUserService`
- Add focused schema, service, ingestion, user service, and controller tests.

## Acceptance Criteria

- P2-074 spec and plan are indexed.
- Migration test proves tombstone table, uniqueness, tenant/type/hash index, and blocked-at audit fields.
- Service tests prove hash/mask persistence, active/revoked decisions, and blocked attempt audit updates.
- Ingestion tests prove tombstoned events do not insert event rows, ensure users, publish events, enqueue retries, update checkpoints, or write the warehouse sink.
- User service tests prove tombstoned subjects cannot recreate profiles or identities.
- Controller tests prove tenant-scoped create, revoke, list, and decision endpoints.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy schema and service.
2. Start creating tombstones after verified erasure requests.
3. Monitor blocked attempt counts for source systems still sending erased subjects.
4. Integrate P2-073 erasure workflows to create tombstones after successful propagation proof.

## Rollback

- Revoke or disable tombstones for false positives.
- Remove tombstone service injection from ingestion/user ensure if emergency rollback is required.
- Tombstone rows are additive audit records and do not delete source data by themselves.
