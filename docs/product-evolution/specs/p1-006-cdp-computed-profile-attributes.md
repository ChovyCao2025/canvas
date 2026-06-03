# P1-006 - CDP Computed Profile Attributes Spec

Priority: P1
Sequence: 006
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006-cdp-computed-profile-attributes-plan.md`

## Goal

Add computed profile attributes so CDP profiles can store governed values derived from ingested events, existing profile properties, tags, and identities.

## Current Baseline

- `cdp_user_profile.properties_json` stores static profile attributes.
- `CdpUserService` can ensure and return user profiles.
- P1-005A adds `cdp_event_log`; P1-005A2 adds internal CDP event publication.
- There is no computed profile definition table, preview flow, run history, or profile attribute change log.

## In Scope

- Computed profile attribute definition metadata.
- RULE and EXPR first-slice computation against existing profile JSON, tags, identities, and recent `cdp_event_log` rows.
- Preview count before activation.
- Activate, pause, run-now, and run-history operations.
- Write computed values back into `cdp_user_profile.properties_json`.
- Record old value, new value, source run id, and changed time.

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

- `backend/canvas-engine/src/main/resources/db/migration/V99__cdp_computed_profile_attributes.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedProfileAttributeService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java`
- `frontend/src/pages/cdp-computed-profile/index.tsx`
- `frontend/src/services/cdpApi.ts`

## Acceptance Criteria

- Computed profile attributes can be created, previewed, activated, paused, run, and audited.
- Profile JSON writeback preserves existing keys.
- Change history shows old and new values.
- Tests cover preview without write, run with write, invalid expression rejection, and idempotent event run.
