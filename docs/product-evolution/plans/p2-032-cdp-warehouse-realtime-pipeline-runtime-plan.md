# CDP Warehouse Realtime Pipeline Runtime Implementation Plan

**Goal:** Add realtime warehouse runtime evidence for CDC/Flink/Kafka-style jobs without replacing the existing direct Stream Load checkpoint path.

**Architecture:** Persist declarative pipeline contracts and append checkpoint reports from external realtime jobs. Evaluate health in canvas-engine using bounded lag, checkpoint age, failure state, and exactly-once evidence. This slice is a control plane and evidence layer; actual Flink/Kafka deployment is handled by later infrastructure work.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ.

## Scope

- Add `cdp_warehouse_stream_pipeline` and `cdp_warehouse_stream_checkpoint`.
- Seed built-in pipeline contracts for CDP ODS, trace ODS, CDP DWD, and CDP DWS realtime streams.
- Add DAL records and mappers.
- Add `CdpWarehouseRealtimePipelineService`.
- Add `/warehouse/realtime/pipelines` APIs.
- Add schema, service, and controller tests.

## Tasks

- [x] **Step 1: Add schema and built-in stream pipeline contracts**

Create `V199__cdp_warehouse_realtime_pipeline_runtime.sql` with pipeline and checkpoint evidence tables.

- [x] **Step 2: Add DAL records and mappers**

Create stream pipeline/checkpoint DO classes plus mapper helpers for upsert and runtime updates.

- [x] **Step 3: Add realtime pipeline service**

Implement list/upsert/report/status. Merge tenant defaults with overrides, write checkpoint evidence idempotently, and classify health as `PASS`, `WARN`, or `FAIL`.

- [x] **Step 4: Add operator/reporting APIs**

Expose tenant-scoped contract list/upsert, checkpoint report, and status endpoints under `/warehouse/realtime/pipelines`.

- [x] **Step 5: Add tests and verify**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CdpWarehouseRealtimePipelineSchemaTest,CdpWarehouseRealtimePipelineServiceTest,CdpWarehouseRealtimePipelineControllerTest test
```

Run compile:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -DskipTests compile
```

## Verification

- [x] Main compile passed.
- [x] P2-032 focused tests passed: `13 tests, 0 failures`.
- [x] Warehouse realtime regression passed: `49 tests, 0 failures`.
- [x] Warehouse/BI/Doris regression passed: `119 tests, 0 failures`.
