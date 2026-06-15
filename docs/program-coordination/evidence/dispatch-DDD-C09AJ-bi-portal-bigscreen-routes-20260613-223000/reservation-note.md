# DDD-C09AJ Reservation Note

Reserved compact BI portal and big-screen resource lifecycle route batch.

Dispatch: dispatch-DDD-C09AJ-bi-portal-bigscreen-routes-20260613-223000
Task: DDD-C09AJ
Status: RESERVED
Gate: R5 after DDD-C09AI closeout
Base SHA: 2a1cdec07ec27a5298958822014aa28d9312869c

## Scope

- `GET /canvas/bi/portals/resources`
- `GET /canvas/bi/portals/resources/{portalKey}`
- `POST /canvas/bi/portals/resources/{portalKey}/draft`
- `POST /canvas/bi/portals/resources/{portalKey}/publish`
- `DELETE /canvas/bi/portals/resources/{portalKey}`
- `GET /canvas/bi/portals/resources/{portalKey}/versions`
- `POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore`
- `GET /canvas/bi/big-screens/resources`
- `GET /canvas/bi/big-screens/resources/{screenKey}`
- `POST /canvas/bi/big-screens/resources/{screenKey}/draft`
- `POST /canvas/bi/big-screens/resources/{screenKey}/publish`
- `DELETE /canvas/bi/big-screens/resources/{screenKey}`
- `GET /canvas/bi/big-screens/resources/{screenKey}/versions`
- `POST /canvas/bi/big-screens/resources/{screenKey}/versions/{version}/restore`

## Reserved Files

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPortalResourceCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPortalResourceView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiBigScreenResourceCommand.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiBigScreenResourceView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceVersionView.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

## Pre-Dispatch Verification

- G0B backup manifest/branch/head check passed on main at
  `2a1cdec07ec27a5298958822014aa28d9312869c`.
- Program coordination checks passed.
- Dispatch-state verifier passed.
- Cutover preflight exited 0 and reported `/canvas/bi` as the top route gap
  with 39 current endpoints out of 169 old endpoints.

## Rollback

Revert only the exact DDD-C09AJ reserved BI API/domain/application/controller
and BI test files listed above.
