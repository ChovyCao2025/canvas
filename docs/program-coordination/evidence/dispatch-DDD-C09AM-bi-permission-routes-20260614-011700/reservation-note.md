# DDD-C09AM Reservation Note

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AM-bi-permission-routes-20260614-011700`
Task: `DDD-C09AM`

## Reservation

Reserved exact-scope BI permission administration route batch after DDD-C09AL
closeout. The current cutover preflight reports `route:/canvas/bi` as the top
candidate with 20 old controllers, 169 old endpoints, 1 current production
controller, and 65 current production endpoints.

Legacy source:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java`

## Reserved Files

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourcePermissionCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourcePermissionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiRowPermissionCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiRowPermissionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiColumnPermissionCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiColumnPermissionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionAuditEntryView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestReviewCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPermissionAdministrationCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

## Required Next Step

Generate the canonical worker prompt, spawn a real code-writing worker, record
the actual worker id/nickname, and only then move the dispatch to `RUNNING`.

## Rollback

Revert only the exact DDD-C09AM reserved BI API/domain/application/controller and
BI test files listed in the worker packet.
