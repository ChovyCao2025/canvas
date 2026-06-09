# CDP Warehouse SLO Policy Gates Implementation Plan

Spec: `../specs/p2-039-cdp-warehouse-slo-policy-gates.md`

**Goal:** Add tenant-governed readiness SLO thresholds and wire them into offline and audience materialization readiness checks.

**Architecture:** Add `CdpWarehouseSloPolicyService` as the SLO policy control plane. `CdpWarehouseReadinessService` resolves the effective policy once per readiness call and uses it for freshness/gap decisions.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-039 spec, plan, and indexes.
- Add SLO policy schema with seeded global default.
- Add policy DO, mapper, service, and controller.
- Integrate policy thresholds into readiness.
- Add focused tests.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-039 docs.

- [x] **Step 2: Add schema and persistence**

Add `cdp_warehouse_slo_policy`, default seed, DO, and mapper.

- [x] **Step 3: Add service and API**

Implement policy list, upsert, effective resolution, and tenant-scoped controller endpoints.

- [x] **Step 4: Wire readiness gates**

Use effective policy thresholds for offline sync run/watermark gap and audience materialization run gap.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `14 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `235 tests, 0 failures, 1 skipped`.
