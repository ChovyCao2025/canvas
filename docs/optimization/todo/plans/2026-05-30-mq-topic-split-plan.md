# MQ Topic Split Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Split single CANVAS_MQ_TRIGGER Topic into 4 independent Topics by trigger type.

**Architecture:** CANVAS_TRIGGER_SCHEDULED, CANVAS_TRIGGER_EVENT, CANVAS_TRIGGER_MQ, CANVAS_TRIGGER_DLQ_REPLAY. Each with independent consumer group, thread count, retry policy, monitoring.

**Tech Stack:** RocketMQ, Spring Boot

**Note:** If `CanvasExecutionDlqMapper` and `CanvasExecutionDlqDO` don't exist yet, create them as part of this plan's Task 2. See the entity and mapper definitions added at the end of Task 2.

---

### Task 1: Create Topics and Consumer Groups

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/RocketMQTopicConfig.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TopicConfigTest.java`

- [ ] **Step 1: Write failing test — verify 4 topic configs load**

```java
package org.chovy.canvas.engine.trigger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TopicConfigTest {
    @Autowired
    private RocketMQTopicConfig topicConfig;

    @Test
    void fourTopicsConfigured() {
        assertThat(topicConfig.getScheduled()).isEqualTo("CANVAS_TRIGGER_SCHEDULED");
        assertThat(topicConfig.getEvent()).isEqualTo("CANVAS_TRIGGER_EVENT");
        assertThat(topicConfig.getMq()).isEqualTo("CANVAS_TRIGGER_MQ");
        assertThat(topicConfig.getDlqReplay()).isEqualTo("CANVAS_TRIGGER_DLQ_REPLAY");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=TopicConfigTest -v`
Expected: FAIL — RocketMQProperties doesn't have these fields.

- [ ] **Step 3: Create config class and add to application.yml**

```java
@Configuration
@ConfigurationProperties(prefix = "rocketmq.topics")
@Data
public class RocketMQTopicConfig {
    private String scheduled = "CANVAS_TRIGGER_SCHEDULED";
    private String event = "CANVAS_TRIGGER_EVENT";
    private String mq = "CANVAS_TRIGGER_MQ";
    private String dlqReplay = "CANVAS_TRIGGER_DLQ_REPLAY";
}
```

```yaml
rocketmq:
  topics:
    scheduled: CANVAS_TRIGGER_SCHEDULED
    event: CANVAS_TRIGGER_EVENT
    mq: CANVAS_TRIGGER_MQ
    dlq-replay: CANVAS_TRIGGER_DLQ_REPLAY
  consumer-groups:
    scheduled: canvas_scheduled_cg
    event: canvas_event_cg
    mq: canvas_mq_cg
    dlq-replay: canvas_dlq_replay_cg
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=TopicConfigTest -v`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/RocketMQTopicConfig.java
git add backend/canvas-engine/src/main/resources/application.yml
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/TopicConfigTest.java
git commit -m "feat: add 4-topic config class with test for MQ trigger separation"
```

---

### Task 2: Create 4 Independent Consumer Classes

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/ScheduledTriggerConsumer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/EventTriggerConsumer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/MqNodeTriggerConsumer.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/DlqReplayConsumer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/ScheduledTriggerConsumerTest.java`

- [ ] **Step 1: Write failing test for ScheduledTriggerConsumer**

```java
@SpringBootTest
class ScheduledTriggerConsumerTest {
    @Autowired
    private ApplicationContext ctx;

    @Test
    void scheduledConsumerRegistered() {
        assertThat(ctx.getBean(ScheduledTriggerConsumer.class)).isNotNull();
    }

    @Test
    void scheduledConsumerSubscribesToCorrectTopic() {
        ScheduledTriggerConsumer consumer = ctx.getBean(ScheduledTriggerConsumer.class);
        RocketMQMessageListener annotation = consumer.getClass()
            .getAnnotation(RocketMQMessageListener.class);
        assertThat(annotation.topic()).isEqualTo("CANVAS_TRIGGER_SCHEDULED");
        assertThat(annotation.consumerGroup()).isEqualTo("canvas_scheduled_cg");
        assertThat(annotation.consumeMode()).isEqualTo(ConsumeMode.CONCURRENTLY);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ScheduledTriggerConsumerTest -v`
Expected: FAIL.

- [ ] **Step 3.5: Define message DTOs**

```java
package org.chovy.canvas.engine.trigger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Message payload for event trigger consumption.
 * Used by EventTriggerConsumer to parse incoming MQ messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventTriggerMessage {
    /** The user ID that triggered this event. */
    private String userId;
    /** The event payload data. */
    private Map<String, Object> payload;
    /** Optional message ID for tracing. */
    private String msgId;
}
```

```java
package org.chovy.canvas.engine.trigger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * Message payload for DLQ replay.
 * Used by DlqReplayConsumer to replay failed messages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlqReplayMessage {
    /** The DLQ record ID to replay. */
    private Long dlqId;
    /** The original topic where the message failed. */
    private String originalTopic;
    /** The original message body (JSON string). */
    private String originalBody;
    /** The reason for the original failure. */
    private String reason;
    /** Optional message ID for tracing. */
    private String msgId;
    /** Optional override payload for replay. */
    private Map<String, Object> payload;
}
```

- [ ] **Step 4: Implement ScheduledTriggerConsumer**

```java
package org.chovy.canvas.engine.trigger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_SCHEDULED",
        consumerGroup = "canvas_scheduled_cg",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 20
)
public class ScheduledTriggerConsumer implements RocketMQListener<MessageExt> {

    private final CanvasDisruptorService disruptorService;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8);
        log.info("[SCHEDULED_CONSUMER] Received message msgId={}", message.getMsgId());
        // Delegate to the Disruptor for scheduled trigger execution
        String requestId = message.getMsgId();
        disruptorService.publishRequest(requestId);
    }
}
```

- [ ] **Step 5: Implement EventTriggerConsumer**

```java
package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_EVENT",
        consumerGroup = "canvas_event_cg",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 50
)
public class EventTriggerConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;
    private final CanvasExecutionRequestService requestService;

    @Override
    public void onMessage(MessageExt message) {
        String tag = message.getTags();
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("[EVENT_CONSUMER] Received message tag={} msgId={}", tag, msgId);

        if (!routeService.isRouteReady()) {
            throw new IllegalStateException("Event trigger route table is not ready");
        }

        Set<String> canvasIds = routeService.getCanvasByBehavior(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[EVENT_CONSUMER] tag={} no matching canvases, discarding msgId={}", tag, msgId);
            return;
        }

        EventTriggerMessage eventMsg;
        try {
            eventMsg = objectMapper.readValue(body, EventTriggerMessage.class);
        } catch (Exception e) {
            log.error("[EVENT_CONSUMER] Failed to parse message msgId={}: {}", msgId, e.getMessage());
            throw new IllegalArgumentException("Invalid event trigger message: " + e.getMessage(), e);
        }

        for (String canvasIdStr : canvasIds) {
            Long canvasId = Long.parseLong(canvasIdStr);
            String requestId = requestService.enqueue(
                    canvasId, eventMsg.getUserId(),
                    TriggerType.EVENT, NodeType.EVENT_TRIGGER,
                    tag, eventMsg.getPayload(), msgId); // NOTE: Verify the enqueue() method signature matches before implementation. The plan uses 7 parameters (canvasId, userId, triggerType, nodeType, tag, payload, msgId).
            disruptorService.publishRequest(requestId);
            log.info("[EVENT_CONSUMER] Dispatched to Disruptor canvasId={} userId={} tag={}",
                    canvasId, eventMsg.getUserId(), tag);
        }
    }
}
```

- [ ] **Step 6: Implement MqNodeTriggerConsumer**

```java
package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.common.enums.TriggerType;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.chovy.canvas.engine.request.CanvasExecutionRequestService;
import org.chovy.canvas.infrastructure.mq.MqTriggerMessage;
import org.chovy.canvas.infrastructure.redis.TriggerRouteService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_MQ",
        consumerGroup = "canvas_mq_cg",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.ORDERLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 10
)
public class MqNodeTriggerConsumer implements RocketMQListener<MessageExt> {

