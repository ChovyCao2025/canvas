# DDD-C09AK Reservation Note

Date: 2026-06-13
Dispatch: dispatch-DDD-C09AK-bi-ai-assistant-routes-20260613-231931
Task: DDD-C09AK BI AI assistant route batch
Coordinator: main agent

## Reservation

The coordinator selected DDD-C09AK because `route:/canvas/bi` remains the top
cutover gap after DDD-C09AJ, with 20 old controllers / 169 old endpoints and
current `canvas-web` coverage at 1 controller / 53 endpoints. The legacy
`BiAiController` is one cohesive five-route slice and can be moved as a compact
final-module seed without widening the current BI controller scope.

## Exact Reserved Files

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAiRequestCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAiResponseView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiAiAssistantCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

## Pre-Dispatch Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` passed with no active dispatches.
- `test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md && git branch --show-current && git rev-parse HEAD` passed on `main` at `2a1cdec07ec27a5298958822014aa28d9312869c`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported current `canvas-web` 15 controllers / 93 endpoints, `route:/canvas/bi` 1 controller / 53 endpoints, and `cutoverReady=false`.

## Rollback

Revert only the exact DDD-C09AK reserved files listed above.
