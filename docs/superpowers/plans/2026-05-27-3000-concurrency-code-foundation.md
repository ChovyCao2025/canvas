# 3000 Concurrency Code Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the code-controlled foundation needed for 3000 cluster-level Canvas execution concurrency: lane budgets, lane resolution, lane-aware Redis ZSET admission, service wiring, flow routing, and metrics.

**Architecture:** Keep the existing WebFlux + RocketMQ + Disruptor + DAG execution model. Extend the current `InFlightExecutionRegistry` from canvas/global admission to canvas/lane/global admission, then wire `CanvasExecutionService` to resolve `LIGHT`, `STANDARD`, `HEAVY`, and `RETRY` lanes before acquiring execution slots. This plan does not physically split Redis clusters or rewrite the execution engine into multiple services.

**Tech Stack:** Java 21, Spring Boot WebFlux, Spring Boot configuration properties, Redis `StringRedisTemplate`, Redis Lua scripts, RocketMQ, Disruptor, Micrometer, JUnit 5, AssertJ, Mockito.

---

## Scope

This plan implements the first code foundation for the 3000 target. It covers:
- typed lane configuration and validation
- lane model and resolver
- Redis key expansion
- lane-aware in-flight registry
- `CanvasExecutionService` admission wiring
- Disruptor and MQ lane metadata
- lane metrics
- focused tests for these changes

This plan intentionally does not implement:
- physical Redis role separation
- physical RocketMQ broker provisioning
- application instance sizing
- downstream service capacity expansion
- full production load test execution

Those remain rollout and infrastructure tasks in `docs/superpowers/plans/2026-05-25-2000-concurrency.md`.

---

## File Structure

### Existing files to modify

- `backend/canvas-engine/src/main/resources/application.yml`
  - Set global concurrency target to 3000 and add lane budget configuration.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/MapFieldKeys.java`
  - Add lane/admission response keys.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
  - Add lane in-flight Redis keys.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
  - Add lane-aware acquire/release and typed rejection result.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
  - Resolve lane and acquire lane-aware slots.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasExecutionEvent.java`
  - Carry lane metadata and reset it.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
  - Publish lane metadata into events.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`
  - Mark request retry execution as retry-lane eligible.
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
  - Add global/lane admission and active metrics.

### New files to create

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLane.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneRequest.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneAdmissionResult.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLaneProperties.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLanePropertiesValidator.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/mq/ExecutionTopicRouter.java`

### Tests to create or update

- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ExecutionLanePropertiesTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtilLaneTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceLaneTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/mq/ExecutionTopicRouterTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CanvasMetricsTest.java`

---

## Task 1: Add lane configuration and validation

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLane.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLaneProperties.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLanePropertiesValidator.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ExecutionLanePropertiesTest.java`

- [ ] **Step 1: Write failing config binding tests**

Create `ExecutionLanePropertiesTest.java`:

```java
package org.chovy.canvas.config;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionLanePropertiesTest {

    @Test
    void applicationYamlBinds3000LaneBudgets() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        MutablePropertySources propertySources = new MutablePropertySources();
        for (PropertySource<?> source : loader.load("application", new ClassPathResource("application.yml"))) {
            propertySources.addLast(source);
        }

        Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
        ExecutionLaneProperties properties = binder
                .bind("canvas.execution-lane", Bindable.of(ExecutionLaneProperties.class))
                .orElseThrow(() -> new AssertionError("canvas.execution-lane should bind from application.yml"));

        assertThat(properties.limitFor(ExecutionLane.LIGHT)).isEqualTo(600);
        assertThat(properties.limitFor(ExecutionLane.STANDARD)).isEqualTo(1800);
        assertThat(properties.limitFor(ExecutionLane.HEAVY)).isEqualTo(300);
        assertThat(properties.limitFor(ExecutionLane.RETRY)).isEqualTo(300);
        assertThat(properties.totalMaxConcurrency()).isEqualTo(3000);
    }

    @Test
    void validatorRejectsLaneTotalAboveGlobalLimit() {
        ExecutionLaneProperties properties = new ExecutionLaneProperties();
        properties.getStandard().setMaxConcurrency(2500);

        ExecutionLanePropertiesValidator validator = new ExecutionLanePropertiesValidator(properties, 3000);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("lane total");
    }

    @Test
    void validatorRejectsNonPositiveLaneLimit() {
        ExecutionLaneProperties properties = new ExecutionLaneProperties();
        properties.getHeavy().setMaxConcurrency(0);

        ExecutionLanePropertiesValidator validator = new ExecutionLanePropertiesValidator(properties, 3000);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("heavy.max-concurrency");
    }
}
```

- [ ] **Step 2: Run config tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionLanePropertiesTest test
```

Expected: FAIL because `ExecutionLane`, `ExecutionLaneProperties`, and `ExecutionLanePropertiesValidator` do not exist.

- [ ] **Step 3: Add `ExecutionLane.java`**

```java
package org.chovy.canvas.engine.lane;

public enum ExecutionLane {
    LIGHT,
    STANDARD,
    HEAVY,
    RETRY
}
```

- [ ] **Step 4: Add `ExecutionLaneProperties.java`**

```java
package org.chovy.canvas.config;

import lombok.Data;
import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution-lane")
public class ExecutionLaneProperties {

    private Lane light = new Lane(600, 2000);
    private Lane standard = new Lane(1800, 10000);
    private Lane heavy = new Lane(300, 1000);
    private Lane retry = new Lane(300, 3000);

