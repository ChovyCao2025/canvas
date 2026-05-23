# Performance Testing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full local performance-testing harness that can generate load, trace each run with `perfRunId`, verify concurrent data correctness, and produce conservative production capacity estimates.

**Architecture:** Add a nullable `perf_run_id` ledger field to the existing execution facts, propagate it from payloads into event logs, execution requests, executions, and DLQ rows, then keep Prometheus for aggregate resource signals. Put load drivers, verifier, capacity formulas, and run documentation under `tools/perf` so the harness is isolated from core business code.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis Plus, MySQL 8, Redis 7, RocketMQ 5.x, Node.js 18 built-in test runner, Maven/JUnit 5/Mockito/AssertJ.

---

## Preflight

The current workspace has unresolved merge conflicts. Do not execute this plan until this command prints no `UU` or `AA` entries:

```bash
git status --short | rg '^(UU|AA) '
```

Expected before implementation starts: no output.

If the command prints conflicted files, resolve or park those conflicts first. This plan intentionally does not resolve existing unrelated conflicts.

## File Structure

- Modify: `backend/canvas-engine/src/main/resources/db/migration/V50__perf_run_tracking.sql`
  - Adds nullable `perf_run_id` columns and indexes to existing ledgers.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLog.java`
  - Maps `event_log.perf_run_id`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecution.java`
  - Maps `canvas_execution.perf_run_id`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequest.java`
  - Maps `canvas_execution_request.perf_run_id`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlq.java`
  - Maps `canvas_execution_dlq.perf_run_id`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java`
  - Persists `perf_run_id` in `insertIgnore`.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/perf/PerfRunContext.java`
  - Extracts and validates `perfRunId` from payload maps.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
  - Carries `perfRunId` inside execution state.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java`
  - Persists event `perfRunId`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java`
  - Persists MQ request `perfRunId`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`
  - Passes request `perfRunId` back into payload before execution.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
  - Sets execution and DLQ `perfRunId`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ExecutionController.java`
  - Direct calls keep `perfRunId` in input payload and idempotency key.
- Create: `tools/perf/perf-runner.mjs`
  - Sends HTTP event, direct-call, and audience-compute pressure.
- Create: `tools/perf/perf-runner.test.mjs`
  - Tests deterministic payload generation and concurrency batching.
- Create: `tools/perf/mq-producer/pom.xml`
  - Small Java CLI module for RocketMQ pressure.
- Create: `tools/perf/mq-producer/src/main/java/org/chovy/canvas/perf/mq/PerfMqProducer.java`
  - Sends `CANVAS_MQ_TRIGGER` messages with deterministic `sourceMsgId`.
- Create: `tools/perf/mq-producer/src/test/java/org/chovy/canvas/perf/mq/PerfMqProducerTest.java`
  - Tests message payload and key generation without connecting to RocketMQ.
- Create: `tools/perf/verifier.mjs`
  - Uses the local `mysql` CLI to query ledgers and produce correctness verdicts.
- Create: `tools/perf/verifier.test.mjs`
  - Tests verdict calculations.
- Create: `tools/perf/capacity-report.mjs`
  - Calculates conservative production capacity from local results and production parameters.
- Create: `tools/perf/capacity-report.test.mjs`
  - Tests bottleneck and safety-factor calculations.
- Create: `tools/perf/cleanup.mjs`
  - Deletes only rows tied to a specific `perfRunId` or `PERF_` fixture namespace.
- Create: `tools/perf/cleanup.test.mjs`
  - Tests cleanup SQL generation.
- Create: `tools/perf/README.md`
  - Documents run order, commands, report interpretation, and cleanup.

## Task 1: Schema And Entity Mapping

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V50__perf_run_tracking.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLog.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecution.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlq.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunEntityMappingTest.java`

- [ ] **Step 1: Write the failing schema test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`:

```java
package org.chovy.canvas.domain.execution;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunTrackingSchemaTest {

    @Test
    void migrationAddsPerfRunIdToAllPerformanceLedgers() throws Exception {
        ClassPathResource migration = new ClassPathResource("db/migration/V50__perf_run_tracking.sql");

        String sql = migration.getContentAsString(StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("ALTER TABLE `event_log`")
                .contains("ADD COLUMN `perf_run_id` VARCHAR(80) NULL")
                .contains("ADD INDEX `idx_event_log_perf_run` (`perf_run_id`, `created_at`)")
                .contains("ALTER TABLE `canvas_execution`")
                .contains("ADD INDEX `idx_execution_perf_run` (`perf_run_id`, `created_at`)")
                .contains("ALTER TABLE `canvas_execution_request`")
                .contains("ADD INDEX `idx_execution_request_perf_run` (`perf_run_id`, `status`, `updated_at`)")
                .contains("ALTER TABLE `canvas_execution_dlq`")
                .contains("ADD INDEX `idx_execution_dlq_perf_run` (`perf_run_id`, `failed_at`)");
    }
}
```

- [ ] **Step 2: Write the failing entity mapping test**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunEntityMappingTest.java`:

```java
package org.chovy.canvas.domain.execution;

