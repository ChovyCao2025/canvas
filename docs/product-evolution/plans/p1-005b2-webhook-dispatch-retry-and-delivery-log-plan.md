# Webhook Dispatch Retry And Delivery Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dispatch matching webhook events with signed HTTP requests, bounded retry classification, and auditable delivery logs.

**Architecture:** The dispatcher reads active subscriptions, matches event types, signs payloads via P1-005B, and writes one delivery log per attempt. Retry decisions live in a small policy class so HTTP outcome classification is testable without network calls.

**Tech Stack:** Java 21, Spring Boot WebFlux, WebClient, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005b2-webhook-dispatch-retry-and-delivery-log.md`
- Depends on: `docs/product-evolution/specs/p1-005b-webhook-subscription-schema-and-signing.md`

## File Structure

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicy.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDeliveryPayload.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDispatcherService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookDispatcherServiceTest.java`

### Task 1: Retry Policy

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicyTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicy.java`

- [ ] **Step 1: Write retry policy tests**

Create `WebhookRetryPolicyTest.java`:

```java
class WebhookRetryPolicyTest {

    private final WebhookRetryPolicy policy = new WebhookRetryPolicy();

    @Test
    void retriesNetworkFailuresHttp429AndHttp5xx() {
        assertThat(policy.classify(null, true, 1, 3).status()).isEqualTo("RETRYING");
        assertThat(policy.classify(429, false, 1, 3).status()).isEqualTo("RETRYING");
        assertThat(policy.classify(503, false, 1, 3).status()).isEqualTo("RETRYING");
    }

    @Test
    void marksNon429Http4xxAsFailed() {
        WebhookRetryPolicy.Decision decision = policy.classify(400, false, 1, 3);

        assertThat(decision.status()).isEqualTo("FAILED");
        assertThat(decision.nextRetryAt()).isNull();
        assertThat(decision.terminalReason()).contains("HTTP_400");
    }

    @Test
    void marksDeadWhenMaxAttemptsReached() {
        WebhookRetryPolicy.Decision decision = policy.classify(500, false, 3, 3);

        assertThat(decision.status()).isEqualTo("DEAD");
        assertThat(decision.nextRetryAt()).isNull();
    }
}
```

- [ ] **Step 2: Run policy tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookRetryPolicyTest
```

Expected: FAIL because policy does not exist.

- [ ] **Step 3: Add retry policy**

Create `WebhookRetryPolicy.java`:

```java
@Service
public class WebhookRetryPolicy {

    public record Decision(String status, LocalDateTime nextRetryAt, String terminalReason) {
    }

    public Decision classify(Integer httpStatus, boolean networkFailure, int attempt, int maxAttempts) {
        if (!networkFailure && httpStatus != null && httpStatus >= 200 && httpStatus < 300) {
            return new Decision(WebhookDeliveryLogDO.SUCCESS, null, null);
        }
        boolean retryable = networkFailure || httpStatus == null || httpStatus == 429 || httpStatus >= 500;
        if (!retryable) {
            return new Decision(WebhookDeliveryLogDO.FAILED, null, "HTTP_" + httpStatus);
        }
        if (attempt >= maxAttempts) {
            return new Decision(WebhookDeliveryLogDO.DEAD, null, "MAX_ATTEMPTS_REACHED");
        }
        return new Decision(
                WebhookDeliveryLogDO.RETRYING,
                LocalDateTime.now().plusSeconds((long) Math.pow(2, attempt)),
                null);
    }
}
```

- [ ] **Step 4: Run policy tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookRetryPolicyTest
```

Expected: PASS.

### Task 2: Dispatcher Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookDispatcherServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDeliveryPayload.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDispatcherService.java`

- [ ] **Step 1: Write dispatcher tests**

Create `WebhookDispatcherServiceTest.java` with these tests:

```java
@Test
void dispatchSkipsSubscriptionsWithoutMatchingEventType() {
    WebhookSubscriptionDO sub = subscription("[\"profile.updated\"]");
    when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));

    dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

    verifyNoInteractions(webClientBuilder);
    verify(deliveryLogMapper, never()).insert(any());
}

@Test
void dispatchSignsHeadersAndLogsSuccess() {
    WebhookSubscriptionDO sub = subscription("[\"cdp.event.ingested\"]");
    when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
    stubHttpStatus(202);

    dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

    ArgumentCaptor<WebhookDeliveryLogDO> captor = ArgumentCaptor.forClass(WebhookDeliveryLogDO.class);
    verify(deliveryLogMapper).insert(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(WebhookDeliveryLogDO.SUCCESS);
    assertThat(captor.getValue().getPayload()).contains("msg-1");
}

@Test
void dispatchRetriesHttp429() {
    WebhookSubscriptionDO sub = subscription("[\"cdp.event.ingested\"]");
    when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
    stubHttpStatus(429);

    dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

    verify(deliveryLogMapper).insert(argThat(log -> WebhookDeliveryLogDO.RETRYING.equals(log.getStatus())
            && log.getNextRetryAt() != null));
}

@Test
void dispatchMarksNon429Http4xxAsFailed() {
    WebhookSubscriptionDO sub = subscription("[\"cdp.event.ingested\"]");
    when(subscriptionMapper.selectList(any())).thenReturn(List.of(sub));
    stubHttpStatus(400);

    dispatcher.dispatch(42L, "cdp.event.ingested", Map.of("messageId", "msg-1"));

    verify(deliveryLogMapper).insert(argThat(log -> WebhookDeliveryLogDO.FAILED.equals(log.getStatus())
            && Integer.valueOf(400).equals(log.getHttpStatus())
            && log.getNextRetryAt() == null));
}
```

