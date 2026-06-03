# Delivery Queue (Outbox Pattern) Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Decouple engine from delivery platform via RocketMQ CANVAS_DELIVERY Topic. Engine publishes delivery request → MQ → independent consumer handles HTTP call + retry + receipt.

**Architecture:** Engine writes to outbox table (PENDING), sends to MQ, then immediately advances DAG. Consumer calls delivery platform via RestTemplate (not WebClient - we're on Spring MVC), handles retry and receipt. Scheduled job reconciles stale PENDING records.

**Tech Stack:** RocketMQ, Spring Boot, MySQL outbox table, RestTemplate

---

### Task 1: Create Outbox Table and Delivery Message Producer

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V82__canvas_delivery_outbox.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryRequest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`

- [ ] **Step 1: Write failing test for outbox persistence**

```java
package org.chovy.canvas.engine.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryOutboxTest {

    @Autowired
    private DeliveryOutboxMapper outboxMapper;

    @Test
    void insertOutbox_shouldPersistWithPendingStatus() {
        DeliveryOutboxDO outbox = new DeliveryOutboxDO();
        outbox.setExecutionId(12345L);
        outbox.setNodeId("node-abc");
        outbox.setDeliveryType("SMS");
        outbox.setPayload("{\"phone\":\"13800138000\",\"content\":\"test\"}");

        outboxMapper.insert(outbox);

        assertThat(outbox.getId()).isNotNull();
        assertThat(outbox.getStatus()).isEqualTo("PENDING");
        assertThat(outbox.getCreatedAt()).isNotNull();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryOutboxTest -v`
Expected: FAIL - DeliveryOutboxDO class not found

- [ ] **Step 3: Create Flyway migration V82**

```sql
-- backend/canvas-engine/src/main/resources/db/migration/V82__canvas_delivery_outbox.sql
CREATE TABLE canvas_delivery_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id BIGINT NOT NULL COMMENT 'DAG execution ID',
    node_id VARCHAR(64) NOT NULL COMMENT 'Node that triggered delivery',
    delivery_type VARCHAR(32) NOT NULL COMMENT 'SMS/EMAIL/PUSH',
    payload TEXT NOT NULL COMMENT 'JSON payload for delivery platform',
    status VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    attempt_count INT DEFAULT 0,
    last_error TEXT,
    sent_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pending_status (status, created_at),
    INDEX idx_execution (execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Delivery outbox for at-least-once guarantee';
```

- [ ] **Step 4: Create DeliveryOutboxDO entity**

```java
package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("canvas_delivery_outbox")
public class DeliveryOutboxDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long executionId;
    private String nodeId;
    private String deliveryType;
    private String payload;
    private String status;
    private Integer attemptCount;
    private String lastError;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: Create DeliveryOutboxMapper**

```java
package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DeliveryOutboxMapper extends BaseMapper<DeliveryOutboxDO> {

    @Update("UPDATE canvas_delivery_outbox SET status = #{status}, sent_at = #{sentAt}, " +
            "attempt_count = attempt_count + 1 WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("sentAt") LocalDateTime sentAt);

    @Update("UPDATE canvas_delivery_outbox SET status = 'FAILED', last_error = #{error}, " +
            "attempt_count = attempt_count + 1 WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("error") String error);

    @Select("SELECT * FROM canvas_delivery_outbox WHERE status = 'PENDING' " +
            "AND created_at < #{threshold} ORDER BY created_at LIMIT 100")
    List<DeliveryOutboxDO> selectStalePending(@Param("threshold") LocalDateTime threshold);
}
```

- [ ] **Step 6: Create DeliveryRequest DTO**

```java
package org.chovy.canvas.engine.delivery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequest implements Serializable {
    private Long outboxId;
    private Long executionId;
    private String nodeId;
    private String deliveryType;
    private String payload;
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryOutboxTest -v`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V82__canvas_delivery_outbox.sql
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxDO.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxMapper.java
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryRequest.java
git commit -m "feat: add delivery outbox table and entity for at-least-once guarantee"
```

---

### Task 2: Modify ReachDeliveryService to Use Outbox + MQ

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceTest.java`

- [ ] **Step 1: Write failing test for async delivery**

```java
package org.chovy.canvas.engine.delivery;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReachDeliveryServiceTest {

    @Autowired
    private ReachDeliveryService deliveryService;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @Autowired
    private DeliveryOutboxMapper outboxMapper;

    @Test
    void deliverAsync_shouldWriteOutboxAndSendMq() {
        DeliveryRequest request = new DeliveryRequest();
        request.setExecutionId(12345L);
        request.setNodeId("node-sms");
        request.setDeliveryType("SMS");
        request.setPayload("{\"phone\":\"13800138000\"}");

        deliveryService.deliverAsync(request);

        // Verify outbox written
        var outboxCaptor = ArgumentCaptor.forClass(DeliveryOutboxDO.class);
        verify(outboxMapper).insert(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getStatus()).isEqualTo("PENDING");

        // Verify MQ sent
        verify(rocketMQTemplate).syncSend(eq("CANVAS_DELIVERY"), any(DeliveryRequest.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ReachDeliveryServiceTest -v`
Expected: FAIL - deliverAsync method not found or wrong signature

- [ ] **Step 3: Implement async delivery in ReachDeliveryService**

```java
// Add to existing ReachDeliveryService.java

@Autowired
private DeliveryOutboxMapper outboxMapper;

@Autowired
private RocketMQTemplate rocketMQTemplate;

public void deliverAsync(DeliveryRequest request) {
    // 1. Write to outbox table (PENDING)
    DeliveryOutboxDO outbox = new DeliveryOutboxDO();
    outbox.setExecutionId(request.getExecutionId());
    outbox.setNodeId(request.getNodeId());
    outbox.setDeliveryType(request.getDeliveryType());
    outbox.setPayload(request.getPayload());
    outbox.setStatus("PENDING");
    outbox.setAttemptCount(0);
    outboxMapper.insert(outbox);

    // 2. Update request with outbox ID
    request.setOutboxId(outbox.getId());

    // 3. Send to RocketMQ CANVAS_DELIVERY
    rocketMQTemplate.syncSend("CANVAS_DELIVERY", request);

    // 4. Return immediately — DAG can advance
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=ReachDeliveryServiceTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServiceTest.java
git commit -m "feat: implement async delivery with outbox pattern in ReachDeliveryService"
```

---

### Task 3: Implement Delivery Consumer with Retry and Receipt

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryConsumer.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryConsumerTest.java`

- [ ] **Step 1: Write failing test for delivery consumer**

```java
package org.chovy.canvas.engine.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DeliveryConsumerTest {

    @Autowired
    private DeliveryConsumer consumer;

    @MockBean
    private RestTemplate deliveryRestTemplate;

    @MockBean
    private DeliveryOutboxMapper outboxMapper;

    @Test
    void onMessage_success_shouldMarkSentAndRecordReceipt() {
        DeliveryRequest msg = new DeliveryRequest();
        msg.setOutboxId(100L);
        msg.setExecutionId(12345L);
        msg.setNodeId("node-sms");
        msg.setDeliveryType("SMS");
        msg.setPayload("{\"phone\":\"13800138000\"}");

        // Mock successful delivery platform response
        when(deliveryRestTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenReturn("{\"code\":0,\"msgId\":\"abc123\"}");

        consumer.onMessage(msg);

        verify(outboxMapper).updateStatus(eq(100L), eq("SENT"), any());
    }

    @Test
    void onMessage_failure_shouldMarkFailedAndRethrow() {
        DeliveryRequest msg = new DeliveryRequest();
        msg.setOutboxId(101L);
        msg.setPayload("{}");

        when(deliveryRestTemplate.postForObject(anyString(), any(), eq(String.class)))
            .thenThrow(new RuntimeException("Connection refused"));

        org.junit.jupiter.api.Assertions.assertThrows(
            RuntimeException.class,
            () -> consumer.onMessage(msg)
        );

        verify(outboxMapper).markFailed(eq(101L), contains("Connection refused"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryConsumerTest -v`
Expected: FAIL - DeliveryConsumer class not found

- [ ] **Step 3: Implement DeliveryConsumer**

```java
package org.chovy.canvas.engine.delivery;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RocketMQMessageListener(
    topic = "CANVAS_DELIVERY",
    consumerGroup = "canvas_delivery_cg",
    consumeThreadMax = 20
)
public class DeliveryConsumer implements RocketMQListener<DeliveryRequest> {

    @Autowired
    private RestTemplate deliveryRestTemplate;

    @Autowired
    private DeliveryOutboxMapper outboxMapper;

    @Value("${canvas.delivery.platform.url:http://localhost:8081}")
    private String deliveryPlatformUrl;

    @Override
    public void onMessage(DeliveryRequest msg) {
        log.info("Processing delivery: outboxId={}, type={}", msg.getOutboxId(), msg.getDeliveryType());

        try {
            // Call delivery platform via RestTemplate (sync, non-reactive)
            String url = deliveryPlatformUrl + "/" + msg.getDeliveryType().toLowerCase() + "/send";
            String response = deliveryRestTemplate.postForObject(url, msg.getPayload(), String.class);

            // Mark outbox SENT
            outboxMapper.updateStatus(msg.getOutboxId(), "SENT", LocalDateTime.now());

            log.info("Delivery succeeded: outboxId={}, response={}", msg.getOutboxId(), response);

        } catch (Exception e) {
            log.error("Delivery failed: outboxId={}, error={}", msg.getOutboxId(), e.getMessage());

            // Mark FAILED - RocketMQ will retry based on retry policy
            outboxMapper.markFailed(msg.getOutboxId(), e.getMessage());

            // Rethrow to trigger MQ retry
            throw e;
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryConsumerTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryConsumer.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryConsumerTest.java
git commit -m "feat: implement delivery consumer with retry and receipt handling"
```

---

### Task 4: Implement PENDING Reconciliation Job

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJobTest.java`

- [ ] **Step 1: Write failing test for reconciliation job**

```java
package org.chovy.canvas.engine.delivery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import java.time.LocalDateTime;
import java.util.List;
import static org.mockito.Mockito.*;

@SpringBootTest
class DeliveryReconciliationJobTest {

    @Autowired
    private DeliveryReconciliationJob job;

    @MockBean
    private DeliveryOutboxMapper outboxMapper;

    @MockBean
    private RocketMQTemplate rocketMQTemplate;

    @Test
    void reconcilePending_shouldResendStalePendingRecords() {
        // Setup: 2 stale PENDING records
        DeliveryOutboxDO stale1 = new DeliveryOutboxDO();
        stale1.setId(1L);
        stale1.setExecutionId(100L);
        stale1.setNodeId("n1");
        stale1.setDeliveryType("SMS");
        stale1.setPayload("{}");

        DeliveryOutboxDO stale2 = new DeliveryOutboxDO();
        stale2.setId(2L);
        stale2.setExecutionId(101L);
        stale2.setNodeId("n2");
        stale2.setDeliveryType("EMAIL");
        stale2.setPayload("{}");

        when(outboxMapper.selectStalePending(any(LocalDateTime.class)))
            .thenReturn(List.of(stale1, stale2));

        job.reconcilePending();

        // Verify both were resent to MQ
        verify(rocketMQTemplate, times(2)).syncSend(eq("CANVAS_DELIVERY"), any(DeliveryRequest.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryReconciliationJobTest -v`
Expected: FAIL - DeliveryReconciliationJob class not found

- [ ] **Step 3: Implement DeliveryReconciliationJob**

```java
package org.chovy.canvas.engine.delivery;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class DeliveryReconciliationJob {

    @Autowired
    private DeliveryOutboxMapper outboxMapper;

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // Scan every 5 minutes for PENDING records older than 5 minutes
    @Scheduled(fixedRate = 300000)
    public void reconcilePending() {
        log.info("Starting PENDING delivery reconciliation...");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<DeliveryOutboxDO> staleRecords = outboxMapper.selectStalePending(threshold);

        log.info("Found {} stale PENDING records", staleRecords.size());

        for (DeliveryOutboxDO outbox : staleRecords) {
            try {
                DeliveryRequest request = new DeliveryRequest(
                    outbox.getId(),
                    outbox.getExecutionId(),
                    outbox.getNodeId(),
                    outbox.getDeliveryType(),
                    outbox.getPayload()
                );

                rocketMQTemplate.syncSend("CANVAS_DELIVERY", request);
                log.info("Resent stale delivery: outboxId={}", outbox.getId());

            } catch (Exception e) {
                log.error("Failed to resend stale delivery: outboxId={}, error={}",
                    outbox.getId(), e.getMessage());
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=DeliveryReconciliationJobTest -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJobTest.java
git commit -m "feat: add PENDING delivery reconciliation job for at-least-once guarantee"
```

---

### Task 5: Add RocketMQ Topic Configuration

**Files:**
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [ ] **Step 1: Add CANVAS_DELIVERY topic configuration**

```yaml
# Add to application.yml under rocketmq section
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: canvas-producer-group
  topic:
    canvas-delivery: CANVAS_DELIVERY
```

- [ ] **Step 2: Verify application starts**

Run: `cd backend/canvas-engine && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"`
Expected: Application starts successfully, no RocketMQ connection errors (if RocketMQ is running)

- [ ] **Step 3: Commit**

```bash
git add backend/canvas-engine/src/main/resources/application.yml
git commit -m "feat: add CANVAS_DELIVERY RocketMQ topic configuration"
```
