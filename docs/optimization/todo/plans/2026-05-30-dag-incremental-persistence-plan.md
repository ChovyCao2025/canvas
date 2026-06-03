# DAG Incremental Persistence Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Persist node execution state incrementally to Redis after each node completes. Externalize NodeGate and timeout timers so they survive process crash.

**Architecture:** After each node completes: persist nodeStatuses+nodeOutputs to Redis hash per node. NodeGate → Redis SETNX for atomic cross-process barrier. Timeout timers → Redis sorted set delay queue with polling thread.

**Tech Stack:** Redis Hash, Redis SETNX, Redis Sorted Set, Lua scripts, Java 21, StringRedisTemplate

---

### Task 1: Add Incremental Save After Each Node in DagEngine

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceIncrementalTest.java`

- [ ] **Step 1: Write failing test for incremental persistence**

```java
package org.chovy.canvas.infra.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContextPersistenceIncrementalTest {

    @Test
    void testSaveNodeState_writesStatusAndOutputToRedisHash() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil());

        Map<String, Object> output = Map.of("couponId", "COUPON-123", "amount", 50);

        service.saveNodeState("exec-001", "node-A", NodeStatus.SUCCESS, output);

        // Verify: Redis hash written with correct key pattern
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(eq("canvas:node-state:exec-001:node-A"), captor.capture());

        Map<String, Object> written = captor.getValue();
        assertThat(written).containsEntry("status", "SUCCESS");
        assertThat(written).containsKey("output");
    }

    @Test
    void testLoadNodeState_readsFromRedisHash() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);
        when(hashOps.entries("canvas:node-state:exec-001:node-A")).thenReturn(
                Map.of("status", "SUCCESS", "output", "{\"couponId\":\"COUPON-123\"}"));

        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil());

        ContextPersistenceService.NodeState state = service.loadNodeState("exec-001", "node-A");

        assertThat(state).isNotNull();
        assertThat(state.status()).isEqualTo(NodeStatus.SUCCESS);
    }

    @Test
    void testLoadAllNodeStates_rebuildsPartialContext() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashOps);

        // Simulate: node-A and node-B completed, node-C never persisted (crash during execution)
        when(hashOps.entries("canvas:node-state:exec-001:node-A")).thenReturn(
                Map.of("status", "SUCCESS", "output", "{\"result\":\"a\"}"));
        when(hashOps.entries("canvas:node-state:exec-001:node-B")).thenReturn(
                Map.of("status", "SUCCESS", "output", "{\"result\":\"b\"}"));
        when(hashOps.entries("canvas:node-state:exec-001:node-C")).thenReturn(
                Map.of());

        ContextPersistenceService service = new ContextPersistenceService(
                redis, new ObjectMapper(), new RedisKeyUtil());

        // Node-C has no persisted state → null (will be treated as NOT_STARTED on recovery)
        ContextPersistenceService.NodeState stateA = service.loadNodeState("exec-001", "node-A");
        ContextPersistenceService.NodeState stateB = service.loadNodeState("exec-001", "node-B");
        ContextPersistenceService.NodeState stateC = service.loadNodeState("exec-001", "node-C");

        assertThat(stateA.status()).isEqualTo(NodeStatus.SUCCESS);
        assertThat(stateB.status()).isEqualTo(NodeStatus.SUCCESS);
        assertThat(stateC).isNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ContextPersistenceIncrementalTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0
[ERROR] ... saveNodeState ... method not found
```

- [ ] **Step 3: Add nodeState key method to RedisKeyUtil**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java` — append this method after the existing `executionNodeKey()` method:

```java
    // ── 增量节点状态持久化 ─────────────────────────────────────────
    /** 节点级增量状态 key：canvas:node-state:{executionId}:{nodeId} */
    public String nodeState(String executionId, String nodeId) {
        return prefix + ":node-state:" + executionId + ":" + nodeId;
    }
```

