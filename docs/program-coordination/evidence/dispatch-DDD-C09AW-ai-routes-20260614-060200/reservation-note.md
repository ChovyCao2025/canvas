# DDD-C09AW Reservation Note

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AW-ai-routes-20260614-060200`
Task: `DDD-C09AW`

## Scope

This dispatch reserves the AI route batch for final-module compatibility after
DDD-C09AV closeout. It targets all 23 legacy `/ai` endpoints currently missing
from `canvas-web`.

## Exact Reserved Files

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AiFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AiApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AiCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AiApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/ai/AiControllerCompatibilityTest.java`

## Pre-Reservation Evidence

- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- Preflight after DDD-C09AV reports `canvas-web` at 18 controllers and 271
  endpoints, with `/ai` as the top route gap candidate: 4 old controllers, 23
  old endpoints, 0 current controllers, 0 current endpoints.
- The exact AI target files did not exist before reservation.
- `backend/canvas-web/pom.xml` already depends on `canvas-platform`, so no POM
  edit is reserved or needed.

## Dispatch Discipline

The dispatch remains `RESERVED` until a real code-writing worker is spawned and
the returned worker id is recorded. After one wait timeout, the coordinator must
inspect changed paths, evidence, and focused test state before any further wait.