import org.chovy.canvas.domain.meta.EventLog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunEntityMappingTest {

    @Test
    void eventLogExposesPerfRunId() {
        EventLog log = new EventLog();
        log.setPerfRunId("perf_20260523_001");

        assertThat(log.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionExposesPerfRunId() {
        CanvasExecution execution = new CanvasExecution();
        execution.setPerfRunId("perf_20260523_001");

        assertThat(execution.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void executionRequestExposesPerfRunId() {
        CanvasExecutionRequest request = new CanvasExecutionRequest();
        request.setPerfRunId("perf_20260523_001");

        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
    }

    @Test
    void dlqExposesPerfRunId() {
        CanvasExecutionDlq dlq = CanvasExecutionDlq.builder()
                .perfRunId("perf_20260523_001")
                .build();

        assertThat(dlq.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,PerfRunEntityMappingTest test
```

Expected: FAIL because `V50__perf_run_tracking.sql` and `perfRunId` properties do not exist.

- [ ] **Step 4: Add the migration**

Create `backend/canvas-engine/src/main/resources/db/migration/V50__perf_run_tracking.sql`:

```sql
ALTER TABLE `event_log`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_event_log_perf_run` (`perf_run_id`, `created_at`);

ALTER TABLE `canvas_execution`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_perf_run` (`perf_run_id`, `created_at`);

ALTER TABLE `canvas_execution_request`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_request_perf_run` (`perf_run_id`, `status`, `updated_at`);

ALTER TABLE `canvas_execution_dlq`
    ADD COLUMN `perf_run_id` VARCHAR(80) NULL COMMENT '压测批次ID',
    ADD INDEX `idx_execution_dlq_perf_run` (`perf_run_id`, `failed_at`);
```

- [ ] **Step 5: Add entity fields**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLog.java`, add this field after `userId`:

```java
    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;
```

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecution.java`, add this field after `userId`:

```java
    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;
```

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequest.java`, add this field after `userId`:

```java
    private String perfRunId;
```

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlq.java`, add this field after `userId`:

```java
    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;
```

- [ ] **Step 6: Persist request perfRunId in the custom insert**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java`, replace the `insertIgnore` SQL column list with:

```java
            INSERT IGNORE INTO canvas_execution_request
            (id, canvas_id, user_id, perf_run_id, trigger_type, trigger_node_type, match_key,
             payload_json, source_msg_id, status, attempt_count, next_retry_at,
             last_error, result_json, created_at, updated_at)
```

Replace the matching values list with:

```java
            (#{id}, #{canvasId}, #{userId}, #{perfRunId}, #{triggerType}, #{triggerNodeType}, #{matchKey},
             #{payloadJson}, #{sourceMsgId}, #{status}, #{attemptCount}, #{nextRetryAt},
             #{lastError}, #{resultJson}, NOW(), NOW())
```

- [ ] **Step 7: Run tests to verify they pass**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,PerfRunEntityMappingTest test
```

Expected: PASS.

- [ ] **Step 8: Commit schema and mapping**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V50__perf_run_tracking.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/EventLog.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecution.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequest.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionDlq.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/execution/CanvasExecutionRequestMapper.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunEntityMappingTest.java
git commit -m "feat: add perf run tracking ledgers"
```

Expected: commit succeeds.

## Task 2: PerfRunId Extraction Utility

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/perf/PerfRunContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/perf/PerfRunContextTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextPerfRunTest.java`

- [ ] **Step 1: Write failing utility tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/perf/PerfRunContextTest.java`:

```java
package org.chovy.canvas.perf;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PerfRunContextTest {

    @Test
    void extractsValidPerfRunIdFromPayload() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "perf_20260523_001")))
                .isEqualTo("perf_20260523_001");
    }

    @Test
    void returnsNullForMissingPerfRunId() {
        assertThat(PerfRunContext.extract(Map.of("orderId", "O-1"))).isNull();
    }

    @Test
    void returnsNullForBlankPerfRunId() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "   "))).isNull();
    }

    @Test
    void rejectsUnsafeCharactersByReturningNull() {
        assertThat(PerfRunContext.extract(Map.of("perfRunId", "perf;drop"))).isNull();
    }
}
```

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextPerfRunTest.java`:

```java
package org.chovy.canvas.engine.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextPerfRunTest {

    @Test
    void storesPerfRunIdOnExecutionContext() {
        ExecutionContext ctx = new ExecutionContext();
        ctx.setPerfRunId("perf_20260523_001");

        assertThat(ctx.getPerfRunId()).isEqualTo("perf_20260523_001");
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=PerfRunContextTest,ExecutionContextPerfRunTest test
```

Expected: FAIL because `PerfRunContext` and `ExecutionContext.perfRunId` do not exist.

- [ ] **Step 3: Add the extraction utility**

Create `backend/canvas-engine/src/main/java/org/chovy/canvas/perf/PerfRunContext.java`:

```java
package org.chovy.canvas.perf;

import java.util.Map;
import java.util.regex.Pattern;

public final class PerfRunContext {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_:-]{1,80}");

    private PerfRunContext() {
    }

    public static String extract(Map<String, Object> payload) {
        if (payload == null) {
            return null;
        }
        Object raw = payload.get("perfRunId");
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank() || !SAFE_ID.matcher(value).matches()) {
            return null;
        }
        return value;
    }
}
```

- [ ] **Step 4: Add the field to execution context**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`, add this field after `userId`:

```java
    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;
```

- [ ] **Step 5: Run tests to verify they pass**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=PerfRunContextTest,ExecutionContextPerfRunTest test
```

Expected: PASS.

- [ ] **Step 6: Commit extraction utility**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/perf/PerfRunContext.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/perf/PerfRunContextTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextPerfRunTest.java
git commit -m "feat: extract perf run context"
```

Expected: commit succeeds.

## Task 3: Backend Propagation Through Existing Ledgers

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ExecutionController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutorTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java`

- [ ] **Step 1: Add failing event-log propagation test**

Append this test to `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java`:

```java
    @Test
    void reportEventPersistsPerfRunIdAndPassesPayloadToDisruptor() {
        EventDefinition def = new EventDefinition();
        def.setEventCode("PERF_EVENT");
        def.setEnabled(CanvasStatusEnum.PUBLISHED.getCode());
        when(eventDefinitionCacheService.getPublishedByCode("PERF_EVENT")).thenReturn(def);
        when(triggerRouteService.getCanvasByBehavior("PERF_EVENT")).thenReturn(Set.of("42"));

        ArgumentCaptor<EventLog> logCaptor = ArgumentCaptor.forClass(EventLog.class);
        doAnswer(invocation -> null).when(logMapper).insert(logCaptor.capture());

        EventReportReq req = new EventReportReq();
        req.setEventCode("PERF_EVENT");
        req.setUserId("perf_user_1");
        req.setAttributes(Map.of("perfRunId", "perf_20260523_001", "amount", 88));

        controller.reportEvent(req).block();

        assertThat(logCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_001");
        verify(disruptorService).publish(
                eq(42L),
                eq("perf_user_1"),
                eq("EVENT"),
                eq("EVENT_TRIGGER"),
                eq("PERF_EVENT"),
                eq(Map.of("perfRunId", "perf_20260523_001", "amount", 88)),
                any()
        );
    }
```

