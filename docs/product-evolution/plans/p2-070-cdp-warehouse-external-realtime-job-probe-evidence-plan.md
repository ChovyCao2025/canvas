# CDP Warehouse External Realtime Job Probe Evidence Implementation Plan

Spec: `../specs/p2-070-cdp-warehouse-external-realtime-job-probe-evidence.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Feed real external realtime job health into the existing warehouse realtime job control plane.

**Architecture:** Persist external probe targets, probe provider endpoints through a small client abstraction, map probe results to existing P2-047 heartbeat commands, and reuse existing incident/certification gates. This slice adds evidence ingestion only; external job deployment and lifecycle actions remain outside canvas-engine.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, Spring scheduling, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-070 spec, plan, and index rows.
- Add probe target persistence.
- Add probe target management API.
- Add external probe client and service mapping to heartbeat.
- Add scheduler with optional warehouse lease protection.
- Add focused TDD tests and compile verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V249__cdp_warehouse_external_realtime_job_probe.sql`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseExternalRealtimeJobProbeTargetDO.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseExternalRealtimeJobProbeTargetMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeClient.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/HttpCdpWarehouseExternalRealtimeJobProbeClient.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseExternalRealtimeJobProbeScheduler.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseExternalRealtimeJobProbeController.java`
- Add focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-070 docs and index rows.

- [x] **Step 2: Add failing schema and service tests**

Cover migration table/unique key, passing probe heartbeat, failed probe heartbeat, and disabled target behavior.

- [x] **Step 3: Add failing scheduler and controller tests**

Cover scheduler disabled/enabled delegation and controller request binding for target management and scan.

- [x] **Step 4: Implement schema and mapper**

Add migration, DO, and mapper helpers for upsert, list, find, enabled list, and enable/disable.

- [x] **Step 5: Implement probe client and heartbeat mapping**

Implement generic HTTP JSON probing and provider mappers for Flink REST, Kafka Connect, Doris routine-load, and generic health responses.

- [x] **Step 6: Implement service, scheduler, and controller**

Wire target registration, list, enable/disable, scan, scheduled scan, and tenant scoping.

- [x] **Step 7: Verify**

Run focused tests and main compile. Update this plan with observed evidence.

## Verification

Observed on 2026-06-05:

- [x] Focused P2-070 tests pass:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine clean test \
  -Dtest=CdpWarehouseExternalRealtimeJobProbeSchemaTest,CdpWarehouseExternalRealtimeJobProbeServiceTest,HttpCdpWarehouseExternalRealtimeJobProbeClientTest,CdpWarehouseExternalRealtimeJobProbeSchedulerTest,CdpWarehouseExternalRealtimeJobProbeControllerTest
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Tests run: 14, failures: 0, errors: 0, skipped: 0. Main source compile also passed in this clean run with 910 source files compiled.

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

Result: PASS on 2026-06-05. Tests run: 353, failures: 0, errors: 0, skipped: 1. `DorisConnectionTest` was skipped because real Doris integration was not enabled.
