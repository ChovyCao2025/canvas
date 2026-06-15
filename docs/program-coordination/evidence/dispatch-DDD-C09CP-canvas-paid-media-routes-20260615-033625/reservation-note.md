# DDD-C09CP Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/paid-media`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PaidMediaAudienceSyncController.java`

Routes in scope:

- `POST /canvas/paid-media/audience-sync/destinations`
- `POST /canvas/paid-media/audience-sync/runs`
- `GET /canvas/paid-media/audience-sync/runs`
- `GET /canvas/paid-media/audience-sync/runs/{runId}/members`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/PaidMediaFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/PaidMediaApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/PaidMediaCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/PaidMediaApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/PaidMediaController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/PaidMediaControllerCompatibilityTest.java`

## Worker

- Worker: `Tesla 019ec7a2-6c3f-7772-a138-11e8403cf20e`
- Spawned before moving `DDD-C09CP` to `RUNNING`.

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; protect destination upsert, sync run state, filters, bounded limits, member listing, tenant/actor mapping, and error envelope.
- After one bounded worker wait, inspect changed paths, evidence, and focused tests instead of idle polling.
