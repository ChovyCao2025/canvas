# Sandbox Demo Sales Guide

## Demo Boundary

- Use only tenants with a `DEMO_TENANT_*` marker.
- Reset the sandbox before each customer-facing walkthrough.
- Do not import production customer data into a demo tenant.

## Core Walkthrough

1. Open the demo tenant.
2. Show the prepared lifecycle canvas.
3. Trigger the mock paid-order event.
4. For conversational flows, open `/demo-sandbox`, send a sandbox conversation reply, and verify that the canvas WAIT branch resumes through `CONVERSATION_REPLY`.
5. Show the message preview, conversation session, and execution trace.
6. Reset the sandbox after the walkthrough.

## Reset Command

```bash
curl -X POST http://localhost:8080/demo-sandboxes/8/reset -H "Authorization: Bearer <token>"
```

## Conversation Reply Command

```bash
curl -X POST http://localhost:8080/demo-sandboxes/8/conversation-replies \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "canvasId": 1001,
    "versionId": 2001,
    "executionId": "exec-1",
    "userId": "demo-user-1",
    "externalMessageId": "sandbox-reply-1",
    "eventId": "sandbox-event-1",
    "text": "yes please",
    "intent": "PRODUCT_A",
    "attributes": {
      "buttonId": "product-a"
    }
  }'
```

## Rollback

Hide the demo sandbox page and stop calling `/demo-sandboxes`; the lifecycle and conversation tables are additive and can remain in place for cleanup.