    public int limitFor(ExecutionLane lane) {
        return laneConfig(lane).getMaxConcurrency();
    }

    public int queueLimitFor(ExecutionLane lane) {
        return laneConfig(lane).getQueueLimit();
    }

    public int totalMaxConcurrency() {
        return light.maxConcurrency + standard.maxConcurrency + heavy.maxConcurrency + retry.maxConcurrency;
    }

    public Lane laneConfig(ExecutionLane lane) {
        return switch (lane) {
            case LIGHT -> light;
            case STANDARD -> standard;
            case HEAVY -> heavy;
            case RETRY -> retry;
        };
    }

    @Data
    public static class Lane {
        private int maxConcurrency;
        private int queueLimit;

        public Lane() {
        }

        public Lane(int maxConcurrency, int queueLimit) {
            this.maxConcurrency = maxConcurrency;
            this.queueLimit = queueLimit;
        }
    }
}
```

- [ ] **Step 5: Add `ExecutionLanePropertiesValidator.java`**

```java
package org.chovy.canvas.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExecutionLanePropertiesValidator {

    private final ExecutionLaneProperties properties;
    private final int globalMaxConcurrency;

    public ExecutionLanePropertiesValidator(
            ExecutionLaneProperties properties,
            @Value("${canvas.execution.max-concurrency:3000}") int globalMaxConcurrency) {
        this.properties = properties;
        this.globalMaxConcurrency = globalMaxConcurrency;
    }

    @PostConstruct
    public void validate() {
        requirePositive("light.max-concurrency", properties.getLight().getMaxConcurrency());
        requirePositive("standard.max-concurrency", properties.getStandard().getMaxConcurrency());
        requirePositive("heavy.max-concurrency", properties.getHeavy().getMaxConcurrency());
        requirePositive("retry.max-concurrency", properties.getRetry().getMaxConcurrency());
        requirePositive("light.queue-limit", properties.getLight().getQueueLimit());
        requirePositive("standard.queue-limit", properties.getStandard().getQueueLimit());
        requirePositive("heavy.queue-limit", properties.getHeavy().getQueueLimit());
        requirePositive("retry.queue-limit", properties.getRetry().getQueueLimit());

        int laneTotal = properties.totalMaxConcurrency();
        if (laneTotal > globalMaxConcurrency) {
            throw new IllegalStateException(
                    "canvas.execution-lane lane total " + laneTotal
                            + " must be less than or equal to canvas.execution.max-concurrency "
                            + globalMaxConcurrency);
        }
    }

    private void requirePositive(String property, int value) {
        if (value <= 0) {
            throw new IllegalStateException("canvas.execution-lane." + property + " must be greater than 0");
        }
    }
}
```

- [ ] **Step 6: Update `application.yml`**

Set the global target and lane budgets:

```yml
canvas:
  execution:
    max-concurrency: 3000
  execution-lane:
    light:
      max-concurrency: 600
      queue-limit: 2000
    standard:
      max-concurrency: 1800
      queue-limit: 10000
    heavy:
      max-concurrency: 300
      queue-limit: 1000
    retry:
      max-concurrency: 300
      queue-limit: 3000
```

Keep the existing `canvas.execution` fields under the same YAML object; do not duplicate the top-level `canvas:` key.

- [ ] **Step 7: Run config tests and verify they pass**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionLanePropertiesTest test
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLane.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLaneProperties.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/config/ExecutionLanePropertiesValidator.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ExecutionLanePropertiesTest.java \
  backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: add execution lane capacity configuration"
```

---

## Task 2: Add lane resolver

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneRequest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java`

- [ ] **Step 1: Write failing resolver tests**

```java
package org.chovy.canvas.engine.lane;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionLaneResolverTest {

    private final ExecutionLaneResolver resolver = new ExecutionLaneResolver();

    @Test
    void resolvesRetryWhenOverflowRetryIsTrue() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.MQ, NodeType.MQ_TRIGGER, true, false, false, false));

        assertThat(lane).isEqualTo(ExecutionLane.RETRY);
    }

    @Test
    void resolvesRetryWhenRequestRetryIsTrue() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.EVENT, NodeType.EVENT_TRIGGER, false, true, false, false));

        assertThat(lane).isEqualTo(ExecutionLane.RETRY);
    }

    @Test
    void resolvesHeavyForAudienceTrigger() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.MQ, NodeType.AUDIENCE_TRIGGER, false, false, false, false));

        assertThat(lane).isEqualTo(ExecutionLane.HEAVY);
    }

    @Test
    void resolvesHeavyForGroovyHint() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.EVENT, NodeType.GROOVY, false, false, true, false));

        assertThat(lane).isEqualTo(ExecutionLane.HEAVY);
    }

    @Test
    void resolvesLightForDirectCallLightHint() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.DIRECT_CALL, NodeType.DIRECT_CALL, false, false, false, true));

        assertThat(lane).isEqualTo(ExecutionLane.LIGHT);
    }

    @Test
    void resolvesStandardByDefault() {
        ExecutionLane lane = resolver.resolve(new ExecutionLaneRequest(
                TriggerType.EVENT, NodeType.EVENT_TRIGGER, false, false, false, false));

        assertThat(lane).isEqualTo(ExecutionLane.STANDARD);
    }
}
```

- [ ] **Step 2: Run resolver tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionLaneResolverTest test
```

