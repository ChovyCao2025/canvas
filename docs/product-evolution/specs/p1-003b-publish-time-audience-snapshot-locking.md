# P1-003B - Publish-Time Audience Snapshot Locking Spec

Priority: P1
Sequence: 003B
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003b-publish-time-audience-snapshot-locking-plan.md`

## Goal

Create durable audience snapshots during canvas publish when a `TAGGER(mode=audience)` node uses `STATIC_LOCKED`.

## Current Baseline

- P1-003 adds `AudienceSnapshotMode`, `AudienceSnapshotDO`, and `audience_snapshot`.
- `AudienceUserResolver.resolve(audienceId)` can return the current user list.
- `CanvasService.publish` persists published graph JSON through the existing transaction path.
- Draft graph JSON is the operator-owned editable graph and should not be mutated just because publish locked a snapshot.

## In Scope

- `AudienceSnapshotService.lockSnapshot` that stores distinct nonblank user IDs and snapshot metadata.
- Configurable maximum snapshot user count with publish failure when exceeded.
- Publish-time graph transformation:
  - resolve blank node mode from the selected audience default;
  - write `audienceSnapshotMode=STATIC_LOCKED` and `audienceSnapshotId` into published graph JSON;
  - remove stale `audienceSnapshotId` for `DYNAMIC_REFRESH`.
- Tests proving draft graph JSON is not permanently mutated by publish binding.

## Out Of Scope

- Runtime fan-out and membership checks; split into P1-003C.
- Audience set operations, overlap, and realtime audience products; covered by P1-006C.
- UI display of snapshot count and timestamp beyond persisted metadata.

## Functional Requirements

1. Publishing a canvas with static TAGGER audience nodes creates one snapshot per node.
2. The snapshot stores `audienceId`, `canvasId`, `canvasVersionId`, `nodeId`, `snapshotMode`, `userCount`, `userIdsJson`, `createdBy`, and `createdAt`.
3. Published graph JSON contains `audienceSnapshotId` only for static nodes.
4. Dynamic nodes publish without stale `audienceSnapshotId`.
5. Snapshot locking fails the publish before version promotion if the resolved user count exceeds `canvas.audience.snapshot.max-users`.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSnapshotService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceSnapshotServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPublishAudienceSnapshotTest.java`

## Acceptance Criteria

- Static publish writes snapshot IDs into the published graph and stores resolved users.
- Dynamic publish removes stale snapshot IDs.
- Snapshot size limit returns a clear publish failure and creates no published version.
- Tests verify service behavior and publish integration.
