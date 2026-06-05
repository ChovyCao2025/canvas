# CDP Warehouse Table Drift Remediation Planning Implementation Plan

**Goal:** Convert table drift inspection evidence into safe, reviewable remediation plans without executing Doris DDL.

**Architecture:** Extend `CdpWarehouseTableGovernanceService` so remediation planning runs a fresh asset/live inspection, maps known violations to conservative steps, and returns executable SQL only for safe property changes. Keep all behavior read-only except the existing inspection evidence write.

**Tech Stack:** Java 21, Spring Boot, Doris SQL text generation, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-053 spec, plan, and indexes.
- Add table remediation plan records and generation logic.
- Add tenant-scoped single/all remediation plan APIs.
- Add focused service and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-053 docs.

- [x] **Step 2: Add remediation planning model**

Add response records for summary, table plan, and remediation step. Include risk, executable flag, action, reason, and optional SQL.

- [x] **Step 3: Map drift violations to conservative actions**

Generate review SQL for safe table property changes and manual high-risk actions for rebuild/migration cases.

- [x] **Step 4: Add API integration**

Expose tenant-scoped single-table and all-table remediation planning under `/warehouse/tables`.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseTableGovernanceServiceTest,CdpWarehouseTableGovernanceControllerTest test` (21 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (410 tests, 0 failures, 0 errors, 1 skipped).
- [x] Temporary Maven settings were removed after verification.
