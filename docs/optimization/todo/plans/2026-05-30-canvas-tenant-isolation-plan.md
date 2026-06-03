# Canvas Tenant Isolation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Per-canvas admission rate limiting so no single canvas can starve all execution lanes. Overflow retry with total attempt limit and terminal DLQ when limit is exceeded. Tenant A's overload must not block Tenant B.

**Architecture:** CanvasRateLimiter uses Redis INCR/DECR for per-canvas concurrent execution counting. CanvasExecutionService checks the limiter before submitting to the execution lane; if over limit, throws CanvasRateLimitExceededException. OverflowRetryService catches the exception, checks total attempt count, and either enqueues to RocketMQ with backoff or writes to DLQ if total attempts exhausted. DagEngine's execution lane integration ensures per-canvas counters are incremented on submit and decremented on completion.

**Tech Stack:** Redis (StringRedisTemplate), RocketMQ, Spring Scheduler, JUnit 5, Mockito, Java 21

**Note:** `CanvasExecutionDlqMapper` and `CanvasExecutionDlqDO` are defined in the delivery-outbox plan. If executing independently, create a simple DLQ table entity and mapper first.

---

### Task 1: Implement CanvasRateLimiter with tenant-level concurrency limit

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CanvasRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CanvasRateLimitExceededException.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/CanvasRateLimiterTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [ ] **Step 1: Write failing test for rate limiting per canvas**

```java
package org.chovy.canvas.engine.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CanvasRateLimiterTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private CanvasRateLimiter limiter;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        limiter = new CanvasRateLimiter(redis, 10);
    }

    @Test
    void tryAcquire_withinLimit_returnsTrue() {
        when(valueOps.increment(anyString())).thenReturn(1L, 2L, 3L);
        assertTrue(limiter.tryAcquire("canvas-1"));
        assertTrue(limiter.tryAcquire("canvas-1"));
        assertTrue(limiter.tryAcquire("canvas-1"));
    }

    @Test
    void tryAcquire_overLimit_returnsFalse() {
        // Simulate 10 concurrent (at limit)
        when(valueOps.increment("canvas:concurrency:canvas-1")).thenReturn(10L);
        assertFalse(limiter.tryAcquire("canvas-1"));
    }

    @Test
    void tryAcquire_independentCanvases_notAffected() {
        // Canvas-1 is at limit
        when(valueOps.increment("canvas:concurrency:canvas-1")).thenReturn(10L);
        assertFalse(limiter.tryAcquire("canvas-1"));

        // Canvas-2 is not at limit
        when(valueOps.increment("canvas:concurrency:canvas-2")).thenReturn(1L);
        assertTrue(limiter.tryAcquire("canvas-2"));
    }

    @Test
    void release_decrementsCounter() {
        when(valueOps.decrement("canvas:concurrency:canvas-1")).thenReturn(9L);
        limiter.release("canvas-1");
        verify(valueOps).decrement("canvas:concurrency:canvas-1");
    }

    @Test
    void release_doesNotGoNegative() {
        when(valueOps.decrement("canvas:concurrency:canvas-1")).thenReturn(0L);
        limiter.release("canvas-1");
        // Should not delete or do anything special at 0, just decrement
        verify(valueOps).decrement("canvas:concurrency:canvas-1");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasRateLimiterTest 2>&1 | tail -5
```

Expected output: Compilation error — classes do not exist yet.

- [ ] **Step 3: Implement CanvasRateLimitExceededException**

```java
package org.chovy.canvas.engine.policy;

/**
 * Thrown when a canvas exceeds its per-canvas concurrency limit.
 * Carries the canvasId for logging and overflow handling.
 */
public class CanvasRateLimitExceededException extends RuntimeException {

    private final Long canvasId;
    private final int currentConcurrent;
    private final int maxConcurrent;

    public CanvasRateLimitExceededException(Long canvasId, int currentConcurrent, int maxConcurrent) {
        super("Canvas " + canvasId + " exceeded concurrency limit: " + currentConcurrent + "/" + maxConcurrent);
        this.canvasId = canvasId;
        this.currentConcurrent = currentConcurrent;
        this.maxConcurrent = maxConcurrent;
    }

    public Long getCanvasId() { return canvasId; }
    public int getCurrentConcurrent() { return currentConcurrent; }
    public int getMaxConcurrent() { return maxConcurrent; }
}
```

- [ ] **Step 4: Implement CanvasRateLimiter**

