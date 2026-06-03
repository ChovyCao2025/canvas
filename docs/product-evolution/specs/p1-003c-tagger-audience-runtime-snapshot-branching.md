# P1-003C - TAGGER Audience Runtime Snapshot Branching Spec

Priority: P1
Sequence: 003C
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003c-tagger-audience-runtime-snapshot-branching-plan.md`

## Goal

Make `TAGGER(mode=audience)` runtime execution honor locked snapshot membership when `audienceSnapshotId` is present and current audience membership otherwise.

## Current Baseline

- `TaggerHandler` resolves scheduled batch users through `AudienceUserResolver.resolve(audienceId)`.
- Non-batch TAGGER audience checks use `AudienceBitmapStore.isMember(audienceId, userId)`.
- P1-003B writes `audienceSnapshotId` into published graph JSON for static nodes.

## In Scope

- Constructor injection of `AudienceSnapshotService` into `TaggerHandler`.
- Scheduled batch fan-out from locked snapshot users when a snapshot ID exists.
- Non-batch membership checks from locked snapshot membership when a snapshot ID exists.
- Dynamic refresh remains the current resolver/bitmap behavior.
- Fan-out payload includes `audienceSnapshotId` for child executions spawned from a snapshot.

## Out Of Scope

- Snapshot creation and publish binding; split into P1-003B.
- Delivery provider behavior and message records.
- Audience computation performance redesign.

## Functional Requirements

1. Scheduled static TAGGER fan-out calls `AudienceSnapshotService.users(snapshotId)`.
2. Scheduled dynamic TAGGER fan-out calls `AudienceUserResolver.resolve(audienceId)`.
3. Realtime static membership calls `AudienceSnapshotService.contains(snapshotId, userId)`.
4. Realtime dynamic membership calls `AudienceBitmapStore.isMember(audienceId, userId)`.
5. Handler output includes `audienceSnapshotId` when snapshot membership is used.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerHandler.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/TaggerHandlerTest.java`

## Acceptance Criteria

- Static scheduled tests prove current audience resolver is not called.
- Dynamic scheduled tests prove snapshot service is not called.
- Static realtime tests prove bitmap store is not called.
- Existing realtime/offline TAGGER tests remain green.