- [ ] **Step 2: Add failing request-service propagation test**

Append this assertion to `enqueueMqRequestPersistsDeterministicPendingRequest` in `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceTest.java` by changing the payload map to include `perfRunId`:

```java
                Map.of("orderId", "O-1", "perfRunId", "perf_20260523_001"),
```

Then add this assertion after `assertThat(request.getUserId()).isEqualTo("user-7");`:

```java
        assertThat(request.getPerfRunId()).isEqualTo("perf_20260523_001");
```

- [ ] **Step 3: Add failing execution persistence test**

Append this test to `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java`:

```java
    @Test
    void triggerPersistsPerfRunIdOnExecutionRecord() {
        Canvas canvas = publishedCanvas(50L, 10);
        when(canvasEntityCache.get(50L)).thenReturn(canvas);
        when(ctxStore.exists(50L, "perf_user_1")).thenReturn(false);
        DagGraph graph = graphWithTriggerNode(NodeType.DIRECT_CALL, null);
        when(configCache.get(50L, 101L)).thenReturn(graph);
        when(executionRegistry.tryAcquire(eq(50L), any(), eq(10), eq(1000)))
                .thenReturn(Optional.of(Disposables.swap()));
        when(dagEngine.execute(eq(graph), eq("trigger"), any()))
                .thenReturn(Mono.just(Map.of("ok", true)));

        sut.trigger(
                50L,
                "perf_user_1",
                TriggerType.DIRECT_CALL,
                NodeType.DIRECT_CALL,
                null,
                Map.of("perfRunId", "perf_20260523_001"),
                "perf_20260523_001:direct:1",
                false
        ).block();

        ArgumentCaptor<CanvasExecution> executionCaptor = ArgumentCaptor.forClass(CanvasExecution.class);
        verify(executionMapper).insert(executionCaptor.capture());
        assertThat(executionCaptor.getValue().getPerfRunId()).isEqualTo("perf_20260523_001");
    }
```

- [ ] **Step 4: Run tests to verify they fail**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=EventDefinitionControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionServiceTest test
```

Expected: FAIL because `perfRunId` is not set in event logs, execution requests, and execution records.

- [ ] **Step 5: Propagate perfRunId in event reporting**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java`, add import:

```java
import org.chovy.canvas.perf.PerfRunContext;
```

After `Map<String, Object> payload = req.getAttributes() != null ? req.getAttributes() : Map.of();`, add:

```java
                    String perfRunId = PerfRunContext.extract(payload);
```

Before `logMapper.insert(eventLog);`, add:

```java
                    eventLog.setPerfRunId(perfRunId);
```

- [ ] **Step 6: Propagate perfRunId in execution requests**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java`, add import:

```java
import org.chovy.canvas.perf.PerfRunContext;
```

Before `request.setTriggerType(triggerType);`, add:

```java
        request.setPerfRunId(PerfRunContext.extract(payload));
```

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java`, inside `Mono.defer`, after `Map<String, Object> payload = parsePayload(request.getPayloadJson());`, add:

```java
                                            if (request.getPerfRunId() != null && !request.getPerfRunId().isBlank()) {
                                                payload = new java.util.HashMap<>(payload);
                                                payload.putIfAbsent("perfRunId", request.getPerfRunId());
                                            }
```

- [ ] **Step 7: Propagate perfRunId in CanvasExecutionService**

In `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`, add import:

```java
import org.chovy.canvas.perf.PerfRunContext;
```

In both normal trigger and dry-run preparation paths, after payload is merged into `ctx.getTriggerPayload()`, add:

```java
                    ctx.setPerfRunId(PerfRunContext.extract(ctx.getTriggerPayload()));
```

In `newContext`, keep `perfRunId` unset. It is derived only after payload merge.

In `createExecution`, add:

```java
        exec.setPerfRunId(ctx.getPerfRunId());
```

In every `CanvasExecutionDlq.builder()` block, add:

```java
                    .perfRunId(ctx.getPerfRunId())
```

For overflow enqueue DLQ built from `OverflowRetryMessage`, add:

```java
                    .perfRunId(PerfRunContext.extract(msg.getPayload()))
```

- [ ] **Step 8: Keep direct calls compatible**

No request DTO field is required. In `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ExecutionController.java`, keep direct-call `perfRunId` inside `inputParams`. The caller controls `idempotencyKey`.

Add this comment above `String dedupKey =`:

```java
        // 压测请求通过 inputParams.perfRunId 贯穿账本，通过 idempotencyKey 控制唯一输入键。
```

- [ ] **Step 9: Run propagation tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=EventDefinitionControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionServiceTest test
```

Expected: PASS.

- [ ] **Step 10: Run broader affected backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasExecutionRequestExecutorTest,MqTriggerConsumerTest,CanvasExecutionDlqSchemaTest test
```

Expected: PASS.

- [ ] **Step 11: Commit propagation changes**

