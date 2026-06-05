# CDP Warehouse E2E Certification Scheduler And Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add scheduled warehouse E2E certification refresh and a production gate that evaluates recent persisted PASS evidence.

**Architecture:** Reuse the P2-067 `CdpWarehouseE2eCertificationRunService` and run ledger. Add one read-only gate service for persisted evidence evaluation, one disabled-by-default lease-protected scheduler for unattended certification runs, and one controller for tenant-scoped gate checks.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, Spring scheduling, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-068 spec, plan, and index rows.
- Add certification gate service.
- Add disabled-by-default scheduler.
- Add tenant-scoped gate API.
- Add canonical run API route alias.
- Add focused service, scheduler, and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Files

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationScheduler.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunController.java`
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationGateServiceTest.java`
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseE2eCertificationSchedulerTest.java`
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationGateControllerTest.java`
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseE2eCertificationRunControllerTest.java`

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create and index P2-068 docs.

- [x] **Step 2: Add failing gate service tests**

Cover fresh PASS evidence, stale evidence, failed evidence, and missing requested contract keys.

- [x] **Step 3: Add failing scheduler tests**

Cover disabled scheduler, enabled scheduler delegation, denied lease, and overlap guard.

- [x] **Step 4: Add failing controller and route tests**

Cover `/warehouse/e2e-certification/gate`, current tenant scoping, request parameter binding, and canonical `/warehouse/e2e-certification/runs` route alias.

- [x] **Step 5: Implement gate service**

Implement recent-run filtering, contract key parsing, freshness evaluation, and gate decision view.

- [x] **Step 6: Implement scheduler**

Implement disabled-by-default scheduling, rolling window calculation, lease integration, and overlap guard.

- [x] **Step 7: Implement controller and route alias**

Add gate controller and add canonical run route alias while preserving the existing P2-067 route.

- [x] **Step 8: Verify**

Run focused tests, compile, and warehouse/BI/audience regression. Update this plan with verification evidence.

## Verification

Observed on 2026-06-05:

- [x] Focused tests pass:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine \
  -Dtest=CdpWarehouseE2eCertificationGateServiceTest,CdpWarehouseE2eCertificationSchedulerTest,CdpWarehouseE2eCertificationGateControllerTest,CdpWarehouseE2eCertificationRunControllerTest test
rc=$?
rm -f "$tmp_settings"
exit $rc
```

Result: `BUILD SUCCESS`; 15 tests, 0 failures, 0 errors, 0 skipped.

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

Result: `BUILD SUCCESS`.

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

Result: `BUILD SUCCESS`; clean compile completed from an empty `target/`, compiling 893 main source files and 392 test source files.

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

Result: `BUILD SUCCESS`; 596 tests, 0 failures, 0 errors, 1 skipped. `DorisConnectionTest` remains skipped unless a real Doris environment is enabled with `DORIS_ENABLED=true`.

- [x] Temporary Maven settings files are removed after verification:

```bash
find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print
```

Result: no files printed.

Additional regression note: the wide regression initially exposed a BI collaboration compile gap where `V246__bi_resource_collaboration.sql` and tests existed but the Java DO/Mapper/Service/Controller layer was missing. The BI collaboration layer was added to unblock the BI regression surface; `BiResourceCollaborationServiceTest`, `BiResourceCollaborationControllerTest`, and `CanvasServiceTenantScopeTest` passed together with 15 tests, 0 failures, 0 errors, 0 skipped.
