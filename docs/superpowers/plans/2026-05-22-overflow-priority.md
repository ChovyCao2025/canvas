# MQ 削峰与优先级队列 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将并发溢出时的"静默丢弃"改为 RocketMQ 延迟重试；按 TriggerType 划分 HIGH/NORMAL/LOW 三档优先级，HIGH 绕过并发检查，LOW 使用更严格的限制。

**Architecture:** `CanvasExecutionService.trigger()` 溢出分支 → 发 RocketMQ delay 消息 → `OverflowRetryConsumer` 延迟重消费。优先级映射通过 `@ConfigurationProperties` 注入。

**Tech Stack:** Java 17, RocketMQ 5.x（已引入），Spring Boot `@ConfigurationProperties`

---

## File Map

| Action | File |
|--------|------|
| Create | `infra/mq/OverflowRetryMessage.java` |
| Create | `infra/mq/OverflowRetryConsumer.java` |
| Create | `engine/trigger/TriggerPriorityConfig.java` |
| Modify | `engine/trigger/CanvasExecutionService.java` |
| Modify | `src/main/resources/application.yml` |

路径前缀：`backend/canvas-engine/src/main/java/org/chovy/canvas/`

---

## Task 1：优先级配置

**Files:**
- Create: `engine/trigger/TriggerPriorityConfig.java`
- Modify: `application.yml`

- [ ] **Step 1：创建配置类**

```java
package org.chovy.canvas.engine.trigger;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 触发优先级配置。
 * HIGH：不受 maxConcurrency 限制，直接执行。
 * NORMAL：标准并发控制，溢出时进 MQ 延迟重试。
 * LOW：使用 maxConcurrency × lowRatio 的更严格限制，溢出时直接丢弃。
 */
@Data
@Component
@ConfigurationProperties(prefix = "canvas.execution.priority")
public class TriggerPriorityConfig {

    private List<String> high   = List.of("DIRECT_CALL");
    private List<String> normal = List.of("MQ", "BEHAVIOR", "EVENT_TRIGGER", "API_CALL");
    private List<String> low    = List.of("SCHEDULED");

    /** LOW 优先级并发系数，默认 0.5（即 maxConc × 0.5） */
    private double lowRatio = 0.5;

    /** 溢出延迟重试间隔（毫秒） */
    private long overflowRetryDelayMs = 5000;

    /** 溢出最大重试次数（超过后写 DLQ） */
    private int overflowMaxRetry = 3;

    public enum Priority { HIGH, NORMAL, LOW }

    public Priority of(String triggerType) {
        if (triggerType == null) return Priority.NORMAL;
        if (high.contains(triggerType))   return Priority.HIGH;
        if (low.contains(triggerType))    return Priority.LOW;
        return Priority.NORMAL;
    }
}
```

- [ ] **Step 2：在 application.yml 追加优先级配置**

```yaml
canvas:
  execution:
    priority:
      high:   [DIRECT_CALL]
      normal: [MQ, BEHAVIOR, EVENT_TRIGGER, API_CALL]
      low:    [SCHEDULED]
      low-ratio: 0.5
      overflow-retry-delay-ms: 5000
      overflow-max-retry: 3
```

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPriorityConfig.java \
        backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: add trigger priority config (HIGH/NORMAL/LOW)"
```

---

## Task 2：溢出重试消息体 + Consumer

**Files:**
- Create: `infra/mq/OverflowRetryMessage.java`
- Create: `infra/mq/OverflowRetryConsumer.java`

- [ ] **Step 1：创建 OverflowRetryMessage**

```java
package org.chovy.canvas.infra.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 并发溢出重试消息体 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OverflowRetryMessage {
    private Long   canvasId;
    private String userId;
    private String triggerType;
    private String triggerNodeType;
    private String matchKey;
    private Map<String, Object> payload;
    private String msgId;
    /** 已重试次数，超过 overflowMaxRetry 后写 DLQ */
    private int    retryCount;
}
```

- [ ] **Step 2：创建 OverflowRetryConsumer**

```java
package org.chovy.canvas.infra.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 并发溢出重试消费者。
 * 读取延迟投递的溢出消息，重新发布到 Disruptor。
 * 超过最大重试次数则记录警告并丢弃（已有 DLQ 机制兜底）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_OVERFLOW",
        consumerGroup = "GID_CANVAS_OVERFLOW_RETRY",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 5
)
public class OverflowRetryConsumer implements RocketMQListener<MessageExt> {

    private final CanvasDisruptorService disruptor;
    private final TriggerPriorityConfig  priorityConfig;
    private final ObjectMapper           objectMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        OverflowRetryMessage msg;
        try {
            msg = objectMapper.readValue(body, OverflowRetryMessage.class);
        } catch (Exception e) {
            log.error("[OVERFLOW_RETRY] 消息解析失败 msgId={}: {}", message.getMsgId(), e.getMessage());
            return;
        }

        if (msg.getRetryCount() >= priorityConfig.getOverflowMaxRetry()) {
            log.warn("[OVERFLOW_RETRY] 超过最大重试次数 canvasId={} userId={} retryCount={}, 丢弃",
                    msg.getCanvasId(), msg.getUserId(), msg.getRetryCount());
            return; // ACK，不再重试
        }

