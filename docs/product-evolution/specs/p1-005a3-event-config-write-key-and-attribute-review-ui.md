# P1-005A3 - Event Config Write Key And Attribute Review UI Spec

Priority: P1
Sequence: 005A3
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/2026-05-30-cdp-sdk-design.md`
Implementation plan: `../plans/p1-005a3-event-config-write-key-and-attribute-review-ui-plan.md`

## Implementation Status

Status: implemented on 2026-06-05.

Implemented files:

- `frontend/src/services/cdpEventApi.ts`
- `frontend/src/services/cdpEventApi.test.ts`
- `frontend/src/pages/event-config/eventAttributeReview.ts`
- `frontend/src/pages/event-config/eventAttributeReview.test.ts`
- `frontend/src/pages/event-config/index.tsx`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventAttributeDiscoveryController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/EventAttributeDiscoveryControllerTest.java`

Notes:

- `frontend/src/components/layout/AppLayout.tsx` already exposes `/event-config` under `事件配置`, so no navigation change was required.
- The frontend-required discovered attribute list API was missing, so `GET /canvas/event-attributes/discovered` was added as a read-only backend endpoint.
- Attribute approve/reject HTTP actions remain out of scope for this slice.
- Backend production compile passes. Focused backend test execution is blocked by unrelated test-source compile errors in the current dirty workspace.

## Verification Evidence

- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- cdpEventApi.test.ts eventAttributeReview.test.ts` passed: 2 files, 6 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.
- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=EventAttributeDiscoveryControllerTest -DfailIfNoTests=true` is blocked during global `testCompile` by unrelated current workspace test sources, including duplicate `KillSwitchSubscriberTest`, missing P2-079 automation run classes, and constructor/API mismatches in existing tests.

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
- `frontend/src/components/layout/AppLayout.tsx` (already had `/event-config` entry)
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventAttributeDiscoveryController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/EventAttributeDiscoveryControllerTest.java`

## Acceptance Criteria

- Frontend tests cover write-key payload defaults, secret-safe rows, pending-first sorting, and status labels.
- Event config page exposes discoverable entry points for write keys and attribute review.
