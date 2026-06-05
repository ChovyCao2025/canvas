# Delivery Outbox, Receipts, And Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make channel delivery crash-safe, retryable, observable, and reconcilable instead of coupling DAG execution directly to downstream channel latency.

**Architecture:** Add a transactional delivery outbox between DAG handlers and external providers. Handlers enqueue delivery intent, a RocketMQ-backed dispatcher owns provider calls and retries, receipt callbacks update message state, and a reconciliation job repairs stale PENDING/SENDING records.

**Tech Stack:** Java 21, Spring Boot WebFlux style controllers, MyBatis, Flyway, RocketMQ, Redis where needed, React 18, TypeScript, Ant Design, JUnit 5, Mockito, Vitest.

## Implementation Status

- Status: implemented and verified on 2026-06-05.
- Commit: not created in this session because the worktree contains many unrelated and parallel product-evolution changes.

---

## Spec Reference

- `docs/product-evolution/specs/p0-003-delivery-outbox-receipts-and-reconciliation.md`
- Optimization sources: `docs/optimization/production-design-gaps.md`, `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumer.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageDeliveryController.java`

**Frontend**
- Create: `frontend/src/pages/message-delivery/index.tsx`
- Create: `frontend/src/services/messageDeliveryApi.ts`
- Modify: `frontend/src/App.tsx`

**Data And Config**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V94__delivery_outbox_receipts.sql`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerOutboxRoutingTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/DeliveryReceiptControllerTest.java`
- Create: `frontend/src/pages/message-delivery/messageDeliveryPresentation.test.ts`

### Task 1: Contract And Failing Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerOutboxRoutingTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/DeliveryReceiptControllerTest.java`
- Create: `frontend/src/pages/message-delivery/messageDeliveryPresentation.test.ts`

- [x] **Step 1: Write outbox service tests**

Create these test methods in `DeliveryOutboxServiceTest`:

```java
@Test void enqueueCreatesPendingOutboxRowAndMessageRecord()
@Test void enqueueReturnsExistingOutboxForSameIdempotencyKey()
@Test void markDispatchingClaimsOnlyPendingOrRetryRows()
@Test void markRetryIncrementsAttemptAndSetsNextRetryAt()
@Test void markDeadStoresTerminalFailureReason()
```

- [x] **Step 2: Write dispatcher and receipt tests**

Create these dispatcher and receipt test methods:

```java
@Test void consumerCallsProviderOnceAndMarksSent()
@Test void consumerRetriesProviderFailureWithBoundedBackoff()
@Test void receiptCallbackUpdatesCurrentStatusIdempotently()
@Test void stalePendingRowsAreReturnedForReconciliation()
```

- [x] **Step 3: Write send handler routing tests**

In `SendMessageHandlerOutboxRoutingTest`, cover the current generic `SendMessageHandler` with node configs where `channel` is `SMS`, `EMAIL`, `PUSH`, `WECHAT`, and `IN_APP`. Assert every channel returns an enqueue/submitted result through `ReachDeliveryService` and never calls provider HTTP directly from the DAG execution path.

- [x] **Step 4: Write frontend presentation tests**

In `messageDeliveryPresentation.test.ts`, cover loading, empty, list filtering, detail drawer, dead-letter replay disabled state, and receipt history rendering.

- [x] **Step 5: Run focused tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DeliveryOutboxServiceTest,SendMessageHandlerOutboxRoutingTest,DeliveryOutboxConsumerTest,DeliveryReceiptControllerTest
cd frontend && npm test -- messageDeliveryPresentation.test.ts
```

Expected: backend and frontend tests fail because outbox services, controllers, API wrapper, and UI are not implemented.

### Task 2: Data Model And Outbox Service

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V94__delivery_outbox_receipts.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerOutboxRoutingTest.java`

- [x] **Step 1: Add additive Flyway migration**

Create `delivery_outbox` with tenant id, message send record id, execution id, canvas id, user id, node id, channel, provider, payload JSON, idempotency key, status, attempt count, next retry time, locked by, locked at, provider message id, last error, created at, updated at, and indexes for status/next retry, idempotency, execution, user, and provider message id.

Create `delivery_receipt_log` with tenant id, outbox id, provider message id, receipt type, raw payload JSON, received at, and unique idempotency key.

- [x] **Step 2: Implement enqueue and claim semantics**

In `DeliveryOutboxService`, implement:

```java
DeliveryOutboxDO enqueue(ReachDeliveryService.DeliveryRequest request);
Optional<DeliveryOutboxDO> claimNext(String workerId, LocalDateTime now);
void markSent(Long outboxId, String providerMessageId, Map<String, Object> response);
void markRetry(Long outboxId, String errorMessage, LocalDateTime nextRetryAt);
void markDead(Long outboxId, String errorMessage);
List<DeliveryOutboxDO> findStalePending(LocalDateTime before, int limit);
```