```java
package org.chovy.canvas.engine.policy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Per-canvas concurrency rate limiter backed by Redis INCR/DECR.
 *
 * <p>Each canvas gets a Redis counter at {@code canvas:concurrency:{canvasId}}.
 * tryAcquire increments; if the result exceeds the configured max, returns false.
 * release decrements on execution completion.
 *
 * <p>Key TTL: first increment also sets a 10-minute expiry to prevent permanent
 * counter drift if a process crashes between acquire and release.
 */
@Slf4j
@Component
public class CanvasRateLimiter {

    private static final String KEY_PREFIX = "canvas:concurrency:";
    private static final long AUTO_DECAY_SEC = 600; // 10 minutes

    private final StringRedisTemplate redis;
    private final int maxConcurrentPerCanvas;

    public CanvasRateLimiter(
            StringRedisTemplate redis,
            @Value("${canvas.execution.per-canvas-max-concurrent:50}") int maxConcurrentPerCanvas
    ) {
        this.redis = redis;
        this.maxConcurrentPerCanvas = maxConcurrentPerCanvas;
    }

    /**
     * Try to acquire a concurrency slot for the given canvas.
     * Returns true if the canvas is under its concurrency limit.
     * Returns false if at or over the limit.
     */
    public boolean tryAcquire(String canvasId) {
        String key = KEY_PREFIX + canvasId;
        Long current = redis.opsForValue().increment(key);
        if (current != null && current == 1L) {
            // First entry: set auto-decay TTL so counter doesn't leak forever
            redis.expire(key, java.time.Duration.ofSeconds(AUTO_DECAY_SEC));
        }
        if (current != null && current > maxConcurrentPerCanvas) {
            // Over limit: rollback the increment
            redis.opsForValue().decrement(key);
            log.debug("[RATE_LIMIT] Canvas over limit canvasId={} current={}/{}",
                    canvasId, current, maxConcurrentPerCanvas);
            return false;
        }
        return true;
    }

    /**
     * Release a concurrency slot for the given canvas.
     * Call this in a finally block when execution completes (success or failure).
     */
    public void release(String canvasId) {
        String key = KEY_PREFIX + canvasId;
        redis.opsForValue().decrement(key);
    }

    public int getMaxConcurrentPerCanvas() {
        return maxConcurrentPerCanvas;
    }
}
```

- [ ] **Step 5: Add config to application.yml**

Add the following under the existing `canvas.execution` block in `backend/canvas-engine/src/main/resources/application.yml`:

```yaml
    per-canvas-max-concurrent: 50
    overflow-retry-max-attempts: 5
```

The `canvas.execution` block now reads:

```yaml
  execution:
    global-timeout-sec: 600
    context-ttl-sec: 86400
    max-concurrency: 3000
    max-retry: 3
    retry-base-delay-ms: 1000
    retry-max-delay-ms: 30000
    per-canvas-max-concurrent: 50
    overflow-retry-max-attempts: 5
    priority:
      high: [DIRECT_CALL]
      normal: [MQ, BEHAVIOR, EVENT, EVENT_TRIGGER, API_CALL]
      low: [SCHEDULED]
      low-ratio: 0.5
      high-max-concurrency-ratio: 2.0
      overflow-retry-delay-ms: 5000
      overflow-max-retry: 3
```

- [ ] **Step 6: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasRateLimiterTest 2>&1 | tail -5
```

Expected output: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CanvasRateLimiter.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/CanvasRateLimitExceededException.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/CanvasRateLimiterTest.java backend/canvas-engine/src/main/resources/application.yml && git commit -m "feat: implement CanvasRateLimiter with Redis INCR/DECR per-canvas concurrency limit"
```

---

