# DDD-C09CN Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/loyalty`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/LoyaltyController.java`

Routes in scope:

- `GET /canvas/loyalty/users/{userId}/account`
- `POST /canvas/loyalty/users/{userId}/earn`
- `POST /canvas/loyalty/users/{userId}/redeem`
- `GET /canvas/loyalty/users/{userId}/benefits`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/LoyaltyFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/LoyaltyApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/LoyaltyCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/LoyaltyApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/LoyaltyController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/LoyaltyControllerCompatibilityTest.java`

## Worker

- Worker: `Meitner 019ec795-0f95-7d13-8f0e-74d0ad2b33f9`
- Spawned before moving `DDD-C09CN` to `RUNNING`.

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; protect loyalty state transitions, idempotency, insufficient-balance behavior, tenant/header mapping, and error envelope.
- After one bounded worker wait, inspect changed paths, evidence, and focused tests instead of idle polling.
