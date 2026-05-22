# MQ_TRIGGER 消费入口 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 接入 RocketMQ 5.x，实现 MQ_TRIGGER 画布的完整触发链路：SendMqHandler 发消息 → RocketMQ → MqTriggerConsumer → Disruptor → 画布执行。

**Architecture:** 单 Topic `CANVAS_MQ_TRIGGER` + Tag 路由；Consumer 投递到 Disruptor Ring Buffer 实现背压；Ring Buffer 满时返回 RECONSUME_LATER。

**Tech Stack:** RocketMQ 5.x, rocketmq-spring-boot-starter 2.3.x, Spring Boot 3, Spring WebFlux

---

## File Map

| Action | File |
|--------|------|
| Modify | `backend/canvas-engine/pom.xml` |
| Modify | `src/main/resources/application.yml` |
| Create | `infra/mq/MqTriggerMessage.java` |
| Create | `infra/mq/MqTriggerConsumer.java` |
| Modify | `engine/handlers/SendMqHandler.java` |

路径前缀：`backend/canvas-engine/src/main/java/org/chovy/canvas/`

---

## Task 1：添加 RocketMQ 依赖 + 配置

**Files:**
- Modify: `backend/canvas-engine/pom.xml`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1：在 pom.xml 的 `<dependencies>` 中添加**

```xml
<!-- RocketMQ 5.x Spring Boot Starter -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.3.1</version>
</dependency>
```

- [ ] **Step 2：在 application.yml 添加 RocketMQ 配置**

在文件末尾追加：

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: PID_CANVAS_ENGINE
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    group: GID_CANVAS_ENGINE

canvas:
  mq:
    topic: CANVAS_MQ_TRIGGER
    consume-thread-number: ${CANVAS_MQ_CONSUME_THREAD:20}
```

- [ ] **Step 3：编译验证依赖下载正常**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`（首次运行会下载依赖）

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/pom.xml backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: add rocketmq-spring-boot-starter 2.3.1 and config"
```

---

## Task 2：创建消息体 DTO

**Files:**
- Create: `infra/mq/MqTriggerMessage.java`

- [ ] **Step 1：创建消息体 DTO**

```java
package org.chovy.canvas.infra.mq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * MQ 触发消息体。
 * 由 SendMqHandler 发送，MqTriggerConsumer 接收并路由到画布执行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqTriggerMessage {
    /** 触发用户 ID */
    private String userId;
    /** 消息类型标识，对应 MqMessageDefinition.messageCode */
    private String messageCode;
    /** 业务载荷，供画布节点通过上下文引用 */
    private Map<String, Object> payload;
}
```

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infra/mq/MqTriggerMessage.java
git commit -m "feat: add MqTriggerMessage DTO"
```

---

## Task 3：实现 MqTriggerConsumer

**Files:**
- Create: `infra/mq/MqTriggerConsumer.java`

- [ ] **Step 1：创建 Consumer 类**

```java
package org.chovy.canvas.infra.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.domain.constant.NodeType;
import org.chovy.canvas.domain.constant.TriggerType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.infra.redis.TriggerRouteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * RocketMQ MQ 触发消费者（设计文档优化点 #6）。
 *
 * 消费链路：RocketMQ CANVAS_MQ_TRIGGER → 按 Tag 路由 → Disruptor Ring Buffer → DagEngine
 * 背压：Ring Buffer 满时抛出 IllegalStateException → RECONSUME_LATER → RocketMQ 自动重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "${canvas.mq.topic:CANVAS_MQ_TRIGGER}",
        consumerGroup = "${rocketmq.consumer.group:GID_CANVAS_ENGINE}",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 20
)
public class MqTriggerConsumer implements RocketMQListener<MessageExt> {

    private final TriggerRouteService   routeService;
    private final CanvasDisruptorService disruptor;
    private final ObjectMapper          objectMapper;

    @Override
    public void onMessage(MessageExt message) {
        String tag  = message.getTags();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String msgId = message.getMsgId();

        log.info("[MQ_CONSUMER] 收到消息 tag={} msgId={}", tag, msgId);

        MqTriggerMessage msg;
        try {
            msg = objectMapper.readValue(body, MqTriggerMessage.class);
        } catch (Exception e) {
            log.error("[MQ_CONSUMER] 消息体解析失败 msgId={} body={}: {}", msgId, body, e.getMessage());
            // 格式错误无法重试，直接丢弃（会进 DLQ 超过重试次数后）
            throw new IllegalArgumentException("消息体格式错误: " + e.getMessage());
        }

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[MQ_CONSUMER] tag={} 无匹配画布，丢弃消息 msgId={}", tag, msgId);
            return; // ACK：无对应画布，正常丢弃
        }

        for (String canvasIdStr : canvasIds) {
            Long canvasId = Long.parseLong(canvasIdStr);
            // publish() 非阻塞；Ring Buffer 满时自旋超时抛出 InsufficientCapacityException
            // → 向上抛 → RocketMQ RECONSUME_LATER
            disruptor.publish(
                    canvasId,
                    msg.getUserId(),
                    TriggerType.MQ,
                    NodeType.MQ_TRIGGER,
                    tag,                    // matchKey = tag = MqMessageDefinition.topic
                    msg.getPayload(),
                    msgId
            );
            log.info("[MQ_CONSUMER] 投递到 Disruptor canvasId={} userId={} tag={}",
                    canvasId, msg.getUserId(), tag);
        }
        // 正常返回 → RocketMQ 自动 ACK
    }
}
```

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：确认 TriggerType 中有 MQ 常量**