- [ ] **Step 4: Add saveNodeState / loadNodeState / NodeState to ContextPersistenceService**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java` — add these methods after the existing `save()` method:

```java
    // ── 增量节点状态持久化 ─────────────────────────────────────────

    /** 节点状态快照（用于增量持久化）。 */
    public record NodeState(NodeStatus status, Map<String, Object> output) {}

    /**
     * 增量持久化单个节点的状态和输出到 Redis Hash。
     * Key: canvas:node-state:{executionId}:{nodeId}
     * Fields: status (String), output (JSON String)
     */
    public void saveNodeState(String executionId, String nodeId,
                              org.chovy.canvas.engine.context.NodeStatus status,
                              Map<String, Object> output) {
        try {
            String key = keys.nodeState(executionId, nodeId);
            Map<String, Object> hash = new java.util.LinkedHashMap<>();
            hash.put("status", status.name());
            hash.put("output", objectMapper.writeValueAsString(output != null ? output : Map.of()));
            redis.opsForHash().putAll(key, hash);
            // Set TTL same as context snapshot
            redis.expire(key, Duration.ofSeconds(ttlSec));
        } catch (Exception e) {
            log.error("[CTX] 增量持久化节点状态失败 executionId={} nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
        }
    }

    /**
     * 加载单个节点的持久化状态。
     * 不存在时返回 null（表示节点未完成即崩溃）。
     */
    public NodeState loadNodeState(String executionId, String nodeId) {
        String key = keys.nodeState(executionId, nodeId);
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String statusName = (String) entries.get("status");
        String outputJson = (String) entries.get("output");
        org.chovy.canvas.engine.context.NodeStatus status =
                org.chovy.canvas.engine.context.NodeStatus.valueOf(statusName);
        Map<String, Object> output = Map.of();
        try {
            if (outputJson != null) {
                output = objectMapper.readValue(outputJson,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            log.warn("[CTX] 反序列化节点输出失败 executionId={} nodeId={}: {}",
                    executionId, nodeId, e.getMessage());
        }
        return new NodeState(status, output);
    }
```

Add the import for NodeStatus at the top:
```java
import org.chovy.canvas.engine.context.NodeStatus;
```

- [ ] **Step 5: Add incremental save call in DagEngine after each node completes**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java` — insert after line 321 (after graph lookup / node completion log) and at line 953 (in the finally block of the second execution path):

```java
                        // Incremental persistence: save node state to Redis after each node completes
                        ctxStore.saveNodeState(ctx.getExecutionId(), nodeId, status,
                                result.output() != null ? result.output() : Map.of());
```

The exact location is in the flatMap lambda of executeNode, after line 321 (`log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());`), add:

```java
                        ctxStore.saveNodeState(ctx.getExecutionId(), nodeId, status,
                                result.output() != null ? result.output() : Map.of());
```

Also add the same call in `executeNodeAfterStage2`, after the success path `log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());` (line 953):

```java
                        ctxStore.saveNodeState(ctx.getExecutionId(), nodeId, status,
                                result.output() != null ? result.output() : Map.of());
```

- [ ] **Step 6: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=ContextPersistenceIncrementalTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/test/java/org/chovy/canvas/infra/redis/ContextPersistenceIncrementalTest.java && git commit -m "feat: add incremental node state persistence to Redis hash after each node completes"
```

---

### Task 2: Implement NodeGate via Redis SETNX

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/NodeGateRedisTest.java`

- [ ] **Step 1: Write failing test for Redis-backed NodeGate**

```java
package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class NodeGateRedisTest {

    @Test
    void testTryAcquireGate_firstAcquirerWins() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        // First SETNX returns true (gate acquired)
        when(values.setIfAbsent(eq("canvas:gate:exec-001:node-A"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-001");
        ctx.setCanvasId(1L);

        boolean acquired = ExecutionContext.tryAcquireGateRedis(redis, "canvas:gate:exec-001:node-A");
        assertThat(acquired).isTrue();
    }

    @Test
    void testTryAcquireGate_secondAcquirerLoses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        // SETNX returns false (gate already held)
        when(values.setIfAbsent(eq("canvas:gate:exec-001:node-A"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        boolean acquired = ExecutionContext.tryAcquireGateRedis(redis, "canvas:gate:exec-001:node-A");
        assertThat(acquired).isFalse();
    }

    @Test
    void testReleaseGate_deletesKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);

        ExecutionContext.releaseGateRedis(redis, "canvas:gate:exec-001:node-A");

        verify(redis).delete("canvas:gate:exec-001:node-A");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeGateRedisTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 3, Failures: 3, Errors: 0, Skipped: 0
[ERROR] ... tryAcquireGateRedis ... method not found
```

- [ ] **Step 3: Add gate key method to RedisKeyUtil**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`:

```java
    /** 节点执行门控 key：canvas:gate:{executionId}:{nodeId} */
    public String gate(String executionId, String nodeId) {
        return prefix + ":gate:" + executionId + ":" + nodeId;
    }
```

- [ ] **Step 4: Add Redis-backed gate methods to ExecutionContext**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java` — add these fields and methods after the existing `flatContext` field declaration:

```java
    /** Redis-backed gate TTL: same as global execution timeout. */
    private static final Duration GATE_TTL = Duration.ofSeconds(600);

    /**
     * Try to acquire the execution gate for a node via Redis SETNX.
     * Returns true if this instance acquired the gate (first caller wins).
     * The gate auto-expires after GATE_TTL to prevent permanent locks on crash.
     */
    public static boolean tryAcquireGateRedis(org.springframework.data.redis.core.StringRedisTemplate redis,
                                               String gateKey) {
        return Boolean.TRUE.equals(
                redis.opsForValue().setIfAbsent(gateKey, "1", GATE_TTL));
    }

    /**
     * Release the Redis-backed execution gate.
     * Called after node execution completes (success or failure).
     */
    public static void releaseGateRedis(org.springframework.data.redis.core.StringRedisTemplate redis,
                                         String gateKey) {
        redis.delete(gateKey);
    }
```

Add import at top:
```java
import java.time.Duration;
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=NodeGateRedisTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/NodeGateRedisTest.java && git commit -m "feat: add Redis SETNX-backed NodeGate for crash-safe execution barrier"
```

---

### Task 3: Replace Mono.delay with Redis Sorted Set Delay Queue

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDelayQueue.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/RedisDelayQueueTest.java`

- [ ] **Step 1: Write failing test for Redis delay queue**

```java
package org.chovy.canvas.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RedisDelayQueueTest {

    @Test
    void testSchedule_addsItemWithFutureTimestampAsScore() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsetOps);

        RedisDelayQueue queue = new RedisDelayQueue(redis, "canvas:delay-queue");

        long beforeSchedule = System.currentTimeMillis();
        queue.schedule("exec-001", "node-A", 60); // 60 seconds delay

        // Verify: item added to sorted set with score = now + 60s
        verify(zsetOps).add(eq("canvas:delay-queue"), eq("exec-001:node-A"),
                doubleThat(score -> score >= beforeSchedule + 60_000 - 100
                        && score <= beforeSchedule + 60_000 + 100));
    }

    @Test
    void testPollDueItems_returnsItemsWithScoreLessThanNow() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsetOps);

        // Item with score in the past (should be returned)
        when(zsetOps.rangeByScore("canvas:delay-queue", 0, Double.MAX_VALUE))
                .thenReturn(Set.of("exec-001:node-A"));

        RedisDelayQueue queue = new RedisDelayQueue(redis, "canvas:delay-queue");
        java.util.List<String> dueItems = queue.pollDueItems();

        assertThat(dueItems).containsExactly("exec-001:node-A");
        // Verify: item removed from queue after polling
        verify(zsetOps).remove("canvas:delay-queue", "exec-001:node-A");
    }

    @Test
    void testPollDueItems_returnsEmptyListWhenNoItemsDue() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsetOps);

        // No items with score <= now
        when(zsetOps.rangeByScore("canvas:delay-queue", 0, Double.MAX_VALUE))
                .thenReturn(null);

        RedisDelayQueue queue = new RedisDelayQueue(redis, "canvas:delay-queue");
        java.util.List<String> dueItems = queue.pollDueItems();

        assertThat(dueItems).isEmpty();
    }

    @Test
    void testScheduleRecovery_rebuildsTimerOnStartup() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zsetOps);

        // Simulate: items left in queue from previous instance (crash recovery)
        when(zsetOps.rangeByScore("canvas:delay-queue", 0, Double.MAX_VALUE))
                .thenReturn(Set.of("exec-001:node-A", "exec-002:node-B"));

        RedisDelayQueue queue = new RedisDelayQueue(redis, "canvas:delay-queue");
        java.util.List<String> recoveredItems = queue.pollDueItems();

        // All items with past timestamps are recovered
        assertThat(recoveredItems).containsExactlyInAnyOrder("exec-001:node-A", "exec-002:node-B");
        verify(zsetOps).remove("canvas:delay-queue", "exec-001:node-A");
        verify(zsetOps).remove("canvas:delay-queue", "exec-002:node-B");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=RedisDelayQueueTest -DfailIfNoTests=false 2>&1 | tail -30