Expected: FAIL because resolver classes do not exist.

- [ ] **Step 3: Add `ExecutionLaneRequest.java`**

```java
package org.chovy.canvas.engine.lane;

public record ExecutionLaneRequest(
        String triggerType,
        String triggerNodeType,
        boolean overflowRetry,
        boolean requestRetry,
        boolean heavyTask,
        boolean lightTask) {
}
```

- [ ] **Step 4: Add `ExecutionLaneResolver.java`**

```java
package org.chovy.canvas.engine.lane;

import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.springframework.stereotype.Component;

@Component
public class ExecutionLaneResolver {

    public ExecutionLane resolve(ExecutionLaneRequest request) {
        if (request.overflowRetry() || request.requestRetry()) {
            return ExecutionLane.RETRY;
        }
        if (request.heavyTask() || isHeavyNode(request.triggerNodeType()) || isHeavyTrigger(request.triggerType())) {
            return ExecutionLane.HEAVY;
        }
        if (request.lightTask() && TriggerType.DIRECT_CALL.equals(request.triggerType())) {
            return ExecutionLane.LIGHT;
        }
        return ExecutionLane.STANDARD;
    }

    private boolean isHeavyNode(String triggerNodeType) {
        return NodeType.AUDIENCE_TRIGGER.equals(triggerNodeType)
                || NodeType.GROOVY.equals(triggerNodeType)
                || NodeType.TAGGER_OFFLINE.equals(triggerNodeType);
    }

    private boolean isHeavyTrigger(String triggerType) {
        return TriggerType.DLQ_REPLAY.equals(triggerType);
    }
}
```

- [ ] **Step 5: Run resolver tests and verify they pass**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionLaneResolverTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneRequest.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneResolver.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/lane/ExecutionLaneResolverTest.java
git commit -m "feat: add execution lane resolver"
```

---

## Task 3: Add Redis lane keys

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtilLaneTest.java`

- [ ] **Step 1: Write failing Redis key tests**

```java
package org.chovy.canvas.infrastructure.redis;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyUtilLaneTest {

    @Test
    void buildsLaneInflightKeysWithConfiguredPrefix() {
        RedisKeyUtil keys = new RedisKeyUtil();
        ReflectionTestUtils.setField(keys, "prefix", "canvas-test");

        assertThat(keys.inflightLane("STANDARD")).isEqualTo("canvas-test:inflight:lane:STANDARD");
        assertThat(keys.inflightCanvasLane(42L, "HEAVY")).isEqualTo("canvas-test:inflight:canvas:42:lane:HEAVY");
        assertThat(keys.laneMaxConcurrencyConfig("RETRY")).isEqualTo("canvas-test:config:max-concurrency:lane:RETRY");
        assertThat(keys.laneBudgetConfigVersion()).isEqualTo("canvas-test:config:lane-budget-version");
    }
}
```

- [ ] **Step 2: Run key tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=RedisKeyUtilLaneTest test
```

Expected: FAIL because new key methods do not exist.

- [ ] **Step 3: Add methods to `RedisKeyUtil`**

Add under the existing in-flight key section:

```java
/** 每个执行 lane 当前正在执行的任务集合（ZSET，score=过期时间戳ms）。 */
public String inflightLane(String lane) {
    return prefix + ":inflight:lane:" + lane;
}

/** 每个画布在某个 lane 当前正在执行的任务集合（ZSET，score=过期时间戳ms）。 */
public String inflightCanvasLane(Long canvasId, String lane) {
    return prefix + ":inflight:canvas:" + canvasId + ":lane:" + lane;
}

/** lane 最大并发配置基准值，用于启动一致性校验。 */
public String laneMaxConcurrencyConfig(String lane) {
    return prefix + ":config:max-concurrency:lane:" + lane;
}

/** lane 预算配置版本，用于后续灰度和热更新校验。 */
public String laneBudgetConfigVersion() {
    return prefix + ":config:lane-budget-version";
}
```

- [ ] **Step 4: Run key tests and verify they pass**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=RedisKeyUtilLaneTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtilLaneTest.java
git commit -m "feat: add lane inflight redis keys"
```

---

## Task 4: Make `InFlightExecutionRegistry` lane-aware

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneAdmissionResult.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java`

- [ ] **Step 1: Write failing registry tests**

Use Mockito for `StringRedisTemplate` and verify script invocation returns typed results:

```java
package org.chovy.canvas.engine.trigger;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.chovy.canvas.engine.lane.ExecutionLaneAdmissionResult;
import org.chovy.canvas.infrastructure.redis.RedisKeyUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InFlightExecutionRegistryLaneTest {

    @Mock StringRedisTemplate redis;
    @Mock RedisKeyUtil keys;

    @Test
    void laneAwareAcquireReturnsAllowedSlotWhenRedisAllows() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys);
        when(keys.inflightCanvas(10L)).thenReturn("canvas:inflight:canvas:10");
        when(keys.inflightLane("STANDARD")).thenReturn("canvas:inflight:lane:STANDARD");
        when(keys.inflightGlobal()).thenReturn("canvas:inflight:global");
        when(redis.execute(any(RedisScript.class), eq(List.of(
                "canvas:inflight:canvas:10",
                "canvas:inflight:lane:STANDARD",
                "canvas:inflight:global")),
                any(String[].class))).thenReturn(1L);

        ExecutionLaneAdmissionResult result = registry.tryAcquire(
                10L, "exec-1", ExecutionLane.STANDARD, 1000, 1800, 3000);

        assertThat(result.allowed()).isTrue();
        assertThat(result.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.NONE);
        assertThat(result.slot()).isPresent();
    }

    @Test
    void laneAwareAcquireMapsLaneLimitRejection() {
        InFlightExecutionRegistry registry = new InFlightExecutionRegistry(redis, keys);
        when(keys.inflightCanvas(10L)).thenReturn("canvas:inflight:canvas:10");
        when(keys.inflightLane("HEAVY")).thenReturn("canvas:inflight:lane:HEAVY");
        when(keys.inflightGlobal()).thenReturn("canvas:inflight:global");
        when(redis.execute(any(RedisScript.class), any(List.class), any(String[].class))).thenReturn(-2L);

        ExecutionLaneAdmissionResult result = registry.tryAcquire(
                10L, "exec-2", ExecutionLane.HEAVY, 1000, 300, 3000);

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo(ExecutionLaneAdmissionResult.Reason.LANE_LIMIT);
        assertThat(result.slot()).isEmpty();
    }
}
```

- [ ] **Step 2: Run registry tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=InFlightExecutionRegistryLaneTest test
```

