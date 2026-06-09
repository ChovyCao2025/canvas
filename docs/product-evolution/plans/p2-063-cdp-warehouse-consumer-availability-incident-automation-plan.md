# CDP Warehouse Consumer Availability Incident Automation Implementation Plan

Spec: `../specs/p2-063-cdp-warehouse-consumer-availability-incident-automation.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

**Goal:** Route P2-060 consumer contract WARN/FAIL/PASS scan evidence into warehouse incidents.

**Architecture:** Reuse `cdp_warehouse_incident` and `CdpWarehouseIncidentService`. Add a consumer availability incident service that evaluates one contract or active contracts, records `WAREHOUSE_CONSUMER_AVAILABILITY` incidents for WARN/FAIL, resolves matching incidents for PASS, and exposes manual scan plus disabled scheduled scan paths.

**Tech Stack:** Java 21, Spring Boot, MyBatis Plus mapper updates, existing P2-060 service, existing incident service, existing warehouse lease service, WebFlux controller style, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-063 spec, plan, and indexes.
- Extend incident service and mapper for consumer availability stable keys.
- Add consumer availability incident scan service.
- Add manual scan controller.
- Add disabled scheduler with lease and overlap guards.
- Add focused tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-063 docs.

- [x] **Step 2: Extend incident stable-key support**

Add consumer availability incident input, open behavior, source-scoped resolution, titles, descriptions, and tests.

- [x] **Step 3: Add contract scan service and API**

Evaluate one contract or active contracts, open/resolve incidents, count failures, and expose tenant-scoped scan API.

- [x] **Step 4: Add scheduled scan**

Add disabled-by-default scheduler using tenant, consumer type, rolling window, lease, and overlap protection.

- [x] **Step 5: Verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseIncidentServiceTest,CdpWarehouseConsumerAvailabilityIncidentServiceTest,CdpWarehouseConsumerAvailabilityIncidentSchedulerTest,CdpWarehouseConsumerAvailabilityIncidentControllerTest test` (30 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (497 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
