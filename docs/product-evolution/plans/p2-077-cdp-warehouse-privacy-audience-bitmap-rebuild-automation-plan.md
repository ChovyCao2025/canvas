# CDP Warehouse Privacy Audience Bitmap Rebuild Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically run P2-076 audience bitmap rebuild proof for privacy erasure requests whose upstream assets are already proven erased.

**Architecture:** Add a small automation service that reads recent P2-073 requests, filters for eligible audience bitmap rebuild candidates, and delegates to the existing P2-076 rebuild service. Add a scheduler that wraps the automation cycle with existing warehouse lease semantics and an in-process running guard.

**Tech Stack:** Java 21, Spring scheduling, MyBatis-backed domain services, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-077 spec, plan, and index rows.
- Add failing automation service and scheduler tests first.
- Implement the automation service.
- Implement the lease-protected scheduler.
- Verify focused P2-077 tests and warehouse/CDP regression.

## Files

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildScheduler.java`
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest.java`
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest.java`
- Modify `docs/product-evolution/specs/INDEX.md`
- Modify `docs/product-evolution/plans/INDEX.md`
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create P2-077 docs and insert index rows after P2-076.

- [x] **Step 2: Write automation service tests**

Cover eligible requests, upstream non-pass skips, failed audience proof skip, and failed audience proof retry.

- [x] **Step 3: Write scheduler tests**

Cover disabled scheduler, lease delegation, and non-overlap guard.

- [x] **Step 4: Implement automation service**

Load recent requests, filter candidates, delegate to P2-076, and return a cycle summary.

- [x] **Step 5: Implement scheduler**

Add `@Scheduled` cycle with config, lease protection, and running guard.

- [x] **Step 6: Verify**

Run focused tests:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyErasureControllerTest
```

Run warehouse/CDP regression:

```bash
cd backend
mvn -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest'
```

## Verification

- Focused P2-077 tests passed on 2026-06-05:
  `CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyErasureControllerTest`
  - Result: 16 tests, 0 failures.
- Warehouse/CDP regression passed on 2026-06-05:
  `CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest`
  - Result: 414 tests, 0 failures, 1 skipped (`DorisConnectionTest`).
- P2-077 robustness audit updated on 2026-06-05:
  `CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest.rebuildExceptionRecordsFailedRequestAndContinuesCycle`
  proves one P2-076 rebuild failure is recorded as a per-request `FAIL` result and later eligible requests still run in the same bounded cycle.
- Focused P2-077/P2-078/P2-079 privacy audience rebuild suite passed on 2026-06-05:
  `CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunSchemaTest`
  - Result: 27 tests, 0 failures.