Expected: FAIL because `ExecutionLaneAdmissionResult` and lane-aware `tryAcquire` do not exist.

- [ ] **Step 3: Add `ExecutionLaneAdmissionResult.java`**

```java
package org.chovy.canvas.engine.lane;

import reactor.core.Disposable;

import java.util.Optional;

public record ExecutionLaneAdmissionResult(
        boolean allowed,
        Reason reason,
        int canvasActive,
        int laneActive,
        int globalActive,
        Optional<Disposable.Swap> slot) {

    public enum Reason {
        NONE,
        CANVAS_LIMIT,
        LANE_LIMIT,
        GLOBAL_LIMIT,
        REGISTRY_UNAVAILABLE
    }

    public static ExecutionLaneAdmissionResult allowed(Disposable.Swap slot) {
        return new ExecutionLaneAdmissionResult(true, Reason.NONE, -1, -1, -1, Optional.of(slot));
    }

    public static ExecutionLaneAdmissionResult rejected(Reason reason) {
        return new ExecutionLaneAdmissionResult(false, reason, -1, -1, -1, Optional.empty());
    }
}
```

- [ ] **Step 4: Add lane-aware `tryAcquire` overload**

Add this public method to `InFlightExecutionRegistry`:

```java
public ExecutionLaneAdmissionResult tryAcquire(Long canvasId,
                                               String executionId,
                                               ExecutionLane lane,
                                               int canvasLimit,
                                               int laneLimit,
                                               int globalLimit) {
    if (canvasLimit <= 0 || laneLimit <= 0 || globalLimit <= 0) {
        return ExecutionLaneAdmissionResult.rejected(ExecutionLaneAdmissionResult.Reason.CANVAS_LIMIT);
    }

    String canvasKey = keys.inflightCanvas(canvasId);
    String laneKey = keys.inflightLane(lane.name());
    String globalKey = keys.inflightGlobal();
    long nowMs = System.currentTimeMillis();
    long expiryMs = nowMs + globalTimeoutSec * 1000L;

    Long result;
    try {
        result = redis.execute(
                ACQUIRE_LANE_SCRIPT,
                List.of(canvasKey, laneKey, globalKey),
                String.valueOf(nowMs),
                String.valueOf(expiryMs),
                String.valueOf(canvasLimit),
                String.valueOf(laneLimit),
                String.valueOf(globalLimit),
                executionId
        );
    } catch (Exception e) {
        log.error("[REGISTRY] Redis lane acquire failed, rejecting execution canvasId={} lane={}: {}",
                canvasId, lane, e.getMessage());
        return ExecutionLaneAdmissionResult.rejected(ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE);
    }

    if (result == null || result <= 0) {
        return ExecutionLaneAdmissionResult.rejected(mapLaneRejection(result));
    }

    Disposable.Swap slot = registerLocalSlot(canvasId, executionId, lane);
    if (slot == null) {
        releaseRedisSlot(canvasKey, laneKey, globalKey, executionId);
        return ExecutionLaneAdmissionResult.rejected(ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE);
    }
    return ExecutionLaneAdmissionResult.allowed(slot);
}
```

Add helper:

```java
private ExecutionLaneAdmissionResult.Reason mapLaneRejection(Long result) {
    if (result == null) {
        return ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE;
    }
    return switch (result.intValue()) {
        case -1 -> ExecutionLaneAdmissionResult.Reason.CANVAS_LIMIT;
        case -2 -> ExecutionLaneAdmissionResult.Reason.LANE_LIMIT;
        case -3 -> ExecutionLaneAdmissionResult.Reason.GLOBAL_LIMIT;
        default -> ExecutionLaneAdmissionResult.Reason.REGISTRY_UNAVAILABLE;
    };
}
```

- [ ] **Step 5: Add lane-aware Lua script**

Add to `InFlightExecutionRegistry`:

