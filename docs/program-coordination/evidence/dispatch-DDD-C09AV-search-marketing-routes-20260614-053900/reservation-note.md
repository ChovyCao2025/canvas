# DDD-C09AV Reservation Note

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AV-search-marketing-routes-20260614-053900`
Task: `DDD-C09AV`

## Scope

This dispatch reserves the Search Marketing route batch for final-module
compatibility after DDD-C09AU closeout. It targets all 24 legacy
`/canvas/search-marketing` endpoints currently missing from `canvas-web`.

## Exact Reserved Files

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/SearchMarketingFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/SearchMarketingApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/SearchMarketingCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/SearchMarketingApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/SearchMarketingController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/SearchMarketingControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`

## Pre-Reservation Evidence

- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- Preflight after DDD-C09AU reports `canvas-web` at 17 controllers and 247
  endpoints, with `/canvas/search-marketing` as the top route gap candidate:
  1 old controller, 24 old endpoints, 0 current controllers, 0 current
  endpoints.
- The exact Search Marketing target files did not exist before reservation.

## Dispatch Discipline

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the returned worker id is recorded. After one wait timeout, the coordinator must
inspect changed paths, evidence, and focused test state before any further wait.