Run:

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/controller/EventDefinitionController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutor.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ExecutionController.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/controller/EventDefinitionControllerTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceTest.java
git commit -m "feat: propagate perf run id"
```

Expected: commit succeeds.

## Task 4: HTTP Load Driver

**Files:**
- Create: `tools/perf/perf-runner.mjs`
- Create: `tools/perf/perf-runner.test.mjs`

- [ ] **Step 1: Write failing runner tests**

Create `tools/perf/perf-runner.test.mjs`:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildEventPayload,
  buildDirectPayload,
  chunkSeq,
  parseRunnerArgs,
} from './perf-runner.mjs'

test('buildEventPayload produces deterministic perf input id', () => {
  const payload = buildEventPayload({
    perfRunId: 'perf_20260523_001',
    eventCode: 'PERF_ORDER_PAID',
    userPrefix: 'perf_user_',
    seq: 7,
    userModulo: 3,
  })

  assert.equal(payload.eventCode, 'PERF_ORDER_PAID')
  assert.equal(payload.userId, 'perf_user_1')
  assert.equal(payload.attributes.perfRunId, 'perf_20260523_001')
  assert.equal(payload.attributes.perfInputId, 'perf_20260523_001:event:7')
})

test('buildDirectPayload uses deterministic idempotency key', () => {
  const payload = buildDirectPayload({
    perfRunId: 'perf_20260523_001',
    seq: 9,
    userPrefix: 'perf_user_',
    userModulo: 4,
  })

  assert.equal(payload.userId, 'perf_user_1')
  assert.equal(payload.idempotencyKey, 'perf_20260523_001:direct:9')
  assert.equal(payload.inputParams.perfRunId, 'perf_20260523_001')
})

test('chunkSeq groups sequence numbers by concurrency', () => {
  assert.deepEqual([...chunkSeq(5, 2)], [[1, 2], [3, 4], [5]])
})

test('parseRunnerArgs reads required flags', () => {
  const args = parseRunnerArgs([
    '--mode', 'event',
    '--base-url', 'http://localhost:8080',
    '--perf-run-id', 'perf_20260523_001',
    '--count', '10',
    '--concurrency', '2',
  ])

  assert.equal(args.mode, 'event')
  assert.equal(args.baseUrl, 'http://localhost:8080')
  assert.equal(args.perfRunId, 'perf_20260523_001')
  assert.equal(args.count, 10)
  assert.equal(args.concurrency, 2)
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
node --test tools/perf/perf-runner.test.mjs
```

Expected: FAIL because `tools/perf/perf-runner.mjs` does not exist.

- [ ] **Step 3: Add the HTTP runner**

Create `tools/perf/perf-runner.mjs`:

```javascript
#!/usr/bin/env node

export function parseRunnerArgs(argv) {
  const args = {
    mode: 'event',
    baseUrl: 'http://localhost:8080',
    perfRunId: '',
    count: 1000,
    concurrency: 20,
    eventCode: 'PERF_ORDER_PAID',
    canvasId: '',
    audienceId: '',
    userPrefix: 'perf_user_',
    userModulo: 1000,
  }
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i]
    const value = argv[i + 1]
    if (key === '--mode') args.mode = value
    if (key === '--base-url') args.baseUrl = value
    if (key === '--perf-run-id') args.perfRunId = value
    if (key === '--count') args.count = Number(value)
    if (key === '--concurrency') args.concurrency = Number(value)
    if (key === '--event-code') args.eventCode = value
    if (key === '--canvas-id') args.canvasId = value
    if (key === '--audience-id') args.audienceId = value
    if (key === '--user-prefix') args.userPrefix = value
    if (key === '--user-modulo') args.userModulo = Number(value)
  }
  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  return args
}

export function buildEventPayload({ perfRunId, eventCode, userPrefix, seq, userModulo }) {
  return {
    eventCode,
    userId: `${userPrefix}${seq % userModulo}`,
    attributes: {
      perfRunId,
      perfInputId: `${perfRunId}:event:${seq}`,
      seq,
      amount: seq % 1000,
    },
  }
}

export function buildDirectPayload({ perfRunId, userPrefix, seq, userModulo }) {
  return {
    userId: `${userPrefix}${seq % userModulo}`,
    idempotencyKey: `${perfRunId}:direct:${seq}`,
    inputParams: {
      perfRunId,
      perfInputId: `${perfRunId}:direct:${seq}`,
      seq,
    },
  }
}

export function* chunkSeq(count, concurrency) {
  let batch = []
  for (let seq = 1; seq <= count; seq += 1) {
    batch.push(seq)
    if (batch.length === concurrency) {
      yield batch
      batch = []
    }
  }
  if (batch.length > 0) {
    yield batch
  }
}

async function postJson(url, body) {
  const startedAt = Date.now()
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
  const text = await response.text()
  return {
    ok: response.ok,
    status: response.status,
    durationMs: Date.now() - startedAt,
    body: text,
  }
}

async function runEvent(args, seq) {
  return postJson(`${args.baseUrl}/canvas/events/report`, buildEventPayload({
    perfRunId: args.perfRunId,
    eventCode: args.eventCode,
    userPrefix: args.userPrefix,
    seq,
    userModulo: args.userModulo,
  }))
}

async function runDirect(args, seq) {
  if (!args.canvasId) {
    throw new Error('--canvas-id is required for direct mode')
  }
  return postJson(`${args.baseUrl}/canvas/execute/direct/${args.canvasId}`, buildDirectPayload({
    perfRunId: args.perfRunId,
    userPrefix: args.userPrefix,
    seq,
    userModulo: args.userModulo,
  }))
}

async function runAudience(args, seq) {
  if (!args.audienceId) {
    throw new Error('--audience-id is required for audience mode')
  }
  return postJson(`${args.baseUrl}/canvas/audiences/${args.audienceId}/compute`, {
    perfRunId: args.perfRunId,
    perfInputId: `${args.perfRunId}:audience:${seq}`,
  })
}

async function run(args) {
  let sent = 0
  let success = 0
  let failed = 0
  const durations = []
  for (const batch of chunkSeq(args.count, args.concurrency)) {
    const results = await Promise.all(batch.map((seq) => {
      if (args.mode === 'event') return runEvent(args, seq)
      if (args.mode === 'direct') return runDirect(args, seq)
      if (args.mode === 'audience') return runAudience(args, seq)
      throw new Error(`Unsupported mode: ${args.mode}`)
    }))
    for (const result of results) {
      sent += 1
      if (result.ok) success += 1
      if (!result.ok) failed += 1
      durations.push(result.durationMs)
    }
  }
  durations.sort((a, b) => a - b)
  const p95Index = Math.max(0, Math.ceil(durations.length * 0.95) - 1)
  const p95Ms = durations.length === 0 ? 0 : durations[p95Index]
  return {
    perfRunId: args.perfRunId,
    mode: args.mode,
    sent,
    success,
    failed,
    p95Ms,
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  run(parseRunnerArgs(process.argv.slice(2)))
    .then((summary) => console.log(JSON.stringify(summary, null, 2)))
    .catch((error) => {
      console.error(error.message)
      process.exit(1)
    })
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
node --test tools/perf/perf-runner.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit HTTP runner**

Run:

```bash
git add tools/perf/perf-runner.mjs tools/perf/perf-runner.test.mjs
git commit -m "feat: add perf http runner"
```

Expected: commit succeeds.

## Task 5: RocketMQ Load Driver

**Files:**
- Create: `tools/perf/mq-producer/pom.xml`
- Create: `tools/perf/mq-producer/src/main/java/org/chovy/canvas/perf/mq/PerfMqProducer.java`
- Create: `tools/perf/mq-producer/src/test/java/org/chovy/canvas/perf/mq/PerfMqProducerTest.java`

- [ ] **Step 1: Write failing MQ producer tests**

Create `tools/perf/mq-producer/src/test/java/org/chovy/canvas/perf/mq/PerfMqProducerTest.java`:

```java
package org.chovy.canvas.perf.mq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PerfMqProducerTest {

    @Test
    void sourceMsgIdIsDeterministic() {
        assertThat(PerfMqProducer.sourceMsgId("perf_20260523_001", 12))
                .isEqualTo("perf_20260523_001:mq:12");
    }

    @Test
    void messageBodyContainsPerfRunIdAndInputId() {
        String body = PerfMqProducer.messageBody("perf_20260523_001", "perf_user_2", 12);

        assertThat(body)
                .contains("\"userId\":\"perf_user_2\"")
                .contains("\"messageCode\":\"PERF_MQ\"")
                .contains("\"perfRunId\":\"perf_20260523_001\"")
                .contains("\"perfInputId\":\"perf_20260523_001:mq:12\"");
    }
}
```

- [ ] **Step 2: Add MQ producer Maven file**

Create `tools/perf/mq-producer/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://www.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.chovy.canvas</groupId>
    <artifactId>perf-mq-producer</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <junit.version>5.10.2</junit.version>
        <assertj.version>3.25.3</assertj.version>
        <rocketmq.version>5.3.1</rocketmq.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.apache.rocketmq</groupId>
            <artifactId>rocketmq-client</artifactId>
            <version>${rocketmq.version}</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
