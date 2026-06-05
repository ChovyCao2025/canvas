# CDP Warehouse Realtime Physical E2E Certification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add realtime pipeline and realtime job proof to warehouse physical E2E certification and production gates.

**Architecture:** Reuse existing realtime pipeline and job control-plane status services. Extend certification evidence, persisted run history, scheduler parameters, and gate matching with `requireRealtime` without creating a new realtime runtime or calling external Flink/Kafka APIs.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, Spring scheduling, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-069 spec, plan, and index rows.
- Add certification run schema fields for realtime proof.
- Extend immediate physical E2E certification with realtime pipeline/job evidence.
- Extend persisted certification run service, scheduler, and gate with `requireRealtime`.
- Extend immediate, run, and gate controllers with `requireRealtime` request binding.
- Add focused TDD tests and regression verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V247__cdp_warehouse_e2e_realtime_evidence.sql`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseE2eCertificationRunDO.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationRunService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- Modify focused tests for physical certification, run history, scheduler, gate, and controllers.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-069 docs and index rows.

- [x] **Step 2: Add failing schema and physical certification tests**

Cover migration columns, realtime PASS evidence, missing realtime evidence failure, and dry-run WARN.

- [x] **Step 3: Add failing run, scheduler, gate, and controller tests**

Cover persisted `requireRealtime`, realtime JSON summaries, scheduler delegation, gate matching, and request parameter binding.

- [x] **Step 4: Implement schema and data object fields**

Add migration and DO fields for `require_realtime`, `realtime_pipeline_status_json`, and `realtime_job_status_json`.

- [x] **Step 5: Implement realtime certification evidence**

Inject optional realtime pipeline/job services, evaluate summaries, add evidence, and expose summaries on certification record.

- [x] **Step 6: Implement run history, scheduler, gate, and controller wiring**

Pass `requireRealtime` through immediate API, run API, scheduler, persisted view, and gate matching.

- [x] **Step 7: Verify**

Run focused tests, main compile, test compile, and warehouse/BI/audience regression. Update this plan with evidence.

## Verification

Observed on 2026-06-05:

- [x] Focused tests pass:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine \
  -Dtest=CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest,CdpWarehousePhysicalE2eCertificationServiceTest,CdpWarehouseE2eCertificationRunServiceTest,CdpWarehouseE2eCertificationGateServiceTest,CdpWarehouseE2eCertificationSchedulerTest,CdpWarehousePhysicalE2eCertificationControllerTest,CdpWarehouseE2eCertificationRunControllerTest,CdpWarehouseE2eCertificationGateControllerTest test
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Included `CdpWarehouseRealtimePhysicalE2eCertificationSchemaTest`, physical certification, persisted run, gate, scheduler, and controller coverage. Tests run: 31, failures: 0, errors: 0, skipped: 0.

- [x] Main compile passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05.

- [x] Warehouse/BI/audience regression passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine clean test-compile
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Clean compile/test-compile passed from a fresh `target`.

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine test \
  -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest'
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: PASS on 2026-06-05. Tests run: 628, failures: 0, errors: 0, skipped: 1. `DorisConnectionTest` was skipped because the real Doris integration environment was not enabled.

- [x] Temporary Maven settings files are removed after verification:

```bash
find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print
```

Result: PASS on 2026-06-05. No `/tmp/canvas-maven-settings.*` files remained.
