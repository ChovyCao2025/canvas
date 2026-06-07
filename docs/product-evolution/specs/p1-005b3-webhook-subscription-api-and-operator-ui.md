# P1-005B3 - Webhook Subscription API And Operator UI Spec

Priority: P1
Sequence: 005B3
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`
Implementation plan: `../plans/p1-005b3-webhook-subscription-api-and-operator-ui-plan.md`

## Goal

Expose webhook subscription management, secret rotation, test delivery, and delivery log inspection to operators.

## Current Baseline

- Implemented and merged into `main` on 2026-06-05.
- P1-005B adds schema, validation, and signing.
- P1-005B2 adds dispatcher and delivery logs.
- Webhook management is available at `/webhook-subscriptions` for administrators.

## In Scope

- `WebhookSubscriptionController` for list, create, update, pause, resume, disable, rotate secret, test delivery, and delivery log list.
- Frontend webhook subscription page with event type picker, status controls, secret rotation, test delivery, and delivery log view.
- Presentation helpers for status, last delivery result, retry labels, and dead labels.

## Out Of Scope

- Inbound webhooks.
- Connector-specific destination adapters.
- Public developer portal.

## Functional Requirements

1. Create and update reuse P1-005B callback URL and event type validation.
2. Rotate secret returns the new secret only in the rotation response.
3. Test delivery uses the dispatcher path and logs the attempt.
4. UI exposes current status, last delivery status, retry/dead labels, and safe secret display.
5. Operators can pause, resume, and disable subscriptions.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/WebhookSubscriptionControllerTest.java`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/pages/webhook-subscriptions/index.tsx`
- `frontend/src/pages/webhook-subscriptions/webhookSubscriptionPresentation.ts`
- `frontend/src/pages/webhook-subscriptions/webhookSubscriptions.test.ts`
- `frontend/src/services/cdpEventApi.ts`

## Acceptance Criteria

- Controller tests prove CRUD, pause/resume/disable, rotate secret, test delivery, and log list.
- Frontend tests prove status labels, retry/dead labels, and secret-safe rows.
- UI can manage subscriptions without database access.

## Implementation Status

- Status: implemented and merged into `main` on 2026-06-05.
- Backend: added `WebhookSubscriptionController` with list/create/update/pause/resume/disable/rotate/test-delivery/delivery-log endpoints under `/cdp/webhooks`.
- Frontend: added webhook API helpers, status/secret presentation helpers, `/webhook-subscriptions` operator page, admin route, and side-nav entry.
- Production compile: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -DskipTests compile` passed.
- Frontend verification: `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- cdpEventApi.test.ts webhookSubscriptions.test.ts` passed; `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.
- Focused webhook backend tests pass in an isolated runner for P1-005B/P1-005B2/P1-005B3. Maven `testCompile` remains blocked by unrelated existing test-source errors in the broader workspace.
