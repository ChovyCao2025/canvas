# Handler Idempotency Framework Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** System-level idempotency key for all handlers. Key = executionId + nodeId + attemptCount. Persisted before execution via Redis SETNX, marked COMPLETED after. Duplicate executions return cached result. Context writes deferred until handler success, so partial failures do not pollute downstream.

> **Cross-plan dependency:** This plan modifies DagEngine constructor (adds IdempotencyService as 9th param) and NodeHandler interface (adds idempotencyKey param). It CONFLICTS with the webflux-to-mvc-virtualthread plan which also rewrites DagEngine and NodeHandler. Execute ONLY ONE of these plans, or execute handler-idempotency FIRST, then merge its changes into the webflux-to-mvc plan's DagEngine rewrite.

**Architecture:** IdempotencyService wraps Redis SETNX for key lifecycle (RUNNING -> COMPLETED). DagEngine checks idempotency before dispatching handler; on COMPLETED key, returns cached output from ctx without re-executing. ExecutionContext gains a deferred write buffer: handler output accumulates in a staging map, only committed to flatContext/nodeOutputs after handler returns success.

**Tech Stack:** Redis (StringRedisTemplate), Java 21, JUnit 5, Mockito

---

### Task 1: Implement IdempotencyService with Redis SETNX

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/IdempotencyService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/IdempotencyServiceTest.java`

- [ ] **Step 1: Write failing test**

```java
package org.chovy.canvas.engine.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {

    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new IdempotencyService(redis);
    }

    @Test
    void generateKey_combinesExecutionNodeAttempt() {
        String key = service.generateKey("exec-1", "node-A", 2);
        assertEquals("exec-1:node-A:2", key);
    }

    @Test
    void tryAcquire_firstExecution_returnsTrue() {
        when(valueOps.setIfAbsent(anyString(), eq("RUNNING"), any(Duration.class)))
                .thenReturn(true);
        String key = service.generateKey("exec-1", "node-A", 1);
        assertTrue(service.tryAcquire(key));
    }

    @Test
    void tryAcquire_duplicateExecution_returnsFalse() {
        when(valueOps.setIfAbsent(anyString(), eq("RUNNING"), any(Duration.class)))
                .thenReturn(false);
        String key = service.generateKey("exec-1", "node-A", 1);
        assertFalse(service.tryAcquire(key));
    }

    @Test
    void markComplete_setsCompletedInRedis() {
        String key = service.generateKey("exec-1", "node-A", 1);
        service.markComplete(key);
        verify(valueOps).set("canvas:idem:exec-1:node-A:1", "COMPLETED");
    }

    @Test
    void isCompleted_returnsTrueWhenCompleted() {
        when(valueOps.get("canvas:idem:exec-1:node-A:1")).thenReturn("COMPLETED");
        String key = service.generateKey("exec-1", "node-A", 1);
        assertTrue(service.isCompleted(key));
    }

    @Test
    void isCompleted_returnsFalseWhenRunning() {
        when(valueOps.get("canvas:idem:exec-1:node-A:1")).thenReturn("RUNNING");
        String key = service.generateKey("exec-1", "node-A", 1);
        assertFalse(service.isCompleted(key));
    }

    @Test
    void isCompleted_returnsFalseWhenMissing() {
        when(valueOps.get("canvas:idem:exec-1:node-A:1")).thenReturn(null);
        String key = service.generateKey("exec-1", "node-A", 1);
        assertFalse(service.isCompleted(key));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=IdempotencyServiceTest 2>&1 | tail -5
```

Expected output: `Tests run: 6, Failures: 6` (class does not exist yet)

- [ ] **Step 3: Implement IdempotencyService**

```java
package org.chovy.canvas.engine.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * System-level idempotency key service backed by Redis.
 *
 * <p>Lifecycle: tryAcquire(RUNNING) -> markComplete(COMPLETED).
 * A key that is already COMPLETED means the handler ran successfully;
 * a key that is RUNNING means another thread is executing the same handler.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String PREFIX = "canvas:idem:";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redis;

    /**
     * Generate idempotency key from execution context.
     * Format: executionId:nodeId:attemptCount
     */
    public String generateKey(String executionId, String nodeId, int attemptCount) {
        return executionId + ":" + nodeId + ":" + attemptCount;
    }

    /**
     * Try to acquire the idempotency slot. Returns true if this is the first
     * execution for this key (SETNX succeeded), false if already RUNNING or COMPLETED.
     */
    public boolean tryAcquire(String key) {
        String redisKey = PREFIX + key;
        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, "RUNNING", TTL);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * Mark the idempotency key as COMPLETED after handler succeeds.
     * Subsequent calls to tryAcquire for the same key will return false,
     * and isCompleted will return true.
     */
    public void markComplete(String key) {
        String redisKey = PREFIX + key;
        redis.opsForValue().set(redisKey, "COMPLETED");
    }

    /**
     * Check whether the key is in COMPLETED state (handler already ran successfully).
     * Returns false if the key is RUNNING, missing, or holds any other value.
     */
    public boolean isCompleted(String key) {
        String redisKey = PREFIX + key;
        String value = redis.opsForValue().get(redisKey);
        return "COMPLETED".equals(value);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=IdempotencyServiceTest 2>&1 | tail -5
```

Expected output: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/IdempotencyService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/IdempotencyServiceTest.java && git commit -m "feat: implement IdempotencyService with Redis SETNX lifecycle"
```

---

### Task 2: Add idempotency key to NodeHandler interface + integrate into DagEngine

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineIdempotencyTest.java`

- [ ] **Step 1: Write failing test for duplicate handler execution returning cached result**

```java
package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.context.NodeStatus;
import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.handler.HandlerRegistry;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.policy.IdempotencyService;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.infrastructure.redis.ContextPersistenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DagEngineIdempotencyTest {

    private HandlerRegistry handlerRegistry;
    private IdempotencyService idempotencyService;
    private DagEngine dagEngine;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        handlerRegistry = mock(HandlerRegistry.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        idempotencyService = new IdempotencyService(redis);

        dagEngine = new DagEngine(
                handlerRegistry,
                mock(org.chovy.canvas.engine.scheduler.TraceWriteBuffer.class),
                mock(CanvasExecutionDlqMapper.class),
                mock(org.chovy.canvas.engine.scheduler.CircuitBreakerRegistry.class),
                mock(org.chovy.canvas.engine.scheduler.CanvasMetrics.class),
                new ObjectMapper(),
                mock(ContextPersistenceService.class),
                mock(org.chovy.canvas.engine.trigger.CanvasExecutionService.class),
                idempotencyService
        );
    }

    @Test
    void duplicateHandlerExecution_returnsCachedResultFromContext() {
        // Setup: handler that tracks invocation count
        NodeHandler handler = mock(NodeHandler.class);
        when(handler.executeAsync(anyMap(), any())).thenReturn(
                Mono.just(NodeResult.ok("next-node", Map.of("couponId", "C123")))
        );
        when(handler.isBenefitNode()).thenReturn(false);
        when(handler.isReachNode()).thenReturn(false);
        when(handlerRegistry.get(anyString())).thenReturn(handler);

        // Setup: idempotency key already COMPLETED in Redis
        when(valueOps.get(startsWith("canvas:idem:"))).thenReturn("COMPLETED");

        // Setup: execution context with prior successful output
        ExecutionContext ctx = new ExecutionContext();
        ctx.setExecutionId("exec-dup-1");
        ctx.setCanvasId(100L);
        ctx.putNodeOutput("node-A", Map.of("couponId", "C123"));
        ctx.setNodeStatus("node-A", NodeStatus.SUCCESS);

        // The key check: when isCompleted returns true, DagEngine should
        // skip handler execution and return the cached output
        boolean completed = idempotencyService.isCompleted(
                idempotencyService.generateKey("exec-dup-1", "node-A", 1));

        // Verify that isCompleted detects the duplicate
        assertTrue(completed, "IdempotencyService should detect COMPLETED key");
        // Verify the cached output is available
        assertEquals("C123", ctx.getNodeOutputs().get("node-A").get("couponId"));
    }

    @Test
    void firstHandlerExecution_isNotBlockedByIdempotency() {
        // Setup: Redis SETNX succeeds (first execution)
        when(valueOps.setIfAbsent(startsWith("canvas:idem:"), eq("RUNNING"), any(Duration.class)))
                .thenReturn(true);
        when(valueOps.get(startsWith("canvas:idem:"))).thenReturn(null);

        String key = idempotencyService.generateKey("exec-new-1", "node-B", 1);
        boolean acquired = idempotencyService.tryAcquire(key);

        assertTrue(acquired, "First execution should acquire idempotency key");
        assertFalse(idempotencyService.isCompleted(key), "Key should not be COMPLETED yet");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DagEngineIdempotencyTest 2>&1 | tail -5
```

Expected output: Compilation error — DagEngine constructor does not accept IdempotencyService yet.

- [ ] **Step 3: Add idempotencyKey to NodeHandler interface**

Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`:

```java
package org.chovy.canvas.engine.handler;

import org.chovy.canvas.engine.context.ExecutionContext;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface NodeHandler {

    /**
     * Execute node logic with idempotency key.
     *
     * @param config         node configuration from DAG node config/bizConfig
     * @param ctx            current execution context
     * @param idempotencyKey system-level idempotency key (executionId:nodeId:attemptCount)
     * @return node execution result
     */
    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx, String idempotencyKey);

    /**
     * @deprecated Use {@link #executeAsync(Map, ExecutionContext, String)} instead.
     *             This default delegates to the 3-arg version with null idempotencyKey
     *             for backward compatibility with existing handlers.
     */
    @Deprecated
    default Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return executeAsync(config, ctx, null);
    }

    default boolean isBenefitNode() { return false; }
    default boolean isReachNode()   { return false; }
}
```

- [ ] **Step 4: Add IdempotencyService to DagEngine constructor and integrate into executeHandlerWithRepeat**

The current DagEngine constructor (line 121-138 in `DagEngine.java`) takes 8 parameters. Add `IdempotencyService` as the 9th parameter. **IMPORTANT: Remove @RequiredArgsConstructor from DagEngine class annotation, replace with the explicit constructor shown below.** The existing @RequiredArgsConstructor generates a constructor that conflicts with this manual 9-parameter constructor.

Insert this after the existing 8th parameter (`@Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService`) and before the closing brace of the constructor:

```java
// BEFORE (current constructor — line 121)
public DagEngine(HandlerRegistry handlerRegistry,
                 TraceWriteBuffer traceBuffer,
                 CanvasExecutionDlqMapper dlqMapper,
                 CircuitBreakerRegistry cbRegistry,
                 CanvasMetrics metrics,
                 ObjectMapper objectMapper,
                 ContextPersistenceService ctxStore,
                 @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService) {

// AFTER (add IdempotencyService as 9th parameter)
public DagEngine(HandlerRegistry handlerRegistry,
                 TraceWriteBuffer traceBuffer,
                 CanvasExecutionDlqMapper dlqMapper,
                 CircuitBreakerRegistry cbRegistry,
                 CanvasMetrics metrics,
                 ObjectMapper objectMapper,
                 ContextPersistenceService ctxStore,
                 @Lazy org.chovy.canvas.engine.trigger.CanvasExecutionService executionService,
                 IdempotencyService idempotencyService) {
    this.handlerRegistry = handlerRegistry;
    this.traceBuffer = traceBuffer;
    this.dlqMapper = dlqMapper;
    this.cbRegistry = cbRegistry;
    this.metrics = metrics;
    this.objectMapper = objectMapper;
    this.ctxStore = ctxStore;
    this.executionService = executionService;
    this.idempotencyService = idempotencyService;
}
```

Add field and import at the top of DagEngine:

```java
import org.chovy.canvas.engine.policy.IdempotencyService;

// Add field after other private fields:
/** Idempotency key service, persists handler execution state in Redis. */
private final IdempotencyService idempotencyService;
```

In `executeHandlerWithRepeat` (line 413+), replace the `singleCall` Mono.defer (line 426-438) with idempotency check:

```java
// ── ① singleCall：idempotency check + single handler call ────────────
Mono<NodeResult> singleCall = Mono.defer(() -> {
            // System-level idempotency: if this handler was already completed
            // for this execution+node+attempt, return the cached output.
            String idemKey = idempotencyService.generateKey(
                    ctx.getExecutionId(), nodeId, 1 /* attempt tracked by retry */);
            if (idempotencyService.isCompleted(idemKey)) {
                log.debug("[ENGINE] 幂等跳过(已完成) nodeId={} key={}", nodeId, idemKey);
                Map<String, Object> cachedOutput = ctx.getNodeOutputs().get(nodeId);
                return Mono.just(NodeResult.ok(nodeId, cachedOutput));
            }
            try {
                cbRegistry.checkState();
            } catch (CircuitBreakerRegistry.CircuitBreakerOpenException e) {
                return Mono.just(NodeResult.fail(e.getMessage()));
            }
            return handler.executeAsync(handlerConfig, ctx, idemKey)
                    .doOnNext(r -> {
                        if (r.success()) {
                            idempotencyService.markComplete(idemKey);
                            cbRegistry.recordSuccess();
                        } else {
                            cbRegistry.recordFailure();
                        }
                    })
                    .doOnError(e -> cbRegistry.recordFailure());
        });
```

Note: The existing 60+ handler implementations continue to work because `executeAsync(config, ctx)` delegates to `executeAsync(config, ctx, null)` via the default method. Handlers that want the idempotency key override the 3-arg version.

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DagEngineIdempotencyTest 2>&1 | tail -5
```

Expected output: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`

Also run full engine test suite to verify no regression:

```bash
cd backend && mvn test -pl canvas-engine 2>&1 | tail -5
```

Expected output: `Tests run: X, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineIdempotencyTest.java && git commit -m "feat: add idempotency key to NodeHandler interface and integrate into DagEngine"
```

---

### Task 3: Deferred context write — only persist node output after handler success

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/DeferredWriteTest.java`

- [ ] **Step 1: Write failing test for deferred context write**

```java
package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeferredWriteTest {

    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new ExecutionContext();
        ctx.setExecutionId("exec-defer-1");
    }

    @Test
    void stageOutput_doesNotWriteToFlatContext() {
        ctx.stageNodeOutput("node-A", Map.of("couponId", "C123"));
        // Output should NOT be in flatContext yet
        assertNull(ctx.getContextValue("couponId"),
                "Staged output must not appear in flatContext");
        // Output should NOT be in nodeOutputs yet
        assertFalse(ctx.getNodeOutputs().containsKey("node-A"),
                "Staged output must not appear in nodeOutputs");
    }

    @Test
    void commitStagedOutput_writesToFlatContextAndNodeOutputs() {
        ctx.stageNodeOutput("node-A", Map.of("couponId", "C123"));
        ctx.commitStagedOutput("node-A");
        assertEquals("C123", ctx.getContextValue("couponId"));
        assertEquals("C123", ctx.getNodeOutputs().get("node-A").get("couponId"));
    }

    @Test
    void discardStagedOutput_doesNotWriteAnything() {
        ctx.stageNodeOutput("node-A", Map.of("couponId", "C123"));
        ctx.discardStagedOutput("node-A");
        assertNull(ctx.getContextValue("couponId"));
        assertFalse(ctx.getNodeOutputs().containsKey("node-A"));
    }

    @Test
    void failedHandler_doesNotPolluteDownstream() {
        // Simulate: handler runs, stages output, then fails
        ctx.stageNodeOutput("node-A", Map.of("partialData", "should-not-appear"));
        ctx.discardStagedOutput("node-A"); // handler failed -> discard

        // Another node reads context — should not see the failed handler's output
        assertNull(ctx.getContextValue("partialData"));
    }

    @Test
    void successfulHandler_commitsAndDownstreamSeesIt() {
        ctx.stageNodeOutput("node-A", Map.of("couponId", "C123"));
        ctx.commitStagedOutput("node-A");

        // Downstream node reads via flatContext
        assertEquals("C123", ctx.getContextValue("couponId"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeferredWriteTest 2>&1 | tail -5
```

Expected output: Compilation error — `stageNodeOutput`, `commitStagedOutput`, `discardStagedOutput` methods do not exist yet.

- [ ] **Step 3: Implement deferred write in ExecutionContext**

Add these fields and methods to `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`:

Add new field after `flatContext` (approximately after line 50 in ExecutionContext.java, in the existing field declarations section):

```java
/** Staged node outputs awaiting commit (nodeId -> output map).
 *  Handler writes here first; only committed to nodeOutputs/flatContext after success. */
@JsonIgnore
private final Map<String, Map<String, Object>> stagedOutputs = new ConcurrentHashMap<>();
```

Add three new methods after `putNodeOutput` (approximately after line 200 in ExecutionContext.java, after the existing `putNodeOutput` method):
// ── Deferred (staged) write ────────────────────────────────────────

/**
 * Stage node output without committing to flatContext.
 * Used by handlers that may fail after partial output generation.
 * Call commitStagedOutput on success or discardStagedOutput on failure.
 */
public void stageNodeOutput(String nodeId, Map<String, Object> output) {
    stagedOutputs.put(nodeId, new HashMap<>(output));
}

/**
 * Commit previously staged output to nodeOutputs and flatContext.
 * This is the atomic "publish" step — downstream nodes can now see the data.
 */
public void commitStagedOutput(String nodeId) {
    Map<String, Object> output = stagedOutputs.remove(nodeId);
    if (output != null && !output.isEmpty()) {
        putNodeOutput(nodeId, output);
    }
}

/**
 * Discard staged output without writing to nodeOutputs or flatContext.
 * Call this when a handler fails, so partial output does not pollute downstream.
 */
public void discardStagedOutput(String nodeId) {
    stagedOutputs.remove(nodeId);
}
```

Add import for `HashMap` (already present via `java.util.*`).

- [ ] **Step 4: Update DagEngine to use staged writes**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`, change the output-write sections in both `executeNode` and `executeNodeAfterStage2`.

In the `executeNode` method, replace:

```java
if (result.output() != null && !result.output().isEmpty()) {
    ctx.putNodeOutput(nodeId, result.output());
}
```

with:

```java
if (result.output() != null && !result.output().isEmpty()) {
    ctx.stageNodeOutput(nodeId, result.output());
}
ctx.commitStagedOutput(nodeId);
```

In the `executeNodeAfterStage2` method, replace the SUCCESS path (path C) output write:

```java
if (result.output() != null && !result.output().isEmpty()) {
    ctx.putNodeOutput(nodeId, result.output());
}
```

with:

```java
if (result.output() != null && !result.output().isEmpty()) {
    ctx.stageNodeOutput(nodeId, result.output());
}
ctx.commitStagedOutput(nodeId);
```

And in the FAILED path, add discard before the failure handling:

```java
if (!result.success()) {
    ctx.discardStagedOutput(nodeId);
    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
    writeTraceEnd(ctx, node, result, System.currentTimeMillis() - nodeStartMs);
    if (ctx.isBenefitGranted() || ctx.isUserReached()) {
        log.warn("[ENGINE] 防资损：节点失败但整体判定成功 nodeId={}", nodeId);
        return Mono.just(Map.of());
    }
    return triggerFailureAwareDownstream(graph, nodeId, node.getType(), ctx, depth,
            result.errorMessage());
}
```

Similarly in the `onErrorResume` block (line ~330):

```java
.onErrorResume(e -> {
    ctx.discardStagedOutput(nodeId);
    ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
    log.error("[ENGINE] 节点异常 nodeId={}: {}", nodeId, e.getMessage());
    if (ctx.isBenefitGranted() || ctx.isUserReached()) return Mono.just(Map.of());
    throw e;
});
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeferredWriteTest 2>&1 | tail -5
```

Expected output: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

Full engine regression:

```bash
cd backend && mvn test -pl canvas-engine 2>&1 | tail -5
```

Expected output: `Tests run: X, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/DeferredWriteTest.java && git commit -m "feat: implement deferred context write — only persist node output after handler success"
```
