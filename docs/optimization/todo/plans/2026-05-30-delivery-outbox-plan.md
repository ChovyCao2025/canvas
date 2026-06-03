# Delivery Outbox + PENDING Reconciliation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Implement outbox pattern for delivery requests. Every delivery request is first written to DB as PENDING, then asynchronously sent. Stale PENDING records are reconciled by a scheduled job. Dead-letter (DEAD_LETTER) records can be manually re-delivered via API.

**Architecture:** ReachDeliveryService.insertPending writes PENDING record to canvas_delivery_dlq table. RocketMQ consumer picks up the message, calls the reach platform, and marks the record SENT or FAILED. DeliveryReconciliationJob scans for PENDING records older than 5 minutes and resends them; records that exceed max_attempts become DEAD_LETTER. DlqRedeliveryService + DlqController provide manual re-delivery of DEAD_LETTER records.

**Tech Stack:** MySQL (Flyway V92), MyBatis-Plus, RocketMQ, Spring Scheduler, JUnit 5, Mockito

---

### Task 1: Create DLQ table (Flyway V92) + DeliveryDlqDO + Mapper

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V92__delivery_dlq.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DeliveryDlqDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/DeliveryDlqMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java`

- [ ] **Step 1: Write failing test for outbox insert and status update**

```java
package org.chovy.canvas.engine.delivery;