        log.info("[OVERFLOW_RETRY] 重试投递 canvasId={} userId={} retryCount={}",
                msg.getCanvasId(), msg.getUserId(), msg.getRetryCount());

        disruptor.publish(
                msg.getCanvasId(), msg.getUserId(), msg.getTriggerType(),
                msg.getTriggerNodeType(), msg.getMatchKey(),
                msg.getPayload(), msg.getMsgId()
        );
    }
}
```

- [ ] **Step 3：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infra/mq/OverflowRetryMessage.java \
        backend/canvas-engine/src/main/java/org/chovy/canvas/infra/mq/OverflowRetryConsumer.java
git commit -m "feat: add OverflowRetryMessage and OverflowRetryConsumer"
```

---

## Task 3：改造 CanvasExecutionService 溢出分支

**Files:**
- Modify: `engine/trigger/CanvasExecutionService.java`

- [ ] **Step 1：注入新依赖**

在 `CanvasExecutionService` 类的字段注入区（`@Value` 区域附近）添加：

```java
private final TriggerPriorityConfig priorityConfig;
private final RocketMQTemplate       rocketMQTemplate;
```

添加 imports：
```java
import org.chovy.canvas.engine.trigger.TriggerPriorityConfig;
import org.chovy.canvas.infra.mq.OverflowRetryMessage;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
```

- [ ] **Step 2：替换并发超限分支（约 L162-173）**

将现有代码：

```java
// 2.5 单画布并发执行上限（设计文档 12.4节）
if (!dryRun) {
    int active = executionRegistry.activeCount(canvasId);
    int maxConc = canvas.getMaxTotalExecutions() != null
            ? Math.min(canvas.getMaxTotalExecutions(), globalMaxConcurrency)
            : globalMaxConcurrency;
    if (active >= maxConc) {
        log.warn("[ENGINE] 画布并发上限已达 canvasId={} active={}/{}", canvasId, active, maxConc);
        return Map.of("overflow", "concurrency_limit_reached",
                "active", active, "limit", maxConc);
    }
}
```

替换为：

```java
// 2.5 单画布并发执行上限（优先级感知）
if (!dryRun) {
    TriggerPriorityConfig.Priority priority = priorityConfig.of(triggerType);
    int active  = executionRegistry.activeCount(canvasId);
    int maxConc = canvas.getMaxTotalExecutions() != null
            ? Math.min(canvas.getMaxTotalExecutions(), globalMaxConcurrency)
            : globalMaxConcurrency;

    // HIGH：不受并发限制，直接执行
    if (priority != TriggerPriorityConfig.Priority.HIGH) {
        // LOW：使用更严格的并发限制
        int effectiveMax = priority == TriggerPriorityConfig.Priority.LOW
                ? (int)(maxConc * priorityConfig.getLowRatio())
                : maxConc;

        if (active >= effectiveMax) {
            log.warn("[ENGINE] 并发上限 canvasId={} active={}/{} priority={}", canvasId, active, effectiveMax, priority);
            if (priority == TriggerPriorityConfig.Priority.NORMAL) {
                // NORMAL：溢出进 MQ 延迟重试
                sendOverflowRetry(canvasId, userId, triggerType, triggerNodeType,
                        matchKey, payload, msgId, 0);
                return Map.of("overflow", "queued_for_retry");
            } else {
                // LOW：直接丢弃
                return Map.of("overflow", "dropped_low_priority");
            }
        }
    }
}
```

- [ ] **Step 3：添加 sendOverflowRetry 私有方法**

在类末尾添加：

```java
private void sendOverflowRetry(Long canvasId, String userId, String triggerType,
                                String triggerNodeType, String matchKey,
                                Map<String, Object> payload, String msgId, int retryCount) {
    try {
        OverflowRetryMessage msg = new OverflowRetryMessage(
                canvasId, userId, triggerType, triggerNodeType,
                matchKey, payload, msgId, retryCount + 1);
        // RocketMQ 5.x 定时消息：deliverTimeMs 设为当前时间 + 延迟
        org.apache.rocketmq.common.message.Message rocketMsg =
                new org.apache.rocketmq.common.message.Message(
                        "CANVAS_TRIGGER_OVERFLOW", triggerType,
                        objectMapper.writeValueAsBytes(msg));
        rocketMsg.putUserProperty("__STARTDELIVERTIME",
                String.valueOf(System.currentTimeMillis() + priorityConfig.getOverflowRetryDelayMs()));
        rocketMQTemplate.getProducer().send(rocketMsg);
        log.info("[ENGINE] 溢出事件入队重试 canvasId={} userId={} retryCount={}", canvasId, userId, retryCount + 1);
    } catch (Exception e) {
        log.error("[ENGINE] 溢出消息发送失败，事件丢失 canvasId={}: {}", canvasId, e.getMessage());
    }
}
```

添加 imports：
```java
import org.chovy.canvas.infra.mq.OverflowRetryMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
```

（`ObjectMapper` 已在项目中注册为 Bean，直接注入即可。）

- [ ] **Step 4：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5：运行全量测试**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java
git commit -m "feat: replace silent overflow drop with priority-aware MQ retry"
```
