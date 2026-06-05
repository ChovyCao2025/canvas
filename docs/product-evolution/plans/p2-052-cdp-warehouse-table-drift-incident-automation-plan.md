# CDP Warehouse Table Drift Incident Automation Implementation Plan

**Goal:** Convert table contract inspection drift into stable warehouse incidents so live/asset DDL drift enters the same operational queue as quality and realtime failures.

**Architecture:** Reuse `cdp_warehouse_incident` and `cdp_warehouse_table_inspection`. `CdpWarehouseTableDriftIncidentService` runs table inspections through `CdpWarehouseTableGovernanceService`, delegates non-PASS reports to `CdpWarehouseIncidentService`, and `CdpWarehouseTableDriftIncidentScheduler` runs the same scan behind the warehouse lease.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-052 spec, plan, and indexes.
- Add table drift incident mapping in the incident service.
- Add table drift incident scanner.
- Add tenant-scoped scan API and disabled-by-default scheduler.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-052 docs.

- [x] **Step 2: Add incident mapping**

Extend `CdpWarehouseIncidentService` with table drift incident input, stable key, title, and bounded description.

- [x] **Step 3: Add scanner**

Run live or asset table inspection and record incidents for WARN/FAIL reports while skipping PASS reports.

- [x] **Step 4: Add API and scheduler**

Expose scan endpoint and add disabled-by-default scheduler with lease and overlap protection.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseIncidentServiceTest,CdpWarehouseTableDriftIncidentServiceTest,CdpWarehouseTableDriftIncidentSchedulerTest,CdpWarehouseTableDriftIncidentControllerTest test` (21 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (403 tests, 0 failures, 0 errors, 1 skipped).

Temporary Maven settings were used to avoid the local Nexus mirror timeout and were removed after verification.
