# Analytics Event Trace Schema And Sink Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add analytics event/trace storage and an OLAP-ready sink boundary while preserving existing MySQL trace compatibility.

**Architecture:** Use `V132__analytics_event_trace_schema_and_sink.sql` as the canonical schema for `analytics_event` and `analytics_event_trace`. `TraceWriteBuffer` writes through `TraceEventSink`, `MySqlTraceEventSink` preserves the existing `canvas_execution_trace` batch insert path, mirrors traces into `analytics_event_trace` when that mapper is available, and exposes written, failed, dropped, and backlog metrics.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md`

## File Structure

- Existing schema: `backend/canvas-engine/src/main/resources/db/migration/V132__analytics_event_trace_schema_and_sink.sql`
- Existing data objects: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventDO.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventTraceDO.java`
- Existing mappers: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventMapper.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventTraceMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/TraceEventSink.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MySqlTraceEventSink.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`

### Task 1: Analytics Schema Contract

**Files:**
- Existing: `backend/canvas-engine/src/main/resources/db/migration/V132__analytics_event_trace_schema_and_sink.sql`
- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventDO.java`
- Existing: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsEventTraceDO.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`

- [x] **Step 1: Write schema contract test**

`TraceSinkTest#migrationProvidesAnalyticsSinkAndRetentionFields` verifies that `V132__analytics_event_trace_schema_and_sink.sql` creates `analytics_event` and `analytics_event_trace` with tenant, session, platform, device, event-time, received-time, schema-version, business-value, retention, archive, and legal-hold fields, and does not contain `DROP TABLE`.

- [x] **Step 2: Verify schema test**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=TraceSinkTest
```

Result on 2026-06-09: PASS as part of `TraceSinkTest` with 5 tests, 0 failures, 0 errors.

### Task 2: Sink Abstraction And Metrics

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/TraceEventSink.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MySqlTraceEventSink.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceSinkTest.java`
- Regression test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/TraceWriteBufferTest.java`

- [x] **Step 1: Write sink behavior tests**

`TraceSinkTest` covers:
- `mysqlSinkWritesTraceBatchAndExposesCounters`
- `mysqlSinkCountsFailuresWithoutThrowing`
- `mysqlSinkWritesAnalyticsEvents`
- `traceWriteBufferUsesSinkAndCountsDroppedRowsWhenFull`

- [x] **Step 2: Confirm red state**

Initial red state on 2026-06-09: `TraceSinkTest` failed to compile because `TraceEventSink` and `MySqlTraceEventSink` did not exist.

- [x] **Step 3: Implement sink interface**

`TraceEventSink` defines `writeTraces`, `writeEvents`, and `SinkMetrics`. `MySqlTraceEventSink` writes existing trace batches through `CanvasExecutionTraceMapper`, mirrors to `analytics_event_trace` when configured, writes `analytics_event`, and counts written/failed rows without propagating sink failures back to runtime execution.

- [x] **Step 4: Wire TraceWriteBuffer through sink**

`TraceWriteBuffer` now calls `sink.writeTraces(batch)`, preserves Doris side-load behavior, keeps old constructors compatible, adds a package-visible test constructor `(TraceEventSink sink, int maxCapacity, int batchSize)`, and exposes dropped/backlog metrics through `metrics()`.

- [x] **Step 5: Run focused sink tests**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=TraceSinkTest
```

Result on 2026-06-09: PASS, 5 tests, 0 failures, 0 errors.

### Task 3: Verification

**Files:**
- Modify: `docs/product-evolution/specs/p2-016-analytics-event-trace-schema-and-sink.md`
- Modify: `docs/product-evolution/plans/p2-016-analytics-event-trace-schema-and-sink-plan.md`

- [x] **Step 1: Run focused regression verification**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=TraceSinkTest,TraceWriteBufferTest
```

Result on 2026-06-09: PASS, 8 tests, 0 failures, 0 errors.

- [ ] **Step 2: Commit and merge**

Commit and merge status are intentionally not claimed by this document. Treat this as an implementation and focused-verification record until the branch is committed and integrated.