cd tools/perf/mq-producer && mvn test
```

Expected: FAIL because `PerfMqProducer` does not exist.

- [ ] **Step 4: Add the MQ producer**

Create `tools/perf/mq-producer/src/main/java/org/chovy/canvas/perf/mq/PerfMqProducer.java`:

```java
package org.chovy.canvas.perf.mq;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PerfMqProducer {

    public static void main(String[] args) throws Exception {
        Map<String, String> parsed = parseArgs(args);
        String nameServer = parsed.getOrDefault("name-server", "localhost:9876");
        String topic = parsed.getOrDefault("topic", "CANVAS_MQ_TRIGGER");
        String tag = parsed.getOrDefault("tag", "PERF_MQ");
        String perfRunId = require(parsed, "perf-run-id");
        int count = Integer.parseInt(parsed.getOrDefault("count", "1000"));
        int userModulo = Integer.parseInt(parsed.getOrDefault("user-modulo", "1000"));

        DefaultMQProducer producer = new DefaultMQProducer("PID_CANVAS_PERF");
        producer.setNamesrvAddr(nameServer);
        producer.start();
        try {
            for (int seq = 1; seq <= count; seq++) {
                String userId = "perf_user_" + (seq % userModulo);
                Message message = new Message(
                        topic,
                        tag,
                        sourceMsgId(perfRunId, seq),
                        messageBody(perfRunId, userId, seq).getBytes(StandardCharsets.UTF_8)
                );
                producer.send(message);
            }
        } finally {
            producer.shutdown();
        }
    }

    static String sourceMsgId(String perfRunId, int seq) {
        return perfRunId + ":mq:" + seq;
    }

    static String messageBody(String perfRunId, String userId, int seq) {
        return "{\"userId\":\"" + userId + "\","
                + "\"messageCode\":\"PERF_MQ\","
                + "\"payload\":{"
                + "\"perfRunId\":\"" + perfRunId + "\","
                + "\"perfInputId\":\"" + sourceMsgId(perfRunId, seq) + "\","
                + "\"seq\":" + seq
                + "}}";
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String key = args[i].replaceFirst("^--", "");
            String value = args[i + 1];
            parsed.put(key, value);
        }
        return parsed;
    }

    private static String require(Map<String, String> parsed, String key) {
        String value = parsed.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("--" + key + " is required");
        }
        return value;
    }
}
```

- [ ] **Step 5: Run MQ producer tests**

Run:

```bash
cd tools/perf/mq-producer && mvn test
```

Expected: PASS.

- [ ] **Step 6: Commit MQ producer**

Run:

```bash
git add tools/perf/mq-producer
git commit -m "feat: add perf mq producer"
```

Expected: commit succeeds.

## Task 6: Correctness Verifier

**Files:**
- Create: `tools/perf/verifier.mjs`
- Create: `tools/perf/verifier.test.mjs`

- [ ] **Step 1: Write failing verifier tests**

Create `tools/perf/verifier.test.mjs`:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import { computeVerdict, parseTabularCount } from './verifier.mjs'

test('computeVerdict passes when all normal ledgers align', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 100,
    success: 100,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'PASS')
  assert.equal(verdict.unexpectedLoss, 0)
})

test('computeVerdict fails on unexpected loss', () => {
  const verdict = computeVerdict({
    planned: 100,
    sentSuccess: 100,
    accepted: 100,
    expectedExecutions: 100,
    actualExecutions: 99,
    success: 99,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
  })

  assert.equal(verdict.verdict, 'FAIL')
  assert.equal(verdict.unexpectedLoss, 1)
})

test('computeVerdict fails on audience count mismatch', () => {
  const verdict = computeVerdict({
    planned: 1,
    sentSuccess: 1,
    accepted: 1,
    expectedExecutions: 1,
    actualExecutions: 1,
    success: 1,
    failedWithRecord: 0,
    dlq: 0,
    rejectedWithRecord: 0,
    retryPending: 0,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: 0,
    wrongAudienceCount: 1,
  })

  assert.equal(verdict.verdict, 'FAIL')
})

test('parseTabularCount reads mysql batch output', () => {
  assert.equal(parseTabularCount('count\\n42\\n'), 42)
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
node --test tools/perf/verifier.test.mjs
```