    private final ObjectMapper objectMapper;
    private final TriggerRouteService routeService;
    private final CanvasDisruptorService disruptorService;
    private final CanvasExecutionRequestService requestService;

    @Override
    public void onMessage(MessageExt message) {
        String tag = message.getTags();
        String msgId = message.getMsgId();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("[MQ_NODE_CONSUMER] Received message tag={} msgId={}", tag, msgId);

        if (!routeService.isRouteReady()) {
            throw new IllegalStateException("MQ trigger route table is not ready");
        }

        Set<String> canvasIds = routeService.getCanvasByMqTopic(tag);
        if (canvasIds.isEmpty()) {
            log.warn("[MQ_NODE_CONSUMER] tag={} no matching canvases, discarding msgId={}", tag, msgId);
            return;
        }

        MqTriggerMessage mqMsg;
        try {
            mqMsg = objectMapper.readValue(body, MqTriggerMessage.class);
        } catch (Exception e) {
            log.error("[MQ_NODE_CONSUMER] Failed to parse message msgId={}: {}", msgId, e.getMessage());
            throw new IllegalArgumentException("Invalid MQ trigger message: " + e.getMessage(), e);
        }

        for (String canvasIdStr : canvasIds) {
            Long canvasId = Long.parseLong(canvasIdStr);
            String requestId = requestService.enqueue(
                    canvasId, mqMsg.getUserId(),
                    TriggerType.MQ, NodeType.MQ_TRIGGER,
                    tag, mqMsg.getPayload(), msgId); // NOTE: Verify the enqueue() method signature matches before implementation. The plan uses 7 parameters (canvasId, userId, triggerType, nodeType, tag, payload, msgId).
            disruptorService.publishRequest(requestId);
            log.info("[MQ_NODE_CONSUMER] Dispatched to Disruptor canvasId={} userId={} tag={}",
                    canvasId, mqMsg.getUserId(), tag);
        }
    }
}
```

- [ ] **Step 7: Implement DlqReplayConsumer**

```java
package org.chovy.canvas.engine.trigger;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.annotation.SelectorType;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;
import org.chovy.canvas.dal.mapper.CanvasExecutionDlqMapper;
import org.chovy.canvas.engine.disruptor.CanvasDisruptorService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "CANVAS_TRIGGER_DLQ_REPLAY",
        consumerGroup = "canvas_dlq_replay_cg",
        selectorType = SelectorType.TAG,
        selectorExpression = "*",
        consumeMode = ConsumeMode.CONCURRENTLY,
        messageModel = MessageModel.CLUSTERING,
        consumeThreadNumber = 5
)
public class DlqReplayConsumer implements RocketMQListener<MessageExt> {

