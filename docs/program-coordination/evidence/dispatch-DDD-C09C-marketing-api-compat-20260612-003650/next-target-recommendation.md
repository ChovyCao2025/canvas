# Next DDD-C09 Compatibility Target Recommendation

reviewer: multi_agent_v1-explorer Bernoulli 019eb7bb-c2a9-7ef3-8654-03c18728160e
status: completed after one coordinator wait timeout

## Recommendation

Use `DDD-C09D` for:

`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java`

Reason: the final conversation context has a single rich
`ConversationFacade`, a real `ConversationApplicationService`, and reusable
in-memory harness patterns, while old engine route shapes map cleanly to the
facade without importing `canvas-engine`.

## Worker Reads

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/ConversationFacade.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java`
- `backend/canvas-context-conversation/src/test/java/org/chovy/canvas/conversation/application/ConversationApplicationServiceTest.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java`
- optional exclude check:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java`

## Behaviors To Assert

- `POST /canvas/conversations/ingress` returns `code=0`,
  `message=success`, `data.status=RECORDED`, `duplicate=false`, and
  `resumedWaitCount=1`.
- Repeating the same ingress message returns existing `sessionId/messageId`,
  `duplicate=true`, and `resumedWaitCount=0`.
- `POST /canvas/conversations/workspace/sessions/{sessionId}/work-item`
  creates `OPEN` / `NORMAL` / `UNROUTED` work item with normalized
  `channel/provider`.
- `POST /canvas/conversations/workspace/work-items/{workItemId}/assign`
  preserves envelope and returns `assignedTo`, `assignedTeam`.
- `POST /canvas/conversations/workspace/work-items/{workItemId}/status`
  normalizes `status=PENDING`, `priority=HIGH`, and preserves
  `nextFollowUpAt`.
- `POST /routing-agents`, `POST /routing-rules`, then
  `POST /work-items/{id}/route` routes to the lowest-load available agent and
  returns `routed=true`, `assignedTo=alice`, required skills, and SLA due time.

## Risks And Non-Selections

- Conversation risk: final facade does not cover old
  list/messages/inbox/timeline/tasks/SLA/AI/public WhatsApp routes. Keep this
  seed scoped to ingress, work item, assignment/status, and routing.
- Execution: old routes include HMAC public direct calls, Disruptor behavior
  triggers, approvals, and reruns; final DDD surface mostly covers
  trigger/trace, so a seed would either stub too much or miss legacy behavior.
- CDP: final behavior is spread across several facades and old routes span
  write-key auth, event ingestion, tags, audience, and warehouse readiness.
- BI: rich final facade, but old BI resource controllers have many routes not
  covered by `BiCatalogFacade`; route compatibility is less exact.
- Risk: clean facade, but too narrow. `RiskDecisionFacade.evaluate` alone
  cannot support 3-6 route-style compatibility assertions without leaning on
  non-final old strategy/scene/list APIs.

## Suggested Verification

```bash
mvn -f backend/pom.xml -pl canvas-web -am -Dtest=ConversationApiCompatibilityTest test
mvn -f backend/pom.xml -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest test
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

`--require-ready` should still fail after this seed until the other four
required compatibility tests land.
