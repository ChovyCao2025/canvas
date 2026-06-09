# P1-006 - CDP Computed Profile Attributes Spec

Priority: P1
Sequence: 006
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006-cdp-computed-profile-attributes-plan.md`

## Goal

Add computed profile attributes so CDP profiles can store governed values derived from ingested events, existing profile properties, tags, and identities.

## Current Baseline

- Implemented in the current workspace record on 2026-06-05; commit and merge status was not verified in this docs-only audit.
- `cdp_user_profile.properties_json` stores static and computed profile attributes.
- `ComputedProfileAttributeService` supports governed definition creation, activation, pause, preview, manual run, event-run idempotency, run history, and change-log lookup.
- Operator UI is available at `/cdp/computed-profile` for administrators.

## In Scope

- Computed profile attribute definition metadata.
- RULE and EXPR first-slice computation against existing profile JSON.
- Preview count before activation.
- Activate, pause, run-now, and run-history operations.
- Write computed values back into `cdp_user_profile.properties_json`.
- Record old value, new value, source run id, and changed time.
- Operator UI for create, preview, activate, pause, run, run-history, and change-log inspection.

## Out Of Scope

- Computed tags; split into P1-006B.
- Realtime audiences, overlap, and snapshots; split into P1-006C.
- Full feature store, ML features, or OLAP warehouse.

## Functional Requirements

1. Definitions must validate `attrCode`, `valueType`, `computeType`, and expression before activation.
2. Preview must return scanned, matched, changed, unchanged, and sample rows without writing profiles.
3. Run-now must write profile values and insert change-log rows.
4. Event-driven mode must be idempotent by source event id.
5. Disabled definitions must not run.
6. Profile JSON updates must preserve unrelated profile fields.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V104__cdp_computed_profile_attributes.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedProfileAttributeDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpComputedProfileRunDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpProfileAttributeChangeLogDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpComputedProfileControllerTest.java`
- `frontend/src/pages/cdp-computed-profile/index.tsx`
- `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.ts`
- `frontend/src/pages/cdp-computed-profile/computedProfilePresentation.test.ts`
- `frontend/src/services/cdpApi.ts`

## Acceptance Criteria

- Computed profile attributes can be created, previewed, activated, paused, run, and audited.
- Profile JSON writeback preserves existing keys.
- Change history shows old and new values.
- Tests cover preview without write, run with write, invalid expression rejection, and idempotent event run.

## Implementation Status

- Status: implemented in the current workspace record on 2026-06-05; commit and merge status was not verified in this docs-only audit.
- Backend: added schema coverage for `V104`, tightened definition validation, added `unchangedCount`, tenant-safe profile scans, pause, run-history, change-log lookup, event idempotency by `(tenant_id, attr_id, source_event_id)`, and `CdpComputedProfileController` endpoints under `/cdp/computed-profile-attributes`.
- Frontend: added typed `cdpApi.computedProfiles` helpers, presentation helpers, `/cdp/computed-profile` admin page, route, and side-nav entry.
- Verification: focused Java 21 backend verification passed on 2026-06-08 with `ComputedProfileAttributeSchemaTest`, `ComputedProfileAttributeServiceTest`, and `CdpComputedProfileControllerTest` covering 9 tests. Focused frontend verification passed on 2026-06-08 with `computedProfilePresentation.test.ts` and `cdpApi.test.ts` covering 7 tests.