Use local mocks for `WebhookSubscriptionMapper`, `WebhookDeliveryLogMapper`, `WebhookSignatureService`, `ObjectMapper`, `WebClient.Builder`, and `WebhookRetryPolicy`. `stubHttpStatus(int)` should configure the mocked WebClient chain to return a `ResponseEntity<Void>` with that status.

- [ ] **Step 2: Run dispatcher tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookDispatcherServiceTest
```

Expected: FAIL because dispatcher does not exist.

- [ ] **Step 3: Add payload record**

Create `WebhookDeliveryPayload.java`:

```java
public record WebhookDeliveryPayload(
        String schemaVersion,
        String eventType,
        String deliveryId,
        Map<String, Object> data
) {
    public static WebhookDeliveryPayload of(String eventType, String deliveryId, Map<String, Object> data) {
        return new WebhookDeliveryPayload("2026-06-03", eventType, deliveryId, data);
    }
}
```

- [ ] **Step 4: Add dispatcher**

Create `WebhookDispatcherService.java`:

```java
@Service
@RequiredArgsConstructor
public class WebhookDispatcherService {
    private final WebhookSubscriptionMapper subscriptionMapper;
    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookSignatureService signatureService;
    private final WebhookRetryPolicy retryPolicy;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    public void dispatch(Long tenantId, String eventType, Map<String, Object> payload) {
        List<WebhookSubscriptionDO> subs = subscriptionMapper.selectList(new LambdaQueryWrapper<WebhookSubscriptionDO>()
                .eq(WebhookSubscriptionDO::getTenantId, tenantId)
                .eq(WebhookSubscriptionDO::getStatus, WebhookSubscriptionDO.ACTIVE));
        for (WebhookSubscriptionDO sub : subs) {
            if (matches(sub.getEventTypes(), eventType)) {
                sendOnce(sub, eventType, payload, UUID.randomUUID().toString(), 1);
            }
        }
    }

    public void sendOnce(WebhookSubscriptionDO sub, String eventType, Map<String, Object> payload,
                         String deliveryId, int attempt) {
        String rawPayload = writeJson(WebhookDeliveryPayload.of(eventType, deliveryId, payload));
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = signatureService.sign(resolveSecret(sub), timestamp, rawPayload);
        WebhookDeliveryLogDO log = newLog(sub, eventType, rawPayload, deliveryId, attempt);
        try {
            var response = webClientBuilder.build()
                    .post()
                    .uri(sub.getCallbackUrl())
                    .header("X-Canvas-Event", eventType)
                    .header("X-Canvas-Delivery", deliveryId)
                    .header("X-Canvas-Timestamp", timestamp)
                    .header("X-Canvas-Signature", signature)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .bodyValue(rawPayload)
                    .retrieve()
                    .toBodilessEntity()
                    .block(Duration.ofSeconds(5));
            applyDecision(log, retryPolicy.classify(
                    response == null ? null : response.getStatusCode().value(),
                    false,
                    attempt,
                    sub.getMaxAttempts()));
        } catch (Exception e) {
            log.setErrorMessage(e.getMessage());
            applyDecision(log, retryPolicy.classify(null, true, attempt, sub.getMaxAttempts()));
        }
        deliveryLogMapper.insert(log);
    }
}
```

Add private helpers `applyDecision`, `newLog`, `matches`, `writeJson`, and `resolveSecret`. `applyDecision` copies `status`, `nextRetryAt`, and `terminalReason` from the policy decision and sets `httpStatus` before calling it when HTTP status is known.

- [ ] **Step 5: Run dispatcher tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookRetryPolicyTest,WebhookDispatcherServiceTest
```

Expected: PASS.

### Task 3: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005b2-webhook-dispatch-retry-and-delivery-log.md`
- Read: `docs/product-evolution/plans/p1-005b2-webhook-dispatch-retry-and-delivery-log-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicy.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDeliveryPayload.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookDispatcherService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookRetryPolicyTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookDispatcherServiceTest.java \
  docs/product-evolution/specs/p1-005b2-webhook-dispatch-retry-and-delivery-log.md \
  docs/product-evolution/plans/p1-005b2-webhook-dispatch-retry-and-delivery-log-plan.md
git commit -m "feat: add webhook dispatch retry logging"
```

Expected: commit contains only webhook dispatch, retry, delivery log behavior, tests, and docs.
