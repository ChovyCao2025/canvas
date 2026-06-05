# CDP Warehouse Realtime Schema Evolution Guard Implementation Plan

**Goal:** Add schema-version registration and compatibility checks to realtime warehouse pipelines.

**Architecture:** Keep P2-032 checkpoint runtime as the control-plane entry point. Add a schema registry service that evaluates schema versions independently, then let checkpoint reports reference schema versions and fold the resulting guard status into existing PASS/WARN/FAIL runtime evaluation.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Jackson, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-046 spec, plan, and indexes.
- Add schema registry migration, data object, and mapper.
- Add schema compatibility service and APIs.
- Extend checkpoint evidence with source/sink schema versions and schema status.
- Integrate schema evaluation into realtime pipeline checkpoint reporting.
- Add focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-046 docs.

- [x] **Step 2: Add schema registry persistence**

Add migration, schema DO/mapper, and checkpoint schema-version columns.

- [x] **Step 3: Add schema compatibility service**

Implement schema registration, latest/list views, hash generation, and backward compatibility checks.

- [x] **Step 4: Integrate checkpoint schema guard**

Extend checkpoint request/report fields and runtime evaluation to warn on unknown schemas and fail on breaking schemas.

- [x] **Step 5: Add APIs and tests**

Add schema controller, update pipeline controller DTOs, run focused tests, compile, and regression.

## Verification

- [x] Focused tests passed: `mvn -pl canvas-engine -Dtest=CdpWarehouseRealtimeSchemaEvolutionSchemaTest,CdpWarehouseRealtimeSchemaServiceTest,CdpWarehouseRealtimePipelineServiceTest,CdpWarehouseRealtimePipelineControllerTest,CdpWarehouseRealtimeSchemaControllerTest test` (`25 tests, 0 failures`).
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (`333 tests, 0 failures, 1 skipped`).
