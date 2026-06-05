# Mautic Inspired Explainability Suite Implementation Plan

**Goal:** Implement six Mautic-inspired operator evidence surfaces without duplicating existing Canvas execution, fallback, audience, or compliance logic.

**Architecture:** Add a read-only insight service above existing mappers and policy-adjacent data. Expose one WebFlux controller and one frontend workbench page. Keep each concept as a compact DTO so future richer screens can reuse the same contract.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, JUnit 5, Mockito, AssertJ, React, Ant Design, Vitest.

## Task 1: Product Documentation

- [x] Create P1-010 spec and plan.
- [x] Add P1-010 to specs and plans indexes.

## Task 2: Backend Read-Only Service

- [x] Write `MauticInspiredInsightServiceTest` first.
- [x] Implement `MauticInspiredInsightService` with methods:
  - `explainAudienceMembership(Long audienceId, String userId)`
  - `explainJourneyPath(String executionId)`
  - `resolveChannelPreference(String userId, String preferredChannel)`
  - `suppressionTimeline(String userId)`
  - `publishHealth(Long canvasId)`
  - `frequencyTemplates()`

## Task 3: Backend Controller

- [x] Write `MauticInspiredInsightControllerTest` first.
- [x] Implement `MauticInspiredInsightController` under `/canvas/mautic-insights`.

## Task 4: Frontend Workbench

- [x] Write API and presentation tests first.
- [x] Implement `mauticInsightsApi` and presentation helpers.
- [x] Add `pages/mautic-insights` with controls for user, audience, execution, and canvas inputs.
- [x] Wire route and navigation menu.

## Task 5: Verification

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=MauticInspiredInsightServiceTest,MauticInspiredInsightControllerTest
```

Run:

```bash
cd frontend
npm run test -- src/services/mauticInsightsApi.test.ts src/pages/mautic-insights/mauticInsightsPresentation.test.ts
npm run build
```

## Acceptance Checklist

- [x] Six concept surfaces are implemented as read-only APIs.
- [x] Frontend workbench is reachable from navigation.
- [x] The slice does not add new policy writes, delivery writes, or audience writes.
- [x] Focused backend and frontend tests pass.
- [x] Frontend build passes.

## Verification Evidence

- [x] Backend focused tests pass on 2026-06-05: `JAVA_HOME=$(/usr/libexec/java_home -v 21) PATH="$JAVA_HOME/bin:$PATH" mvn -f backend/canvas-engine/pom.xml -Dtest=MauticInspiredInsightServiceTest,MauticInspiredInsightControllerTest test` (5 tests, 0 failures, 0 errors, 0 skipped).
- [x] Frontend focused tests pass on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run test -- src/services/mauticInsightsApi.test.ts src/pages/mautic-insights/mauticInsightsPresentation.test.ts` (4 tests, 0 failures).
- [x] Frontend production build passes on 2026-06-05: `PATH="/opt/homebrew/bin:$PATH" npm run build`.
