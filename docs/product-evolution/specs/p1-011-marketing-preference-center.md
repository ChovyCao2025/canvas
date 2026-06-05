# P1-011 - Marketing Preference Center Spec

Priority: P1
Sequence: 011
Source: Mautic preference center and Do Not Contact concepts, P1-009 contactability explainer, P1-010 Mautic-inspired explainability suite, existing Canvas marketing policy tables
Implementation plan: `../plans/p1-011-marketing-preference-center-plan.md`

## Goal

Add an operator-facing marketing preference center that lets authenticated operators inspect and manage a user's channel consent, reachable channel addresses, and suppression records from one place.

## Why This Is Not Duplicate

- Canvas already enforces consent, suppression, and channel availability in the send path.
- P1-009 and P1-010 explain those decisions, but they deliberately stay read-only.
- This slice adds the missing write/admin surface on top of existing `marketing_consent`, `customer_channel`, and `marketing_suppression` tables.

## Mautic Concepts Borrowed

- Preference center: centralize opt-in/opt-out and channel availability controls.
- Do Not Contact: show and deactivate active suppression records.
- Contact channels: maintain the address and verification status that delivery policies already inspect.

## In Scope

- Backend service and controller under `/canvas/marketing-preferences`.
- User-level report containing consent rows, channel rows, suppression rows, and a summary.
- Upsert consent by user/channel.
- Upsert channel address and enabled/verified state by user/channel.
- Add suppression and deactivate suppression records.
- Frontend API wrapper, presentation helpers, page, route, navigation, and focused tests.

## Out Of Scope

- Public unsubscribe pages or anonymous token flows.
- New database tables or migrations.
- Changing delivery-path policy behavior.
- Topic/category-level preferences beyond existing channel-level data.
- Full audit log or DSAR workflow.

## Functional Requirements

1. A user report returns all consent rows, channel rows, and suppression rows for the current tenant and user.
2. The report summary returns total channel count, opt-in count, opt-out count, active suppression count, and reachable channel count.
3. Consent upsert normalizes channels to uppercase and stores `OPT_IN` or `OPT_OUT`.
4. Channel upsert normalizes channels, stores address, enabled flag, verified flag, and metadata.
5. Suppression creation supports channel-specific or `ALL` suppression, reason, active state, and optional expiry time.
6. Suppression deactivation is tenant-scoped and only flips the selected row inactive.
7. The UI must expose query, edit, add suppression, deactivate suppression, and stable status labels.

## Technical Scope

### Backend

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingPreferenceCenterController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/policy/MarketingPreferenceCenterServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingPreferenceCenterControllerTest.java`

### Frontend

- `frontend/src/services/marketingPreferencesApi.ts`
- `frontend/src/services/marketingPreferencesApi.test.ts`
- `frontend/src/pages/marketing-preferences/index.tsx`
- `frontend/src/pages/marketing-preferences/marketingPreferencesPresentation.ts`
- `frontend/src/pages/marketing-preferences/marketingPreferencesPresentation.test.ts`
- route and menu wiring in `frontend/src/App.tsx`, `frontend/src/components/layout/AppLayout.tsx`, and route announcements.

## Acceptance Criteria

- Backend tests prove report aggregation, summary counts, upsert behavior, and suppression deactivation.
- Controller tests prove tenant fallback and response wrapping.
- Frontend tests prove endpoint contracts and presentation labels.
- The page is reachable from the authenticated app navigation as `偏好中心`.
- Focused backend tests, focused frontend tests, and frontend production build pass.
