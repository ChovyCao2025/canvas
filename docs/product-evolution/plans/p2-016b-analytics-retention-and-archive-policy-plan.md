# Analytics Retention And Archive Policy Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-bounded retention and archive policy execution for analytics event and trace records.

**Architecture:** Use existing `V133__analytics_retention_policy.sql`, `AnalyticsRetentionPolicyDO`, `AnalyticsRetentionRunDO`, and analytics event/trace mappers. `RetentionPolicyService` resolves tenant policy with tenant `0` default fallback, enforces platform bounds from `canvas.analytics.retention.*`, runs dry-run/archive/delete in one bounded batch, skips legal-hold rows, and records every run to `analytics_retention_run`.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md`
- Depends on P2-016 analytics event/trace archive and retention fields.

## File Structure

- Existing schema: `backend/canvas-engine/src/main/resources/db/migration/V133__analytics_retention_policy.sql`
- Existing data objects: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionPolicyDO.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AnalyticsRetentionRunDO.java`
- Existing mappers: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionPolicyMapper.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsRetentionRunMapper.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventMapper.java`, `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AnalyticsEventTraceMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Modify: `docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md`
- Modify: `docs/product-evolution/plans/p2-016b-analytics-retention-and-archive-policy-plan.md`

### Task 1: Schema And Bounds Contract

**Files:**
- Existing: `backend/canvas-engine/src/main/resources/db/migration/V133__analytics_retention_policy.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`

- [x] **Step 1: Write schema and bounds tests**

`RetentionPolicyServiceTest` verifies the existing V133 migration creates retention policy/run tables with policy fields, dry-run flag, and archive/delete counters. It also verifies retention days below/above platform bounds, non-positive batch size, and unsupported record kinds are rejected.

- [x] **Step 2: Confirm red state**

Initial red state on 2026-06-09: `RetentionPolicyServiceTest` failed to compile because `RetentionPolicyService` did not exist.

- [x] **Step 3: Implement policy validation and config**

`RetentionPolicyService` accepts platform bounds through `canvas.analytics.retention.min-days`, `max-days`, `default-days`, and `max-batch-size`. It supports the current analytics targets `EVENT` and `TRACE`, and actions `ARCHIVE` and `DELETE`.

### Task 2: Tenant Policy Resolution And Bounded Jobs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/RetentionPolicyService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/RetentionPolicyServiceTest.java`

- [x] **Step 1: Add tenant default/override tests**

`tenantOverrideFallsBackToPlatformDefaultPolicy` proves tenant-specific policy lookup falls back to tenant `0` platform defaults while preserving the requested tenant scope.

- [x] **Step 2: Add dry-run/archive/delete tests**

`dryRunCountsEligibleRowsWithoutMutating` proves dry-run only counts eligible rows and records an audit result. `archiveAndDeleteAreLimitedToMaxBatchSizeAndSkipLegalHold` proves archive/delete call one bounded mapper operation per invocation and compute skipped rows as eligible rows not changed in that batch.

- [x] **Step 3: Implement run repositories**

The service wraps existing MyBatis mappers behind small repository interfaces:
- `PolicyRepository` reads enabled retention policies.
- `RetentionTargetRepository` delegates count/archive/delete to `AnalyticsEventMapper` and `AnalyticsEventTraceMapper`.
- `RunRepository` writes `AnalyticsRetentionRunDO` audit rows.

### Task 3: Verification

**Files:**
- Modify: `docs/product-evolution/specs/p2-016b-analytics-retention-and-archive-policy.md`
- Modify: `docs/product-evolution/plans/p2-016b-analytics-retention-and-archive-policy-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest
```

Result on 2026-06-09: PASS, 5 tests, 0 failures, 0 errors.

- [ ] **Step 2: Commit and merge**

Commit and merge status are intentionally not claimed by this document. Treat this as an implementation and focused-verification record until the branch is committed and integrated.
