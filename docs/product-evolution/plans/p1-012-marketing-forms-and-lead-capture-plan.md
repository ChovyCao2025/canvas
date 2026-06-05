# Marketing Forms And Lead Capture Implementation Plan

**Goal:** Implement the missing Mautic-style public form and lead-capture capability, with operator management UI and CDP/policy side effects.

**Architecture:** Add additive form definition/submission tables, a tenant-scoped domain service, authenticated operator endpoints, anonymous public endpoints, and React routes for both operator and public form experiences.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React, Ant Design, Vitest.

## Task 1: Product Documentation

- [x] Create P1-012 spec and plan.
- [x] Add P1-012 to specs and plans indexes.
- [x] Register the future `form-collect-node` plugin candidate.

## Task 2: Backend Data And API

- [x] Add `V257__marketing_forms_lead_capture.sql`.
- [x] Add form definition/submission DO and mapper classes.
- [x] Implement `MarketingFormService` with operator CRUD, public metadata, public submit, CDP enrichment, channel upsert, consent upsert, idempotency, and optional behavior trigger.
- [x] Implement `MarketingFormController`.
- [x] Permit anonymous public form GET/POST routes in `SecurityConfig`.

## Task 3: Backend Tests

- [x] Add `MarketingFormServiceTest`.
- [x] Add `MarketingFormControllerTest`.
- [x] Extend `SecurityConfigRouteTest` for public form submit.

## Task 4: Frontend Workbench And Public Form

- [x] Add `marketingFormsApi`.
- [x] Add presentation helpers for schema parsing, status labels, public path, and response previews.
- [x] Add authenticated `pages/marketing-forms` workbench.
- [x] Add anonymous `pages/public-marketing-form` route.
- [x] Wire route, menu item, and route announcements.

## Task 5: Verification

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -pl canvas-engine test -Dmaven.test.skip=true
```

Run:

```bash
cd frontend
npm run test -- src/services/marketingFormsApi.test.ts src/pages/marketing-forms/marketingFormsPresentation.test.ts src/components/layout/AppLayout.a11y.test.tsx
npm run build
```

## Acceptance Checklist

- [x] Operator can create/edit/enable/disable forms.
- [x] Operator can copy or preview the public form route.
- [x] Public form can submit anonymous lead data.
- [x] Submit creates a submission record and enriches CDP/channel/consent when identifying fields exist.
- [x] Frontend focused tests pass.
- [x] Frontend build passes.
- [x] Backend main code compiles.

## Verification Evidence

- [x] Backend main compile passes on 2026-06-05: `JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -pl canvas-engine test -Dmaven.test.skip=true`.
- [x] New backend test sources compile standalone on 2026-06-05 with Maven-generated test classpath.
- [ ] Full backend test run is blocked by pre-existing dirty-tree test compile errors in BI/CanvasProject tests unrelated to P1-012.
- [x] Frontend focused tests pass on 2026-06-05: `npm run test -- src/services/marketingFormsApi.test.ts src/pages/marketing-forms/marketingFormsPresentation.test.ts src/components/layout/AppLayout.a11y.test.tsx` (3 files, 8 tests).
- [x] Frontend production build passes on 2026-06-05: `npm run build`.
