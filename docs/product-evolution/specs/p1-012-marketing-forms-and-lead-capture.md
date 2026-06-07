# P1-012 - Marketing Forms And Lead Capture Spec

Priority: P1
Sequence: 012
Source: Mautic Forms, P1-011 marketing preference center, P2-002 plugin extension point discovery
Implementation plan: `../plans/p1-012-marketing-forms-and-lead-capture-plan.md`

## Goal

Add public marketing forms that operators can create, publish, and use to capture leads into Canvas CDP, channel, consent, and journey-trigger data.

## Why This Is Not Duplicate

- Existing `USER_INPUT` waits inside a running journey; it is not a public lead-capture form.
- P1-011 manages preferences after a user exists; this slice creates or updates the user from public form submissions.
- P2-002 defines plugin extension points. This slice implements the product capability directly and registers a future `form-collect-node` plugin candidate for journey-node reuse.

## Mautic Concepts Borrowed

- Forms as public lead-capture endpoints.
- Form submissions that enrich contact profile fields.
- Consent checkbox capture tied to channel opt-in/out.
- Optional post-submit automation trigger.

## In Scope

- Tenant-scoped form definitions with `publicKey`, status, field schema JSON, submit action JSON, and success message.
- Public anonymous GET and POST endpoints for form render metadata and submit.
- Submission ledger with response JSON, UTM JSON, anonymous ID, idempotency key, user ID, consent result, and optional trigger event code.
- Submit-side enrichment of CDP profile, customer channel addresses, and marketing consent.
- Operator UI route `表单中心` for create/edit/enable/disable/copy public URL/recent submissions.
- Public frontend route `/public/forms/:publicKey`.
- Built-in plugin registry seed for the future form collect node handler.

## Out Of Scope

- Drag-and-drop form builder.
- CAPTCHA, spam scoring, and bot mitigation.
- Hosted landing page templates beyond the public form route.
- Third-party runtime plugin loading.
- Full form analytics dashboard.

## Functional Requirements

1. Operators can create, update, list, enable, and disable forms inside the current tenant.
2. Public form metadata can be loaded anonymously by `publicKey`.
3. Public submissions are rejected when the form is inactive.
4. Submissions are stored with idempotency keys to tolerate client retries.
5. Email and phone fields create or merge CDP users and upsert `EMAIL`/`SMS` customer channels.
6. Consent fields or explicit submit payload consent writes `marketing_consent` as `OPT_IN` or `OPT_OUT`.
7. Submit action JSON may include `canvasId` and `triggerEventCode`; when a user is resolved and the disruptor is available, the form publishes a behavior trigger.
8. Operator UI shows recent submissions and can copy/preview the public form path.

## Technical Scope

### Backend

- `backend/canvas-engine/src/main/resources/db/migration/V257__marketing_forms_lead_capture.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingFormDefinitionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingFormSubmissionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingFormDefinitionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingFormSubmissionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingFormService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`

### Frontend

- `frontend/src/services/marketingFormsApi.ts`
- `frontend/src/pages/marketing-forms/index.tsx`
- `frontend/src/pages/marketing-forms/marketingFormsPresentation.ts`
- `frontend/src/pages/public-marketing-form/index.tsx`
- route, menu, and route announcement wiring.

## Acceptance Criteria

- Backend main code compiles.
- Focused form service/controller/security tests exist and compile.
- Frontend API and presentation tests pass.
- Frontend production build passes.
- The page is reachable from authenticated navigation as `表单中心`.
- Public route `/public/forms/:publicKey` renders the actual form without requiring authentication.

## Implementation Status

Completed on 2026-06-05.

- `MarketingFormService` implements tenant-scoped operator CRUD, anonymous public metadata, public submit, idempotent submission capture, CDP enrichment, channel/consent upsert, and optional behavior trigger publishing.
- `MarketingFormController` exposes authenticated `/canvas/marketing-forms` endpoints plus anonymous `/public/marketing-forms/{publicKey}` GET and submit endpoints.
- `SecurityConfig` permits anonymous public marketing form GET/POST routes while keeping the operator workbench authenticated.
- Frontend `marketingFormsApi`, presentation helpers, authenticated `表单中心`, anonymous `/public/forms/:publicKey` page, route, navigation, and route announcements are wired.
- This session reverified focused backend tests, focused frontend tests, plugin candidate registration, and the frontend production build.
