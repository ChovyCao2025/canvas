# CDP Warehouse Semantic Metric Contracts Implementation Plan

**Goal:** Reuse existing BI metric contracts and enforce metric-level dimension compatibility in BI queries while exposing a warehouse semantic metric API.

**Architecture:** Extend runtime `BiMetricSpec` with allowed dimensions. Keep `bi_metric` as the source of persisted metric contracts. Add a read-only warehouse service over `BiDatasetSpecResolver`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

## Scope

- Add P2-040 spec, plan, and indexes.
- Extend `BiMetricSpec` with allowed dimensions while keeping the existing constructor usable.
- Propagate `BiMetricResource.allowedDimensions` into runtime specs.
- Enforce allowed dimensions in `BiQueryCompiler`.
- Add warehouse semantic metric service and controller.
- Add focused tests.
- No Flyway migration.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-040 docs.

- [x] **Step 2: Extend metric runtime contract**

Add allowed dimensions to `BiMetricSpec`, built-in metrics, and persisted dataset spec conversion.

- [x] **Step 3: Enforce metric dimensions**

Reject incompatible dimension/metric combinations during compile.

- [x] **Step 4: Add semantic metric API**

Expose tenant-scoped metric contracts under `/warehouse/semantic-metrics`.

- [x] **Step 5: Add tests and verify**

Run focused tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `14 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `249 tests, 0 failures, 1 skipped`.
