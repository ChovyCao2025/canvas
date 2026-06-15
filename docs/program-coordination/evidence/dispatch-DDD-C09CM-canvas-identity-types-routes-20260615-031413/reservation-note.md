# DDD-C09CM Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/identity-types`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/IdentityTypeController.java`

Routes in scope:

- `GET /canvas/identity-types`
- `POST /canvas/identity-types`
- `PUT /canvas/identity-types/{id}`
- `DELETE /canvas/identity-types/{id}`

Routes explicitly out of scope:

- `GET /meta/identity-types`

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpIdentityTypeFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpIdentityTypeApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpIdentityTypeCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpIdentityTypeApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpIdentityTypeController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpIdentityTypeControllerCompatibilityTest.java`

## Worker

- Worker: `Anscombe 019ec78e-17d5-7cf3-b2d8-e9902eb6bc59`
- Spawned before moving `DDD-C09CM` to `RUNNING`.

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; protect normalization/defaults, filters/order, path-id authority, and delete guard/error envelope.
- After one bounded worker wait, inspect changed paths, evidence, and focused tests instead of idle polling.
