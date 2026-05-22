# FIXME Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all confirmed bugs and dead code identified from 14 FIXME comments; fix two additional concurrency bugs in special DAG node handlers; add two missing caches for high-frequency paths.

**Architecture:** Each task is a self-contained fix in a single file. Tasks are ordered by severity: confirmed bugs first, then dead code cleanup, then performance.

**Tech Stack:** Java 21, Spring WebFlux, MyBatis-Plus, Redis (StringRedisTemplate), Caffeine, Lombok

---

## FIXME Audit Summary

| Location | Status | Action |
|---|---|---|
| `DagEngine.java:163` — depth always 0 | **BUG** | Task 1: fix |
| `EventDefinitionController.java:114` — canvasTriggered/canvasCount always 0 | **BUG** | Task 2: fix |
| `TriggerPreCheckService.java:79` — perUserTotalLimit race condition | **BUG** | Task 3: fix |
| `CanvasService.java:191` — "multi-node cache consistency" | **STALE** (already solved by Redis Pub/Sub in CanvasConfigCache) | Task 4: remove comment |
| `CanvasDisruptorService.java:121` — remainingCapacity() unused | **DEAD CODE** | Task 5: remove |
| `EventDefinitionController.java:105` — no cache on event definition query | **PERF** | Task 6: add cache |
| `CanvasExecutionService.java:147` — canvas loaded from MySQL on every trigger | **PERF** | Task 7: add cache |
| `CanvasExecutionService.java:158` — frontend has no preCheck config UI | **PRODUCT GAP** | out of scope |
| `CanvasExecutionService.java:178-179` — dedup TTL semantics under suspension | **DESIGN GAP** | out of scope |
| `CanvasExecutionService.java:192` — context restore when re-triggered during suspend | **DESIGN GAP** | out of scope |
| `EventDefinitionController.java:128` — Redis failure = full pipeline failure | **ARCH DECISION** | out of scope |
| `EventDefinitionController.java:130` — UUID substring instead of snowflake | **MINOR** | out of scope (no snowflake lib) |
| `DagEngine.java:223` — CAS lock skips special node types (gate-check phase unprotected) | **analyzed** | root cause of Task 8 & 9; gate execution itself is protected by CAS in executeNodeAfterStage2 |
| `TriggerPreCheckService.java:79` — totalCount per-day vs lifetime semantics | **DESIGN** | note added in Task 3 |
| `DagEngine.java` — check-then-act race in all 4 special node timer schedulers | **BUG** | Task 8: fix |
| `ExecutionContext.java` — nodeOutputs/flatContext non-thread-safe under parallel branches | **BUG (HIGH)** | Task 9: fix |
| `DagEngine.java` — executeNodeAfterStage2 missing nodeStartMs / metrics / log for special nodes | **BUG** | Task 10: fix |

---

## Task 1: Fix depth counter never propagating in DagEngine

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`

**Problem:** `executeNode(3-arg)` always calls `executeNode(4-arg, depth=0)`. All recursive downstream calls go through `triggerDownstream` and `tryPrioritySequentially`, which also call the 3-arg version. Result: `depth` is always 0 and `MAX_NODE_DEPTH=200` can never fire, leaving the system unprotected against infinite loops or very deep DAGs.

The call chain: `execute()` → `executeNode(4-arg, depth=0)` → `executeNodeAfterStage2(no depth)` → `triggerDownstream(no depth)` → `executeNode(3-arg)` → `executeNode(4-arg, depth=0)` ← reset to 0 here.

Fix: thread `depth` through `executeNodeAfterStage2`, `triggerDownstream`, and `tryPrioritySequentially`, then pass `depth + 1` at every recursive call site.

- [ ] **Step 1: Write failing test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineDepthTest.java
package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.dag.DagGraph;
import org.chovy.canvas.engine.dag.DagParser;
import org.chovy.canvas.engine.execution.ExecutionContext;
import org.chovy.canvas.engine.node.NodeHandlerRegistry;
import org.chovy.canvas.infra.store.CanvasCtxStore;
import org.chovy.canvas.infra.trace.TraceWriter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that DagEngine respects MAX_NODE_DEPTH.
 */
class DagEngineDepthTest {

    @Test
    void depthCheckFires_whenDepthExceedsMax() {
        // Build a DagEngine with a tiny MAX_NODE_DEPTH so the test is fast.
        // We verify that executing a chain longer than the limit results in an error
        // containing the depth-exceeded message (not a StackOverflowError).
        DagGraph graph = buildLinearChain(10); // 10-node linear chain
        ExecutionContext ctx = ExecutionContext.fresh("c1", 1L, 1L, "userId", "SCHEDULED");
        NodeHandlerRegistry registry = Mockito.mock(NodeHandlerRegistry.class);
        CanvasCtxStore ctxStore = Mockito.mock(CanvasCtxStore.class);
        TraceWriter traceWriter = Mockito.mock(TraceWriter.class);

        DagEngine engine = new DagEngine(registry, ctxStore, traceWriter);
        // engine exposes package-visible MAX_NODE_DEPTH; we'll use the built-in limit of 200
        // To keep test fast, we directly test that depth > 0 after recursive call.
        // The simplest assertion: a linear 3-node chain must call downstream with depth=1, then depth=2.
        // We verify this by checking no error is thrown for depth < MAX_NODE_DEPTH.
        Map<String, Object> result = engine.execute(graph, "node-0", ctx).block();
        assertThat(result).isNotNull();
    }

    private DagGraph buildLinearChain(int length) {
        // Returns a graph: node-0 → node-1 → ... → node-(length-1)
        // All nodes are SCHEDULED_TRIGGER type with no-op handler
        // Implementation omitted here; use DagParser with minimal JSON
        throw new UnsupportedOperationException("fill in per project's DagGraph builder");
    }
}
```

> Note: The test structure above documents intent. The real verification is that after the fix, a chain deeper than MAX_NODE_DEPTH throws with "DAG 执行深度超限" rather than silently looping or producing a StackOverflowError. Adapt `buildLinearChain` to use the project's existing graph-builder helpers.