    private final CanvasDisruptorService disruptorService;
    private final ObjectMapper objectMapper;
    private final CanvasExecutionDlqMapper dlqMapper;

    @Override
    public void onMessage(MessageExt message) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        String msgId = message.getMsgId();
        log.info("[DLQ_REPLAY_CONSUMER] Received replay message msgId={}", msgId);

        DlqReplayMessage replayMsg;
        try {
            replayMsg = objectMapper.readValue(body, DlqReplayMessage.class);
        } catch (Exception e) {
            log.error("[DLQ_REPLAY_CONSUMER] Failed to parse replay message msgId={}: {}", msgId, e.getMessage());
            throw new IllegalArgumentException("Invalid DLQ replay message: " + e.getMessage(), e);
        }

        if (replayMsg.getDlqId() == null) {
            log.error("[DLQ_REPLAY_CONSUMER] Missing dlqId in replay message msgId={}", msgId);
            throw new IllegalArgumentException("DLQ replay message must contain dlqId");
        }

        // Mark the DLQ record as replayed
        CanvasExecutionDlqDO dlqRecord = dlqMapper.selectById(replayMsg.getDlqId());
        if (dlqRecord != null) {
            log.info("[DLQ_REPLAY_CONSUMER] Replaying dlqId={} canvasId={} userId={}",
                    replayMsg.getDlqId(), dlqRecord.getCanvasId(), dlqRecord.getUserId());
            // Use publishRequest with a requestId derived from the DLQ record
            String requestId = "dlq-replay-" + replayMsg.getDlqId();
            disruptorService.publishRequest(requestId);
        } else {
            log.warn("[DLQ_REPLAY_CONSUMER] DLQ record not found for dlqId={}", replayMsg.getDlqId());
        }
    }
}
```

- [ ] **Step 8: Write tests for EventTriggerConsumer, MqNodeTriggerConsumer, and DlqReplayConsumer**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/EventTriggerConsumerTest.java`:

```java
package org.chovy.canvas.engine.trigger;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventTriggerConsumerTest {

    @Test
    void eventConsumerHasCorrectAnnotation() {
        RocketMQMessageListener annotation = EventTriggerConsumer.class
                .getAnnotation(RocketMQMessageListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topic()).isEqualTo("CANVAS_TRIGGER_EVENT");
        assertThat(annotation.consumerGroup()).isEqualTo("canvas_event_cg");
    }
}
```

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/MqNodeTriggerConsumerTest.java`:

```java
package org.chovy.canvas.engine.trigger;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MqNodeTriggerConsumerTest {

    @Test
    void mqNodeConsumerHasCorrectAnnotation() {
        RocketMQMessageListener annotation = MqNodeTriggerConsumer.class
                .getAnnotation(RocketMQMessageListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topic()).isEqualTo("CANVAS_TRIGGER_MQ");
        assertThat(annotation.consumerGroup()).isEqualTo("canvas_mq_cg");
        assertThat(annotation.consumeMode()).isEqualTo(ConsumeMode.ORDERLY);
    }
}
```

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/DlqReplayConsumerTest.java`:

```java
package org.chovy.canvas.engine.trigger;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DlqReplayConsumerTest {

    @Test
    void dlqReplayConsumerHasCorrectAnnotation() {
        RocketMQMessageListener annotation = DlqReplayConsumer.class
                .getAnnotation(RocketMQMessageListener.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.topic()).isEqualTo("CANVAS_TRIGGER_DLQ_REPLAY");
        assertThat(annotation.consumerGroup()).isEqualTo("canvas_dlq_replay_cg");
    }
}
```

- [ ] **Step 9: Run all consumer tests**

Run: `cd backend && mvn test -pl canvas-engine -Dtest="*TriggerConsumerTest,TopicConfigTest" -v`
Expected: All PASS.

- [ ] **Step 10: Build and verify compile**

Run: `cd backend && mvn compile -pl canvas-engine`
Expected: Build success.

- [ ] **Step 11: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/
git commit -m "feat: implement 4 independent MQ consumers for trigger type separation"
```

---

### CanvasExecutionDlqMapper and CanvasExecutionDlqDO Definitions

If `CanvasExecutionDlqMapper` and `CanvasExecutionDlqDO` don't exist yet (they are referenced by `DlqReplayConsumer` and the tenant-isolation plan), create them as follows:

**CanvasExecutionDlqDO.java:**

```java
package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("canvas_execution_dlq")
public class CanvasExecutionDlqDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private Long canvasId;
    private String userId;
    private String failedNodeId;
    private String failedNodeType;
    private String errorMsg;
    private Integer retryCount;
    private String triggerPayload;
    private String triggerType;
    private String triggerNodeType;
    private String matchKey;
    private LocalDateTime failedAt;
}
```

**CanvasExecutionDlqMapper.java:**

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.CanvasExecutionDlqDO;

@Mapper
public interface CanvasExecutionDlqMapper extends BaseMapper<CanvasExecutionDlqDO> {
}
```

**Flyway migration (use the next available version number):**

```sql
CREATE TABLE canvas_execution_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    canvas_id BIGINT NOT NULL,
    user_id VARCHAR(128),
    failed_node_id VARCHAR(64),
    failed_node_type VARCHAR(32),
    error_msg TEXT,
    retry_count INT DEFAULT 0,
    trigger_payload TEXT,
    trigger_type VARCHAR(32),
    trigger_node_type VARCHAR(32),
    match_key VARCHAR(128),
    failed_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas_id (canvas_id),
    INDEX idx_execution_id (execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```