```java
private static final RedisScript<Long> ACQUIRE_LANE_SCRIPT = RedisScript.of(
        "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])\n" +
        "redis.call('ZREMRANGEBYSCORE', KEYS[2], 0, ARGV[1])\n" +
        "redis.call('ZREMRANGEBYSCORE', KEYS[3], 0, ARGV[1])\n" +
        "local cc = tonumber(redis.call('ZCARD', KEYS[1]))\n" +
        "local lc = tonumber(redis.call('ZCARD', KEYS[2]))\n" +
        "local gc = tonumber(redis.call('ZCARD', KEYS[3]))\n" +
        "if cc >= tonumber(ARGV[3]) then return -1 end\n" +
        "if lc >= tonumber(ARGV[4]) then return -2 end\n" +
        "if gc >= tonumber(ARGV[5]) then return -3 end\n" +
        "redis.call('ZADD', KEYS[1], tonumber(ARGV[2]), ARGV[6])\n" +
        "redis.call('ZADD', KEYS[2], tonumber(ARGV[2]), ARGV[6])\n" +
        "redis.call('ZADD', KEYS[3], tonumber(ARGV[2]), ARGV[6])\n" +
        "redis.call('PEXPIREAT', KEYS[1], tonumber(ARGV[2]) + 60000)\n" +
        "redis.call('PEXPIREAT', KEYS[2], tonumber(ARGV[2]) + 60000)\n" +
        "redis.call('PEXPIREAT', KEYS[3], tonumber(ARGV[2]) + 60000)\n" +
        "return 1",
        Long.class
);
```

- [ ] **Step 6: Store lane metadata for release**

Replace the local registry value with a small holder:

```java
private record LocalExecutionSlot(Disposable.Swap slot, ExecutionLane lane) {
}
```

Use `ConcurrentHashMap<Long, ConcurrentHashMap<String, LocalExecutionSlot>> localRegistry`.

Add:

```java
private Disposable.Swap registerLocalSlot(Long canvasId, String executionId, ExecutionLane lane) {
    Disposable.Swap slot = Disposables.swap();
    localRegistry.compute(canvasId, (id, current) -> {
        ConcurrentHashMap<String, LocalExecutionSlot> map =
                current != null ? current : new ConcurrentHashMap<>();
        map.put(executionId, new LocalExecutionSlot(slot, lane));
        return map;
    });
    return slot;
}
```

Update `deregister` so it releases canvas, lane, and global:

```java
public void deregister(Long canvasId, String executionId) {
    ConcurrentHashMap<String, LocalExecutionSlot> map = localRegistry.get(canvasId);
    if (map == null) {
        return;
    }
    LocalExecutionSlot removed = map.remove(executionId);
    if (removed != null) {
        if (map.isEmpty()) {
            localRegistry.remove(canvasId);
        }
        releaseRedisSlot(
                keys.inflightCanvas(canvasId),
                keys.inflightLane(removed.lane().name()),
                keys.inflightGlobal(),
                executionId);
    }
}
```

Update `cancelAll` to dispose the holder and release the matching lane key:

```java
public int cancelAll(Long canvasId) {
    ConcurrentHashMap<String, LocalExecutionSlot> map = localRegistry.remove(canvasId);
    if (map == null) {
        return 0;
    }
    String canvasKey = keys.inflightCanvas(canvasId);
    String globalKey = keys.inflightGlobal();
    map.forEach((execId, local) -> {
        if (!local.slot().isDisposed()) {
            local.slot().dispose();
            log.info("[REGISTRY] FORCE cancel execution canvasId={} executionId={}", canvasId, execId);
        }
        releaseRedisSlot(canvasKey, keys.inflightLane(local.lane().name()), globalKey, execId);
    });
    return map.size();
}
```

Add a three-key release script and keep the old two-key release helper only for the legacy overload:

```java
private void releaseRedisSlot(String canvasKey, String laneKey, String globalKey, String executionId) {
    try {
        redis.execute(RELEASE_LANE_SCRIPT, List.of(canvasKey, laneKey, globalKey), executionId);
    } catch (Exception e) {
        log.warn("[REGISTRY] Redis lane ZREM failed, waiting for TTL self-heal executionId={}: {}",
                executionId, e.getMessage());
    }
}

private static final RedisScript<Long> RELEASE_LANE_SCRIPT = RedisScript.of(
        "redis.call('ZREM', KEYS[1], ARGV[1])\n" +
        "redis.call('ZREM', KEYS[2], ARGV[1])\n" +
        "redis.call('ZREM', KEYS[3], ARGV[1])\n" +
        "return 1",
        Long.class
);
```

- [ ] **Step 7: Run existing and new registry tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=InFlightExecutionRegistryTest,InFlightExecutionRegistryLaneTest test
```

Expected: PASS. If existing tests still use a no-arg constructor, update them to pass mocked `StringRedisTemplate` and `RedisKeyUtil`, because production code requires Redis-backed distributed counting.

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/lane/ExecutionLaneAdmissionResult.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryLaneTest.java
git commit -m "feat: add lane-aware execution registry"
```

---

