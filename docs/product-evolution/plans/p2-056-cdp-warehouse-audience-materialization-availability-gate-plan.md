# CDP Warehouse Audience Materialization Availability Gate Implementation Plan

Spec: `../specs/p2-056-cdp-warehouse-audience-materialization-availability-gate.md`

**Goal:** Add an availability-gated audience materialization path without changing the existing manual materialization endpoint.

**Architecture:** Reuse `CdpWarehouseAvailabilityService` from P2-055 inside `AudienceMaterializationOperationsService`. Return a combined response that carries the availability decision and, only when allowed, the materialization result.

**Tech Stack:** Java 21, Spring Boot, existing warehouse availability service, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-056 spec, plan, and indexes.
- Add gated materialization result model and operation.
- Add tenant-scoped gated API endpoint.
- Add focused service and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-056 docs.

- [x] **Step 2: Add gated operation**

Evaluate warehouse availability and invoke materialization only when PASS or allowed WARN.

- [x] **Step 3: Add API integration**

Expose a tenant-scoped gated materialization endpoint with window, mode, allowWarn, and operator fields.

- [x] **Step 4: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=AudienceMaterializationOperationsServiceTest,CdpWarehouseAudienceMaterializationControllerTest test` (14 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (427 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
