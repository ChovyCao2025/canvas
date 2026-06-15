# DDD-C09BP Public Ingress Route Aliases Reservation

Date: 2026-06-14T11:51:34+08:00

Coordinator: main thread

## Classification

CONTINUE. `dispatch-state.json` validated with `activeDispatches` empty after
DDD-C09BO closeout. Fresh preflight reported `route:/public` as the top route
gap with old 4 controllers / 8 endpoints and current 0 / 0.

## Reserved Scope

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/PublicIngressFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/PublicIngressApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/PublicIngressCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/PublicIngressApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/publicingress/PublicIngressController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/publicingress/PublicIngressControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BP-public-ingress-routes-20260614-115134/**`

## Target Routes

- `GET /public/marketing-forms/{publicKey}`
- `POST /public/marketing-forms/{publicKey}/submit`
- `GET /public/conversation-webhooks/{tenantId}/whatsapp`
- `POST /public/conversation-webhooks/{tenantId}/whatsapp`
- `GET /public/conversations/webhooks/{tenantId}/whatsapp`
- `POST /public/conversations/webhooks/{tenantId}/whatsapp`
- `POST /public/marketing/content/assets/upload-callbacks/{tenantId}/{provider}`
- `POST /public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}`

## Scheduler Rule

Spawn a real worker before marking RUNNING. The worker is a bounded sidecar; the
coordinator continues local RED/GREEN implementation and verification without
idle waiting. At most one wait is allowed before inspecting paths/evidence/tests
and closing or integrating the worker.
