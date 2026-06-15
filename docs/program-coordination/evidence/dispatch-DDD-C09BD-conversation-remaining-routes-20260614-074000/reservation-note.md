# DDD-C09BD Reservation

- Dispatch: `dispatch-DDD-C09BD-conversation-remaining-routes-20260614-074000`
- Task: `DDD-C09BD`
- Scope: remaining `/canvas/conversations` compatibility route aliases.
- Gate: R5 after DDD-C09BC BI remaining route closeout.
- Base commit: `2a1cdec07ec27a5298958822014aa28d9312869c`

## Preflight

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported:

- `canvas-web`: 23 controllers / 424 endpoints.
- Top route gap: `route:/canvas/conversations`, old 4 controllers / 24 endpoints, current 1 controller / 7 endpoints.
- Representative old controllers:
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java`
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java`
  - `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java`

## Exact Reserved Files

- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/ConversationFacade.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java`

## Target Legacy Aliases

Add compatibility coverage for missing final-module routes including:

- `POST /canvas/conversations/adapters/{adapterKey}/ingress`
- `GET /canvas/conversations`
- `GET /canvas/conversations/{sessionId}/messages`
- `GET /canvas/conversations/workspace/inbox`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/tasks`
- `POST /canvas/conversations/workspace/tasks/{taskId}/complete`
- `GET /canvas/conversations/workspace/work-items/{workItemId}/timeline`
- `POST /canvas/conversations/workspace/sla-breaches/evaluate`
- `GET /canvas/conversations/workspace/sla-breaches`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions/generate`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review`
- `GET /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions`
- `POST /canvas/conversations/private-domain/sync-runs`
- `GET /canvas/conversations/private-domain/contacts`
- `GET /canvas/conversations/private-domain/groups`
- `GET /canvas/conversations/private-domain/sync-runs`
- `POST /canvas/conversations/provider-webhooks/whatsapp`

## Coordination Rules

- Do not edit `backend/canvas-engine/**`; read it only as legacy reference.
- Do not edit POM files.
- Coordinator owns `dispatch-state.json`, `progress-ledger.md`, and this evidence directory.
- Use TDD: add focused RED controller compatibility coverage first, then implement minimal final-module facade/application/controller behavior.
- Avoid old engine/domain imports in final web/context modules.
- Scheduler rule: coordinator waits at most once, then inspects changed paths/evidence/tests and continues locally if needed.
