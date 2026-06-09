# CDP Warehouse Physical E2E Certification Implementation Plan

Spec: `../specs/p2-066-cdp-warehouse-physical-e2e-certification.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a read-only physical E2E certification report that combines P2-065 logical readiness, live Doris JDBC connectivity, and live physical table contract inspection.

**Architecture:** Introduce `CdpWarehousePhysicalE2eCertificationService` as an aggregator over `CdpWarehouseProductionReadinessProofService`, optional `dorisJdbcTemplate`, and `CdpWarehouseTableGovernanceService.inspectLiveAll`. Expose it with a tenant-scoped WebFlux controller and keep the behavior read-only and fail-closed by default.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, Spring `ObjectProvider<JdbcTemplate>`, existing warehouse domain services, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-066 spec, plan, and index rows.
- Add physical E2E certification service records and verdict logic.
- Add controller endpoint `/warehouse/e2e-certification`.
- Add focused service and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Files

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePhysicalE2eCertificationServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePhysicalE2eCertificationControllerTest.java`
- Create: `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`
- Create: `docs/product-evolution/plans/p2-066-cdp-warehouse-physical-e2e-certification-plan.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create and index P2-066 docs.

- [x] **Step 2: Add failing service tests**

Cover full PASS, missing Doris fail-closed, missing Doris dry-run WARN, and live table inspection FAIL.

- [x] **Step 3: Add failing controller tests**

Cover current tenant scoping, parameter binding for `from`, `to`, `mode`, repeated `contractKey`, and `requirePhysical`.

- [x] **Step 4: Implement service**

Create `CdpWarehousePhysicalE2eCertificationService` with `certify(Long tenantId, LocalDateTime from, LocalDateTime to, String mode, List<String> contractKeys, boolean requirePhysical)`.

- [x] **Step 5: Implement controller**

Create `CdpWarehousePhysicalE2eCertificationController` under `/warehouse/e2e-certification` and delegate with tenant context.

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
  -Dtest=CdpWarehousePhysicalE2eCertificationServiceTest,CdpWarehousePhysicalE2eCertificationControllerTest test
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

Result: 535 tests run, 0 failures, 0 errors, 1 skipped. `DorisConnectionTest` remains skipped unless `DORIS_ENABLED=true` is configured.

- [x] Temporary Maven settings files are removed after verification:

```bash
find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print
```

Result: no files returned.