## Task 5: Wire lane admission into `CanvasExecutionService`

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/MapFieldKeys.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceLaneTest.java`

- [ ] **Step 1: Write failing service test for lane rejection**

Create `CanvasExecutionServiceLaneTest.java` using the same Mockito fixture style as `CanvasExecutionServiceTest`. Add this test:

```java
@Test
void heavyLaneRejectionReturnsLaneAndReason() {
    CanvasDO canvas = publishedCanvas(70L, 10);
    when(canvasEntityCache.get(70L)).thenReturn(canvas);
    when(ctxStore.exists(70L, "user-1")).thenReturn(false);
    when(executionRegistry.tryAcquire(eq(70L), any(), eq(ExecutionLane.HEAVY), eq(3000), eq(300), eq(3000)))
            .thenReturn(ExecutionLaneAdmissionResult.rejected(ExecutionLaneAdmissionResult.Reason.LANE_LIMIT));

    Map<String, Object> result = sut.trigger(
            70L,
            "user-1",
            TriggerType.MQ,
            NodeType.AUDIENCE_TRIGGER,
            "audience",
            Map.of(),
            "msg-70",
            false
    ).block();

    assertThat(result).containsEntry(MapFieldKeys.OVERFLOW, "concurrency_limit_reached");
    assertThat(result).containsEntry(MapFieldKeys.EXECUTION_LANE, "HEAVY");
    assertThat(result).containsEntry(MapFieldKeys.ADMISSION_REASON, "LANE_LIMIT");
    verify(dagEngine, never()).execute(any(), any(), any());
}
```

If helper methods are private in `CanvasExecutionServiceTest`, duplicate only the minimal helper setup in this new test instead of changing unrelated tests.

- [ ] **Step 2: Run service lane test and verify it fails**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=CanvasExecutionServiceLaneTest test
```

Expected: FAIL because service does not yet inject resolver/properties or return lane rejection fields.

- [ ] **Step 3: Add map keys**

Add to `MapFieldKeys` near admission-related keys:

```java
// 执行 lane
public static final String EXECUTION_LANE = "executionLane";

// 准入拒绝原因
public static final String ADMISSION_REASON = "admissionReason";

// lane 当前活跃数
public static final String LANE_ACTIVE = "laneActive";

// 全局当前活跃数
public static final String GLOBAL_ACTIVE = "globalActive";
```

- [ ] **Step 4: Inject resolver and properties**

Add final fields to `CanvasExecutionService`:

```java
private final ExecutionLaneResolver executionLaneResolver;
private final ExecutionLaneProperties executionLaneProperties;
```

Update tests that manually construct `CanvasExecutionService` to pass:

```java
new ExecutionLaneResolver(),
new ExecutionLaneProperties()
```

- [ ] **Step 5: Resolve lane in `prepareExecution`**

Before admission, add:

```java
ExecutionLane lane = executionLaneResolver.resolve(new ExecutionLaneRequest(
        triggerType,
        triggerNodeType,
        overflowRetry,
        persistentRequest,
        isHeavyTask(triggerNodeType, payload),
        isLightTask(triggerType, triggerNodeType, payload)
));
```

Add helpers:

```java
private boolean isHeavyTask(String triggerNodeType, Map<String, Object> payload) {
    return NodeType.AUDIENCE_TRIGGER.equals(triggerNodeType)
            || NodeType.GROOVY.equals(triggerNodeType)
            || Boolean.TRUE.equals(payload != null ? payload.get("heavyTask") : null);
}

private boolean isLightTask(String triggerType, String triggerNodeType, Map<String, Object> payload) {
    return TriggerType.DIRECT_CALL.equals(triggerType)
            && NodeType.DIRECT_CALL.equals(triggerNodeType)
            && !Boolean.TRUE.equals(payload != null ? payload.get("heavyTask") : null);
}
```

- [ ] **Step 6: Carry lane in prep map**

Update `buildPrepMap` and `buildPrepareResultMap` signatures to include `ExecutionLane lane`. Add:

```java
prep.put(MapFieldKeys.EXECUTION_LANE, lane);
```

- [ ] **Step 7: Use lane-aware registry in `tryAcquireSlot`**

Change the method signature:

```java
private SlotAcquisitionResult tryAcquireSlot(Long canvasId,
                                             ExecutionContext ctx,
                                             ExecutionLane lane,
                                             int admissionLimit,
                                             boolean dryRun,
                                             boolean isResume,
                                             String acquiredDedupKey) {
```

Use:

```java
int laneLimit = executionLaneProperties.limitFor(lane);
ExecutionLaneAdmissionResult admission = executionRegistry.tryAcquire(
        canvasId,
        ctx.getExecutionId(),
        lane,
        admissionLimit,
        laneLimit,
        globalMaxConcurrency);

if (!admission.allowed()) {
    int active = executionRegistry.activeCount(canvasId);
    if (isResume) ctxStore.releaseResumeLock(ctx.getCanvasId(), ctx.getUserId(), ctx.getResumeLockToken());
    if (acquiredDedupKey != null) ctxStore.releaseDedup(acquiredDedupKey);
    return SlotAcquisitionResult.overflow(Mono.just(Map.of(
            MapFieldKeys.OVERFLOW, "concurrency_limit_reached",
            MapFieldKeys.EXECUTION_LANE, lane.name(),
            MapFieldKeys.ADMISSION_REASON, admission.reason().name(),
            MapFieldKeys.ACTIVE, active,
            MapFieldKeys.LIMIT, admissionLimit)));
}
return SlotAcquisitionResult.acquired(admission.slot().orElseThrow());
```

- [ ] **Step 8: Run focused service tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=CanvasExecutionServiceLaneTest,CanvasExecutionServiceTest test
```

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/common/MapFieldKeys.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceLaneTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java
git commit -m "feat: wire lane-aware execution admission"
```

---

