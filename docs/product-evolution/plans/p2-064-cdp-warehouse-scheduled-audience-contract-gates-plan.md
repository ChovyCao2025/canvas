# CDP Warehouse Scheduled Audience Contract Gates Implementation Plan

**Goal:** Gate scheduled OLAP audience refreshes with P2-060 consumer availability contracts on a per-audience basis.

**Architecture:** Extend `AudienceMaterializationScheduleService` with a contract-gated refresh method that scans existing materialization candidates, checks due status, derives a contract key, evaluates the P2-060 contract, and materializes only allowed audiences. Extend `CdpWarehouseAudienceMaterializationScheduler` with disabled-by-default consumer contract gate configuration that takes precedence over the existing P2-058 window-level scheduled gate.

**Tech Stack:** Java 21, Spring Boot, Jackson, MyBatis Plus mapper boundaries, existing P2-060 service, existing audience materialization service, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-064 spec, plan, and index row.
- Add contract-gated scheduled refresh service method.
- Add contract key derivation from `data_source_config` with deterministic fallback.
- Add scheduler configuration and precedence.
- Add focused tests.
- Run focused tests, compile, and warehouse/BI/audience regression.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create and index P2-064 docs.

- [x] **Step 2: Add failing service tests**

Cover allowed contract materialization, blocked contract short-circuit, config override, and missing service fail-closed behavior.

- [x] **Step 3: Add failing scheduler tests**

Cover consumer contract gate precedence and default disabled compatibility.

- [x] **Step 4: Implement service contract gate**

Inject optional `CdpWarehouseConsumerAvailabilityService`, parse contract key overrides from `dataSourceConfig`, evaluate contracts per due audience, and return contract-gated scheduled refresh counts.

- [x] **Step 5: Implement scheduler configuration**

Add `consumer-contract-gate.enabled` and `consumer-contract-gate.contract-prefix` properties and route enabled cycles to the new service method.

- [x] **Step 6: Verify**

Run focused tests, compile, and warehouse/BI/audience regression. Update this plan with verification evidence.

## Verification

- [x] Focused tests pass: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest=AudienceMaterializationScheduleServiceTest,CdpWarehouseAudienceMaterializationSchedulerTest test` (15 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS).
- [x] Main compile passes: `mvn -s "$tmp_settings" -pl canvas-engine -DskipTests compile` (BUILD SUCCESS, 2026-06-05 10:00 CST).
- [x] Clean warehouse/BI/audience regression passes: `mvn -s "$tmp_settings" -pl canvas-engine -Dtest='CdpWarehouse*Test,CdpAudience*Test,Bi*Test,MarketingBi*Test,Doris*Test,AudienceMaterialization*Test,CdpOlapAudienceSchemaTest,StableUserIndexServiceTest,VersionedAudienceBitmapStoreTest,BehaviorAudienceRuleCompilerTest,AudienceQualityServiceTest,MyBatisAudienceDefinitionRepositoryTest' clean test` (511 tests, 0 failures, 0 errors, 1 skipped; BUILD SUCCESS, 2026-06-05 10:08 CST).
- [x] Index audit passes: `rg -n "P2-064|p2-064-cdp-warehouse-scheduled-audience-contract-gates" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/p2-064-cdp-warehouse-scheduled-audience-contract-gates-plan.md docs/product-evolution/specs/p2-064-cdp-warehouse-scheduled-audience-contract-gates.md`.
- [x] Temporary Maven settings were removed after each Maven run; final `find /tmp -maxdepth 1 -name 'canvas-maven-settings.*' -print` returned no files.
