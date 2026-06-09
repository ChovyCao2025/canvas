# CDP Warehouse Table Drift Incident Auto-Resolution Implementation Plan

Spec: `../specs/p2-054-cdp-warehouse-table-drift-incident-auto-resolution.md`

**Goal:** Resolve table drift incidents automatically when later inspection evidence passes.

**Architecture:** Extend the existing incident mapper/service with stable-key table drift resolution. Update `CdpWarehouseTableDriftIncidentService.scan` so PASS reports resolve `TABLE_DRIFT:{TABLE_KEY}` incidents, while WARN/FAIL reports keep opening incidents through the existing path.

**Tech Stack:** Java 21, Spring Boot, MyBatis annotations, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-054 spec, plan, and indexes.
- Add stable-key table drift incident resolution helper.
- Update table drift scan accounting with `resolved`.
- Add focused service, scheduler, and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-054 docs.

- [x] **Step 2: Add stable-key table drift resolution**

Add mapper/service behavior to resolve only `WAREHOUSE_TABLE_DRIFT` incidents with matching stable keys and active statuses.

- [x] **Step 3: Update table drift scan lifecycle**

Resolve matching incidents for PASS reports, skip clean reports without active incidents, and preserve WARN/FAIL opening.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseIncidentServiceTest,CdpWarehouseTableDriftIncidentServiceTest,CdpWarehouseTableDriftIncidentSchedulerTest,CdpWarehouseTableDriftIncidentControllerTest test` (23 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (413 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
