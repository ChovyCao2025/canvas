# P0-003 - Delivery Outbox, Receipts, And Reconciliation Spec

Priority: P0
Sequence: 003
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p0-003-delivery-outbox-receipts-and-reconciliation-plan.md`

## Implementation Status

Implemented and verified on 2026-06-05. Verification evidence is recorded in `../plans/p0-003-delivery-outbox-receipts-and-reconciliation-plan.md`.

## Goal

Make channel delivery crash-safe, retryable, observable, and reconcilable instead of coupling DAG execution directly to downstream channel latency.

## User And Business Value

Operators can trust that a message is either delivered, retrying, reconciled, or visible in a dead-letter workflow; ambiguous PENDING records and silent channel failures stop being normal operating states.

## Evidence From Optimization

- `ReachDeliveryService` writes a PENDING `MessageSendRecordDO` and then calls the reach platform synchronously through `WebClient`.
- There is no `CANVAS_DELIVERY` queue, outbox dispatcher, receipt callback contract, or PENDING reconciliation job.
- Optimization sources flag delivery queue, outbox, receipt tracking, provider backpressure, and channel fallback as production blockers.

## In Scope

- Add a delivery outbox table and dispatcher around all channel send-like side effects.
- Publish delivery jobs to a dedicated RocketMQ topic after the outbox row is committed.
- Record attempt count, next retry time, terminal dead-letter state, provider request id, and idempotency key.
- Add provider receipt callbacks for delivered, failed, opened, clicked, unsubscribed, and bounced states where providers support them.
- Add reconciliation for stale PENDING/SENDING records and an operator replay path.
- Keep DAG semantics explicit: nodes that only need submit-confirmation proceed after outbox enqueue; nodes that require final delivery must wait on a receipt or timeout branch.

## Out Of Scope

- Full provider-specific SDK implementations for every channel in the first slice.
- Marketing attribution based on opens/clicks; that belongs to analytics specs.
- Removing existing `MessageSendRecordDO` before migration is proven safe.

## Functional Requirements

1. Every channel send-like handler must create or reuse a delivery idempotency key before external side effects occur.
2. External channel calls must be performed by the dispatcher, not directly inside the DAG node execution path.
3. Failed delivery attempts must use bounded retry with backoff and a terminal dead-letter state.
4. Stale PENDING/SENDING records must be detectable and reconcilable without rerunning the whole DAG.
5. Receipts must update message records idempotently and preserve raw provider payload for audit.
6. Operators must be able to search delivery records by canvas, execution, user, channel, status, and provider message id.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumer.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageDeliveryController.java`

### Frontend Touchpoints

- `frontend/src/pages/message-delivery/index.tsx`
- `frontend/src/services/messageDeliveryApi.ts`
- `frontend/src/App.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V94__delivery_outbox_receipts.sql`
- `backend/canvas-engine/src/main/resources/application.yml`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerOutboxRoutingTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/DeliveryReceiptControllerTest.java`
- `frontend/src/pages/message-delivery/messageDeliveryPresentation.test.ts`

## Dependencies

- Requires current send handlers to pass execution id, canvas id, node id, user id, channel, template, payload, and idempotency key into one delivery boundary.
- Requires RocketMQ configured in environments where asynchronous delivery is enabled.

## Risks And Controls

- Duplicate delivery risk: enforce unique idempotency key at the outbox layer and keep provider idempotency where supported.
- Semantic drift risk: require every handler to declare whether it waits for enqueue or final receipt.
- Operational backlog risk: expose queue depth, oldest retry age, dead-letter count, and reconciliation count.

## Acceptance Criteria

- Send handlers no longer call downstream delivery HTTP directly in the normal production path.
- A forced process crash after outbox insert and before dispatch can be reconciled without duplicate sends.
- Receipt callbacks are idempotent and update both raw receipt history and current message status.
- Stale PENDING/SENDING reconciliation produces retry, terminal failure, or confirmed sent states with audit trail.
- Backend and frontend tests named in the plan pass with the documented commands.
