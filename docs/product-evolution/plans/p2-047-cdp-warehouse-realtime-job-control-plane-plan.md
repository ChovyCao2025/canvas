# CDP Warehouse Realtime Job Control Plane Implementation Plan

**Goal:** Add realtime warehouse job instance heartbeats and auditable operator actions for external streaming jobs.

**Architecture:** Keep external Flink/Kafka/Doris jobs outside canvas-engine. Canvas persists job runtime state and action requests; external jobs call heartbeat and pending-action APIs. Status evaluation is deterministic and tenant scoped.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-047 spec, plan, and indexes.
- Add job instance and action audit schema.
- Add DAL objects and mappers.
- Add realtime job control service.
- Add tenant-scoped REST APIs.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-047 docs.

- [x] **Step 2: Add job control persistence**

Add migration, job instance/action DOs, and mappers.

- [x] **Step 3: Add service logic**

Implement heartbeat upsert, status evaluation, action request, pending action polling, acknowledgement, and completion.

- [x] **Step 4: Add APIs**

Expose tenant-scoped job control endpoints.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseRealtimeJobControlSchemaTest,CdpWarehouseRealtimeJobControlServiceTest,CdpWarehouseRealtimeJobControllerTest test` (`11 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`345 tests, 0 failures, 1 skipped`).
