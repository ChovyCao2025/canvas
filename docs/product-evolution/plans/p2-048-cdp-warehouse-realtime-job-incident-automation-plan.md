# CDP Warehouse Realtime Job Incident Automation Implementation Plan

**Goal:** Route realtime warehouse job health into incidents and readiness so failed or stale external streaming jobs are visible without manual polling.

**Architecture:** Reuse the existing incident table and scheduler lease pattern. `CdpWarehouseRealtimeJobIncidentService` scans P2-047 job status, `CdpWarehouseIncidentService` owns incident row creation, and `CdpWarehouseReadinessService` combines pipeline and job health for realtime readiness.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-048 spec, plan, and indexes.
- Extend incident service for realtime job incidents.
- Add realtime job incident scan service and tenant-scoped scan API.
- Add disabled-by-default scheduler with lease and overlap guard.
- Add realtime job health into readiness realtime status.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-048 docs.

- [x] **Step 2: Extend incident model behavior**

Add realtime job incident input, stable incident key, title, and bounded description using existing `cdp_warehouse_incident` persistence.

- [x] **Step 3: Add job incident scan API**

Add scan service and `/warehouse/realtime/jobs/incidents/scan` controller endpoint.

- [x] **Step 4: Add scheduler**

Add disabled-by-default scheduled cycle with tenant id, max heartbeat age, scan limit, lease TTL, fixed delay, and overlap guard.

- [x] **Step 5: Add readiness integration**

Update realtime readiness to include both pipeline and job health counts.

- [x] **Step 6: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s <temp-central-settings> -pl canvas-engine -Dtest=CdpWarehouseRealtimeJobIncidentServiceTest,CdpWarehouseRealtimeJobIncidentSchedulerTest,CdpWarehouseRealtimeJobIncidentControllerTest,CdpWarehouseIncidentServiceTest,CdpWarehouseReadinessServiceTest test` (`25 tests, 0 failures`).
- [x] Main compile passed: `mvn -s <temp-central-settings> -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s <temp-central-settings> -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`364 tests, 0 failures, 1 skipped`).

Note: verification used a temporary Maven settings file without the corporate Central mirror because `https://nmvn.corp.photontech.cc/repository/public` timed out during dependency resolution. No project or user Maven settings were changed.
