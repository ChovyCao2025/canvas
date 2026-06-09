# BI Dataset Query Compiler Foundation Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a white-listed BI query compiler for Doris-backed marketing analytics datasets.

**Architecture:** Represent datasets as registered field and metric specs. Compile operator requests into SQL only from registered dimensions, metrics, filters, and sorts, with tenant filtering and bound parameters.

**Tech Stack:** Java 21 records, JUnit 5, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-023-bi-dataset-query-compiler-foundation.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused
verification passed on 2026-06-09:

- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=BiQueryCompilerTest,MarketingBiDatasetRegistryTest` passed with 10 tests, zero failures, and zero errors.

Historical RED-state checks were not reproduced because the current worktree
already contains the implementation. No commit or merge was created in this
audit, so commit and merge status remain unverified.

## File Structure

- Create BI query records under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/`.
- Create `BiQueryCompiler` in the same package.
- Create `MarketingBiDatasetRegistry` with the `canvas_daily_stats` dataset.
- Add tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/`.

### Task 1: Query Compiler Value Objects

**Files:**
- Create: `BiFieldSpec.java`
- Create: `BiMetricSpec.java`
- Create: `BiDatasetSpec.java`
- Create: `BiFilter.java`
- Create: `BiSort.java`
- Create: `BiQueryRequest.java`
- Create: `BiCompiledQuery.java`

- [x] **Step 1: Write failing compiler tests**

Create `BiQueryCompilerTest` with grouped query, filter binding, unknown dimension, unknown metric, and unsafe limit cases.

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=BiQueryCompilerTest
```

Expected: FAIL because compiler records and compiler do not exist.

- [x] **Step 3: Implement value objects and compiler**

Implement immutable records, safe operator handling, tenant predicate injection, parameter binding, group by, order by, and limit validation.

- [x] **Step 4: Run compiler tests**

Expected: PASS.

### Task 2: Marketing Dataset Registry

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/query/MarketingBiDatasetRegistryTest.java`

- [x] **Step 1: Write failing registry test**

Create `MarketingBiDatasetRegistryTest` proving `canvas_daily_stats` maps to `canvas_dws.canvas_daily_stats`, tenant column `tenant_id`, and core canvas stats fields and metrics.

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Expected: FAIL because the registry does not exist.

- [x] **Step 3: Implement registry**

Register dimensions `stat_date`, `canvas_id`, `canvas_name`, `trigger_type`; measures and metrics for executions, success/fail counts, unique users, duration, and success rate.

- [x] **Step 4: Run registry test**

Expected: PASS.

### Task 3: Focused Verification

- [x] **Step 1: Run BI tests**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=BiQueryCompilerTest,MarketingBiDatasetRegistryTest
```

Expected: PASS with zero failures and zero errors.

## Self-Review

- Spec coverage: tasks cover value objects, compiler, registry, and focused verification.
- Placeholder scan: no placeholder markers remain.
- Type consistency: file and class names match the implemented package.
