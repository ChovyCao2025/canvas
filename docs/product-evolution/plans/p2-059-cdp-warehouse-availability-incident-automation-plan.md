# CDP Warehouse Availability Incident Automation Implementation Plan

**Goal:** Route warehouse availability WARN/FAIL gate evidence into incidents and resolve matching incidents when availability later passes.

**Architecture:** Reuse `CdpWarehouseAvailabilityService` for decisions and `CdpWarehouseIncidentService` for durable incident lifecycle. Add stable `AVAILABILITY:{MODE}:{GATE_KEY}` incident keys, a scan service, an API, and a disabled-by-default lease-protected scheduler.

**Tech Stack:** Java 21, Spring Boot, MyBatis mapper updates, existing warehouse incident table, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-059 spec, plan, and indexes.
- Add availability incident record/resolve methods.
- Add availability incident scan service.
- Add scan API and scheduled scanner.
- Add focused service, scheduler, controller, and incident tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-059 docs.

- [x] **Step 2: Add incident lifecycle support**

Add stable availability incident keys, record logic, and source-scoped resolve logic.

- [x] **Step 3: Add scan API and scheduler**

Evaluate availability gates, open/resolve incidents, expose manual scan, and add disabled scheduled scanning.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseAvailabilityIncidentServiceTest,CdpWarehouseAvailabilityIncidentSchedulerTest,CdpWarehouseAvailabilityIncidentControllerTest,CdpWarehouseIncidentServiceTest test` (26 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (458 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
