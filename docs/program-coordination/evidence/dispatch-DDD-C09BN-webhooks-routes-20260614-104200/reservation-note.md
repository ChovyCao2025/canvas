# DDD-C09BN Webhooks Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/cdp/webhooks` as the top current route gap after DDD-C09BM:

- old controllers: 1
- old endpoints: 9
- current controllers: 0
- current endpoints: 0
- legacy reference:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java`

## Legacy Routes

- `GET /cdp/webhooks`
- `POST /cdp/webhooks`
- `PUT /cdp/webhooks/{id}`
- `PUT /cdp/webhooks/{id}/pause`
- `PUT /cdp/webhooks/{id}/resume`
- `DELETE /cdp/webhooks/{id}`
- `POST /cdp/webhooks/{id}/rotate-secret`
- `POST /cdp/webhooks/{id}/test`
- `GET /cdp/webhooks/{id}/deliveries`

## Exact Reserved Files

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWebhookFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWebhookApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWebhookCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWebhookApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWebhookController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWebhookControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BN-webhooks-routes-20260614-104200/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and
verification. Harvest the worker at most once when useful; do not idle poll.
