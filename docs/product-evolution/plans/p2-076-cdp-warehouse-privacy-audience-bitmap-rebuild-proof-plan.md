# CDP Warehouse Privacy Audience Bitmap Rebuild Proof Implementation Plan

Spec: `../specs/p2-076-cdp-warehouse-privacy-audience-bitmap-rebuild-proof.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild CDP warehouse audience bitmap materializations after privacy erasure and record `AUDIENCE_BITMAP_VERSION` proof in the existing P2-073 ledger.

**Architecture:** Add a focused domain service that loads the erasure request, gates rebuilds on upstream asset proof, selects enabled offline or hybrid audience definitions, delegates rebuilds to the existing audience materialization operations service, and records the aggregate result through `CdpWarehousePrivacyErasureService`. Expose the worker through the existing privacy erasure controller so operators can run it for one request.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-076 spec, plan, and index rows.
- Add failing service and controller tests first.
- Implement the rebuild proof service.
- Wire an operator endpoint to the existing privacy erasure controller.
- Verify focused P2-076 tests and warehouse/CDP regression.

## Files

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`
- Add `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildServiceTest.java`
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java`
- Modify `docs/product-evolution/specs/INDEX.md`
- Modify `docs/product-evolution/plans/INDEX.md`
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create P2-076 docs and insert index rows after P2-075.

- [x] **Step 2: Write audience rebuild service tests**

Cover upstream proof blocking, successful rebuild proof, no-candidate skip proof, and failed materialization proof.

- [x] **Step 3: Write controller binding test**

Cover `POST /warehouse/privacy/erasure/requests/{id}/audience-rebuild` tenant binding and request body forwarding.

- [x] **Step 4: Implement rebuild proof service**

Load the request, evaluate upstream proof statuses, select eligible audiences, rebuild through `AudienceMaterializationOperationsService`, and record `AUDIENCE_BITMAP_VERSION` proof.

- [x] **Step 5: Wire controller endpoint**

Inject the rebuild service optionally and expose the rebuild endpoint.

- [x] **Step 6: Verify**

Run focused tests:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyErasureControllerTest
```

Run warehouse/CDP regression:

```bash
cd backend
mvn -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest'
```

## Verification

- Focused P2-076 tests passed on 2026-06-05:
  `CdpWarehousePrivacyAudienceBitmapRebuildServiceTest,CdpWarehousePrivacyErasureControllerTest`
  - Result: 9 tests, 0 failures.
- Warehouse/CDP regression passed on 2026-06-05:
  `CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest`
  - Result: 407 tests, 0 failures, 1 skipped (`DorisConnectionTest`).
