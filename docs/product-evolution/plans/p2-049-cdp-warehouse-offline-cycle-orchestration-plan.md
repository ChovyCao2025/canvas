# CDP Warehouse Offline Cycle Orchestration Implementation Plan

**Goal:** Add a dependency-aware offline warehouse cycle so backfill and aggregation can be planned, run, audited, and scheduled through one production-safe entrypoint.

**Architecture:** Reuse the existing `cdp_warehouse_sync_run` and `cdp_warehouse_watermark` tables. `CdpWarehouseOperationsService` owns planning and execution, direct backfill/aggregation services remain unchanged, and `CdpWarehouseScheduler` delegates to the orchestration entrypoint behind the existing lease.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-049 spec, plan, and indexes.
- Add offline cycle plan and run records to operations service.
- Expose tenant-scoped plan and run APIs.
- Update scheduler to use orchestration.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-049 docs.

- [x] **Step 2: Add planning model**

Add plan DTOs that read backfill and aggregation watermarks and return bounded step plans.

- [x] **Step 3: Add orchestration execution**

Insert `OFFLINE_CYCLE` run rows, execute backfill, gate aggregation on backfill success, and update run status.

- [x] **Step 4: Add API and scheduler integration**

Expose plan/run endpoints and change the scheduler to call the orchestration entrypoint.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseOperationsServiceTest,CdpWarehouseSchedulerTest,CdpWarehouseControllerTest test` (16 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (371 tests, 0 failures, 0 errors, 1 skipped).

Temporary Maven settings were used to avoid the local Nexus mirror timeout and were removed after verification.
