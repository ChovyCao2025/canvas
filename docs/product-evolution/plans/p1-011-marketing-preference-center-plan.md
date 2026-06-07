# Marketing Preference Center Implementation Plan

**Goal:** Implement the missing Mautic-inspired operator/admin surface for marketing consent, customer channels, and suppression records using existing Canvas policy tables.

**Architecture:** Add a tenant-scoped service above the existing MyBatis-Plus mappers. Expose WebFlux endpoints for user report and policy writes. Add a React/Ant Design workbench that manages one user at a time and keeps delivery-path policy logic unchanged.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, JUnit 5, Mockito, AssertJ, React, Ant Design, Vitest.

## Task 1: Product Documentation

- [x] Create P1-011 spec and plan.
- [x] Add P1-011 to specs and plans indexes.

## Task 2: Backend Service

- [x] Write `MarketingPreferenceCenterServiceTest` first.
- [x] Implement `MarketingPreferenceCenterService` with:
  - `report(Long tenantId, String userId)`
  - `upsertConsent(Long tenantId, String userId, ConsentUpdateCommand command)`
  - `upsertChannel(Long tenantId, String userId, ChannelUpdateCommand command)`
  - `addSuppression(Long tenantId, String userId, SuppressionCreateCommand command)`
  - `deactivateSuppression(Long tenantId, Long suppressionId)`

## Task 3: Backend Controller

- [x] Write `MarketingPreferenceCenterControllerTest` first.
- [x] Implement `/canvas/marketing-preferences` endpoints with tenant fallback to `0`.

## Task 4: Frontend Workbench

- [x] Write API and presentation tests first.
- [x] Implement `marketingPreferencesApi` and presentation helpers.
- [x] Add `pages/marketing-preferences` with report, edit forms, suppression form, and deactivate actions.
- [x] Wire route, menu, and route announcement.

## Task 5: Verification

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=MarketingPreferenceCenterServiceTest,MarketingPreferenceCenterControllerTest
```

Run:

```bash
cd frontend
npm run test -- src/services/marketingPreferencesApi.test.ts src/pages/marketing-preferences/marketingPreferencesPresentation.test.ts
npm run build
```

## Acceptance Checklist

- [x] Operator can load a user's combined preference report.
- [x] Operator can upsert consent and channel records.
- [x] Operator can add and deactivate suppressions.
- [x] No send-path behavior or schema migration changes are introduced.
- [x] Focused backend and frontend tests pass.
- [x] Frontend build passes.

## Verification Evidence

- [x] Backend focused tests pass on 2026-06-05: `JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -pl canvas-engine test -Dtest=MarketingPreferenceCenterServiceTest,MarketingPreferenceCenterControllerTest` (5 tests, 0 failures, 0 errors, 0 skipped).
- [x] Frontend focused tests pass on 2026-06-05: `npm run test -- src/services/marketingPreferencesApi.test.ts src/pages/marketing-preferences/marketingPreferencesPresentation.test.ts src/components/layout/AppLayout.a11y.test.tsx` (7 tests, 0 failures).
- [x] Frontend production build passes on 2026-06-05: `npm run build`.
- [x] Reverified in this session on 2026-06-05: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine -Dtest=MarketingPreferenceCenterServiceTest,MarketingPreferenceCenterControllerTest test` from `backend` (5 tests, 0 failures, 0 errors, 0 skipped).
- [x] Reverified in this session on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run test -- marketingPreferencesApi.test.ts marketingPreferencesPresentation.test.ts AppLayout.a11y.test.tsx` from `frontend` (7 tests, 0 failures).
- [x] Reverified in this session on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run build` from `frontend` (build succeeded; generated `marketing-preferences-DpyPrR39.js`).
