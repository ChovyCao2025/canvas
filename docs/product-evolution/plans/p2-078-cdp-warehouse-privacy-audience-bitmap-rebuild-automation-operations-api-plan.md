# CDP Warehouse Privacy Audience Bitmap Rebuild Automation Operations API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a manual operations API that runs the existing P2-077 privacy audience bitmap rebuild automation cycle and returns its summary.

**Architecture:** Extend the existing `CdpWarehousePrivacyErasureController` with one endpoint that resolves tenant context and delegates to `CdpWarehousePrivacyAudienceBitmapRebuildAutomationService`. Keep the existing P2-076 single-request rebuild endpoint, P2-077 service, and scheduler semantics unchanged.

**Tech Stack:** Java 21, Spring WebFlux controller wrappers, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-078 spec, plan, and index rows.
- Add a failing controller test first.
- Inject the P2-077 automation service into `CdpWarehousePrivacyErasureController`.
- Add `POST /warehouse/privacy/erasure/audience-rebuild/automation/run`.
- Verify focused P2-078 tests and warehouse/CDP regression.

## Files

- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureControllerTest.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create P2-078 docs and insert index rows after P2-077.

- [ ] **Step 2: Write failing controller test**

Add a test that constructs `CdpWarehousePrivacyErasureController` with a mocked `CdpWarehousePrivacyAudienceBitmapRebuildAutomationService`, calls `runAudienceBitmapRebuildAutomation`, and verifies tenant id plus command delegation.

Expected focused red command:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyErasureControllerTest
```

Expected failure before implementation: controller constructor or method for automation run is missing.

- [ ] **Step 3: Implement controller injection and endpoint**

Add the automation service field, keep existing test-friendly constructors source-compatible, add the Spring constructor with the automation service, and implement:

```java
@PostMapping("/audience-rebuild/automation/run")
public Mono<R<CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult>>
runAudienceBitmapRebuildAutomation(
        @RequestBody(required = false)
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command)
```

The method resolves current tenant, fails closed if the automation service is absent, delegates to `automationService.run(tenantId, command)`, and returns `R.ok(result)`.

- [ ] **Step 4: Verify focused tests**

Run:

```bash
cd backend
mvn -pl canvas-engine test -Dtest=CdpWarehousePrivacyErasureControllerTest,CdpWarehousePrivacyAudienceBitmapRebuildAutomationServiceTest,CdpWarehousePrivacyAudienceBitmapRebuildSchedulerTest,CdpWarehousePrivacyAudienceBitmapRebuildServiceTest
```

Expected: all selected tests pass.

- [ ] **Step 5: Verify warehouse/CDP regression**

Run:

```bash
cd backend
mvn -pl canvas-engine test -Dtest='CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest'
```

Expected: all selected tests pass; `DorisConnectionTest` may be skipped unless `DORIS_ENABLED=true`.

## Verification

Record fresh command output after implementation.