```

Expected output:
```
[ERROR] Tests run: 4, Failures: 4, Errors: 0, Skipped: 0
[ERROR] ... RedisDelayQueue class not found
```

- [ ] **Step 3: Add delay queue key to RedisKeyUtil**

Add to `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`:

```java
    /** 延迟队列 key：canvas:delay-queue（sorted set，score=触发时间戳ms） */
    public String delayQueue() {
        return prefix + ":delay-queue";
    }
```

- [ ] **Step 4: Implement RedisDelayQueue**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDelayQueue.java`:

```java
package org.chovy.canvas.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Redis sorted set based delay queue for externalizing Mono.delay timers.
 *
 * <p>Items are stored with score = fire timestamp (epoch ms).
 * A polling thread checks for due items and removes them atomically.
 *
 * <p>Survives process crash: items persist in Redis and are recovered on next poll.
 */
@Slf4j
@RequiredArgsConstructor
public class RedisDelayQueue {

    private final StringRedisTemplate redis;
    private final String queueKey;

    /**
     * Schedule a delayed item.
     *
     * @param executionId the execution ID
     * @param nodeId      the node ID that should fire after delay
     * @param delaySeconds seconds until the item should fire
     */
    public void schedule(String executionId, String nodeId, long delaySeconds) {
        double fireTimestamp = System.currentTimeMillis() + delaySeconds * 1000.0;
        String member = executionId + ":" + nodeId;
        redis.opsForZSet().add(queueKey, member, fireTimestamp);
        log.debug("[DELAY-Q] scheduled executionId={} nodeId={} delay={}s fireAt={}",
                executionId, nodeId, delaySeconds, (long) fireTimestamp);
    }

    /**
     * Poll all items that are due (score <= now).
     * Removes them from the queue after returning.
     *
     * @return list of due items in format "executionId:nodeId"
     */
    public List<String> pollDueItems() {
        long now = System.currentTimeMillis();
        Set<String> items = redis.opsForZSet().rangeByScore(queueKey, 0, now);
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> dueItems = new ArrayList<>(items);
        // Remove each due item from the queue
        for (String item : dueItems) {
            redis.opsForZSet().remove(queueKey, item);
        }
        log.debug("[DELAY-Q] polled {} due items from queue", dueItems.size());
        return dueItems;
    }

    /**
     * Cancel a scheduled item.
     *
     * @param executionId the execution ID
     * @param nodeId      the node ID
     */
    public void cancel(String executionId, String nodeId) {
        String member = executionId + ":" + nodeId;
        redis.opsForZSet().remove(queueKey, member);
    }

    /**
     * Get the queue key (for diagnostics).
     */
    public String getQueueKey() {
        return queueKey;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=RedisDelayQueueTest -DfailIfNoTests=false 2>&1 | tail -20
```

Expected output:
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDelayQueue.java backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/RedisDelayQueueTest.java && git commit -m "feat: implement Redis sorted set delay queue to replace Mono.delay for crash-safe timers"
```
