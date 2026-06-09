# CDP Warehouse Live Doris DDL Drift Implementation Plan

Spec: `../specs/p2-051-cdp-warehouse-live-doris-ddl-drift.md`

**Goal:** Extend warehouse physical table governance from repository DDL inspection to live Doris table drift detection.

**Architecture:** Reuse `CdpWarehouseTableGovernanceService` and `cdp_warehouse_table_inspection`. Add a live DDL reader backed by optional `dorisJdbcTemplate`, feed live `SHOW CREATE TABLE` output through the existing contract checks, and expose tenant-scoped live inspection endpoints.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, optional Doris `JdbcTemplate`, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-051 spec, plan, and indexes.
- Add live DDL reader and live inspection methods.
- Expose tenant-scoped live single/all inspection APIs.
- Add focused service and controller tests.
- Run focused tests and regression verification.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-051 docs.

- [x] **Step 2: Add live DDL reader**

Add an optional Doris-backed reader that uses `SHOW CREATE TABLE` and fails closed when Doris is unavailable.

- [x] **Step 3: Reuse contract inspection rules**

Run live DDL through the existing partition, retention, replica, bucket, and distribution checks.

- [x] **Step 4: Add API integration**

Expose live single-table and live all-table inspection endpoints under `/warehouse/tables`.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=CdpWarehouseTableGovernanceServiceTest,CdpWarehouseTableGovernanceControllerTest test` (15 tests, 0 failures, 0 errors, 0 skipped).
- [x] Main compile passed: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' test` (390 tests, 0 failures, 0 errors, 1 skipped).

Temporary Maven settings were used to avoid the local Nexus mirror timeout and were removed after verification.