Expected: FAIL because `verifier.mjs` does not exist.

- [ ] **Step 3: Add verifier**

Create `tools/perf/verifier.mjs`:

```javascript
#!/usr/bin/env node

import { execFileSync } from 'node:child_process'

export function parseTabularCount(output) {
  const lines = output.trim().split(/\r?\n/).filter(Boolean)
  return Number(lines.length > 1 ? lines[1] : 0)
}

export function computeVerdict(input) {
  const accounted = input.success
    + input.failedWithRecord
    + input.dlq
    + input.rejectedWithRecord
    + input.retryPending
  const unexpectedLoss = Math.max(0, input.expectedExecutions - accounted)
  const wrongAudienceCount = input.wrongAudienceCount || 0
  const failures = [
    unexpectedLoss,
    input.duplicateExecution,
    input.unexpectedDedup,
    input.ackWithoutLedger,
    wrongAudienceCount,
  ].filter((value) => value > 0)
  return {
    ...input,
    unexpectedLoss,
    wrongAudienceCount,
    verdict: failures.length === 0 ? 'PASS' : 'FAIL',
  }
}

function queryCount({ mysql, database, perfRunId, sql }) {
  const output = execFileSync(mysql, [
    '-uroot',
    '-proot',
    '-D',
    database,
    '-e',
    sql.replaceAll(':perfRunId', `'${perfRunId.replaceAll("'", "''")}'`),
  ], { encoding: 'utf8' })
  return parseTabularCount(output)
}

function parseArgs(argv) {
  const args = {
    mysql: 'mysql',
    database: 'canvas_db',
    perfRunId: '',
    sentSuccess: 0,
    matchedCanvasCount: 1,
    audienceId: 0,
    expectedAudienceCount: -1,
  }
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i]
    const value = argv[i + 1]
    if (key === '--mysql') args.mysql = value
    if (key === '--database') args.database = value
    if (key === '--perf-run-id') args.perfRunId = value
    if (key === '--sent-success') args.sentSuccess = Number(value)
    if (key === '--matched-canvas-count') args.matchedCanvasCount = Number(value)
    if (key === '--audience-id') args.audienceId = Number(value)
    if (key === '--expected-audience-count') args.expectedAudienceCount = Number(value)
  }
  if (!args.perfRunId) throw new Error('--perf-run-id is required')
  return args
}

function verify(args) {
  const accepted = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM event_log WHERE perf_run_id = :perfRunId',
  })
  const executions = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId',
  })
  const success = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId AND status = 2',
  })
  const failed = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution WHERE perf_run_id = :perfRunId AND status = 3',
  })
  const dlq = queryCount({
    ...args,
    sql: 'SELECT COUNT(*) AS count FROM canvas_execution_dlq WHERE perf_run_id = :perfRunId',
  })
  const retryPending = queryCount({
    ...args,
    sql: "SELECT COUNT(*) AS count FROM canvas_execution_request WHERE perf_run_id = :perfRunId AND status IN ('PENDING','RETRY','RUNNING')",
  })
  let wrongAudienceCount = 0
  if (args.audienceId > 0 && args.expectedAudienceCount >= 0) {
    const actualAudienceCount = queryCount({
      ...args,
      sql: `SELECT COALESCE(MAX(estimated_size), -1) AS count FROM audience_stat WHERE audience_id = ${args.audienceId}`,
    })
    wrongAudienceCount = actualAudienceCount === args.expectedAudienceCount ? 0 : 1
  }
  return computeVerdict({
    planned: args.sentSuccess,
    sentSuccess: args.sentSuccess,
    accepted,
    expectedExecutions: args.sentSuccess * args.matchedCanvasCount,
    actualExecutions: executions,
    success,
    failedWithRecord: failed,
    dlq,
    rejectedWithRecord: 0,
    retryPending,
    duplicateExecution: 0,
    unexpectedDedup: 0,
    ackWithoutLedger: Math.max(0, args.sentSuccess - accepted),
    wrongAudienceCount,
  })
}

if (import.meta.url === `file://${process.argv[1]}`) {
  try {
    const result = verify(parseArgs(process.argv.slice(2)))
    console.log(JSON.stringify(result, null, 2))
    process.exit(result.verdict === 'PASS' ? 0 : 2)
  } catch (error) {
    console.error(error.message)
    process.exit(1)
  }
}
```

- [ ] **Step 4: Run verifier tests**

Run:

```bash
node --test tools/perf/verifier.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit verifier**

Run:

```bash
git add tools/perf/verifier.mjs tools/perf/verifier.test.mjs
git commit -m "feat: add perf correctness verifier"
```

Expected: commit succeeds.

## Task 7: Capacity Report

**Files:**
- Create: `tools/perf/capacity-report.mjs`
- Create: `tools/perf/capacity-report.test.mjs`

- [ ] **Step 1: Write failing capacity tests**

Create `tools/perf/capacity-report.test.mjs`:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import { estimateCapacity } from './capacity-report.mjs'

