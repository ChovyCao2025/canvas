# P1-006C - Realtime Audiences, Overlap, And Snapshots Spec

Priority: P1
Sequence: 006C
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006c-realtime-audiences-overlap-and-snapshots-plan.md`

## Implementation Status

Implemented on 2026-06-05. This repo already used `V106__realtime_audience_overlap_snapshots.sql` and CDP-prefixed tables, so the implementation follows `cdp_realtime_audience_event_log` and `cdp_audience_snapshot` instead of creating unprefixed tables. Commit step was not executed because the user requested no commit.

## Goal

Upgrade offline audience membership into event-driven realtime updates, overlap analysis, guarded merge/exclusion, and audience snapshots.

## Current Baseline

- `AudienceBatchComputeService` computes offline audiences and writes Redis RoaringBitmap data through `AudienceBitmapStore`.
- `AudienceBitmapStore` supports save, load, membership, and delete.
- P1-005A2 emits internal CDP events.
- Implemented on 2026-06-05: realtime audience event processing, event idempotency ledger, overlap API, merge/exclusion guard, and `cdp_audience_snapshot` history are available in this slice.

## In Scope

- Realtime audience rules evaluated from P1-005A CDP event rows and P1-005A2 internal events.
- Idempotent membership add/remove by source event id.
- Overlap analysis using RoaringBitmap AND.
- Guarded merge and exclusion using OR and ANDNOT under a safe-size limit.
- Audience snapshot table with estimated size, bitmap key, source, and created time.
- Operator UI for realtime status, overlap result, guarded operations, and snapshot trend.

## Out Of Scope

- Bitmap collision remediation; P2-018 must decide that before large-scale merge/exclusion rollout.
- Complex streaming engine migration.
- Full cohort/retention analytics.

## Functional Requirements

1. Realtime membership updates must process each source event once.
2. A matching event must add a user to the realtime audience bitmap.
3. A non-matching event must remove a previously included user when the rule declares `removeOnNoMatch=true`.
4. Overlap must return intersection count and percentages for both audiences.
5. Merge and exclusion must block above configured safe-size limits.
6. Snapshots must store audience id, estimated size, bitmap key, source, and created time.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V106__realtime_audience_overlap_snapshots.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpRealtimeAudienceEventLogDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpAudienceSnapshotDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpRealtimeAudienceEventLogMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpAudienceSnapshotMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/RealtimeAudienceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/RealtimeAudienceController.java`
- `frontend/src/pages/realtime-audiences/index.tsx`
- `frontend/src/services/cdpApi.ts`

## Acceptance Criteria

- Realtime update tests prove event idempotency and membership add/remove.
- Overlap tests prove AND counts and percentages.
- Merge/exclusion tests prove safe operations and blocked operations.
- Snapshot trend is visible in the frontend helper tests.

## Implementation Notes

- Actual table names use the CDP prefix: `cdp_realtime_audience_event_log` and `cdp_audience_snapshot`, to avoid colliding with the existing publish-time `audience_snapshot` table from P1-003.
- `RealtimeAudienceController` accepts event payloads with `properties` and optional `removeOnNoMatch`; the default is `true`.
- `AudienceBitmapStore` exposes non-mutating `overlap`, `merge`, and `exclude` set operations.
- Frontend route: `/cdp/realtime-audiences`.

## Verification

- Backend production compile: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- Backend isolated P1-006C suite passed: `RealtimeAudienceSchemaTest`, `AudienceBitmapStoreSetOpsTest`, `RealtimeAudienceServiceTest`, `RealtimeAudienceControllerTest` ran 11 tests, 0 failures.
- Frontend targeted tests passed: `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- realtimeAudiencePresentation.test.ts cdpApi.test.ts biWorkbench.test.ts` ran 40 tests, 0 failures.
- Frontend production build passed: `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build`.
