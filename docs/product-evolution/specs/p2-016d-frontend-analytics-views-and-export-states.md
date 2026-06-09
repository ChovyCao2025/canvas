# P2-016D - Frontend Analytics Views And Export States Spec

Priority: P2
Sequence: 016D
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/archive/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p2-016d-frontend-analytics-views-and-export-states-plan.md`

Status: Current frontend implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Add frontend analytics views and bounded export states on top of currently exposed P2-016C APIs.

## Current Baseline

- The product has canvas stats and CDP detail pages.
- P2-016C currently exposes event counts, event totals, user timeline, and attribute distribution endpoints under `/analytics`.
- Funnel, alert preview, and export job backend endpoints are not exposed in the current backend slice.

## In Scope

- `frontend/src/services/analyticsApi.ts`.
- `frontend/src/pages/analytics/index.tsx`.
- Analytics presentation helpers and tests.
- CDP user detail timeline link or embedded summary.
- Loading, empty, error, and unavailable export states.

## Out Of Scope

- Backend analytics query behavior; split into P2-016C.
- Backend funnel, alert preview, and export job endpoints not currently exposed by P2-016C.
- Full dashboard personalization.

## Acceptance Criteria

- Frontend tests cover date-range guard, event row formatting, user timeline formatting, event count ordering, export state labels, and the API wrapper contract for current backend endpoints.
- Production build compiles the analytics route, menu entry, page, and CDP user timeline link.

## Verification Evidence

- 2026-06-09: `cd frontend && npm test -- analyticsApi.test.ts analyticsPresentation.test.ts` passed with 2 test files, 6 tests, 0 failures.
- 2026-06-09: `cd frontend && npm run build` passed.
