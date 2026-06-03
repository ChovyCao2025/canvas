# P2-016D - Frontend Analytics Views And Export States Spec

Priority: P2
Sequence: 016D
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p2-016d-frontend-analytics-views-and-export-states-plan.md`

## Goal

Add frontend analytics views and bounded export states on top of P2-016C APIs.

## Current Baseline

- The product has canvas stats and CDP detail pages, but no consolidated analytics command center for event analysis, funnels, user timelines, attribute distribution, alerts, or exports.
- There is no `analyticsApi.ts` wrapper.

## In Scope

- `frontend/src/services/analyticsApi.ts`.
- `frontend/src/pages/analytics/index.tsx`.
- Analytics presentation helpers and tests.
- CDP user detail timeline link or embedded summary.
- Loading, empty, error, permission-denied, and export queued states.

## Out Of Scope

- Backend analytics query behavior; split into P2-016C.
- Full dashboard personalization.

## Acceptance Criteria

- Frontend tests cover required date range, event table/chart state, funnel steps, user timeline, attribute distribution, alert preview, export queued state, empty/error/permission states.
