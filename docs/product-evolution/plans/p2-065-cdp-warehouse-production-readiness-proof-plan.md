# CDP Warehouse Production Readiness Proof Implementation Plan

**Goal:** Add a read-only production readiness proof that combines readiness, window availability, and requested consumer contract evidence for a bounded OLAP/CDP warehouse window.

**Architecture:** Introduce `CdpWarehouseProductionReadinessProofService` as a thin aggregator over existing P2-036 readiness, P2-055 availability, and P2-060 consumer contract services. Expose it through a tenant-scoped controller without adding schema or replacing existing readiness/gate APIs.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, existing warehouse domain services, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-065 spec, plan, and index rows.
- Add production readiness proof service records and verdict logic.
- Add controller endpoint `/warehouse/production-readiness`.
- Add focused service and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create and index P2-065 docs.

- [x] **Step 2: Add failing service tests**

Cover full PASS, missing contract keys WARN, blocked contract FAIL, and missing consumer availability service fail-closed behavior.

- [x] **Step 3: Add failing controller tests**

Cover current tenant scoping, parameter binding for `from`, `to`, `mode`, and repeated `contractKey`.

- [x] **Step 4: Implement service**

Create `CdpWarehouseProductionReadinessProofService` with `proof(Long tenantId, LocalDateTime from, LocalDateTime to, String mode, List<String> contractKeys)`.

- [x] **Step 5: Implement controller**

Create `CdpWarehouseProductionReadinessController` under `/warehouse/production-readiness` and delegate with tenant context.

- [x] **Step 6: Verify**

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
  -Dtest=CdpWarehouseProductionReadinessProofServiceTest,CdpWarehouseProductionReadinessControllerTest test
rm -f "$tmp_settings"
```

Result: 6 tests run, 0 failures, 0 errors, BUILD SUCCESS.

- [x] Main compile passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile
rm -f "$tmp_settings"
```

Result: BUILD SUCCESS.

- [x] Warehouse/BI/audience regression passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine \
  -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test
rm -f "$tmp_settings"
```

Result: 525 tests run, 0 failures, 0 errors, 1 skipped.

- [x] Temporary Maven settings files were removed after verification:

```bash
find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print
```

Result: no files returned.
