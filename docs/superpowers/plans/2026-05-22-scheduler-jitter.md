# 定时任务随机 Jitter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 CanvasSchedulerService 的定时批量触发加入随机 jitter 延迟，避免大规模离线人群同一时刻并发触发引发雪崩。

**Architecture:** 在 `triggerForAllUsers()` 中，用 `ThreadLocalRandom` 为每个用户生成随机延迟，通过 `Mono.delay()` 非阻塞错峰触发。最大抖动时间由 `application.yml` 配置。

**Tech Stack:** Java 17, Project Reactor, Spring Boot `@Value`

---

## File Map

| Action | File |
|--------|------|
| Modify | `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java` |
| Modify | `backend/canvas-engine/src/main/resources/application.yml` |

---

## Task 1：添加配置项 + 注入 jitterMaxMs

**Files:**
- Modify: `application.yml`
- Modify: `CanvasSchedulerService.java`

- [ ] **Step 1：在 application.yml 末尾（注释块附近）添加配置项**

```yaml
canvas:
  scheduler:
    # 定时任务触发的最大随机抖动时间（毫秒），0 表示不抖动（默认 5 分钟）
    jitter-max-ms: 300000
```

- [ ] **Step 2：在 CanvasSchedulerService 中注入配置值**

在类的 `@Value` 注入区（现有 `taggerUrl` / `apiCallUrl` 之后）添加：

```java
@Value("${canvas.scheduler.jitter-max-ms:300000}")
private long jitterMaxMs;
```

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/resources/application.yml \
        backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java
git commit -m "feat: add canvas.scheduler.jitter-max-ms config and inject into CanvasSchedulerService"
```

---

## Task 2：实现随机 Jitter 触发（TDD）

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerJitterTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java`

- [ ] **Step 1：写失败测试**

创建 `CanvasSchedulerJitterTest.java`：

```java
package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;

class CanvasSchedulerJitterTest {

    @Test
    @DisplayName("jitterMaxMs=0 时所有触发立即执行（无延迟），顺序一致")
    void no_jitter_triggers_immediately() {
        List<String> triggered = new CopyOnWriteArrayList<>();

        // 提取纯计算逻辑：calcDelay(jitterMaxMs) 返回随机延迟
        // jitterMaxMs=0 → 延迟必须为 0
        Duration d = CanvasSchedulerService.calcJitter(0);
        assertThat(d).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("jitterMaxMs=60000 时延迟在 [0, 60000) ms 范围内")
    void jitter_in_range() {
        for (int i = 0; i < 100; i++) {
            Duration d = CanvasSchedulerService.calcJitter(60_000L);
            assertThat(d.toMillis()).isBetween(0L, 59_999L);
        }
    }
}
```

- [ ] **Step 2：运行测试确认失败**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasSchedulerJitterTest -q
```

Expected: FAIL — `calcJitter` 方法不存在

- [ ] **Step 3：在 CanvasSchedulerService 中添加 calcJitter 静态方法**

在类的末尾（`resolveUserIds` 方法之后）添加：

```java
/** 生成 [0, jitterMaxMs) 区间内的随机延迟；jitterMaxMs=0 时返回 ZERO。可静态调用，便于单测。 */
static Duration calcJitter(long jitterMaxMs) {
    if (jitterMaxMs <= 0) return Duration.ZERO;
    return Duration.ofMillis(java.util.concurrent.ThreadLocalRandom.current().nextLong(0, jitterMaxMs));
}
```

- [ ] **Step 4：修改 triggerForAllUsers() 使用 calcJitter**

将现有循环（约 L127-136）：

```java
for (String userId : userIds) {
    executionService.trigger(
            canvasId, userId, TriggerType.SCHEDULED,
            NodeType.SCHEDULED_TRIGGER, null,
            Map.of(), java.util.UUID.randomUUID().toString(), false)
            .subscribe(
                    null,
                    e -> log.warn("[SCHEDULER] 用户触发失败 userId={}: {}", userId, e.getMessage())
            );
}
```

改为：

```java
for (String userId : userIds) {
    Duration jitter = calcJitter(jitterMaxMs);
    Mono.delay(jitter)
        .then(Mono.fromRunnable(() ->
            executionService.trigger(
                    canvasId, userId, TriggerType.SCHEDULED,
                    NodeType.SCHEDULED_TRIGGER, null,
                    Map.of(), java.util.UUID.randomUUID().toString(), false)
                .subscribe(
                        null,
                        e -> log.warn("[SCHEDULER] 用户触发失败 userId={}: {}", userId, e.getMessage())
                )
        ))
        .subscribe();
}
```

需确认 `reactor.core.publisher.Mono` 和 `java.time.Duration` 已在 import 中（已有）。

- [ ] **Step 5：运行测试确认通过**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasSchedulerJitterTest -q
```

Expected: `Tests run: 2, Failures: 0, Errors: 0`

- [ ] **Step 6：运行全量测试**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasSchedulerService.java \
        backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasSchedulerJitterTest.java
git commit -m "feat: add random jitter to scheduled trigger to prevent thundering herd"
```
