# P1-010 - Mautic Inspired Explainability Suite Spec

Priority: P1
Sequence: 010
Source: Mautic 4.4.12 local UI/source review, P1-009 contactability explainer, existing Canvas audience, trace, channel connector, suppression, and policy work
Implementation plan: `../plans/p1-010-mautic-inspired-explainability-suite-plan.md`

## Goal

Add a read-only operator explainability suite inspired by Mautic concepts that Canvas does not yet expose as first-class operator evidence: audience membership reasons, journey path reasons, channel preference resolution, suppression history, publish health checks, and frequency policy templates.

## Why This Is Not Duplicate

- Canvas already computes audiences, stores execution traces, enforces marketing policy, routes channel fallback, and validates publish behavior.
- Existing P1/P2 specs cover execution, persistence, fallback, timeline display, and compliance controls.
- This slice does not add new CRUD, delivery behavior, or data writes. It adds compact explanation APIs and a frontend workbench that help operators understand existing decisions before changing a journey.

## Mautic Concepts Borrowed

- Segments: explain whether a user currently belongs to an audience and which rule/stat evidence supports that answer.
- Campaign path: explain the executed and skipped journey nodes for a user execution.
- Preference center and channel preferences: resolve usable channels and recommended primary/fallback channel.
- Do Not Contact: show suppression reason history and active/expired state.
- Pre-flight checks: summarize publish readiness risks in an operator-oriented response.
- Frequency rules: expose reusable frequency policy templates instead of hiding defaults in node JSON.

## In Scope

- Backend read-only service and controller under `/canvas/mautic-insights`.
- Frontend API wrapper, presentation helpers, and an admin workbench page.
- Product spec and plan entries.
- Focused tests for the service/controller and frontend presentation/API wrappers.

## Out Of Scope

- Writing preference center data.
- Changing channel fallback execution.
- Replacing existing execution timeline or dry-run traces.
- Replacing existing audience computation.
- Persisting frequency templates.
- Creating Mautic-compatible import/export APIs.

## Functional Requirements

1. Audience membership explanation returns audience metadata, stat status, latest run summary, and a membership status of `MATCHED`, `NOT_MATCHED`, `UNKNOWN`, or `NOT_READY`.
2. Journey path explanation returns ordered trace steps for an execution and a compact reason for each status.
3. Channel preference resolution returns eligible channels, suppressed channels, unavailable channels, and a recommended channel.
4. Suppression timeline returns active and expired suppression records ordered from newest to oldest.
5. Publish health check returns a score and checks for published version presence, graph presence, trigger presence, send node presence, and active status.
6. Frequency templates return built-in policy presets for global, journey, channel, and node scopes.
7. All endpoints must be read-only and must not consume frequency counters or write delivery/audience state.

## Technical Scope

### Backend

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/insights/MauticInspiredInsightService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MauticInspiredInsightController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/insights/MauticInspiredInsightServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MauticInspiredInsightControllerTest.java`

### Frontend

- `frontend/src/services/mauticInsightsApi.ts`
- `frontend/src/services/mauticInsightsApi.test.ts`
- `frontend/src/pages/mautic-insights/index.tsx`
- `frontend/src/pages/mautic-insights/mauticInsightsPresentation.ts`
- `frontend/src/pages/mautic-insights/mauticInsightsPresentation.test.ts`
- route and menu wiring in `frontend/src/App.tsx` and `frontend/src/components/layout/AppLayout.tsx`

## Acceptance Criteria

- Backend tests prove all six concept surfaces return stable read-only evidence from existing mappers/services.
- Controller tests prove each endpoint delegates request parameters and wraps responses in `R.ok`.
- Frontend tests prove API wrappers call the documented endpoints and presentation helpers map statuses/checks to stable labels.
- Frontend production build passes.
- The feature is visible from the authenticated app navigation as an operator workbench.

## Implementation Status

Status: Completed on 2026-06-05.

- `MauticInspiredInsightService` exposes read-only explanations for audience membership, journey path, channel preference resolution, suppression timeline, publish health, and frequency templates.
- `MauticInspiredInsightController` serves the suite under `/canvas/mautic-insights` without mutating delivery, audience, or policy state.
- Frontend `mauticInsightsApi`, presentation helpers, route, navigation, and the operator workbench are wired into the authenticated app.
- This session reverified focused backend tests, focused frontend tests, and the frontend production build.
