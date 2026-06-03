# P1-003 - Audience Snapshot Mode And Defaults Spec

Priority: P1
Sequence: 003
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003-audience-snapshot-mode-and-defaults-plan.md`

## Goal

Add explicit audience send-mode defaults so every `TAGGER(mode=audience)` node can choose stable publish-time membership or refreshed run-time membership.

## Current Baseline

- `AudienceDefinitionDO` has rule, data source, evaluation strategy, cron, and enabled fields, but no default send mode.
- `TaggerHandler` supports `mode=audience`, but node config does not expose `audienceSnapshotMode`.
- `AudienceController` accepts `AudienceDefinitionDO` request bodies directly.
- Frontend audience create/edit/list screens do not display a default send mode.

## In Scope

- `AudienceSnapshotMode` enum with `STATIC_LOCKED` and `DYNAMIC_REFRESH`.
- `audience_definition.default_snapshot_mode` with service/controller normalization.
- `audience_snapshot` table contract used by P1-003B and P1-003C.
- `TAGGER` node registry schema entries for `audienceSnapshotMode` and server-written `audienceSnapshotId`.
- Audience edit/list frontend controls and helpers for the default mode.

## Out Of Scope

- Publish-time snapshot creation; split into P1-003B.
- Runtime TAGGER branching; split into P1-003C.
- Message preview, canvas import/export, project folders, and AI policy; split into P1-003D through P1-003G.

## Functional Requirements

1. Existing audiences default to `STATIC_LOCKED`.
2. Audience create/update accepts only `STATIC_LOCKED` or `DYNAMIC_REFRESH`; blank input is saved as `STATIC_LOCKED`.
3. Audience list/detail responses include `defaultSnapshotMode`.
4. TAGGER audience node config exposes `audienceSnapshotMode` and hides `audienceSnapshotId` from operator editing.
5. Frontend audience edit form saves the normalized value and audience list labels the selected mode.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V96__audience_snapshot_mode_and_defaults.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/AudienceSnapshotMode.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceSnapshotDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceSnapshotMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- `frontend/src/services/audienceApi.ts`
- `frontend/src/pages/audience-edit/audienceSnapshotMode.ts`
- `frontend/src/pages/audience-edit/index.tsx`
- `frontend/src/pages/audience-list/index.tsx`

## Acceptance Criteria

- Migration contract proves the default mode column, snapshot table, and TAGGER config schema exist.
- Backend tests prove blank and invalid modes are normalized or rejected before persistence.
- Frontend tests prove default mode normalization and labels.
- Audience edit/list UI exposes `发布时锁定` and `每次刷新` without changing audience compute behavior.