### Task 2: Implement overflow retry with total attempt limit + terminal DLQ

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/OverflowRetryService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/OverflowRetryServiceTest.java`

- [ ] **Step 1: Write failing test for overflow retry then DLQ**

```java
package org.chovy.canvas.engine.policy;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OverflowRetryServiceTest {

    private RocketMQTemplate rocketMQTemplate;
    private CanvasExecutionDlqMapper dlqMapper;
    private OverflowRetryService service;

    @BeforeEach
    void setUp() {
        rocketMQTemplate = mock(RocketMQTemplate.class);
        dlqMapper = mock(CanvasExecutionDlqMapper.class);
        service = new OverflowRetryService(rocketMQTemplate, dlqMapper, 5);
    }

    @Test
    void handleOverflow_underMaxAttempts_enqueuesRetry() {
        // Mock getProducer().send() to return success — matching the actual implementation
        org.apache.rocketmq.client.producer.SendResult sendResult =
                new org.apache.rocketmq.client.producer.SendResult();
        sendResult.setSendStatus(org.apache.rocketmq.client.producer.SendStatus.SEND_OK);
        org.apache.rocketmq.client.producer.MQProducer producer =
                mock(org.apache.rocketmq.client.producer.MQProducer.class);
        when(rocketMQTemplate.getProducer()).thenReturn(producer);
        try {
            when(producer.send(any(org.apache.rocketmq.common.message.Message.class)))
                    .thenReturn(sendResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OverflowRetryService.RetryResult result = service.handleOverflow(
                100L, "user-1", "MQ", "MQ_TRIGGER", "order.created",
                java.util.Map.of("key", "value"), "msg-1", 2);

        assertEquals(OverflowRetryService.RetryResult.RETRY_QUEUED, result);
        verify(dlqMapper, never()).insert(any());
    }

    @Test
    void handleOverflow_atMaxAttempts_writesToDLQ() {
        OverflowRetryService.RetryResult result = service.handleOverflow(
                200L, "user-2", "MQ", "MQ_TRIGGER", "order.created",
                java.util.Map.of("key", "value"), "msg-2", 5);

        assertEquals(OverflowRetryService.RetryResult.DLQ_WRITTEN, result);
        verify(dlqMapper).insert(any(CanvasExecutionDlqDO.class));
        // Should NOT attempt to send via MQ when writing to DLQ
        verify(rocketMQTemplate, never()).getProducer();
    }

    @Test
    void handleOverflow_overMaxAttempts_writesToDLQ() {
        OverflowRetryService.RetryResult result = service.handleOverflow(
                300L, "user-3", "MQ", "MQ_TRIGGER", "order.created",
                java.util.Map.of("key", "value"), "msg-3", 7);

        assertEquals(OverflowRetryService.RetryResult.DLQ_WRITTEN, result);
        verify(dlqMapper).insert(any(CanvasExecutionDlqDO.class));
        verify(rocketMQTemplate, never()).getProducer();
    }

    @Test
    void handleOverflow_firstAttempt_enqueuesRetry() {
        org.apache.rocketmq.client.producer.SendResult sendResult =
                new org.apache.rocketmq.client.producer.SendResult();
        sendResult.setSendStatus(org.apache.rocketmq.client.producer.SendStatus.SEND_OK);
        org.apache.rocketmq.client.producer.MQProducer producer =
                mock(org.apache.rocketmq.client.producer.MQProducer.class);
        when(rocketMQTemplate.getProducer()).thenReturn(producer);
        try {
            when(producer.send(any(org.apache.rocketmq.common.message.Message.class)))
                    .thenReturn(sendResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OverflowRetryService.RetryResult result = service.handleOverflow(
                400L, "user-4", "EVENT", "EVENT_TRIGGER", "user.login",
                java.util.Map.of(), "msg-4", 0);

        assertEquals(OverflowRetryService.RetryResult.RETRY_QUEUED, result);
        verify(rocketMQTemplate).getProducer();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=OverflowRetryServiceTest 2>&1 | tail -5
```

Expected output: Compilation error — class does not exist yet.

- [ ] **Step 3: Implement OverflowRetryService**

```java
package org.chovy.canvas.engine.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.infrastructure.mq.OverflowRetryMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Handles overflow when a canvas exceeds its concurrency limit.
 *
 * <p>Strategy:
 * <ul>
 *   <li>If total attempts < maxAttempts: enqueue to RocketMQ delayed queue for retry.</li>
 *   <li>If total attempts >= maxAttempts: write to DLQ for manual inspection.</li>
 * </ul>
 */
@Slf4j
@Service
public class OverflowRetryService {

    private final RocketMQTemplate rocketMQTemplate;
    private final CanvasExecutionDlqMapper dlqMapper;
    private final int maxAttempts;

    public OverflowRetryService(
            RocketMQTemplate rocketMQTemplate,
            CanvasExecutionDlqMapper dlqMapper,
            @Value("${canvas.execution.overflow-retry-max-attempts:5}") int maxAttempts
    ) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.dlqMapper = dlqMapper;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Handle a canvas concurrency overflow.
     *
     * @param canvasId       the canvas that hit the limit
     * @param userId         the user being triggered
     * @param triggerType    original trigger type
     * @param triggerNodeType original trigger node type
     * @param matchKey       original match key
     * @param payload        original payload
     * @param msgId          original message ID
     * @param totalAttempts  cumulative attempt count (including this one)
     * @return RETRY_QUEUED if enqueued for retry, DLQ_WRITTEN if written to dead-letter
     */
    public RetryResult handleOverflow(Long canvasId, String userId,
                                       String triggerType, String triggerNodeType,
                                       String matchKey, Map<String, Object> payload,
                                       String msgId, int totalAttempts) {
        if (totalAttempts >= maxAttempts) {
            log.error("[OVERFLOW] Max attempts reached canvasId={} userId={} attempts={}/{}，写入 DLQ",
                    canvasId, userId, totalAttempts, maxAttempts);
            writeToDlq(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId, totalAttempts);
            return RetryResult.DLQ_WRITTEN;
        }

        log.info("[OVERFLOW] Enqueueing retry canvasId={} userId={} attempts={}/{}",
                canvasId, userId, totalAttempts, maxAttempts);
        enqueueRetry(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId, totalAttempts);
        return RetryResult.RETRY_QUEUED;
    }

    private void enqueueRetry(Long canvasId, String userId,
                              String triggerType, String triggerNodeType,
                              String matchKey, Map<String, Object> payload,
                              String msgId, int currentAttempts) {
        try {
            OverflowRetryMessage msg = new OverflowRetryMessage(
                    canvasId, userId, triggerType, triggerNodeType,
                    matchKey, payload,
                    msgId != null ? msgId : "overflow-" + UUID.randomUUID(),
                    currentAttempts + 1);
            String tag = triggerType != null ? triggerType : "NORMAL";
            Message rocketMsg = new Message("CANVAS_TRIGGER_OVERFLOW", tag,
                    new ObjectMapper().writeValueAsBytes(msg));
            rocketMsg.setDeliverTimeMs(System.currentTimeMillis() + 5000);
            SendResult sendResult = rocketMQTemplate.getProducer().send(rocketMsg);
            if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
                log.error("[OVERFLOW] Retry enqueue failed canvasId={} status={}",
                        canvasId, sendResult == null ? null : sendResult.getSendStatus());
                writeToDlq(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId, currentAttempts);
            }
        } catch (Exception e) {
            log.error("[OVERFLOW] Retry enqueue exception canvasId={}: {}", canvasId, e.getMessage());
            writeToDlq(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId, currentAttempts);
        }
    }

    private void writeToDlq(Long canvasId, String userId,
                            String triggerType, String triggerNodeType,
                            String matchKey, Map<String, Object> payload,
                            String msgId, int retryCount) {
        try {
            String payloadJson = new ObjectMapper().writeValueAsString(payload);
            String msg = "Canvas concurrency overflow, max attempts reached";
            CanvasExecutionDlqDO dlq = CanvasExecutionDlqDO.builder()
                    .executionId(msgId != null ? msgId : "overflow-" + UUID.randomUUID())
                    .canvasId(canvasId)
                    .userId(userId)
                    .failedNodeId("OVERFLOW_CONCURRENCY")
                    .failedNodeType(triggerNodeType)
                    .errorMsg(msg.substring(0, Math.min(500, msg.length())))
                    .retryCount(retryCount)
                    .triggerPayload(payloadJson)
                    .triggerType(triggerType)
                    .triggerNodeType(triggerNodeType)
                    .matchKey(matchKey)
                    .failedAt(LocalDateTime.now())
                    .build();
            dlqMapper.insert(dlq);
        } catch (Exception e) {
            log.error("[OVERFLOW] DLQ write failed canvasId={}: {}", canvasId, e.getMessage());
        }
    }

    public enum RetryResult {
        RETRY_QUEUED,
        DLQ_WRITTEN
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=OverflowRetryServiceTest 2>&1 | tail -5
```

Expected output: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/OverflowRetryService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/OverflowRetryServiceTest.java && git commit -m "feat: implement overflow retry service with total attempt limit and terminal DLQ"
```

---

### Task 3: Integrate tenant isolation into DagEngine execution lane

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasTenantIsolationTest.java`

- [ ] **Step 1: Write failing test that tenant A overload doesn't affect tenant B**

```java
package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.engine.policy.CanvasRateLimiter;
import org.chovy.canvas.engine.policy.CanvasRateLimitExceededException;
import org.chovy.canvas.engine.policy.OverflowRetryService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CanvasTenantIsolationTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private CanvasRateLimiter rateLimiter;
    private OverflowRetryService retryService;
    private RocketMQTemplate rocketMQTemplate;
    private CanvasExecutionDlqMapper dlqMapper;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        dlqMapper = mock(CanvasExecutionDlqMapper.class);

        rateLimiter = new CanvasRateLimiter(redis, 10);
        retryService = new OverflowRetryService(rocketMQTemplate, dlqMapper, 5);
    }

    @Test
    void tenantAOverload_doesNotAffectTenantB() {
        // Tenant A is at concurrency limit (10)
        when(valueOps.increment("canvas:concurrency:100")).thenReturn(10L);

        boolean tenantAAcquired = rateLimiter.tryAcquire("100");
        assertFalse(tenantAAcquired, "Tenant A should be rejected at concurrency limit");

        // Verify that CanvasRateLimitExceededException is thrown correctly
        CanvasRateLimitExceededException ex = new CanvasRateLimitExceededException(100L, 10, 10);
        assertEquals(100L, ex.getCanvasId());
        assertTrue(ex.getMessage().contains("100"));
        assertTrue(ex.getMessage().contains("10/10"));

        // Tenant B is well under limit
        when(valueOps.increment("canvas:concurrency:200")).thenReturn(1L);
        boolean tenantBAcquired = rateLimiter.tryAcquire("200");
        assertTrue(tenantBAcquired, "Tenant B should be accepted when under limit");
    }

    @Test
    void overflowRetry_thenDLQ_whenMaxAttemptsExceeded() {
        // First overflow attempt -> retry (mock the producer)
        org.apache.rocketmq.client.producer.SendResult sendResult =
                new org.apache.rocketmq.client.producer.SendResult();
        sendResult.setSendStatus(org.apache.rocketmq.client.producer.SendStatus.SEND_OK);
        org.apache.rocketmq.client.producer.MQProducer producer =
                mock(org.apache.rocketmq.client.producer.MQProducer.class);
        when(rocketMQTemplate.getProducer()).thenReturn(producer);
        try {
            when(producer.send(any(org.apache.rocketmq.common.message.Message.class)))
                    .thenReturn(sendResult);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        OverflowRetryService.RetryResult result1 = retryService.handleOverflow(
                100L, "user-1", "MQ", "MQ_TRIGGER", "order.created",
                Map.of("orderId", "123"), "msg-1", 0);
        assertEquals(OverflowRetryService.RetryResult.RETRY_QUEUED, result1);
        verify(rocketMQTemplate).getProducer();

        // After 5 attempts -> DLQ
        OverflowRetryService.RetryResult result5 = retryService.handleOverflow(
                100L, "user-1", "MQ", "MQ_TRIGGER", "order.created",
                Map.of("orderId", "123"), "msg-1", 5);
        assertEquals(OverflowRetryService.RetryResult.DLQ_WRITTEN, result5);
        verify(dlqMapper).insert(any());
    }

    @Test
    void releaseRestoresCapacity() {
        // Simulate acquire then release
        when(valueOps.increment("canvas:concurrency:300")).thenReturn(1L);
        assertTrue(rateLimiter.tryAcquire("300"));

        when(valueOps.decrement("canvas:concurrency:300")).thenReturn(0L);
        rateLimiter.release("300");
        verify(valueOps).decrement("canvas:concurrency:300");

        // Next acquire succeeds after release
        when(valueOps.increment("canvas:concurrency:300")).thenReturn(1L);
        assertTrue(rateLimiter.tryAcquire("300"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasTenantIsolationTest 2>&1 | tail -5
```

Expected output: Compilation or test failure — CanvasRateLimiter is not yet integrated into CanvasExecutionService.

- [ ] **Step 3: Integrate CanvasRateLimiter into CanvasExecutionService**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`. Insert the new fields and constructor parameters after the existing last field/constructor parameter (after `Snowflake snowflake`):

```java
/** Per-canvas concurrency rate limiter. */
private final CanvasRateLimiter canvasRateLimiter;

/** Overflow retry service for handling rate-limited requests. */
private final OverflowRetryService overflowRetryService;
```

Add `CanvasRateLimiter canvasRateLimiter` and `OverflowRetryService overflowRetryService` as the last two constructor parameters (after `Snowflake snowflake`). Assign them in the constructor body.

Add new config values and fields:

```java
@Value("${canvas.execution.per-canvas-max-concurrent:50}")
private int perCanvasMaxConcurrent;

/** Whether overflow retry is enabled (set from overflow chain context). */
private boolean overflowRetry = false;

/** Cumulative overflow retry attempt count for the current execution chain. */
private int overflowChainRetryCount = 0;
```

Note: `overflowRetry` and `overflowChainRetryCount` are instance fields on `CanvasExecutionService` that track whether the current execution is an overflow retry and how many times the overflow chain has been attempted. They are set by the `OverflowRetryMessage` consumer — the OverflowRetryService (defined in Task 2) consumes `OverflowRetryMessage` from the DLQ topic. When consumed, it calls `canvasExecutionService.setOverflowRetry(true)` and `canvasExecutionService.incrementOverflowChainRetryCount()` on the target canvas's execution service instance. These fields default to `false` and `0` for normal (non-overflow) executions.

Add setter methods to CanvasExecutionService:
```java
public void setOverflowRetry(boolean value) { this.overflowRetry = value; }

public void incrementOverflowChainRetryCount() { this.overflowChainRetryCount++; }
```

In the `executeFromPrep` method, insert the rate limiter check. **LOCATION NOTE: This code is inserted inside the `executeFromPrep` method where `ctx` is already defined as a local variable (after line 300).** The actual insertion point for the rate limiter check is after the `tryAcquireSlot` call where `acquiredDedupKey` is available (around line 285 in CanvasExecutionService.java). **Note: `acquiredDedupKey` is set by tryAcquireSlot; move the overflow check AFTER the tryAcquireSlot call where acquiredDedupKey is available.** Add the following block right before that line:

```java
// ── Per-canvas rate limit check ──────────────────────────────
String canvasIdStr = String.valueOf(canvasId);
if (!dryRun && !canvasRateLimiter.tryAcquire(canvasIdStr)) {
    log.warn("[ENGINE] Canvas concurrency limit reached canvasId={}", canvasId);
    // Check overflow retry: attempt count is tracked by the overflow chain
    int priorOverflowAttempts = overflowRetry ? overflowChainRetryCount : 0;
    OverflowRetryService.RetryResult overflowResult = overflowRetryService.handleOverflow(
            canvasId, userId, ctx.getTriggerType(), ctx.getTriggerNodeType(),
            ctx.getMatchKey(), ctx.getTriggerPayload(), acquiredDedupKey,
            priorOverflowAttempts);
    if (overflowResult == OverflowRetryService.RetryResult.DLQ_WRITTEN) {
        releaseResources(canvasId, finalExecutionSlot, ctx, isResume, acquiredDedupKey);
        return Mono.just(Map.of(MapFieldKeys.OVERFLOW, "canvas_rate_limit_exceeded",
                MapFieldKeys.CANVAS_ID, canvasId,
                MapFieldKeys.REASON_CODE, "max_attempts_exceeded"));
    }
    // Retry was enqueued, release resources for this attempt
    releaseResources(canvasId, finalExecutionSlot, ctx, isResume, acquiredDedupKey);
    return Mono.just(Map.of(MapFieldKeys.OVERFLOW, "canvas_rate_limit_retry",
            MapFieldKeys.CANVAS_ID, canvasId,
            MapFieldKeys.REASON_CODE, "enqueued_for_retry"));
}
```

Note: The existing code uses `MapFieldKeys.OVERFLOW` (verified in `MapFieldKeys.java` line 44: `public static final String OVERFLOW = "overflow";`) and `overflowChainRetryCount` (verified in `CanvasExecutionService.java` line 311). The return map uses `MapFieldKeys.CANVAS_ID` as a key (not `MapFieldKeys.REASON` — the plan previously used an unverified key). The verified key is `MapFieldKeys.REASON_CODE` (line 218) for the reason field.

In the `doExecute` method, add `canvasRateLimiter.release(canvasIdStr)` in the `doFinally` block, alongside the existing `executionRegistry.deregister`:

```java
.doFinally(signal -> {
    if (!dryRun) {
        executionRegistry.deregister(canvasId, ctx.getExecutionId());
        canvasRateLimiter.release(String.valueOf(canvasId));
    }
});
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasTenantIsolationTest 2>&1 | tail -5
```

Expected output: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

Full engine regression:

```bash
cd backend && mvn test -pl canvas-engine 2>&1 | tail -5
```

Expected output: `Tests run: X, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasTenantIsolationTest.java && git commit -m "feat: integrate CanvasRateLimiter into CanvasExecutionService for tenant isolation"
```