# DDD-C09CL Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/event-definitions`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java`

Routes in scope:

- `GET /canvas/event-definitions`
- `POST /canvas/event-definitions`
- `PUT /canvas/event-definitions/{id}`
- `DELETE /canvas/event-definitions/{id}`

Routes explicitly out of scope:

- `POST /canvas/events/report`

Exact reserved files:

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/EventDefinitionFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/EventDefinitionApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/EventDefinitionCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/EventDefinitionApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/EventDefinitionController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/EventDefinitionControllerCompatibilityTest.java`

## Worker

- Worker: `Kierkegaard 019ec786-2e3f-7571-baad-53075ae799ad`
- Spawned before moving `DDD-C09CL` to `RUNNING`.

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; protect meaningful route compatibility and event-code invalidation behavior only.
- After one bounded worker wait, inspect changed paths, evidence, and focused tests instead of idle polling.
