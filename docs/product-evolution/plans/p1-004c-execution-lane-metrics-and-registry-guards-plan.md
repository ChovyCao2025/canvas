# Execution Lane Metrics And Registry Guards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Instrument runtime admission and lane behavior so 3000 hardening can observe registry availability, lane active counts, retry backlog, and protected lane routing.

**Architecture:** Extend existing Spring Boot runtime metrics around `InFlightExecutionRegistry`, backlog metric refresh, and `ExecutionLaneResolver` tests without changing lane budgets. Registry failures remain conservative rejections and are now observable.

**Tech Stack:** Java 21, Spring Boot, Micrometer, Redis, Maven, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-004c-execution-lane-metrics-and-registry-guards.md`
- Source: `docs/optimization/3000-concurrency-hardening-checklist.md`

## File Structure

**Backend runtime metrics and guards**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java` - adds registry admission, registry latency, active lane, and trace backlog metrics.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java` - records admission outcome and lane active gauges.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java` - keeps retry backlog metric publication covered.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java` - keeps retry precedence and lane mapping behavior.
- Modify: `backend/canvas-engine/src/main/resources/application.yml` - confirms 3000 lane budgets.

**Tests**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`

### Task 1: Registry Admission Metrics

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`

- [x] **Step 1: Add registry metric tests**

Modify `InFlightExecutionRegistryLaneTest` by adding:

```java
@Mock
CanvasMetrics metrics;
```

Update the helper:

```java
private InFlightExecutionRegistry registry() {
    InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys, metrics);
    ReflectionTestUtils.setField(registry, "globalTimeoutSec", 600L);
    return registry;
}
```

Add tests:

```java
@Test
void redisFailureRecordsRegistryUnavailableMetric() {
    InFlightExecutionRegistry registry = registry();
    when(redis.execute(any(RedisScript.class), anyList(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new IllegalStateException("redis down"));

    ExecutionLaneAdmissionResult result = registry.tryAcquire(
            10L, "exec-1", ExecutionLane.LIGHT, 3000, 600, 3000);

    assertThat(result.allowed()).isFalse();
    assertThat(result.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE);
    verify(metrics).recordExecutionRegistryAdmission("LIGHT", "REGISTRY_UNAVAILABLE");
}

@Test
void successfulAcquireRecordsLaneActiveGauge() {
    InFlightExecutionRegistry registry = registry();
    when(redis.execute(any(RedisScript.class), anyList(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(1L);

    ExecutionLaneAdmissionResult result = registry.tryAcquire(
            10L, "exec-1", ExecutionLane.HEAVY, 3000, 300, 3000);

    assertThat(result.allowed()).isTrue();
    verify(metrics).recordExecutionRegistryAdmission("HEAVY", "NONE");
    verify(metrics).setExecutionLaneActive("HEAVY", 1L);
}
```

- [x] **Step 2: Run registry tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest
```

Expected: FAIL because `CanvasMetrics` does not expose registry methods and `InFlightExecutionRegistry` does not inject metrics.

Observed: FAIL on 2026-06-05 because `CanvasMetrics` did not expose `recordExecutionRegistryAdmission` or `setExecutionLaneActive`, and `InFlightExecutionRegistry` still had the two-argument constructor.

- [x] **Step 3: Add CanvasMetrics registry methods**

Modify `CanvasMetrics` imports and fields:

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

private final ConcurrentMap<String, AtomicLong> executionLaneActive = new ConcurrentHashMap<>();
private final ConcurrentMap<String, AtomicLong> traceBufferBacklog = new ConcurrentHashMap<>();
```

Add methods:

```java
public void recordExecutionRegistryAdmission(String lane, String reason) {
    Counter.builder("canvas.execution.registry.admission.total")
            .tag("lane", lane != null ? lane : "UNKNOWN")
            .tag("reason", reason != null ? reason : "UNKNOWN")
            .register(registry)
            .increment();
}

public void recordExecutionRegistryLatency(long latencyMs) {
    Timer.builder("canvas.execution.registry.latency")
            .publishPercentiles(0.95, 0.99)
            .register(registry)
            .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
}

public void setExecutionLaneActive(String lane, long count) {
    String normalizedLane = lane != null ? lane : "UNKNOWN";
    AtomicLong gauge = executionLaneActive.computeIfAbsent(normalizedLane, key -> {
        AtomicLong value = new AtomicLong();
        Gauge.builder("canvas.execution.lane.active", value, AtomicLong::get)
                .tag("lane", key)
                .register(registry);
        return value;
    });
    gauge.set(Math.max(0L, count));
}

public void setTraceBufferBacklog(String buffer, long count) {
    String normalizedBuffer = buffer != null ? buffer : "default";
    AtomicLong gauge = traceBufferBacklog.computeIfAbsent(normalizedBuffer, key -> {
        AtomicLong value = new AtomicLong();
        Gauge.builder("canvas.trace.buffer.backlog", value, AtomicLong::get)
                .tag("buffer", key)
                .register(registry);
        return value;
    });
    gauge.set(Math.max(0L, count));
}
```

- [x] **Step 4: Record registry metrics in admission**

Modify `InFlightExecutionRegistry` constructor state:

```java
private final CanvasMetrics metrics;

public InFlightExecutionRegistry(StringRedisTemplate redis, CanvasRedisKeyProperties keys, CanvasMetrics metrics) {
    this.redis = redis;
    this.keys = keys;
    this.metrics = metrics;
}
```

Wrap Redis acquire timing in `tryAcquire`:

```java
long startNs = System.nanoTime();
try {
    result = redis.execute(...);
    metrics.recordExecutionRegistryLatency(
            java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs));
} catch (Exception e) {
    metrics.recordExecutionRegistryAdmission(effectiveLane.name(), "REGISTRY_UNAVAILABLE");
    return ExecutionLaneAdmissionResult.rejected(
            ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE,
            activeCount(canvasId),
            laneActiveCount(effectiveLane),
            totalActiveCount());
}
```

After a Redis rejection is mapped:

```java
ExecutionLaneAdmissionResult.Reason reason = mapReason(result);
metrics.recordExecutionRegistryAdmission(effectiveLane.name(), reason.name());
metrics.setExecutionLaneActive(effectiveLane.name(), laneActiveCount(effectiveLane));
return ExecutionLaneAdmissionResult.rejected(
        reason,
        activeCount(canvasId),
        laneActiveCount(effectiveLane),
        totalActiveCount());
```

After successful local registration:

```java
metrics.recordExecutionRegistryAdmission(effectiveLane.name(), "NONE");
metrics.setExecutionLaneActive(effectiveLane.name(), laneActiveCount(effectiveLane));
```

- [x] **Step 5: Run registry tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest
```

Expected: PASS with Redis failure and success metrics covered.

Observed: PASS on 2026-06-05 as part of focused backend metric and lane tests.

### Task 2: Backlog And Lane Resolver Coverage

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`

- [x] **Step 1: Add backlog metric test for retry pressure**

Modify `CanvasExecutionRequestBacklogMetricsTest`:

```java
@Test
void refreshPublishesRetryBacklogPressure() {
    CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
    CanvasMetrics metrics = mock(CanvasMetrics.class);
    CanvasExecutionRequestBacklogMetrics backlogMetrics =
            new CanvasExecutionRequestBacklogMetrics(mapper, metrics);
    when(mapper.countByStatus()).thenReturn(List.of(
            new CanvasExecutionRequestStatusCount(CanvasExecutionRequestStatus.RETRY, 30L)
    ));

    backlogMetrics.refresh();

    verify(metrics).setExecutionRequestBacklog(CanvasExecutionRequestStatus.RETRY, 30L);
}
```

- [x] **Step 2: Add exhaustive lane resolver matrix**

Modify `ExecutionLaneResolverTest`:

```java
@Test
void resolvesHeavyLaneForChecklistHeavyWork() {
    assertThat(resolver.resolve(TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.DLQ_REPLAY, NodeType.MQ_TRIGGER, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.EVENT, NodeType.GROOVY, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.EVENT, NodeType.TAGGER, Map.of("mode", "audience"), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
    assertThat(resolver.resolve(TriggerType.SUB_FLOW_REF, NodeType.SUB_FLOW_REF, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.HEAVY);
}

@Test
void resolvesLightLaneForDirectAndInternalContinuationWork() {
    assertThat(resolver.resolve(TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.WAIT_RESUME, NodeType.WAIT, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.HUB_TIMEOUT, NodeType.HUB, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.AGGREGATE_TIMEOUT, NodeType.AGGREGATE, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
    assertThat(resolver.resolve(TriggerType.THRESHOLD_TIMEOUT, NodeType.THRESHOLD, Map.of(), false, false, 0))
            .isEqualTo(ExecutionLane.LIGHT);
}

@Test
void resolvesRetryLaneForOverflowAndPersistentRequestRetryBeforeOtherRules() {
    assertThat(resolver.resolve(TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, Map.of(), true, false, 0))
            .isEqualTo(ExecutionLane.RETRY);
    assertThat(resolver.resolve(TriggerType.SCHEDULED, NodeType.SCHEDULED_TRIGGER, Map.of(), false, true, 2))
            .isEqualTo(ExecutionLane.RETRY);
}
```

- [x] **Step 3: Confirm 3000 lane config remains explicit**

Confirm `backend/canvas-engine/src/main/resources/application.yml` contains:

```yaml
canvas:
  execution:
    max-concurrency: 3000
  execution-lane:
    light:
      max-concurrency: 600
    standard:
      max-concurrency: 1800
    heavy:
      max-concurrency: 300
    retry:
      max-concurrency: 300
```

If these values already match, leave the file unchanged and mention the confirmation in the implementation PR.

Observed: `application.yml` already contains `max-concurrency: 3000` with LIGHT `600`, STANDARD `1800`, HEAVY `300`, and RETRY `300`; file left unchanged.

- [x] **Step 4: Run backend metric and lane tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest,CanvasExecutionRequestBacklogMetricsTest,ExecutionLaneResolverTest
```

Expected: PASS with registry admission, backlog metric, and lane resolver matrix coverage.

Observed: PASS on 2026-06-05, 12 tests passed.

### Task 3: Verification And Commit

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`
- Modify: `docs/product-evolution/specs/p1-004c-execution-lane-metrics-and-registry-guards.md`
- Modify: `docs/product-evolution/plans/p1-004c-execution-lane-metrics-and-registry-guards-plan.md`

- [x] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest,CanvasExecutionRequestBacklogMetricsTest,ExecutionLaneResolverTest
```

Expected: PASS.

Observed: PASS on 2026-06-05:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryLaneTest,CanvasExecutionRequestBacklogMetricsTest,ExecutionLaneResolverTest -DfailIfNoTests=true
```

Result: 12 tests, 0 failures, 0 errors, 0 skipped.

Additional regression check:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=InFlightExecutionRegistryConcurrencyTest,CanvasMetricsTest -DfailIfNoTests=true
```

Result: 4 tests, 0 failures, 0 errors, 0 skipped.

- [ ] **Step 2: Commit implementation slice**

Run:

```bash
git add \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetrics.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java \
  docs/product-evolution/specs/p1-004c-execution-lane-metrics-and-registry-guards.md \
  docs/product-evolution/plans/p1-004c-execution-lane-metrics-and-registry-guards-plan.md
git commit -m "test: instrument execution lane registry guards"
```

Expected: commit contains only registry metric, backlog metric, lane resolver coverage, config confirmation, and related docs.
