# CDP Warehouse Ingestion And Aggregation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first operational CDP warehouse loop with realtime best-effort Doris ingestion, offline backfill, bounded aggregation, and run/watermark metadata.

**Architecture:** Keep MySQL `cdp_event_log` authoritative, mirror accepted rows to Doris through a shared sink, and run bounded ODS-to-DWD-to-DWS SQL jobs. Doris remains optional; failures never block CDP ingestion.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, Apache Doris Stream Load/JDBC, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`
- Depends on P1-005A CDP event ingestion and P2-021 CDP OLAP audience materialization.

## File Structure

- Create `V190__cdp_warehouse_runs_and_watermarks.sql` for run and watermark metadata.
- Create `CdpWarehouseSyncRunDO`, `CdpWarehouseWatermarkDO`, and mappers under `dal`.
- Create `CdpWarehouseEventSink` under `domain/warehouse`.
- Create `DorisCdpEventStreamLoader` under `infrastructure/doris`.
- Create `CdpWarehouseBackfillService` and `CdpWarehouseAggregationService` under `domain/warehouse`.
- Modify `CdpEventIngestionService` to call the optional warehouse sink after durable insert.
- Add focused tests under `domain/warehouse`, `domain/cdp`, and `infrastructure/doris`.

### Task 1: Schema Metadata

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V190__cdp_warehouse_runs_and_watermarks.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSchemaTest.java`

- [ ] **Step 1: Write failing schema tests**

Create `CdpWarehouseSchemaTest` to assert the migration creates `cdp_warehouse_sync_run`, `cdp_warehouse_watermark`, status indexes, and watermark uniqueness.

- [ ] **Step 2: Run schema test and verify red**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseSchemaTest
```

Expected: FAIL because `V190__cdp_warehouse_runs_and_watermarks.sql` does not exist.

- [ ] **Step 3: Add metadata migration**

Create additive MySQL tables:

- `cdp_warehouse_sync_run`
- `cdp_warehouse_watermark`

- [ ] **Step 4: Run schema test**

Expected: PASS.

### Task 2: Doris CDP Event Stream Load

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEventSink.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisCdpEventStreamLoader.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/infrastructure/doris/DorisCdpEventStreamLoaderTest.java`

- [ ] **Step 1: Write failing serialization tests**

Test that `toJsonLines(List<CdpEventLogDO>)` emits `event_log_id`, `tenant_id`, `message_id`, `event_code`, `user_id`, `anonymous_id`, `session_id`, `device_id`, `platform`, `properties`, `event_time`, and `received_at`.

- [ ] **Step 2: Run loader tests and verify red**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=DorisCdpEventStreamLoaderTest
```

Expected: FAIL because the loader does not exist.

- [ ] **Step 3: Implement sink and loader**

Implement a `CdpWarehouseEventSink` interface and a Doris Stream Load component with the same disabled behavior and response handling pattern as `DorisStreamLoader`.

- [ ] **Step 4: Run loader tests**

Expected: PASS.

### Task 3: Realtime Best-Effort Ingestion Hook

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionWarehouseSinkTest.java`

- [ ] **Step 1: Write failing ingestion tests**

Test that ingestion calls the sink after `cdp_event_log` insert and still returns accepted when the sink throws.

- [ ] **Step 2: Run ingestion tests and verify red**

Expected: FAIL because the service does not call a warehouse sink.

- [ ] **Step 3: Add optional sink dependency and best-effort call**

Inject `ObjectProvider<CdpWarehouseEventSink>`, call the sink after the row is inserted, and catch runtime failures with a warning.

- [ ] **Step 4: Run ingestion tests**

Expected: PASS.

### Task 4: Offline Backfill Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyncRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseWatermarkDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyncRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseWatermarkMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseBackfillService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseBackfillServiceTest.java`

- [ ] **Step 1: Write failing backfill tests**

Test that backfill rejects non-positive limits, loads accepted rows by `id > lastId`, records success counts, and records failed runs when the sink throws.

- [ ] **Step 2: Run backfill tests and verify red**

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement backfill service and metadata objects**

Use `CdpEventLogMapper.selectList` with a `LambdaQueryWrapper` ordered by id and limited by page size. Record a sync run before and after sink work.

- [ ] **Step 4: Run backfill tests**

Expected: PASS.

### Task 5: Bounded Aggregation Service

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAggregationServiceTest.java`

- [ ] **Step 1: Write failing aggregation tests**

Test invalid windows, disabled Doris no-op, and enabled Doris SQL execution for ODS-to-DWD and DWD-to-DWS with tenant scope and bounded timestamps.

- [ ] **Step 2: Run aggregation tests and verify red**

Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement aggregation service**

Inject optional `dorisJdbcTemplate`, run bounded `INSERT INTO ... SELECT ...` statements, and update run and watermark metadata.

- [ ] **Step 4: Run aggregation tests**

Expected: PASS.

### Task 6: Indexes And Focused Verification

**Files:**
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`

- [ ] **Step 1: Add P2-022 to indexes**

Add `P2-022 | CDP Warehouse Ingestion And Aggregation`.

- [ ] **Step 2: Run focused verification**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseSchemaTest,DorisCdpEventStreamLoaderTest,CdpEventIngestionWarehouseSinkTest,CdpWarehouseBackfillServiceTest,CdpWarehouseAggregationServiceTest,CdpOlapAudienceSchemaTest,AudienceMaterializationServiceTest
```

Expected: PASS with zero failures and zero errors.

## Self-Review

- Spec coverage: Tasks cover schema, realtime sink, offline backfill, aggregation, metadata, and verification.
- Placeholder scan: no placeholders or open-ended implementation steps.
- Type consistency: service and test names match the planned Java packages.
