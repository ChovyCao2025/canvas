# CDP Warehouse E2E Certification History Implementation Plan

Spec: `../specs/p2-067-cdp-warehouse-e2e-certification-history.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist P2-066 physical E2E certification runs so warehouse production readiness decisions are auditable.

**Architecture:** Add a small certification run ledger table and a service that delegates to `CdpWarehousePhysicalE2eCertificationService`, serializes its evidence, and stores one row per run. Expose tenant-scoped run/list/get APIs without changing the immediate P2-066 certification endpoint.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway SQL migrations, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-067 spec, plan, and index rows.
- Add Flyway schema for persisted E2E certification run history.
- Add DO/Mapper for the run ledger.
- Add service that runs P2-066, persists success/failure, and exposes list/get.
- Add controller endpoint family under `/warehouse/e2e-certification/runs`.
- Add focused schema, service, and controller tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create and index P2-067 docs.

- [x] **Step 2: Add failing schema test**

Cover creation of `cdp_warehouse_e2e_certification_run`, tenant/status indexes, and JSON evidence columns.

- [x] **Step 3: Add failing service tests**

Cover persisted PASS, persisted FAIL on delegated certification exception, tenant-scoped list, and tenant-scoped get.

- [x] **Step 4: Add failing controller tests**

Cover current tenant scoping, request parameter binding, run listing, and single run lookup.

- [x] **Step 5: Implement schema and DAL**

Create migration, DO, and Mapper.

- [x] **Step 6: Implement service**

Create `CdpWarehouseE2eCertificationRunService` with `run`, `recent`, and `get`.

- [x] **Step 7: Implement controller**

Create `CdpWarehouseE2eCertificationRunController` under `/warehouse/e2e-certification/runs`.

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
  -Dtest=CdpWarehouseE2eCertificationRunSchemaTest,CdpWarehouseE2eCertificationRunServiceTest,CdpWarehouseE2eCertificationRunControllerTest test
rm -f "$tmp_settings"
```

Result: `BUILD SUCCESS`; 8 tests run, 0 failures, 0 errors, 0 skipped.

- [x] Main compile passes:

```bash
tmp_settings="$(mktemp /tmp/canvas-maven-settings.XXXXXX)"
printf '%s\n' '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd"></settings>' > "$tmp_settings"
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
  mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile
rm -f "$tmp_settings"
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

Result: `BUILD SUCCESS`.

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

Result: `BUILD SUCCESS`; 561 tests run, 0 failures, 0 errors, 1 skipped. `DorisConnectionTest` remains skipped unless a real Doris environment is enabled with `DORIS_ENABLED=true`.

- [x] Temporary Maven settings files are removed after verification:

```bash
find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print
```

Result: no matching files printed.