## Task 6: Route lane flow metadata through Disruptor and MQ router

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/mq/ExecutionTopicRouter.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasExecutionEvent.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/mq/ExecutionTopicRouterTest.java`

- [ ] **Step 1: Write failing topic-router tests**

```java
package org.chovy.canvas.engine.mq;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionTopicRouterTest {

    @Test
    void routesRetryToRetryTopic() {
        ExecutionTopicRouter router = new ExecutionTopicRouter(
                "CANVAS_EXECUTION_NORMAL",
                "CANVAS_EXECUTION_LIGHT",
                "CANVAS_EXECUTION_HEAVY",
                "CANVAS_EXECUTION_RETRY");

        assertThat(router.route(ExecutionLane.STANDARD, true)).isEqualTo("CANVAS_EXECUTION_RETRY");
        assertThat(router.route(ExecutionLane.RETRY, false)).isEqualTo("CANVAS_EXECUTION_RETRY");
    }

    @Test
    void routesLanesToConfiguredTopics() {
        ExecutionTopicRouter router = new ExecutionTopicRouter("normal", "light", "heavy", "retry");

        assertThat(router.route(ExecutionLane.LIGHT, false)).isEqualTo("light");
        assertThat(router.route(ExecutionLane.STANDARD, false)).isEqualTo("normal");
        assertThat(router.route(ExecutionLane.HEAVY, false)).isEqualTo("heavy");
    }
}
```

- [ ] **Step 2: Run topic-router tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionTopicRouterTest test
```

Expected: FAIL because router does not exist.

- [ ] **Step 3: Add `ExecutionTopicRouter.java`**

```java
package org.chovy.canvas.engine.mq;

import org.chovy.canvas.engine.lane.ExecutionLane;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ExecutionTopicRouter {

    private final String normalTopic;
    private final String lightTopic;
    private final String heavyTopic;
    private final String retryTopic;

    public ExecutionTopicRouter(
            @Value("${canvas.mq-flow.normal-topic:CANVAS_EXECUTION_NORMAL}") String normalTopic,
            @Value("${canvas.mq-flow.light-topic:CANVAS_EXECUTION_LIGHT}") String lightTopic,
            @Value("${canvas.mq-flow.heavy-topic:CANVAS_EXECUTION_HEAVY}") String heavyTopic,
            @Value("${canvas.mq-flow.retry-topic:CANVAS_EXECUTION_RETRY}") String retryTopic) {
        this.normalTopic = normalTopic;
        this.lightTopic = lightTopic;
        this.heavyTopic = heavyTopic;
        this.retryTopic = retryTopic;
    }

    public String route(ExecutionLane lane, boolean retry) {
        if (retry || lane == ExecutionLane.RETRY) {
            return retryTopic;
        }
        return switch (lane) {
            case LIGHT -> lightTopic;
            case HEAVY -> heavyTopic;
            case STANDARD -> normalTopic;
            case RETRY -> retryTopic;
        };
    }
}
```

- [ ] **Step 4: Add lane metadata to `CanvasExecutionEvent`**

Add field:

```java
public org.chovy.canvas.engine.lane.ExecutionLane executionLane;
```

Update `reset()`:

```java
executionLane = null;
```

- [ ] **Step 5: Add lane-aware publish overload**

In `CanvasDisruptorService`, add overload:

```java
public void publish(Long canvasId, String userId, String triggerType,
                    String triggerNodeType, String matchKey,
                    Map<String, Object> payload, String msgId,
                    ExecutionLane executionLane) {
    publish(canvasId, userId, triggerType, triggerNodeType, matchKey, payload, msgId,
            DispatchOptions.NORMAL, executionLane);
}
```

Update the private `publish` method to accept `ExecutionLane executionLane` and set:

```java
event.executionLane = executionLane;
```

Keep the existing public overload by forwarding `null` lane for compatibility.

- [ ] **Step 6: Run router and Disruptor tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionTopicRouterTest,CanvasDisruptorServiceTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/mq/ExecutionTopicRouter.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasExecutionEvent.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/mq/ExecutionTopicRouterTest.java
git commit -m "feat: route execution flow metadata by lane"
```

---

## Task 7: Add lane metrics

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CanvasMetricsTest.java`

- [ ] **Step 1: Write failing metrics tests**

```java
package org.chovy.canvas.engine.scheduler;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanvasMetricsTest {

    @Test
    void recordsLaneAdmissionRejection() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasMetrics metrics = new CanvasMetrics(registry);

        metrics.recordAdmissionRejected("HEAVY", "LANE_LIMIT");

        assertThat(registry.counter("canvas.execution.admission.rejected",
                "lane", "HEAVY",
                "reason", "LANE_LIMIT").count()).isEqualTo(1.0);
    }

    @Test
    void exposesLaneActiveGauge() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CanvasMetrics metrics = new CanvasMetrics(registry);

        metrics.setLaneActiveExecutions("STANDARD", 123);

        assertThat(registry.find("canvas.execution.active.lane")
                .tag("lane", "STANDARD")
                .gauge()
                .value()).isEqualTo(123.0);
    }
}
```

- [ ] **Step 2: Run metrics tests and verify they fail**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=CanvasMetricsTest test
```

Expected: FAIL because new metric methods do not exist.

- [ ] **Step 3: Add gauge maps to `CanvasMetrics`**

Add fields:

```java
private final ConcurrentMap<String, AtomicLong> laneActiveExecutions = new ConcurrentHashMap<>();
private final AtomicLong globalActiveExecutions = new AtomicLong();
```

Register global gauge in a `@PostConstruct` method:

```java
@PostConstruct
void bindCapacityGauges() {
    Gauge.builder("canvas.execution.active.global", globalActiveExecutions, AtomicLong::get)
            .register(registry);
}
```

- [ ] **Step 4: Add metric methods**

```java
public void setGlobalActiveExecutions(long count) {
    globalActiveExecutions.set(Math.max(0L, count));
}