test('estimateCapacity picks the minimum bottleneck and applies safety factor', () => {
  const result = estimateCapacity({
    localStableQps: 1200,
    localAppCores: 8,
    prodAppCoresTotal: 32,
    writesPerEvent: 4,
    prodDbSafeWriteQps: 12000,
    redisOpsPerEvent: 3,
    prodRedisSafeOps: 30000,
    rocketmqCapacity: 7000,
    downstreamRateLimitPerSec: 5000,
    downstreamCallsPerEvent: 1,
    cpuEfficiencyFactor: 0.75,
    safetyFactor: 0.5,
  })

  assert.equal(result.rawCapacity, 3000)
  assert.equal(result.recommendedCapacity, 1500)
  assert.equal(result.bottleneck, 'DB_WRITE')
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
node --test tools/perf/capacity-report.test.mjs
```

Expected: FAIL because `capacity-report.mjs` does not exist.

- [ ] **Step 3: Add capacity report**

Create `tools/perf/capacity-report.mjs`:

```javascript
#!/usr/bin/env node

export function estimateCapacity(input) {
  const appCpuCapacity = input.localStableQps
    * (input.prodAppCoresTotal / input.localAppCores)
    * input.cpuEfficiencyFactor
  const dbWriteCapacity = input.prodDbSafeWriteQps / input.writesPerEvent
  const redisOpsCapacity = input.prodRedisSafeOps / input.redisOpsPerEvent
  const downstreamCapacity = input.downstreamRateLimitPerSec / input.downstreamCallsPerEvent
  const candidates = [
    ['APP_CPU', appCpuCapacity],
    ['DB_WRITE', dbWriteCapacity],
    ['REDIS_OPS', redisOpsCapacity],
    ['ROCKETMQ', input.rocketmqCapacity],
    ['DOWNSTREAM_API', downstreamCapacity],
  ]
  candidates.sort((left, right) => left[1] - right[1])
  const [bottleneck, rawCapacity] = candidates[0]
  return {
    bottleneck,
    rawCapacity: Math.floor(rawCapacity),
    recommendedCapacity: Math.floor(rawCapacity * input.safetyFactor),
    alertThreshold: Math.floor(rawCapacity * input.safetyFactor * 0.7),
  }
}

function parseArgs(argv) {
  const defaults = {
    cpuEfficiencyFactor: 0.75,
    safetyFactor: 0.5,
  }
  for (let i = 0; i < argv.length; i += 2) {
    const key = argv[i].replace(/^--/, '')
    defaults[key.replace(/-([a-z])/g, (_, ch) => ch.toUpperCase())] = Number(argv[i + 1])
  }
  return defaults
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const result = estimateCapacity(parseArgs(process.argv.slice(2)))
  console.log(JSON.stringify(result, null, 2))
}
```

- [ ] **Step 4: Run capacity tests**

Run:

```bash
node --test tools/perf/capacity-report.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit capacity report**

Run:

```bash
git add tools/perf/capacity-report.mjs tools/perf/capacity-report.test.mjs
git commit -m "feat: add perf capacity report"
```

Expected: commit succeeds.

## Task 8: Cleanup Script

**Files:**
- Create: `tools/perf/cleanup.mjs`
- Create: `tools/perf/cleanup.test.mjs`

- [ ] **Step 1: Write failing cleanup tests**

Create `tools/perf/cleanup.test.mjs`:

```javascript
import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCleanupSql, escapeSql } from './cleanup.mjs'

test('escapeSql doubles single quotes', () => {
  assert.equal(escapeSql("perf'1"), "perf''1")
})

test('buildCleanupSql deletes only perf run and PERF namespace rows', () => {
  const sql = buildCleanupSql('perf_20260523_001')

  assert.match(sql, /canvas_execution_trace/)
  assert.match(sql, /perf_run_id = 'perf_20260523_001'/)
  assert.match(sql, /event_code LIKE 'PERF_%'/)
  assert.match(sql, /message_code LIKE 'PERF_%'/)
  assert.doesNotMatch(sql, /DELETE FROM canvas;$/m)
})
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
node --test tools/perf/cleanup.test.mjs
```

Expected: FAIL because `cleanup.mjs` does not exist.

- [ ] **Step 3: Add cleanup script**

Create `tools/perf/cleanup.mjs`:

```javascript
#!/usr/bin/env node

import { execFileSync } from 'node:child_process'

export function escapeSql(value) {
  return String(value).replaceAll("'", "''")
}

export function buildCleanupSql(perfRunId) {
  const id = escapeSql(perfRunId)
  return `
SELECT COUNT(*) AS event_log_rows FROM event_log WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS execution_rows FROM canvas_execution WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS request_rows FROM canvas_execution_request WHERE perf_run_id = '${id}';
SELECT COUNT(*) AS dlq_rows FROM canvas_execution_dlq WHERE perf_run_id = '${id}';

DELETE FROM canvas_execution_trace
WHERE execution_id IN (
  SELECT id FROM canvas_execution WHERE perf_run_id = '${id}'
);
DELETE FROM canvas_execution_dlq WHERE perf_run_id = '${id}';
DELETE FROM canvas_execution_request WHERE perf_run_id = '${id}';
DELETE FROM canvas_execution WHERE perf_run_id = '${id}';
DELETE FROM event_log WHERE perf_run_id = '${id}';

DELETE FROM event_definition WHERE event_code LIKE 'PERF_%';
DELETE FROM mq_message_definition WHERE message_code LIKE 'PERF_%';
`
}

function parseArgs(argv) {
  const args = {
    mysql: 'mysql',
    database: 'canvas_db',
    perfRunId: '',
    execute: false,
  }
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i]
    const value = argv[i + 1]
    if (key === '--mysql') {
      args.mysql = value
      i += 1
    } else if (key === '--database') {
      args.database = value
      i += 1
    } else if (key === '--perf-run-id') {
      args.perfRunId = value
      i += 1
    } else if (key === '--execute') {
      args.execute = true
    }
  }
  if (!args.perfRunId) {
    throw new Error('--perf-run-id is required')
  }
  return args
}

if (import.meta.url === `file://${process.argv[1]}`) {
  try {
    const args = parseArgs(process.argv.slice(2))
    const sql = buildCleanupSql(args.perfRunId)
    if (!args.execute) {
      console.log(sql)
      process.exit(0)
    }
    execFileSync(args.mysql, ['-uroot', '-proot', '-D', args.database, '-e', sql], { stdio: 'inherit' })
  } catch (error) {
    console.error(error.message)
    process.exit(1)
  }
}
```

- [ ] **Step 4: Run cleanup tests**

Run:

```bash
node --test tools/perf/cleanup.test.mjs
```

Expected: PASS.

- [ ] **Step 5: Commit cleanup script**

Run:

```bash
git add tools/perf/cleanup.mjs tools/perf/cleanup.test.mjs
git commit -m "feat: add perf cleanup script"
```

Expected: commit succeeds.

## Task 9: Documentation And Local Runbook

**Files:**
- Create: `tools/perf/README.md`
- Modify: `docs/superpowers/specs/INDEX.md`

- [ ] **Step 1: Write the runbook**

Create `tools/perf/README.md`:

```markdown
# Canvas Performance Testing

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 18+
- Docker dependencies from `docker-compose.local.yml`
- Local backend on `http://localhost:8080`
- Local MySQL reachable with `mysql -uroot -proot canvas_db`

## Run IDs

Every run uses a unique `perfRunId`:

```bash
export PERF_RUN_ID=perf_$(date +%Y%m%d_%H%M%S)
```

## HTTP Event Test

```bash
node tools/perf/perf-runner.mjs \
  --mode event \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --event-code PERF_ORDER_PAID \
  --count 10000 \
  --concurrency 100
```

## Direct Call Test

```bash
node tools/perf/perf-runner.mjs \
  --mode direct \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --canvas-id 1 \
  --count 1000 \
  --concurrency 50
```

## Audience Compute Test

```bash
node tools/perf/perf-runner.mjs \
  --mode audience \
  --base-url http://localhost:8080 \
  --perf-run-id "$PERF_RUN_ID" \
  --audience-id 1 \
  --count 10 \
  --concurrency 2
```

## RocketMQ Test

```bash
cd tools/perf/mq-producer
mvn -q test
mvn -q exec:java \
  -Dexec.mainClass=org.chovy.canvas.perf.mq.PerfMqProducer \
  -Dexec.args="--name-server localhost:9876 --topic CANVAS_MQ_TRIGGER --tag PERF_MQ --perf-run-id $PERF_RUN_ID --count 10000 --user-modulo 1000"
```

## Verify Correctness

```bash
node tools/perf/verifier.mjs \
  --perf-run-id "$PERF_RUN_ID" \
  --sent-success 10000 \
  --matched-canvas-count 1
```

Normal runs must report `verdict: "PASS"`.

## Estimate Capacity

```bash
node tools/perf/capacity-report.mjs \
  --local-stable-qps 1200 \
  --local-app-cores 8 \
  --prod-app-cores-total 32 \
  --writes-per-event 4 \
  --prod-db-safe-write-qps 12000 \
  --redis-ops-per-event 3 \
  --prod-redis-safe-ops 30000 \
  --rocketmq-capacity 7000 \
  --downstream-rate-limit-per-sec 5000 \
  --downstream-calls-per-event 1
```

Use `recommendedCapacity` as the first production planning value. Use `alertThreshold` for the first alert line.

## Cleanup

Preview SQL:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID"
```

Execute cleanup:

```bash
node tools/perf/cleanup.mjs --perf-run-id "$PERF_RUN_ID" --execute
```
```

- [ ] **Step 2: Add spec index entry**

In `docs/superpowers/specs/INDEX.md`, add this row:

```markdown
| PERF-TEST | 全链路压测、容量外推与并发准确性验证 | `2026-05-23-performance-testing-design.md` | `rg "PERF-TEST\|全链路压测" docs/superpowers/specs` |
```

- [ ] **Step 3: Run documentation checks**

Run:

```bash
wc -l tools/perf/README.md docs/superpowers/specs/INDEX.md
```

Expected: both files are present and have non-zero line counts.

- [ ] **Step 4: Commit runbook**

Run:

```bash
git add tools/perf/README.md docs/superpowers/specs/INDEX.md
git commit -m "docs: add perf testing runbook"
```

Expected: commit succeeds.

## Task 10: Final Verification

**Files:**
- Use all files changed by Tasks 1-8.

- [ ] **Step 1: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,PerfRunEntityMappingTest,PerfRunContextTest,ExecutionContextPerfRunTest,EventDefinitionControllerTest,CanvasExecutionRequestServiceTest,CanvasExecutionServiceTest test
```

Expected: PASS.

- [ ] **Step 2: Run affected async/MQ backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasExecutionRequestExecutorTest,MqTriggerConsumerTest,CanvasExecutionDlqSchemaTest test
```

Expected: PASS.

- [ ] **Step 3: Run perf tool tests**

Run:

```bash
node --test tools/perf/perf-runner.test.mjs tools/perf/verifier.test.mjs tools/perf/capacity-report.test.mjs tools/perf/cleanup.test.mjs
```

Expected: PASS.

- [ ] **Step 4: Run MQ producer tests**

Run:

```bash
cd tools/perf/mq-producer && mvn test
```

Expected: PASS.

- [ ] **Step 5: Verify migration ordering**

Run:

```bash
find backend/canvas-engine/src/main/resources/db/migration -maxdepth 1 -name 'V*.sql' -print | sort | tail -10
```

Expected: `V50__perf_run_tracking.sql` appears after existing migrations and no other migration uses `V50`.

- [ ] **Step 6: Commit any final doc corrections**

If the verification steps required only documentation corrections, run:

```bash
git add docs/superpowers/specs/2026-05-23-performance-testing-design.md docs/superpowers/plans/2026-05-23-performance-testing.md tools/perf/README.md
git commit -m "docs: finalize perf testing guidance"
```

Expected: commit succeeds when there are doc corrections. Skip this commit when there are no final doc corrections.
