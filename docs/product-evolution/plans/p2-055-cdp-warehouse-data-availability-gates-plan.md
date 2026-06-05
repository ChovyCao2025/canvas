# CDP Warehouse Data Availability Gates Implementation Plan

**Goal:** Add a concrete time-window availability decision for offline and realtime warehouse consumers.

**Architecture:** Build a read-only `CdpWarehouseAvailabilityService` over existing warehouse status, realtime pipeline status, and SLO policy thresholds. Expose a tenant-scoped controller endpoint that returns per-gate evidence and an overall PASS/WARN/FAIL verdict.

**Tech Stack:** Java 21, Spring Boot, existing warehouse services, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-055 spec, plan, and indexes.
- Add availability decision records and evaluation logic.
- Add tenant-scoped availability API.
- Add focused service and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-055 docs.

- [x] **Step 2: Add availability service**

Evaluate OFFLINE, REALTIME, and HYBRID modes using existing watermarks, runtime status, and SLO thresholds.

- [x] **Step 3: Add API integration**

Expose `/warehouse/availability` with tenant context, mode, and requested window parameters.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseAvailabilityServiceTest,CdpWarehouseAvailabilityControllerTest test` (7 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (423 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
