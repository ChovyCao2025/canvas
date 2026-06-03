# Analytics Event Trace Schema And Sink Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add additive analytics event/trace fields and an OLAP-ready sink boundary while preserving MySQL compatibility.

**Architecture:** Extend `event_log` and `canvas_execution_trace` with nullable analytics fields, introduce `TraceEventSink`, and route `TraceWriteBuffer` through `MySqlTraceEventSink` with explicit metrics for written, failed, dropped, and backlog counts.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, Micrometer, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V112__analytics_event_trace_schema_and_sink.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/TraceEventSink.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MySqlTraceEventSink.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`

### Task 1: Additive Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V112__analytics_event_trace_schema_and_sink.sql`

- [ ] **Step 1: Write schema contract test**

Create `TraceSinkTest.java` with a schema assertion:

```java
@Test
void migrationAddsAnalyticsFieldsWithoutDroppingExistingTables() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V112__analytics_event_trace_schema_and_sink.sql"));

    assertThat(sql)
            .contains("ALTER TABLE event_log")
            .contains("tenant_id")
            .contains("session_id")
            .contains("platform")
            .contains("device_type")
            .contains("event_time")
            .contains("received_at")
            .contains("schema_version")
            .contains("business_value")
            .contains("ALTER TABLE canvas_execution_trace")
            .contains("retention_class")
            .contains("archive_status")
            .contains("legal_hold")
            .doesNotContain("DROP TABLE");
}
```

- [ ] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceSinkTest#migrationAddsAnalyticsFieldsWithoutDroppingExistingTables
```

Expected: FAIL because the migration file does not exist.

- [ ] **Step 3: Add additive migration**

Create `V112__analytics_event_trace_schema_and_sink.sql` with guarded `ALTER TABLE` statements for `event_log` and `canvas_execution_trace`. Add nullable fields only, plus indexes for `(tenant_id, event_time)`, `(tenant_id, user_id, event_time)`, `(tenant_id, archive_status)`, and `(tenant_id, retention_class)`.

- [ ] **Step 4: Update data objects**

Add matching Java fields to `EventLogDO` and `CanvasExecutionTraceDO`: `tenantId`, `sessionId`, `platform`, `deviceType`, `source`, `eventTime`, `receivedAt`, `schemaVersion`, `businessValue`, `retentionClass`, `archiveStatus`, `archivedAt`, and `legalHold` where applicable.

- [ ] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceSinkTest#migrationAddsAnalyticsFieldsWithoutDroppingExistingTables
```

Expected: PASS.

### Task 2: Sink Abstraction And Metrics

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/TraceEventSink.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MySqlTraceEventSink.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`

- [ ] **Step 1: Add sink behavior tests**

Append tests:

```java
@Test
void mysqlSinkWritesTraceBatchAndExposesCounters() {
    CanvasExecutionTraceMapper mapper = mock(CanvasExecutionTraceMapper.class);
    MySqlTraceEventSink sink = new MySqlTraceEventSink(mapper, mock(EventLogMapper.class));

    sink.writeTraces(List.of(CanvasExecutionTraceDO.builder().executionId("exec-1").nodeId("n1").build()));

    verify(mapper).insertBatch(argThat(list -> list.size() == 1));
    assertThat(sink.metrics().writtenCount()).isEqualTo(1);
    assertThat(sink.metrics().failedCount()).isZero();
}

@Test
void traceWriteBufferCountsDroppedRowsWhenFull() {
    TraceEventSink sink = mock(TraceEventSink.class);
    TraceWriteBuffer buffer = new TraceWriteBuffer(sink, 1, 1);

    buffer.offer(CanvasExecutionTraceDO.builder().executionId("exec-1").nodeId("n1").build());
    buffer.offer(CanvasExecutionTraceDO.builder().executionId("exec-2").nodeId("n2").build());

    assertThat(buffer.metrics().droppedCount()).isEqualTo(1);
    assertThat(buffer.metrics().backlog()).isEqualTo(1);
}
```

- [ ] **Step 2: Run sink tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceSinkTest
```

Expected: FAIL because sink classes and buffer constructor are missing.

- [ ] **Step 3: Implement sink interface**

Create:

```java
public interface TraceEventSink {
    void writeTraces(List<CanvasExecutionTraceDO> traces);
    void writeEvents(List<EventLogDO> events);
    SinkMetrics metrics();
    record SinkMetrics(long writtenCount, long failedCount, long droppedCount, long backlog) {}
}
```

Implement `MySqlTraceEventSink` by calling existing MyBatis mappers and incrementing counters.

- [ ] **Step 4: Wire TraceWriteBuffer through sink**

Keep the existing Spring constructor for production wiring, add a package-visible test constructor `(TraceEventSink sink, int capacity, int batchSize)`, increment dropped/backlog metrics, and call `sink.writeTraces(batch)`.

- [ ] **Step 5: Run focused tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceSinkTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md`
- Modify: `docs/product-evolution/plans/p2-016-analytics-event-trace-schema-and-sink-plan.md`

- [ ] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TraceSinkTest
```

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V112__analytics_event_trace_schema_and_sink.sql backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md docs/product-evolution/plans/p2-016-analytics-event-trace-schema-and-sink-plan.md
git commit -m "feat: add analytics trace sink foundation"
```

Expected: commit contains only additive analytics schema, sink abstraction, trace buffer metrics, tests, and related docs.