Use unique idempotency key to return an existing row without inserting a duplicate.

- [x] **Step 3: Route the generic send-message handler through the outbox boundary**

Keep `AbstractSendMessageHandler` request construction stable and verify `SendMessageHandler` preserves the configured channel value for `SMS`, `EMAIL`, `PUSH`, `WECHAT`, and `IN_APP`. Change production `ReachDeliveryService.send` behavior so it enqueues outbox work and returns an enqueue result. Keep the provider HTTP call available behind a package-private method used only by `DeliveryOutboxConsumer`.

- [x] **Step 4: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DeliveryOutboxServiceTest,SendMessageHandlerOutboxRoutingTest
```

Expected: PASS for enqueue, dedupe, claim, retry, dead-letter, and all send-message handler routing tests.

### Task 3: Dispatcher, Receipts, Reconciliation, And UI

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/DeliveryOutboxConsumer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DeliveryReceiptController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MessageDeliveryController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java`
- Create: `frontend/src/services/messageDeliveryApi.ts`
- Create: `frontend/src/pages/message-delivery/index.tsx`
- Modify: `frontend/src/App.tsx`

- [x] **Step 1: Add dispatcher configuration**

Add `canvas.delivery.outbox.topic=CANVAS_DELIVERY`, retry backoff properties, stale pending threshold, and dispatcher enabled flag in `application.yml`.

- [x] **Step 2: Implement dispatcher**

`DeliveryOutboxConsumer` claims pending/retry rows, calls the provider method, marks sent on success, marks retry on recoverable failure, and marks dead after max attempts. It must log outbox id, channel, provider, attempt, and final status.

- [x] **Step 3: Implement receipt callback**

`DeliveryReceiptController` accepts provider, provider message id, receipt type, event time, and raw payload. It stores a receipt log row, idempotently updates current message status, and rejects invalid signatures or unknown provider message ids.

- [x] **Step 4: Implement operator API and UI**

`MessageDeliveryController` exposes search, detail, receipt history, replay dead-letter, and reconcile stale pending actions. The frontend page uses filters for canvas, execution, user, channel, provider, status, and provider message id.

- [x] **Step 5: Run focused backend and frontend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DeliveryOutboxConsumerTest,DeliveryReceiptControllerTest
cd frontend && npm test -- messageDeliveryPresentation.test.ts
```

Expected: PASS for dispatcher, receipt, reconciliation, and presentation tests.

### Task 4: Verification And Rollout

**Files:**
- Modify: `docs/product-evolution/specs/p0-003-delivery-outbox-receipts-and-reconciliation.md`
- Modify: `docs/product-evolution/plans/p0-003-delivery-outbox-receipts-and-reconciliation-plan.md`

- [x] **Step 1: Run focused backend slice**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DeliveryOutboxServiceTest,SendMessageHandlerOutboxRoutingTest,DeliveryOutboxConsumerTest,DeliveryReceiptControllerTest,CouponHandlerTest,CommitActionHandlerTest
```

Expected: PASS.

- [x] **Step 2: Run frontend focused slice**

Run:

```bash
cd frontend && npm test -- messageDeliveryPresentation.test.ts
```

Expected: PASS.

- [x] **Step 3: Run module regression**

Run:

```bash
cd backend && mvn -pl canvas-engine test
cd frontend && npm test -- --run
```

Expected: PASS or record each unrelated failure with file, test name, and reproduction command.

- [x] **Step 4: Add rollout notes**

Document feature flag, migration order, dispatcher enablement, rollback to direct-send mode, reconciliation command, and manual receipt replay procedure in the implementation PR.

### Verification Evidence

- Focused backend slice:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=DeliveryOutboxServiceTest,SendMessageHandlerOutboxRoutingTest,DeliveryOutboxConsumerTest,DeliveryReceiptControllerTest,CouponHandlerTest,CommitActionHandlerTest
```

Result: 19 tests, 0 failures, 0 errors, 0 skipped.

- Frontend focused slice:

```bash
cd frontend && npm test -- messageDeliveryPresentation.test.ts
```

Result: 1 test file, 3 tests passed.

- Backend module regression:

```bash
cd backend && mvn -pl canvas-engine test
```

Result: 1402 tests, 0 failures, 0 errors, 1 skipped.

- Frontend module regression:

```bash
cd frontend && npm test
```

Result: 73 test files, 271 tests passed.

- Frontend production build:

```bash
cd frontend && npm run build
```

Result: passed.

- [ ] **Step 5: Commit implementation slice**

Run:

```bash
git add backend/canvas-engine/src frontend/src docs/product-evolution/specs docs/product-evolution/plans
git commit -m "feat: add delivery outbox and receipts"
```

Expected: commit contains only delivery outbox, receipt, reconciliation, UI, spec, and plan files.
