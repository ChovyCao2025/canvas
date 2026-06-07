# CDP Warehouse Synthetic ODS Data Path Proof Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove the real CDP warehouse source-to-Doris-ODS data path with a reserved synthetic event and persisted audit evidence.

**Architecture:** Generate a synthetic CDP event in canvas-engine, write it through an explicit source mode, verify it through Doris JDBC, and persist proof evidence in MySQL. `DIRECT_SINK` exercises the existing `CdpWarehouseEventSink`; `MYSQL_CDC` inserts into `cdp_event_log` and skips direct Stream Load so Flink CDC must make the row visible in Doris ODS. Keep DWD/DWS aggregation out of this slice to avoid synthetic metric pollution.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-071 spec, plan, and index rows.
- Add synthetic data-path probe run persistence.
- Add service that supports `DIRECT_SINK` and `MYSQL_CDC` source modes and reads back from Doris ODS.
- Add operator API for manual run and recent run listing.
- Add focused TDD tests and regression verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V251__cdp_warehouse_synthetic_ods_data_path_probe.sql`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSyntheticDataPathProbeRunDO.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSyntheticDataPathProbeRunMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSyntheticDataPathProbeService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSyntheticDataPathProbeController.java`
- Add focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-071 docs and index rows.

- [x] **Step 2: Add failing schema and service tests**

Cover migration table, PASS proof, strict missing Doris failure, sink failure, and dry-run missing row WARN.

- [x] **Step 3: Add failing controller tests**

Cover manual run and recent-list request binding with tenant scoping.

- [x] **Step 4: Implement schema and mapper**

Add migration, DO, and mapper helpers for insert, update completion, and recent list.

- [x] **Step 5: Implement synthetic data-path proof service**

Generate synthetic event, write through the requested source mode, read ODS with bounded attempts, persist step evidence, and return proof view.

- [x] **Step 6: Implement controller**

Expose manual run and recent list APIs under `/warehouse/data-path-probes/synthetic-ods`.

- [x] **Step 7: Verify**

Run focused tests and warehouse regression. Update this plan with observed evidence.

## Verification

Observed on 2026-06-05:

- [x] Focused P2-071 tests pass:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine test \
  -Dtest=CdpWarehouseSyntheticDataPathProbeSchemaTest,CdpWarehouseSyntheticDataPathProbeServiceTest,CdpWarehouseSyntheticDataPathProbeControllerTest
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Tests run: 8, failures: 0, errors: 0, skipped: 0.

- [x] Warehouse/Doris regression passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine test \
  -Dtest='CdpWarehouse*Test,Doris*Test'
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Tests run: 361, failures: 0, errors: 0, skipped: 1. `DorisConnectionTest` was skipped because real Doris integration was not enabled.
