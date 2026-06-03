# P1-006C - Realtime Audiences, Overlap, And Snapshots Spec

Priority: P1
Sequence: 006C
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006c-realtime-audiences-overlap-and-snapshots-plan.md`

## Goal

Upgrade offline audience membership into event-driven realtime updates, overlap analysis, guarded merge/exclusion, and audience snapshots.

## Current Baseline

- `AudienceBatchComputeService` computes offline audiences and writes Redis RoaringBitmap data through `AudienceBitmapStore`.
- `AudienceBitmapStore` supports save, load, membership, and delete.
- P1-005A2 emits internal CDP events.
- There is no realtime audience event consumer, event idempotency table, overlap API, merge/exclusion guard, or snapshot history.

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

- `backend/canvas-engine/src/main/resources/db/migration/V101__realtime_audience_overlap_snapshots.sql`
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