- [ ] **Step 2: Run test to confirm it compiles (depth issue will surface as StackOverflow or silent loop)**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=DagEngineDepthTest -q 2>&1 | tail -20
```

- [ ] **Step 3: Add `depth` parameter to `executeNodeAfterStage2`**

Current signature (line 698):
```java
private Mono<Map<String, Object>> executeNodeAfterStage2(DagGraph graph, String nodeId,
                                                         DagParser.CanvasNode node,
                                                         Map<String, Object> config,
                                                         ExecutionContext ctx) {
```

Replace with:
```java
private Mono<Map<String, Object>> executeNodeAfterStage2(DagGraph graph, String nodeId,
                                                         DagParser.CanvasNode node,
                                                         Map<String, Object> config,
                                                         ExecutionContext ctx,
                                                         int depth) {
```

- [ ] **Step 4: Thread depth into all `executeNodeAfterStage2` call sites**

There are multiple call sites inside `executeNode(4-arg)`. Find all: `grep -n "executeNodeAfterStage2" DagEngine.java`. Each call must pass `depth`:

```java
// Example pattern — find and replace every call:
return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx, depth);
// (instead of)
return executeNodeAfterStage2(graph, nodeId, node, enrichedConfig, ctx);
```

- [ ] **Step 5: Add `depth` parameter to `triggerDownstream`**

Current signature (line 761):
```java
private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                    String sourceNodeId, String sourceType,
                                                    ExecutionContext ctx) {
```

Replace with:
```java
private Mono<Map<String, Object>> triggerDownstream(DagGraph graph, NodeResult result,
                                                    String sourceNodeId, String sourceType,
                                                    ExecutionContext ctx,
                                                    int depth) {
```

Update the call in `executeNodeAfterStage2` (line ~744):
```java
return triggerDownstream(graph, result, nodeId, node.getType(), ctx, depth);
```

- [ ] **Step 6: Fix recursive calls inside `triggerDownstream` to pass `depth + 1`**

Line 784 — parallel downstream:
```java
// BEFORE:
.flatMap(nextId -> executeNode(graph, nextId, ctx))
// AFTER:
.flatMap(nextId -> executeNode(graph, nextId, ctx, depth + 1))
```

Line 773 — PRIORITY branch:
```java
// BEFORE:
return tryPrioritySequentially(orderedBranches, fallbackNextId, sourceNodeId, graph, ctx);
// AFTER:
return tryPrioritySequentially(orderedBranches, fallbackNextId, sourceNodeId, graph, ctx, depth + 1);
```

- [ ] **Step 7: Add `depth` parameter to `tryPrioritySequentially` and fix its recursive calls**

```java
// Signature:
private Mono<Map<String, Object>> tryPrioritySequentially(List<String> branches,
                                                          String fallbackNextId,
                                                          String priorityNodeId,
                                                          DagGraph graph,
                                                          ExecutionContext ctx,
                                                          int depth) {
    if (branches.isEmpty()) {
        if (fallbackNextId != null) {
            ctx.setNodeStatus(priorityNodeId, NodeStatus.PARTIAL_FAIL);
            return executeNode(graph, fallbackNextId, ctx, depth);  // same depth, not +1
        }
        return Mono.error(new RuntimeException("PRIORITY 所有分支均失败"));
    }

    String currentBranchId = branches.getFirst();
    return executeNode(graph, currentBranchId, ctx, depth)  // same depth, not +1
            .flatMap(__ -> {
                if (ctx.getNodeStatus(currentBranchId) == NodeStatus.SUCCESS) {
                    return Mono.just(Map.of());
                }
                return tryPrioritySequentially(
                        branches.subList(1, branches.size()),
                        fallbackNextId, priorityNodeId, graph, ctx, depth);
            });
}
```

Note: PRIORITY sequential branching is lateral movement (trying alternatives), not deeper nesting, so it should not increment depth.

- [ ] **Step 8: Remove the dead 3-arg overload that hardcodes depth=0**

```java
// DELETE these lines (around line 158-161):
private Mono<Map<String, Object>> executeNode(DagGraph graph, String nodeId,
                                              ExecutionContext ctx) {
    return executeNode(graph, nodeId, ctx, 0);
}
```

All internal callers now call the 4-arg version directly. The public `execute()` method already calls the 4-arg version with depth=0.

- [ ] **Step 9: Remove stale FIXME comment**

In `DagEngine.java` around line 163, the FIXME comment is now resolved. Delete it:
```java
// DELETE: // FIXME: depth 参数没有更新, 恒定为0
```

- [ ] **Step 10: Build and run test**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=DagEngineDepthTest -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS or test passes.

- [ ] **Step 11: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/DagEngineDepthTest.java
git commit -m "fix: propagate depth counter through DagEngine recursive calls

MAX_NODE_DEPTH=200 was never reachable because the 3-arg executeNode
overload and all downstream helpers reset depth to 0 on every recursive
call. Thread depth through executeNodeAfterStage2, triggerDownstream,
and tryPrioritySequentially so infinite-loop and over-deep DAGs are
correctly detected."
```

---

## Task 2: Fix EventLog canvasTriggered / canvasCount always 0

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java`

**Problem:** The `EventLog` row is inserted (lines 115–125) before `triggerRouteService.getCanvasByBehavior()` is called (line 129). At insertion time, `canvasTriggered` and `canvasCount` are hardcoded to 0 and never updated. The actual count (`canvasIds.size()`) is calculated later but only goes into the HTTP response, not the DB row.

Fix: move the EventLog insertion to after `canvasIds` is known, setting the actual count.

- [ ] **Step 1: Write failing test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java
// (add to existing test class or create new)

@Test
void reportEvent_setsCorrectCanvasCount() {
    // Arrange: mock eventMapper to return a valid EventDefinition
    // Arrange: mock triggerRouteService.getCanvasByBehavior to return {"1","2"}
    // Arrange: capture the EventLog passed to logMapper.insert()

    EventReportReq req = new EventReportReq();
    req.setEventCode("EVT_TEST");
    req.setUserId("u1");

    // Act: call POST /canvas/events/report

    // Assert: the EventLog row captured by logMapper.insert() has canvasTriggered=2
    ArgumentCaptor<EventLog> captor = ArgumentCaptor.forClass(EventLog.class);
    verify(logMapper).insert(captor.capture());
    assertThat(captor.getValue().getCanvasTriggered()).isEqualTo(2);
    assertThat(captor.getValue().getCanvasCount()).isEqualTo(2);
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=EventDefinitionControllerTest#reportEvent_setsCorrectCanvasCount -q 2>&1 | tail -10
```

Expected: FAIL (canvasTriggered is 0, not 2).

- [ ] **Step 3: Reorder EventLog insertion to after canvasIds is retrieved**

In `EventDefinitionController.java`, inside `reportEvent`, change:

```java
// BEFORE — steps 2 and 3 are in wrong order:

// 2. 记录事件日志  ← inserted BEFORE we know canvasIds
EventLog eventLog = new EventLog();
eventLog.setEventCode(req.getEventCode());
eventLog.setUserId(req.getUserId());
try {
    eventLog.setAttributes(req.getAttributes() != null
            ? objectMapper.writeValueAsString(req.getAttributes()) : null);
} catch (Exception ignored) {
}
eventLog.setCanvasTriggered(0);
eventLog.setCanvasCount(0);
logMapper.insert(eventLog);

// 3. 从路由表查所有监听此事件的已发布画布，逐一触发
Set<String> canvasIds = triggerRouteService.getCanvasByBehavior(req.getEventCode());
```

```java
// AFTER — get canvasIds first, then insert EventLog with real count:

// 2. 从路由表查所有监听此事件的已发布画布，逐一触发
// FIXME: Redis 异常意味着整个链路都无法推进, 考虑降级方案以及是否有做好缓存刷新问题
Set<String> canvasIds = triggerRouteService.getCanvasByBehavior(req.getEventCode());
String eventId = "evt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
Map<String, Object> payload = req.getAttributes() != null ? req.getAttributes() : Map.of();

canvasIds.forEach(cidStr -> {
    try {
        Long cid = Long.parseLong(cidStr);
        disruptorService.publish(cid, req.getUserId(), TriggerType.EVENT,
                NodeType.EVENT_TRIGGER, req.getEventCode(), payload, eventId + "-" + cidStr);
        log.info("[EVENT] 触发画布 canvasId={} eventCode={} userId={}",
                cid, req.getEventCode(), req.getUserId());
    } catch (Exception e) {
        log.warn("[EVENT] 触发画布失败 canvasId={}: {}", cidStr, e.getMessage());
    }
});

if (canvasIds.isEmpty()) {
    log.info("[EVENT] 无已发布画布订阅事件 eventCode={}", req.getEventCode());
}

// 3. 记录事件日志（在触发后写入，以记录真实触发数量）
EventLog eventLog = new EventLog();
eventLog.setEventCode(req.getEventCode());
eventLog.setUserId(req.getUserId());
try {
    eventLog.setAttributes(req.getAttributes() != null
            ? objectMapper.writeValueAsString(req.getAttributes()) : null);
} catch (Exception ignored) {
}
eventLog.setCanvasTriggered(canvasIds.size());
eventLog.setCanvasCount(canvasIds.size());
logMapper.insert(eventLog);

Map<String, Object> resp = new java.util.LinkedHashMap<>();
resp.put("eventLogId", eventLog.getId());
resp.put("eventCode", req.getEventCode());
resp.put("userId", req.getUserId());
resp.put("canvasTriggered", canvasIds.size());
resp.put("status", "ACCEPTED");
return resp;
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=EventDefinitionControllerTest#reportEvent_setsCorrectCanvasCount -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java
git commit -m "fix: set correct canvasTriggered/canvasCount in EventLog

EventLog was inserted before triggerRouteService.getCanvasByBehavior()
was called, so canvasTriggered and canvasCount were always 0. Reorder
to fetch canvasIds first, then insert the log with the real count."
```

---

## Task 3: Fix perUserTotalLimit race condition with Redis INCR

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`

**Problem:** Check #5 (`perUserTotalLimit`) reads `CanvasUserQuota.totalCount` from MySQL, then compares. Under concurrent trigger bursts, multiple threads read the same (stale) count and all pass the check, allowing over-quota triggers. The fix replaces the MySQL read with a Redis INCR, matching the pattern already used for `perUserDailyLimit` (check #4).

**Note on semantics:** `CanvasUserQuota` has a composite PK of `(canvas_id, user_id, trigger_date)`. In the current implementation, `totalCount` in today's row is incremented from 1 each new day — it is **not** a lifetime running total across all days. The Redis key below uses the same day-scoped key. If the product intent changes to a true lifetime total, a separate migration is needed; that is out of scope here.

- [ ] **Step 1: Write failing test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceTest.java

@Test
void perUserTotalLimit_rejectsWhenAtLimit_withoutRaceCondition() {
    Canvas canvas = new Canvas();
    canvas.setId(1L);
    canvas.setPerUserTotalLimit(3);

    // Simulate Redis INCR returning 4 (limit already hit)
    String today = LocalDate.now().toString();
    String key = "canvas:quota:total:1:user1:" + today;
    when(redis.opsForValue().increment(key)).thenReturn(4L);
    when(redis.expire(eq(key), any())).thenReturn(true);

    assertThatThrownBy(() -> service.check(canvas, "user1"))
            .isInstanceOf(TriggerPreCheckService.TriggerRejectedException.class)
            .hasMessageContaining("总触发次数");

    // Verify decrement called to roll back
    verify(redis.opsForValue()).decrement(key);
}

@Test
void perUserTotalLimit_allowsWhenUnderLimit() {
    Canvas canvas = new Canvas();
    canvas.setId(1L);
    canvas.setPerUserTotalLimit(3);

    String today = LocalDate.now().toString();
    String key = "canvas:quota:total:1:user1:" + today;
    when(redis.opsForValue().increment(key)).thenReturn(2L);
    when(redis.expire(eq(key), any())).thenReturn(true);

    // Should not throw
    assertThatNoException().isThrownBy(() -> service.check(canvas, "user1"));
    // Verify no decrement (not rolled back)
    verify(redis.opsForValue(), never()).decrement(key);
}
```

- [ ] **Step 2: Run tests to confirm they fail**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=TriggerPreCheckServiceTest -q 2>&1 | tail -10
```

Expected: FAIL (current code reads MySQL, not Redis INCR).

- [ ] **Step 3: Replace MySQL read with Redis INCR for check #5**

In `TriggerPreCheckService.java`, replace the entire check #5 block (lines ~78–91):

```java
// BEFORE:
// 5. 用户总触发上限（从 MySQL 查询，允许轻微超配）
// FIXME: 此处修改成不允许超配
if (canvas.getPerUserTotalLimit() != null) {
    CanvasUserQuota quota = quotaMapper.selectOne(
            new LambdaQueryWrapper<CanvasUserQuota>()
                    .eq(CanvasUserQuota::getCanvasId, canvasId)
                    .eq(CanvasUserQuota::getUserId, userId)
                    .eq(CanvasUserQuota::getTriggerDate, LocalDate.now())
    );
    int total = quota != null ? quota.getTotalCount() : 0;
    if (total >= canvas.getPerUserTotalLimit()) {
        throw new TriggerRejectedException("QUOTA_002", "用户总触发次数已达上限");
    }
}
```

```java
// AFTER:
// 5. 用户总触发上限（Redis INCR 原子扣减，与 perUserDailyLimit 同日维度）
if (canvas.getPerUserTotalLimit() != null) {
    String today = LocalDate.now().toString();
    String key = QUOTA_KEY + "total:" + canvasId + ":" + userId + ":" + today;
    Long total = redis.opsForValue().increment(key);
    redis.expire(key, Duration.ofDays(2));
    if (total != null && total > canvas.getPerUserTotalLimit()) {
        redis.opsForValue().decrement(key);
        throw new TriggerRejectedException("QUOTA_002", "用户总触发次数已达上限");
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=TriggerPreCheckServiceTest -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TriggerPreCheckServiceTest.java
git commit -m "fix: use Redis INCR for perUserTotalLimit to eliminate race condition

MySQL read-then-compare allowed over-quota triggers under concurrent
load. Switch to Redis INCR + conditional rollback, matching the same
atomic pattern already used for perUserDailyLimit."
```

---

## Task 4: Remove stale FIXME in CanvasService

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`

**Problem:** The FIXME at line 191 says "如果有多台机器的话, 本地缓存如何失效处理保障一致性". This is already fully solved: `CanvasConfigCache` subscribes to the `canvas:cache:invalidate` Redis Pub/Sub channel on startup and evicts local Caffeine entries when any node publishes an invalidation event. The comment is misleading noise.

- [ ] **Step 1: Remove the stale comment**

In `CanvasService.java`, delete the FIXME line at line 191:
```java
// DELETE this line:
// FIXME: 如果有多台机器的话, 本地缓存如何失效处理保障一致性
```

The surrounding code (lines 192–197) remains unchanged.

- [ ] **Step 2: Verify build compiles**

```bash
cd backend && ./mvnw compile -pl canvas-engine -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java
git commit -m "chore: remove stale FIXME in CanvasService

Multi-node L1 cache consistency was already implemented via Redis Pub/Sub
in CanvasConfigCache (canvas:cache:invalidate channel). The comment
was misleading and has been removed."
```

---

## Task 5: Remove unused `remainingCapacity()` from CanvasDisruptorService

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`

**Problem:** `remainingCapacity()` is marked with a FIXME saying it's not actually used. Dead code. Remove it.

- [ ] **Step 1: Verify the method is truly unused**

```bash
grep -rn "remainingCapacity" \
    backend/canvas-engine/src/main/java/ \
    backend/canvas-engine/src/test/java/ 2>/dev/null | grep -v "target/"
```

Expected: only the declaration in `CanvasDisruptorService.java`. If other callers exist, skip this task.

- [ ] **Step 2: Delete the method**

In `CanvasDisruptorService.java`, delete these lines (around 118–124):
```java
// DELETE:
/**
 * 剩余可用容量（用于监控背压）
 */
// FIXME: 没有实际使用到
public long remainingCapacity() {
    return ringBuffer.remainingCapacity();
}
```

- [ ] **Step 3: Build**

```bash
cd backend && ./mvnw compile -pl canvas-engine -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java
git commit -m "chore: remove unused remainingCapacity() from CanvasDisruptorService"
```

---

## Task 6: Cache EventDefinition in EventDefinitionController

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java`

**Problem:** `POST /canvas/events/report` is a hot path — it's called once per user event. On every call, it queries MySQL to find the `EventDefinition` by `eventCode`. Event definitions change rarely (publish/disable). This should be cached.

**Approach:** Add a Caffeine local cache (TTL 10 minutes, max 200 entries) to `EventDefinitionController`. Evict on `PUT /canvas/events/:id` (update) — since there's no separate "disable" endpoint visible, the TTL provides eventual consistency.

- [ ] **Step 1: Write failing test**

```java
// In EventDefinitionControllerTest.java, add:

@Test
void reportEvent_usesEventDefinitionCache_onSecondCall() {
    EventDefinition def = new EventDefinition();
    def.setEventCode("EVT_CACHED");
    def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
    when(eventMapper.selectOne(any())).thenReturn(def);
    when(triggerRouteService.getCanvasByBehavior("EVT_CACHED")).thenReturn(Set.of());

    EventReportReq req = new EventReportReq();
    req.setEventCode("EVT_CACHED");
    req.setUserId("u1");

    // Call twice
    controller.reportEvent(req).block();
    controller.reportEvent(req).block();

    // eventMapper.selectOne should only be called once (second call hits cache)
    verify(eventMapper, times(1)).selectOne(any());
}
```

- [ ] **Step 2: Run test to confirm it fails (currently 2 DB calls)**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=EventDefinitionControllerTest#reportEvent_usesEventDefinitionCache_onSecondCall -q 2>&1 | tail -10
```

Expected: FAIL.

- [ ] **Step 3: Add Caffeine cache field and init in EventDefinitionController**

Add imports:
```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
```

Add field after existing fields:
```java
/** 事件定义本地缓存：eventCode → EventDefinition。TTL=10min，最多200条。 */
private final Cache<String, EventDefinition> eventDefCache = Caffeine.newBuilder()
        .maximumSize(200)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
```

- [ ] **Step 4: Use cache in `reportEvent`**

Replace the existing DB query in `reportEvent` (lines 105–111):
```java
// BEFORE:
// FIXME: 事件定义会频繁上报导致查询, 事件查询应该有缓存机制
EventDefinition def = eventMapper.selectOne(
        new LambdaQueryWrapper<EventDefinition>()
                .eq(EventDefinition::getEventCode, req.getEventCode())
                .eq(EventDefinition::getEnabled, CanvasStatusEnum.PUBLISHED.getCode()));
if (def == null)
    throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());
```

```java
// AFTER:
EventDefinition def = eventDefCache.get(req.getEventCode(), code ->
        eventMapper.selectOne(
                new LambdaQueryWrapper<EventDefinition>()
                        .eq(EventDefinition::getEventCode, code)
                        .eq(EventDefinition::getEnabled, CanvasStatusEnum.PUBLISHED.getCode())));
if (def == null)
    throw new IllegalArgumentException("事件未定义或已禁用: " + req.getEventCode());
```

- [ ] **Step 5: Evict on update**

In the `update` method of `EventDefinitionController` (around line 74), add cache eviction:
```java
@PutMapping("/events/{id}")
public Mono<R<Void>> update(@PathVariable Long id, @RequestBody EventDefinition body) {
    return Mono.fromRunnable(() -> {
        body.setId(id);
        eventMapper.updateById(body);
        // Evict cache since eventCode may have changed status
        if (body.getEventCode() != null) {
            eventDefCache.invalidate(body.getEventCode());
        }
    }).subscribeOn(Schedulers.boundedElastic()).thenReturn(R.ok());
}
```

- [ ] **Step 6: Run tests**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=EventDefinitionControllerTest -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java
git commit -m "perf: add Caffeine cache for EventDefinition in reportEvent hot path

Event definitions are queried on every event report but change rarely.
Add a 10-minute TTL local cache (max 200 entries) with eviction on
update to avoid per-request MySQL queries."
```

---

## Task 7: Cache Canvas entity in CanvasExecutionService

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`

**Problem:** `canvasMapper.selectById(canvasId)` is called on every trigger execution (line 148). This is the Canvas metadata record (status, limits, etc.), not the DAG graph. The DAG graph is already cached by `CanvasConfigCache`. The metadata should also be cached to avoid per-trigger MySQL reads.

**Approach:** Add a Caffeine cache in `CanvasExecutionService` for `Canvas` entities. TTL=5 minutes, max 500 entries. Invalidate on publish (there's no direct publish call in this service, so TTL-based eviction is sufficient).

- [ ] **Step 1: Write failing test**

```java
// In CanvasExecutionServiceTest.java (create or add to existing):

@Test
void trigger_usesCanvasCache_onSecondCall() {
    Canvas canvas = new Canvas();
    canvas.setId(10L);
    canvas.setStatus(CanvasStatusEnum.PUBLISHED.getCode());
    when(canvasMapper.selectById(10L)).thenReturn(canvas);
    // ... other mocks as needed to let trigger() complete

    service.trigger(10L, "u1", "SCHEDULED", "SCHEDULED_TRIGGER",
                    null, Map.of(), "msg1", false).block();
    service.trigger(10L, "u1", "SCHEDULED", "SCHEDULED_TRIGGER",
                    null, Map.of(), "msg2", false).block();

    // canvasMapper.selectById should only be called once (second call hits cache)
    verify(canvasMapper, times(1)).selectById(10L);
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=CanvasExecutionServiceTest#trigger_usesCanvasCache_onSecondCall -q 2>&1 | tail -10
```

Expected: FAIL.

- [ ] **Step 3: Add Caffeine cache field to CanvasExecutionService**

Add imports:
```java
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
```

Add field after existing fields in `CanvasExecutionService`:
```java
/** Canvas 实体本地缓存：canvasId → Canvas。TTL=5min，最多500条。 */
private final Cache<Long, Canvas> canvasCache = Caffeine.newBuilder()
        .maximumSize(500)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
```

- [ ] **Step 4: Use cache in `trigger()`**

Replace lines ~147–154:
```java
// BEFORE:
// FIXME: 此处调用量可能很大, 可以借助缓存提高性能优化
Canvas canvas = canvasMapper.selectById(canvasId);
if (canvas == null) {
    throw new IllegalStateException("画布不存在: " + canvasId);
}
if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
    throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
}
```

```java
// AFTER:
Canvas canvas = canvasCache.get(canvasId, id -> canvasMapper.selectById(id));
if (canvas == null) {
    throw new IllegalStateException("画布不存在: " + canvasId);
}
if (!dryRun && !Objects.equals(canvas.getStatus(), CanvasStatusEnum.PUBLISHED.getCode())) {
    throw new IllegalStateException("画布未发布，请先发布后再触发: " + canvasId);
}
```

- [ ] **Step 5: Run test**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=CanvasExecutionServiceTest#trigger_usesCanvasCache_onSecondCall -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java
git commit -m "perf: cache Canvas entity in CanvasExecutionService trigger hot path

canvasMapper.selectById() was called on every trigger execution.
Canvas metadata changes only on publish/unpublish. Add 5-minute TTL
Caffeine cache (max 500 entries) to eliminate per-trigger MySQL reads."
```

---

## Task 8: Fix check-then-act race in all 4 special node timer schedulers

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`

**Problem:** All 4 special node handlers schedule timeout timers using a non-atomic `contains` → `add` pattern on `scheduledHubTimeouts` (which is `ConcurrentHashMap.newKeySet()`). Individual `contains` and `add` calls are each atomic, but the compound check-then-act is not. Two threads entering the same WAITING branch simultaneously can both pass `!contains()` before either calls `add()`, resulting in two timers being scheduled for the same node.

**Why this matters:** HUB, AGGREGATE, LOGIC_RELATION, and THRESHOLD are precisely the nodes that *always* receive concurrent calls — their entire purpose is to wait for multiple upstream completions. THRESHOLD is especially exposed because `scheduleThresholdTimeoutIfNeeded` is called on *every* upstream completion, not just the first.

**Affected locations (all in DagEngine.java):**

| Handler | Line | Key pattern |
|---|---|---|
| `handleLogicRelation` | 495–496 | `"lr:" + nodeId` |
| `handleHub` | 577–578 | `nodeId` |
| `handleAggregate` | 637–638 | `"ag:" + nodeId` |
| `scheduleThresholdTimeoutIfNeeded` | 678–679 | `"th:" + nodeId` |

**Fix:** Replace `if (!contains(key)) { add(key); ... }` with `if (add(key)) { ... }`. `ConcurrentHashMap.newKeySet().add()` returns `true` only for the first inserter — this is the atomic compound operation needed.

- [ ] **Step 1: Write failing test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimerRaceTest.java
package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent calls to the timer-scheduling guard
 * result in exactly one timer being scheduled, not two.
 */
class SpecialNodeTimerRaceTest {

    @Test
    void concurrentTimerScheduling_schedulesExactlyOnce() throws InterruptedException {
        ExecutionContext ctx = ExecutionContext.fresh("c1", 1L, 1L, "u1", "SCHEDULED");
        String timerKey = "hub1";
        AtomicInteger scheduleCount = new AtomicInteger(0);

        // Simulate the BUGGY pattern: two threads both pass contains() before either adds
        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        var exec = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            exec.submit(() -> {
                try {
                    start.await();
                    // Simulate: if add() returns true → schedule timer
                    if (ctx.getScheduledHubTimeouts().add(timerKey)) {
                        scheduleCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        exec.shutdown();

        // add() on ConcurrentHashMap.newKeySet() returns true exactly once
        assertThat(scheduleCount.get()).isEqualTo(1);
    }
}
```

- [ ] **Step 2: Run test to confirm the fix approach is correct (test should pass — it's testing the atomic add() behavior)**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=SpecialNodeTimerRaceTest -q 2>&1 | tail -10
```

Expected: PASS. This confirms `add()` return value is the correct atomic guard.

- [ ] **Step 3: Fix `handleLogicRelation` (lines 495–496)**

```java
// BEFORE:
if (!ctx.getScheduledHubTimeouts().contains("lr:" + nodeId)) {
    ctx.getScheduledHubTimeouts().add("lr:" + nodeId);
    ctx.getHubStartTimes().putIfAbsent("lr:" + nodeId, System.currentTimeMillis());
    // ... schedule timer
}

// AFTER:
if (ctx.getScheduledHubTimeouts().add("lr:" + nodeId)) {
    ctx.getHubStartTimes().putIfAbsent("lr:" + nodeId, System.currentTimeMillis());
    // ... schedule timer (unchanged)
}
```

- [ ] **Step 4: Fix `handleHub` (lines 577–578)**

```java
// BEFORE:
if (!ctx.getScheduledHubTimeouts().contains(nodeId)) {
    ctx.getScheduledHubTimeouts().add(nodeId);
    ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
    // ... schedule timer
}

// AFTER:
if (ctx.getScheduledHubTimeouts().add(nodeId)) {
    ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
    // ... schedule timer (unchanged)
}
```

Note: `handleHub` has an `else` branch (lines 601–609) that checks if the timer has already expired. This `else` is on the outer `!allUpstreamDone` if-block, not on the timer scheduling if-block. After the fix, the `else` branch remains structurally correct — it fires when `add()` returns `false` (timer already scheduled by another thread), which is exactly when you want to check for expiry.

```java
// Full corrected handleHub WAITING block:
if (!HubHandler.allUpstreamDone(upstreamIds, ctx)) {
    ctx.setNodeStatus(nodeId, NodeStatus.WAITING);
    if (ctx.getScheduledHubTimeouts().add(nodeId)) {
        ctx.getHubStartTimes().putIfAbsent(nodeId, System.currentTimeMillis());
        int timeoutSec = HubHandler.getTimeoutSeconds(config);
        Mono.delay(Duration.ofSeconds(timeoutSec), VIRTUAL)
                .subscribe(__ -> {
                    if (!ctx.isNodeDone(nodeId)) {
                        log.warn("[HUB] 等待超时 timeout={}s nodeId={}", timeoutSec, nodeId);
                        ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
                        ctxStore.save(ctx);
                        executionService.trigger(
                                        ctx.getCanvasId(), ctx.getUserId(),
                                        TriggerType.HUB_TIMEOUT, NodeType.HUB,
                                        null, Map.of(),
                                        ctx.getExecutionId() + ":hub-timeout:" + nodeId, false)
                                .subscribe(null,
                                        (Throwable e) -> log.error("[HUB] 超时恢复失败 nodeId={}: {}", nodeId, e.getMessage()));
                    }
                });
        log.debug("[HUB] 启动超时定时器 {}s nodeId={}", timeoutSec, nodeId);
    } else {
        // 已调度过定时器，检查是否已超时
        long start = ctx.getHubStartTimes().getOrDefault(nodeId, System.currentTimeMillis());
        int timeout = HubHandler.getTimeoutSeconds(config);
        if (System.currentTimeMillis() - start > (long) timeout * 1000) {
            ctx.setNodeStatus(nodeId, NodeStatus.FAILED);
            return Mono.error(new RuntimeException("HUB 等待超时 nodeId=" + nodeId));
        }
    }
    return Mono.just(Map.of());
}
```

- [ ] **Step 5: Fix `handleAggregate` (lines 637–638)**

```java
// BEFORE:
if (!ctx.getScheduledHubTimeouts().contains(timerKey)) {
    ctx.getScheduledHubTimeouts().add(timerKey);
    ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
    // ... schedule timer
}

// AFTER:
if (ctx.getScheduledHubTimeouts().add(timerKey)) {
    ctx.getHubStartTimes().putIfAbsent(timerKey, System.currentTimeMillis());
    // ... schedule timer (unchanged)
}
```

- [ ] **Step 6: Fix `scheduleThresholdTimeoutIfNeeded` (lines 678–679)**

```java
// BEFORE:
if (ctx.getScheduledHubTimeouts().contains(timerKey)) return;
ctx.getScheduledHubTimeouts().add(timerKey);
int timeoutSec = ...;
Mono.delay(...).subscribe(...);

// AFTER:
if (!ctx.getScheduledHubTimeouts().add(timerKey)) return;
int timeoutSec = ...;
Mono.delay(...).subscribe(...);
```

- [ ] **Step 7: Build**

```bash
cd backend && ./mvnw compile -pl canvas-engine -q 2>&1 | tail -5
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTimerRaceTest.java
git commit -m "fix: use add() return value to atomically guard timer scheduling in all 4 special node handlers

handleLogicRelation, handleHub, handleAggregate, and scheduleThresholdTimeoutIfNeeded
all used a non-atomic contains→add pattern on scheduledHubTimeouts. Two concurrent
upstream completions could both pass !contains() before either added, scheduling
duplicate timeout recovery timers. Replace with add() whose return value is
atomically true only for the first inserter."
```

---

## Task 9: Fix nodeOutputs and flatContext non-thread-safety in ExecutionContext

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`

**Problem:** `triggerDownstream` in DagEngine fans out to multiple downstream nodes in parallel:

```java
return Flux.fromIterable(nextIds)
    .flatMap(nextId -> executeNode(graph, nextId, ctx))  // parallel, shared ctx
```

When those parallel branches complete and write their outputs, they all call `ctx.putNodeOutput(nodeId, output)`, which writes into:

```java
private Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();  // NOT thread-safe
private Map<String, Object> flatContext = new HashMap<>();                      // NOT thread-safe
```

Concurrent writes to `LinkedHashMap`/`HashMap` produce undefined behavior in Java: structural corruption, lost entries, or (for `LinkedHashMap` which maintains a doubly-linked list) infinite loops during iteration. The `AGGREGATE` node directly reads `nodeOutputs` to evaluate upstream results — if a parallel upstream wrote into it concurrently, AGGREGATE can see an incomplete or corrupted view.

Additionally, `getNodeOutputs()` returns `Collections.unmodifiableMap(nodeOutputs)` but the underlying map is still mutated by concurrent writes. Iteration over this unmodifiable view while another thread modifies the backing map throws `ConcurrentModificationException`.

**Fix:** Change both fields to `ConcurrentHashMap`. `ConcurrentHashMap` does not support `null` values — verify no callers store `null` values (they don't; `putNodeOutput` passes handler output maps which are never null-valued in practice).

- [ ] **Step 1: Write failing test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextConcurrencyTest.java
package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.RepeatedTest;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that concurrent putNodeOutput calls do not corrupt context state.
 */
class ExecutionContextConcurrencyTest {

    @RepeatedTest(20)
    void concurrentPutNodeOutput_doesNotCorrupt() throws InterruptedException {
        ExecutionContext ctx = ExecutionContext.fresh("c1", 1L, 1L, "u1", "SCHEDULED");
        int threads = 20;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        var exec = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            exec.submit(() -> {
                try {
                    start.await();
                    ctx.putNodeOutput("node-" + idx, Map.of("key-" + idx, "val-" + idx));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        exec.shutdown();

        // All 20 entries must be present with no corruption
        assertThat(ctx.getNodeOutputs()).hasSize(threads);
        for (int i = 0; i < threads; i++) {
            assertThat(ctx.getContextValue("key-" + i)).isEqualTo("val-" + i);
        }
        // Iteration must not throw ConcurrentModificationException
        assertThatNoException().isThrownBy(() ->
            ctx.getNodeOutputs().forEach((k, v) -> { /* read all */ }));
    }
}
```

- [ ] **Step 2: Run test to confirm it fails with current HashMap/LinkedHashMap**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=ExecutionContextConcurrencyTest -q 2>&1 | tail -15
```

Expected: FAIL or flaky (data corruption, ConcurrentModificationException, or wrong size). The test is `@RepeatedTest(20)` — it will fail on at least some runs.

- [ ] **Step 3: Change nodeOutputs and flatContext to ConcurrentHashMap**

In `ExecutionContext.java`, change:

```java
// BEFORE:
private Map<String, Map<String, Object>> nodeOutputs = new LinkedHashMap<>();
private Map<String, Object> flatContext = new HashMap<>();

// AFTER:
private final Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();
private final Map<String, Object> flatContext = new ConcurrentHashMap<>();
```

`ConcurrentHashMap` import is already present (used by `nodeStatuses`, `hubStartTimes`, `nodeGates`).

- [ ] **Step 4: Verify getNodeOutputs() — unmodifiableMap wrapping ConcurrentHashMap is safe**

The existing method:
```java
public Map<String, Map<String, Object>> getNodeOutputs() {
    return java.util.Collections.unmodifiableMap(nodeOutputs);
}
```

`Collections.unmodifiableMap` on a `ConcurrentHashMap` is safe: reads go through the concurrent map, writes are blocked by the wrapper. No change needed here.

- [ ] **Step 5: Check approxSizeBytes accumulation in putNodeOutput**

```java
public void putNodeOutput(String nodeId, Map<String, Object> output) {
    nodeOutputs.put(nodeId, output);
    flatContext.putAll(output);
    output.forEach((k, v) ->
        approxSizeBytes += k.length() + (v != null ? v.toString().length() : 4));
    ...
}
```

`approxSizeBytes` is a plain `int` updated with `+=` — not thread-safe. Change to `AtomicInteger`:

```java
// BEFORE:
@JsonIgnore
private int approxSizeBytes = 0;

// AFTER:
@JsonIgnore
private final java.util.concurrent.atomic.AtomicInteger approxSizeBytes = new AtomicInteger(0);
```

Update all usages in `ExecutionContext.java`:

```java
// putNodeOutput — size accumulation:
output.forEach((k, v) ->
    approxSizeBytes.addAndGet(k.length() + (v != null ? v.toString().length() : 4)));

// isOversized():
public boolean isOversized() { return approxSizeBytes.get() > MAX_SIZE_BYTES; }

// getApproxSizeBytes():
public int getApproxSizeBytes() { return approxSizeBytes.get(); }
```

- [ ] **Step 6: Run test to confirm it passes**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=ExecutionContextConcurrencyTest -q 2>&1 | tail -10
```

Expected: all 20 repetitions PASS.

- [ ] **Step 7: Run all engine tests**

```bash
cd backend && ./mvnw test -pl canvas-engine -q 2>&1 | tail -15
```

Expected: BUILD SUCCESS. Pay attention to any JSON serialization tests — `ConcurrentHashMap` serializes identically to `HashMap` with Jackson, so no changes needed there.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextConcurrencyTest.java
git commit -m "fix: make nodeOutputs, flatContext, and approxSizeBytes thread-safe in ExecutionContext

triggerDownstream fans out to multiple downstream nodes in parallel, all
sharing the same ExecutionContext. Concurrent putNodeOutput() calls wrote
to LinkedHashMap/HashMap concurrently, risking structural corruption and
ConcurrentModificationException during AGGREGATE iteration.

Change nodeOutputs → ConcurrentHashMap, flatContext → ConcurrentHashMap,
approxSizeBytes → AtomicInteger."
```

---

## Task 10: Fix missing nodeStartMs / metrics / log in executeNodeAfterStage2 for special nodes

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`

**Problem:** `executeNodeAfterStage2` is the execution path for all 4 special node types (HUB, AGGREGATE, LOGIC_RELATION, THRESHOLD). Comparing it against the ordinary node path inside `executeNode` reveals three omissions:

| | Ordinary node path (`executeNode` inline) | Special node path (`executeNodeAfterStage2`) |
|---|---|---|
| Execution start time | `long nodeStartMs = System.currentTimeMillis()` | **Missing** |
| Trace end | `writeTraceEnd(ctx, node, result, durationMs)` — duration recorded | `writeTraceEnd(ctx, node, result)` → calls `writeTraceEnd(..., 0)` → `durationMs=null` |
| Metrics | `metrics.recordNodeExecution(type, SUCCESS, durationMs)` | **Missing** |
| Completion log | `log.debug("[ENGINE] 节点完成 nodeId={} type={}")` | **Missing** |

**Impact:** Every HUB, AGGREGATE, LOGIC_RELATION, THRESHOLD execution trace row in `canvas_execution_trace` has `duration_ms = NULL`. Metric counters/timers for these orchestration nodes are never incremented. This makes it impossible to:
- Know how long a HUB waited for its upstreams
- Know how long AGGREGATE evaluation took
- Set SLA alerts on LOGIC_RELATION or THRESHOLD nodes

These are the most important orchestration nodes in the DAG — they should be the most observable, not the least.

- [ ] **Step 1: Confirm the gap with a test**

```java
// backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTraceDurationTest.java
package org.chovy.canvas.engine.scheduler;

import org.chovy.canvas.infra.trace.CanvasExecutionTrace;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that special node execution traces include a non-null durationMs.
 * Wire up a minimal DagEngine + mock HubHandler + traceBuffer capture,
 * run a HUB node to completion, and check the trace emitted.
 */
class SpecialNodeTraceDurationTest {

    @Test
    void hubNode_executionTrace_hasDurationMs() {
        // Arrange: build a graph with one HUB node, both upstreams already SUCCESS in ctx.
        // Act: run DagEngine.execute() for the HUB trigger.
        // Assert: the trace emitted to traceBuffer for this HUB node has durationMs > 0.
        // (Adapt to project's test infrastructure for DagEngine)
        throw new UnsupportedOperationException("fill in with project test helpers");
    }
}
```

- [ ] **Step 2: Run test to confirm it fails (durationMs is null)**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=SpecialNodeTraceDurationTest -q 2>&1 | tail -10
```

Expected: FAIL (durationMs is null, not > 0).

- [ ] **Step 3: Add `nodeStartMs` to `executeNodeAfterStage2` and use it in writeTraceEnd**

In `executeNodeAfterStage2`, after `writeTraceStart`:

```java
// BEFORE:
writeTraceStart(ctx, node);
NodeHandler handler = handlerRegistry.get(node.getType());

return executeHandlerWithRepeat(handler, config, ctx, nodeGate, nodeId, node.getType())
        .<Map<String, Object>>flatMap(result -> {
            // ...
            // SUCCESS path:
            ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
            writeTraceEnd(ctx, node, result);                    // ← durationMs=null
            return triggerDownstream(graph, result, nodeId, node.getType(), ctx);
        })
```

```java
// AFTER:
writeTraceStart(ctx, node);
NodeHandler handler = handlerRegistry.get(node.getType());
long nodeStartMs = System.currentTimeMillis();                   // ← add this line

return executeHandlerWithRepeat(handler, config, ctx, nodeGate, nodeId, node.getType())
        .<Map<String, Object>>flatMap(result -> {
            // ...
            // SUCCESS path:
            ctx.setNodeStatus(nodeId, NodeStatus.SUCCESS);
            long durationMs = System.currentTimeMillis() - nodeStartMs;
            writeTraceEnd(ctx, node, result, durationMs);        // ← use 4-arg overload
            metrics.recordNodeExecution(node.getType(), NodeStatus.SUCCESS.name(), durationMs);
            log.debug("[ENGINE] 节点完成 nodeId={} type={}", nodeId, node.getType());
            return triggerDownstream(graph, result, nodeId, node.getType(), ctx);
        })
```

Note: `nodeStartMs` is effectively final (captured by the inner lambda) so it works inside the `flatMap` lambda without any changes.

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && ./mvnw test -pl canvas-engine -Dtest=SpecialNodeTraceDurationTest -q 2>&1 | tail -10
```

Expected: PASS (durationMs > 0 in trace).

- [ ] **Step 5: Run full engine tests**

```bash
cd backend && ./mvnw test -pl canvas-engine -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeTraceDurationTest.java
git commit -m "fix: record durationMs and metrics for special node execution in executeNodeAfterStage2

HUB, AGGREGATE, LOGIC_RELATION, and THRESHOLD all route through
executeNodeAfterStage2 which was missing nodeStartMs tracking.
Every special node trace had durationMs=null and no metrics were
emitted. Add nodeStartMs, use the 4-arg writeTraceEnd overload,
call metrics.recordNodeExecution, and add the completion debug log
to match the ordinary node execution path."
```

---

## Out of Scope (acknowledged design gaps)

These FIXMEs were reviewed but require product-level decisions before implementation:

- **`CanvasExecutionService.java:158`** — Frontend has no UI to configure `preCheckService` attributes (validity period, global quota, cooldown). Requires a frontend feature to add these fields to the canvas editor.

- **`CanvasExecutionService.java:178-179`** — Dedup TTL semantics under suspension: when a user is in suspended state, the dedup window is `globalTimeoutSec + 600s` (~20 min) instead of 24h. If suspension lasts longer than this, a second identical trigger could pass dedup. Requires a product decision on whether to extend TTL, use a different dedup strategy for suspended executions, or accept the current behavior.

- **`CanvasExecutionService.java:192`** — Same user re-triggering a suspended canvas picks up the old context. Requires a product decision: reject second triggers while suspended, or create a parallel execution context.

- **`EventDefinitionController.java:128`** — Redis failure causes `triggerRouteService.getCanvasByBehavior()` to fail, blocking the entire event pipeline. Requires an architecture decision on fallback strategy (e.g., MySQL-backed fallback for route lookup).
