# DDD-C09CO Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/mq-definitions`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MqDefinitionController.java`

Routes in scope:

- `GET /canvas/mq-definitions`
- `POST /canvas/mq-definitions`
- `PUT /canvas/mq-definitions/{id}`
- `DELETE /canvas/mq-definitions/{id}`

Exact reserved files:

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/MqDefinitionFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/MqDefinitionApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/MqDefinitionCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/MqDefinitionApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/MqDefinitionController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/MqDefinitionControllerCompatibilityTest.java`

## Worker

- Worker: `Poincare 019ec79b-d06a-7b02-a1f6-6ceca24d7bda`
- Spawned before moving `DDD-C09CO` to `RUNNING`.

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; protect enabled filtering, legacy page shape, path-id authority, route rebuild/invalidation behavior, and error envelope if relevant.
- After one bounded worker wait, inspect changed paths, evidence, and focused tests instead of idle polling.