import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeliveryOutboxServiceTest {

    private DeliveryDlqMapper mapper;
    private DeliveryOutboxService service;

    @BeforeEach
    void setUp() {
        mapper = mock(DeliveryDlqMapper.class);
        service = new DeliveryOutboxService(mapper);
    }

    @Test
    void insertPending_createsOutboxRecordWithPendingStatus() {
        when(mapper.insert(any())).thenReturn(1);

        service.insertPending("exec-1", "node-A", "SMS", "sms_channel",
                "{\"userId\":\"u1\",\"content\":\"hello\"}", "exec-1:node-A:SMS:u1");

        ArgumentCaptor<DeliveryDlqDO> captor = ArgumentCaptor.forClass(DeliveryDlqDO.class);
        verify(mapper).insert(captor.capture());

        DeliveryDlqDO record = captor.getValue();
        assertEquals("exec-1", record.getExecutionId());
        assertEquals("node-A", record.getNodeId());
        assertEquals("SMS", record.getDeliveryType());
        assertEquals("sms_channel", record.getChannel());
        assertEquals("PENDING", record.getStatus());
        assertEquals("exec-1:node-A:SMS:u1", record.getIdempotencyKey());
        assertEquals(0, record.getAttemptCount());
        assertEquals(3, record.getMaxAttempts());
    }

    @Test
    void markSent_updatesStatusToSentAndSetsSentAt() {
        when(mapper.updateById(any())).thenReturn(1);

        service.markSent(42L);

        ArgumentCaptor<DeliveryDlqDO> captor = ArgumentCaptor.forClass(DeliveryDlqDO.class);
        verify(mapper).updateById(captor.capture());

        DeliveryDlqDO record = captor.getValue();
        assertEquals(42L, record.getId());
        assertEquals("SENT", record.getStatus());
        assertNotNull(record.getSentAt());
    }

    @Test
    void markFailed_incrementsAttemptAndSetsError() {
        when(mapper.updateById(any())).thenReturn(1);

        service.markFailed(42L, "connection timeout");

        ArgumentCaptor<DeliveryDlqDO> captor = ArgumentCaptor.forClass(DeliveryDlqDO.class);
        verify(mapper).updateById(captor.capture());

        DeliveryDlqDO record = captor.getValue();
        assertEquals(42L, record.getId());
        assertEquals("FAILED", record.getStatus());
        assertEquals("connection timeout", record.getErrorMessage());
        assertNotNull(record.getLastAttemptAt());
    }

    @Test
    void escalateToDeadLetter_whenMaxAttemptsExceeded() {
        when(mapper.updateById(any())).thenReturn(1);

        service.escalateToDeadLetter(42L);

        ArgumentCaptor<DeliveryDlqDO> captor = ArgumentCaptor.forClass(DeliveryDlqDO.class);
        verify(mapper).updateById(captor.capture());

        DeliveryDlqDO record = captor.getValue();
        assertEquals(42L, record.getId());
        assertEquals("DEAD_LETTER", record.getStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeliveryOutboxServiceTest 2>&1 | tail -5
```

Expected output: Compilation error — classes do not exist yet.

- [ ] **Step 3: Create Flyway migration V92**

```sql
-- V92__delivery_dlq.sql
CREATE TABLE canvas_delivery_dlq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    execution_id VARCHAR(64) NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    delivery_type VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    payload TEXT NOT NULL,
    idempotency_key VARCHAR(128),
    status VARCHAR(16) DEFAULT 'PENDING',
    attempt_count INT DEFAULT 0,
    max_attempts INT DEFAULT 3,
    last_attempt_at DATETIME,
    sent_at DATETIME,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pending_status (status, created_at),
    INDEX idx_execution_node (execution_id, node_id),
    UNIQUE KEY uk_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4: Create DeliveryDlqDO**

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
@TableName("canvas_delivery_dlq")
public class DeliveryDlqDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private String nodeId;
    private String deliveryType;
    private String channel;
    private String payload;
    private String idempotencyKey;
    private String status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_DEAD_LETTER = "DEAD_LETTER";
}
```

- [ ] **Step 5: Create DeliveryDlqMapper**

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;

@Mapper
public interface DeliveryDlqMapper extends BaseMapper<DeliveryDlqDO> {
}
```

- [ ] **Step 6: Implement DeliveryOutboxService**

```java
package org.chovy.canvas.engine.delivery;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeliveryOutboxService {

    private final DeliveryDlqMapper outboxMapper;

    /**
     * Insert a PENDING outbox record before sending to the reach platform.
     */
    public void insertPending(String executionId, String nodeId,
                              String deliveryType, String channel,
                              String payloadJson, String idempotencyKey) {
        DeliveryDlqDO outbox = DeliveryDlqDO.builder()
                .executionId(executionId)
                .nodeId(nodeId)
                .deliveryType(deliveryType)
                .channel(channel)
                .payload(payloadJson)
                .idempotencyKey(idempotencyKey)
                .status(DeliveryDlqDO.STATUS_PENDING)
                .attemptCount(0)
                .maxAttempts(3)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        outboxMapper.insert(outbox);
    }

    /**
     * Mark outbox record as SENT after successful delivery.
     */
    public void markSent(Long outboxId) {
        DeliveryDlqDO update = DeliveryDlqDO.builder()
                .id(outboxId)
                .status(DeliveryDlqDO.STATUS_SENT)
                .sentAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        outboxMapper.updateById(update);
    }

    /**
     * Mark outbox record as FAILED after delivery failure, incrementing attempt count.
     */
    public void markFailed(Long outboxId, String errorMessage) {
        DeliveryDlqDO existing = outboxMapper.selectById(outboxId);
        if (existing == null) return;

        DeliveryDlqDO update = DeliveryDlqDO.builder()
                .id(outboxId)
                .status(DeliveryDlqDO.STATUS_FAILED)
                .attemptCount(existing.getAttemptCount() + 1)
                .errorMessage(errorMessage != null
                        ? errorMessage.substring(0, Math.min(500, errorMessage.length()))
                        : "unknown")
                .lastAttemptAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (existing.getAttemptCount() + 1 >= existing.getMaxAttempts()) {
            update.setStatus(DeliveryDlqDO.STATUS_DEAD_LETTER);
        }
        outboxMapper.updateById(update);
    }

    /**
     * Escalate a record to DEAD_LETTER when reconciliation exhausts max attempts.
     */
    public void escalateToDeadLetter(Long outboxId) {
        DeliveryDlqDO update = DeliveryDlqDO.builder()
                .id(outboxId)
                .status(DeliveryDlqDO.STATUS_DEAD_LETTER)
                .updatedAt(LocalDateTime.now())
                .build();
        outboxMapper.updateById(update);
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeliveryOutboxServiceTest 2>&1 | tail -5
```

Expected output: `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 8: Commit**

```bash
cd backend && git add backend/canvas-engine/src/main/resources/db/migration/V92__delivery_dlq.sql backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DeliveryDlqDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/DeliveryDlqMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryOutboxService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryOutboxServiceTest.java && git commit -m "feat: add delivery DLQ table (V92), DeliveryDlqDO, Mapper, and Service"
```

---

### Task 2: Implement PENDING reconciliation job

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJobTest.java`

- [ ] **Step 1: Write failing test for stale PENDING reconciliation**

```java
package org.chovy.canvas.engine.delivery;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeliveryReconciliationJobTest {

    private DeliveryDlqMapper mapper;
    private RocketMQTemplate rocketMQTemplate;
    private DeliveryReconciliationJob job;

    @BeforeEach
    void setUp() {
        mapper = mock(DeliveryDlqMapper.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        DeliveryOutboxService outboxService = new DeliveryOutboxService(mapper);
        job = new DeliveryReconciliationJob(mapper, outboxService, rocketMQTemplate);
    }

    @Test
    void reconcile_resendsStalePendingRecord() {
        // A PENDING record older than 5 minutes
        DeliveryDlqDO staleRecord = DeliveryDlqDO.builder()
                .id(1L)
                .executionId("exec-1")
                .nodeId("node-A")
                .deliveryType("SMS")
                .channel("sms_channel")
                .payload("{\"userId\":\"u1\",\"content\":\"hello\"}")
                .idempotencyKey("idem-1")
                .status("PENDING")
                .attemptCount(1)
                .maxAttempts(3)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(staleRecord));

        job.reconcile();

        // Verify stale record was resent to MQ
        verify(rocketMQTemplate).convertAndSend(eq("CANVAS_DELIVERY"), anyString());
        // Verify attempt count was NOT escalated to DEAD_LETTER (attemptCount=1 < maxAttempts=3)
        verify(mapper, never()).updateById(argThat(d ->
                d.getStatus() != null && d.getStatus().equals("DEAD_LETTER")));
    }

    @Test
    void reconcile_escalatesExhaustedRecordToDeadLetter() {
        // A PENDING record with attemptCount >= maxAttempts
        DeliveryDlqDO exhaustedRecord = DeliveryDlqDO.builder()
                .id(2L)
                .executionId("exec-2")
                .nodeId("node-B")
                .deliveryType("EMAIL")
                .channel("email_channel")
                .payload("{\"userId\":\"u2\"}")
                .idempotencyKey("idem-2")
                .status("PENDING")
                .attemptCount(3)
                .maxAttempts(3)
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(exhaustedRecord));

        job.reconcile();

        // Exhausted record should NOT be resent; it should be escalated to DEAD_LETTER
        verify(rocketMQTemplate, never()).convertAndSend(anyString(), any());
        // Verify escalation to DEAD_LETTER
        verify(mapper).updateById(argThat(d ->
                d.getId().equals(2L) && d.getStatus().equals("DEAD_LETTER")));
    }

    @Test
    void reconcile_noStaleRecords_doesNothing() {
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        job.reconcile();

        verify(rocketMQTemplate, never()).convertAndSend(anyString(), any());
        verify(mapper, never()).updateById(any());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeliveryReconciliationJobTest 2>&1 | tail -5
```

Expected output: Compilation error — class does not exist yet.

- [ ] **Step 3: Implement DeliveryReconciliationJob**

```java
package org.chovy.canvas.engine.delivery;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryReconciliationJob {

    private static final int STALE_THRESHOLD_MINUTES = 5;
    private static final int BATCH_SIZE = 100;

    private final DeliveryDlqMapper outboxMapper;
    private final DeliveryOutboxService outboxService;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Scan for stale PENDING records every 60 seconds.
     * Records older than 5 minutes with attemptCount < maxAttempts are resent.
     * Records with attemptCount >= maxAttempts are escalated to DEAD_LETTER.
     */
    @Scheduled(fixedRate = 60000)
    public void reconcile() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_THRESHOLD_MINUTES);
        List<DeliveryDlqDO> staleRecords = outboxMapper.selectList(
                new LambdaQueryWrapper<DeliveryDlqDO>()
                        .eq(DeliveryDlqDO::getStatus, DeliveryDlqDO.STATUS_PENDING)
                        .le(DeliveryDlqDO::getCreatedAt, threshold)
                        .last("LIMIT " + BATCH_SIZE));

        if (staleRecords.isEmpty()) {
            return;
        }

        log.info("[RECONCILE] Found {} stale PENDING records", staleRecords.size());

        for (DeliveryDlqDO record : staleRecords) {
            if (record.getAttemptCount() >= record.getMaxAttempts()) {
                log.warn("[RECONCILE] Escalating to DEAD_LETTER id={} attempts={}/{}",
                        record.getId(), record.getAttemptCount(), record.getMaxAttempts());
                outboxService.escalateToDeadLetter(record.getId());
                continue;
            }
            // Resend to MQ for async delivery
            String messageJson = record.getPayload();
            rocketMQTemplate.convertAndSend("CANVAS_DELIVERY", messageJson);
            log.info("[RECONCILE] Resent stale record id={} attempt={}/{}",
                    record.getId(), record.getAttemptCount(), record.getMaxAttempts());
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DeliveryReconciliationJobTest 2>&1 | tail -5
```

Expected output: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 5: Commit**

```bash
cd backend && git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJob.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DeliveryReconciliationJobTest.java && git commit -m "feat: add delivery PENDING reconciliation job with DLQ escalation"
```

---

### Task 3: Implement DLQ redelivery service + controller

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DlqRedeliveryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DlqRedeliveryController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DlqRedeliveryServiceTest.java`

- [ ] **Step 1: Write failing test for DLQ manual redelivery**

```java
package org.chovy.canvas.engine.delivery;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DlqRedeliveryServiceTest {

    private DeliveryDlqMapper mapper;
    private RocketMQTemplate rocketMQTemplate;
    private DeliveryOutboxService outboxService;
    private DlqRedeliveryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(DeliveryDlqMapper.class);
        rocketMQTemplate = mock(RocketMQTemplate.class);
        outboxService = new DeliveryOutboxService(mapper);
        service = new DlqRedeliveryService(mapper, outboxService, rocketMQTemplate);
    }

    @Test
    void redeliver_resetsStatusToPendingAndResends() {
        DeliveryDlqDO deadLetterRecord = DeliveryDlqDO.builder()
                .id(99L)
                .executionId("exec-dlq")
                .nodeId("node-D")
                .deliveryType("SMS")
                .channel("sms_channel")
                .payload("{\"userId\":\"u-dlq\"}")
                .idempotencyKey("idem-dlq")
                .status("DEAD_LETTER")
                .attemptCount(3)
                .maxAttempts(3)
                .createdAt(java.time.LocalDateTime.now().minusHours(1))
                .build();

        when(mapper.selectById(99L)).thenReturn(deadLetterRecord);
        when(mapper.updateById(any())).thenReturn(1);

        DlqRedeliveryService.RedeliveryResult result = service.redeliver(99L);

        assertTrue(result.success());
        assertEquals(99L, result.outboxId());

        // Verify record was reset to PENDING with attemptCount=0
        verify(mapper).updateById(argThat(d ->
                d.getId().equals(99L)
                && d.getStatus().equals("PENDING")
                && d.getAttemptCount() == 0));

        // Verify payload was resent to MQ
        verify(rocketMQTemplate).convertAndSend(eq("CANVAS_DELIVERY"), anyString());
    }

    @Test
    void redeliver_nonDeadLetterRecord_fails() {
        DeliveryDlqDO sentRecord = DeliveryDlqDO.builder()
                .id(100L)
                .status("SENT")
                .build();

        when(mapper.selectById(100L)).thenReturn(sentRecord);

        DlqRedeliveryService.RedeliveryResult result = service.redeliver(100L);

        assertFalse(result.success());
        assertEquals("Record is not DEAD_LETTER, current status: SENT", result.errorMessage());

        // Verify no MQ send
        verify(rocketMQTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    void redeliver_nonExistentRecord_fails() {
        when(mapper.selectById(999L)).thenReturn(null);

        DlqRedeliveryService.RedeliveryResult result = service.redeliver(999L);

        assertFalse(result.success());
        assertEquals("Outbox record not found: 999", result.errorMessage());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DlqRedeliveryServiceTest 2>&1 | tail -5
```

Expected output: Compilation error — class does not exist yet.

- [ ] **Step 3: Implement DlqRedeliveryService**

```java
package org.chovy.canvas.engine.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.chovy.canvas.dal.dataobject.DeliveryDlqDO;
import org.chovy.canvas.dal.mapper.DeliveryDlqMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DlqRedeliveryService {

    private final DeliveryDlqMapper outboxMapper;
    private final DeliveryOutboxService outboxService;
    private final RocketMQTemplate rocketMQTemplate;

    /**
     * Manually re-deliver a DEAD_LETTER record by resetting it to PENDING
     * and resending the payload to MQ.
     */
    public RedeliveryResult redeliver(Long outboxId) {
        DeliveryDlqDO record = outboxMapper.selectById(outboxId);
        if (record == null) {
            return RedeliveryResult.fail(outboxId, "Outbox record not found: " + outboxId);
        }
        if (!DeliveryDlqDO.STATUS_DEAD_LETTER.equals(record.getStatus())) {
            return RedeliveryResult.fail(outboxId,
                    "Record is not DEAD_LETTER, current status: " + record.getStatus());
        }

        // Reset to PENDING with attemptCount=0 for fresh delivery
        DeliveryDlqDO update = DeliveryDlqDO.builder()
                .id(outboxId)
                .status(DeliveryDlqDO.STATUS_PENDING)
                .attemptCount(0)
                .errorMessage(null)
                .updatedAt(java.time.LocalDateTime.now())
                .build();
        outboxMapper.updateById(update);

        // Resend payload to MQ
        rocketMQTemplate.convertAndSend("CANVAS_DELIVERY", record.getPayload());
        log.info("[DLQ_REDELIVER] Manual redelivery initiated outboxId={}", outboxId);

        return RedeliveryResult.success(outboxId);
    }

    /**
     * Result of a DLQ re-delivery attempt.
     */
    public record RedeliveryResult(
            Long outboxId,
            boolean success,
            String errorMessage
    ) {
        static RedeliveryResult success(Long outboxId) {
            return new RedeliveryResult(outboxId, true, null);
        }
        static RedeliveryResult fail(Long outboxId, String errorMessage) {
            return new RedeliveryResult(outboxId, false, errorMessage);
        }
    }
}
```

- [ ] **Step 4: Implement DlqRedeliveryController**

```java
package org.chovy.canvas.engine.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
public class DlqRedeliveryController {

    private final DlqRedeliveryService dlqRedeliveryService;

    /**
     * Manually re-deliver a DEAD_LETTER delivery record.
     * Resets the record to PENDING and resends the payload to MQ.
     */
    @PostMapping("/{outboxId}/redeliver")
    public ResponseEntity<DlqRedeliveryResponse> redeliver(@PathVariable Long outboxId) {
        DlqRedeliveryService.RedeliveryResult result = dlqRedeliveryService.redeliver(outboxId);
        if (result.success()) {
            return ResponseEntity.ok(new DlqRedeliveryResponse(outboxId, "REDELIVERED", null));
        }
        return ResponseEntity.badRequest()
                .body(new DlqRedeliveryResponse(outboxId, "FAILED", result.errorMessage()));
    }

    record DlqRedeliveryResponse(Long outboxId, String status, String errorMessage) {}
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
cd backend && mvn test -pl canvas-engine -Dtest=DlqRedeliveryServiceTest 2>&1 | tail -5
```

Expected output: `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Commit**

```bash
cd backend && git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DlqRedeliveryService.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/DlqRedeliveryController.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/DlqRedeliveryServiceTest.java && git commit -m "feat: add DLQ manual redelivery service and controller"
```