public void setLaneActiveExecutions(String lane, long count) {
    String normalizedLane = lane != null ? lane : "UNKNOWN";
    AtomicLong gauge = laneActiveExecutions.computeIfAbsent(normalizedLane, key -> {
        AtomicLong value = new AtomicLong();
        Gauge.builder("canvas.execution.active.lane", value, AtomicLong::get)
                .tag("lane", key)
                .register(registry);
        return value;
    });
    gauge.set(Math.max(0L, count));
}

public void recordAdmissionRejected(String lane, String reason) {
    Counter.builder("canvas.execution.admission.rejected")
            .tag("lane", lane != null ? lane : "UNKNOWN")
            .tag("reason", reason != null ? reason : "UNKNOWN")
            .register(registry)
            .increment();
}

public void recordAdmissionAccepted(String lane) {
    Counter.builder("canvas.execution.admission.accepted")
            .tag("lane", lane != null ? lane : "UNKNOWN")
            .register(registry)
            .increment();
}
```

- [ ] **Step 5: Wire metrics from service**

In `CanvasExecutionService.tryAcquireSlot`, after an accepted lane admission:

```java
metrics.recordAdmissionAccepted(lane.name());
metrics.setGlobalActiveExecutions(executionRegistry.totalActiveCount());
metrics.setLaneActiveExecutions(lane.name(), executionRegistry.laneActiveCount(lane));
```

After rejection:

```java
metrics.recordAdmissionRejected(lane.name(), admission.reason().name());
metrics.setGlobalActiveExecutions(executionRegistry.totalActiveCount());
metrics.setLaneActiveExecutions(lane.name(), executionRegistry.laneActiveCount(lane));
```

If `CanvasExecutionService` does not currently inject `CanvasMetrics`, add it as a constructor dependency and update tests.

- [ ] **Step 6: Run metrics and service tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=CanvasMetricsTest,CanvasExecutionServiceLaneTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CanvasMetricsTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceLaneTest.java
git commit -m "feat: add lane admission metrics"
```

---

## Task 8: Final focused verification

**Files:**
- Modify only if needed: `docs/superpowers/plans/2026-05-27-3000-concurrency-code-foundation.md`

- [ ] **Step 1: Run focused test suite**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=ExecutionLanePropertiesTest,ExecutionLaneResolverTest,RedisKeyUtilLaneTest,InFlightExecutionRegistryLaneTest,CanvasExecutionServiceLaneTest,ExecutionTopicRouterTest,CanvasMetricsTest test
```

Expected: PASS.

- [ ] **Step 2: Run existing impacted tests**

Run:

```bash
mvn -pl backend/canvas-engine -Dtest=InFlightExecutionRegistryTest,CanvasExecutionServiceTest,CanvasDisruptorServiceTest test
```

Expected: PASS.

- [ ] **Step 3: Run module test suite**

Run:

```bash
mvn -pl backend/canvas-engine test
```

Expected: PASS. If unrelated existing tests fail, capture the failing test names and confirm whether failures predate this work before proceeding.

- [ ] **Step 4: Verify no placeholders in concurrency docs**

Run:

```bash
pattern="$(printf '%s|%s|%s|%s|%s' '20{2}0\\+' 'T[B]D' 'TO[D]O' '待''定' '占''位')"
rg -n "$pattern" docs/superpowers/specs/2026-05-25-2000-concurrency-design.md docs/superpowers/plans/2026-05-25-2000-concurrency.md docs/superpowers/plans/2026-05-27-3000-concurrency-code-foundation.md
```

Expected: no matches.

- [ ] **Step 5: Commit final verification notes if documentation changed**

If this task required doc edits:

```bash
git add docs/superpowers/plans/2026-05-27-3000-concurrency-code-foundation.md
git commit -m "docs: finalize 3000 concurrency code foundation plan"
```

---

## Self-Review

### Spec coverage

This plan implements the code-controlled foundation from the 3000 design:
- global budget remains `canvas.execution.max-concurrency`
- lane budgets are typed and validated
- `LIGHT` / `STANDARD` / `HEAVY` / `RETRY` are defined
- retry and heavy resolution are explicit
- Redis keys support lane in-flight tracking
- `InFlightExecutionRegistry` gains canvas/lane/global admission
- `CanvasExecutionService` resolves lane and acquires lane-aware slots
- Disruptor events can carry lane metadata
- metrics expose lane rejection and active counts

### Placeholder scan

The plan avoids placeholder markers and vague implementation steps. Thresholds that require production measurement remain in rollout docs, not in this code foundation plan.

### Type consistency

Core names are consistent across tasks:
- `ExecutionLane`
- `ExecutionLaneRequest`
- `ExecutionLaneResolver`
- `ExecutionLaneAdmissionResult`
- `ExecutionLaneProperties`
- `ExecutionLanePropertiesValidator`
- `ExecutionTopicRouter`

### Scope check

The plan is intentionally narrower than the full 3000 rollout. It produces a testable code foundation without bundling infrastructure provisioning, physical Redis separation, or production load testing into the same implementation slice.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-27-3000-concurrency-code-foundation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - dispatch a fresh subagent per task, review between tasks, faster iteration.

**2. Inline Execution** - execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