```bash
grep "MQ" backend/canvas-engine/src/main/java/org/chovy/canvas/domain/constant/TriggerType.java
```

若不存在，在 `TriggerType.java` 中添加 `public static final String MQ = "MQ";`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/infra/mq/MqTriggerConsumer.java
git commit -m "feat: add RocketMQ MQ_TRIGGER consumer with Disruptor back-pressure"
```

---

## Task 4：实现 SendMqHandler（替换 TODO）

**Files:**
- Modify: `engine/handlers/SendMqHandler.java`

- [ ] **Step 1：完整替换 SendMqHandler**

```java
package org.chovy.canvas.engine.handlers;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.domain.meta.MqMessageDefinition;
import org.chovy.canvas.domain.meta.MqMessageDefinitionMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.infra.mq.MqTriggerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发送 MQ 消息节点：将业务消息发送到 RocketMQ CANVAS_MQ_TRIGGER topic。
 * Tag = MqMessageDefinition.topic，供消费端路由到对应画布。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@NodeHandlerType("SEND_MQ")
public class SendMqHandler implements NodeHandler {

    private final RocketMQTemplate             rocketMQTemplate;
    private final MqMessageDefinitionMapper    mqMapper;
    private final ObjectMapper                 objectMapper;

    @Value("${canvas.mq.topic:CANVAS_MQ_TRIGGER}")
    private String mqTopic;

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        String messageCodeKey = (String) config.get("messageCodeKey");
        String nextNodeId     = (String) config.get("nextNodeId");

        if (messageCodeKey == null || messageCodeKey.isBlank()) {
            return Mono.just(NodeResult.fail("SEND_MQ: messageCodeKey 未配置"));
        }

        MqMessageDefinition def = mqMapper.selectOne(
                new LambdaQueryWrapper<MqMessageDefinition>()
                        .eq(MqMessageDefinition::getMessageCode, messageCodeKey)
                        .eq(MqMessageDefinition::getEnabled, 1));
        if (def == null) {
            return Mono.just(NodeResult.fail("SEND_MQ: 找不到消息定义 messageCode=" + messageCodeKey));
        }

        // 构建 payload：将 params 中 ${ctxKey} 占位符替换为上下文值
        List<Map<String, Object>> paramsList =
                (List<Map<String, Object>>) config.getOrDefault("params", List.of());
        Map<String, Object> payload = new HashMap<>();
        for (Map<String, Object> p : paramsList) {
            String key = (String) p.get("key");
            Object val = p.get("value");
            if (val instanceof String s) {
                String norm = s.startsWith("$${") ? s.substring(1) : s;
                if (norm.startsWith("${") && norm.endsWith("}")) {
                    val = ctx.getContextValue(norm.substring(2, norm.length() - 1));
                }
            }
            if (key != null) payload.put(key, val);
        }

        MqTriggerMessage message = new MqTriggerMessage(ctx.getUserId(), messageCodeKey, payload);
        String destination = mqTopic + ":" + def.getTopic(); // topic:tag 格式

        return Mono.fromRunnable(() -> {
            rocketMQTemplate.syncSend(destination, message);
            log.info("[SEND_MQ] 发送成功 destination={} userId={}", destination, ctx.getUserId());
        })
        .subscribeOn(Schedulers.boundedElastic())
        .thenReturn(NodeResult.ok(nextNodeId, Map.of("mqSent", true)))
        .onErrorResume(e -> {
            log.error("[SEND_MQ] 发送失败 destination={}: {}", destination, e.getMessage());
            return Mono.just(NodeResult.fail("SEND_MQ: 消息发送失败: " + e.getMessage()));
        });
    }
}
```

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：运行全量测试**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`（RocketMQ 连接失败不影响单测，Consumer 在无 MQ 时不启动）

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java
git commit -m "feat: implement SendMqHandler with RocketMQ producer (replaces TODO)"
```

---

## Task 5：本地验证（需启动 RocketMQ）

- [ ] **Step 1：启动 RocketMQ 5.x（docker-compose）**

在 `docker-compose.local.yml` 中追加（若无）：

```yaml
rocketmq-namesrv:
  image: apache/rocketmq:5.3.1
  command: sh mqnamesrv
  ports:
    - "9876:9876"

rocketmq-broker:
  image: apache/rocketmq:5.3.1
  command: sh mqbroker -n namesrv:9876 autoCreateTopicEnable=true
  environment:
    - JAVA_OPT_EXT=-Xms512m -Xmx512m
  depends_on:
    - rocketmq-namesrv
  ports:
    - "10911:10911"
```

```bash
docker-compose -f docker-compose.local.yml up -d rocketmq-namesrv rocketmq-broker
```

- [ ] **Step 2：启动服务，验证 Consumer 正常注册**

日志中应出现：
```
[RocketMQ] Consumer registered: topic=CANVAS_MQ_TRIGGER group=GID_CANVAS_ENGINE
```

- [ ] **Step 3：发布一个含 MQ_TRIGGER 节点的画布，验证端到端触发**

1. 在管理页面创建 `MqMessageDefinition`（messageCode=`test.event`，topic=`test.event`）
2. 创建画布，START 节点选择 MQ_TRIGGER，messageCode=`test.event`
3. 发布画布
4. 调用 `SendMqHandler`（或直接用 API 工具发一条测试消息）
5. 观察画布执行日志，确认正常触发

- [ ] **Step 4：Commit**

```bash
git add docker-compose.local.yml
git commit -m "chore: add RocketMQ 5.x to local docker-compose"
```
