# P1-005A3 - Event Config Write Key And Attribute Review UI Spec

Priority: P1
Sequence: 005A3
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`
Implementation plan: `../plans/p1-005a3-event-config-write-key-and-attribute-review-ui-plan.md`

## Goal

Expose CDP write keys and pending discovered event attributes in the existing event configuration area.

## Current Baseline

- `frontend/src/pages/event-config/index.tsx` manages event definitions.
- There is no frontend service for write keys or discovered attributes.
- P1-005 and P1-005A2 add backend APIs/data needed by the UI.

## In Scope

- `frontend/src/services/cdpEventApi.ts`.
- Write-key list/create/disable helpers and tests.
- Discovered attribute row normalization and review status presentation helpers.
- Event config page entry points for write keys and pending attributes.

## Out Of Scope

- Full developer portal and SDK onboarding wizard.
- Attribute approval backend endpoints beyond list/read if not already present.
- Browser SDK implementation; split into P1-005C.

## Functional Requirements

1. Write-key create form sends default platform `WEB`, default QPS `100`, optional daily quota, and description.
2. Write-key list never displays hash or raw keys.
3. Newly created raw key is shown only from the create response.
4. Pending attributes sort before approved/rejected attributes.
5. Status labels use `待审核`, `已通过`, and `已拒绝`.

## Technical Scope

- `frontend/src/services/cdpEventApi.ts`
- `frontend/src/services/cdpEventApi.test.ts`
- `frontend/src/pages/event-config/eventAttributeReview.ts`
- `frontend/src/pages/event-config/eventAttributeReview.test.ts`
- `frontend/src/pages/event-config/index.tsx`
- `frontend/src/components/layout/AppLayout.tsx`

## Acceptance Criteria

- Frontend tests cover write-key payload defaults, secret-safe rows, pending-first sorting, and status labels.
- Event config page exposes discoverable entry points for write keys and attribute review.
