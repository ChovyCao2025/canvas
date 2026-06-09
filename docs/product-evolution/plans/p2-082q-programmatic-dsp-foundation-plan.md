# Programmatic DSP Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend first slice for DSP/programmatic advertising management with seats, campaigns, line items, supply paths, daily snapshots, and pacing summaries.

**Architecture:** Use additive Flyway tables and a focused domain service under `org.chovy.canvas.domain.programmatic`. Keep the slice connector-neutral: provider connectors later write normalized line-item and delivery evidence into these tables.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor `StepVerifier`.

**Implementation status (2026-06-06):** Completed. The actual Flyway migration is `V324__programmatic_dsp_foundation.sql`. Focused DSP tests passed 8/8, and paid-media + creator + search + DSP regression passed 31/31.

Status: Completed on 2026-06-06.

---

## Scope

This plan implements P2-082Q:

- Product docs and indexes.
- Additive schema for DSP seats, campaigns, line items, supply paths, and performance snapshots.
- Backend DOs/mappers.
- Domain commands, views, queries, and service.
- Tenant-aware controller.
- Focused schema/service/controller tests.

## Files

- Create `docs/product-evolution/specs/p2-082q-programmatic-dsp-foundation.md`.
- Create `docs/product-evolution/plans/p2-082q-programmatic-dsp-foundation-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V324__programmatic_dsp_foundation.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspSeatDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspCampaignDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspLineItemDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspSupplyPathDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ProgrammaticDspPerformanceSnapshotDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspSeatMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspCampaignMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspLineItemMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspSupplyPathMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ProgrammaticDspPerformanceSnapshotMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspService.java`.
- Create command/view/query records under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/programmatic/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/programmatic/ProgrammaticDspServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ProgrammaticDspControllerTest.java`.

## Tasks

### Task 1: Index P2-082Q Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082Q after P2-082P in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with DSP slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082Q|p2-082q-programmatic-dsp-foundation"`**

### Task 2: Add Programmatic DSP Schema With TDD

- [x] **Step 1: Write failing `ProgrammaticDspSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add `V324__programmatic_dsp_foundation.sql`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Domain Service With TDD

- [x] **Step 1: Write failing service tests for seat/campaign/line-item upserts and tenant guards**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DOs, mappers, commands, views, and service**
- [x] **Step 4: Verify GREEN**
- [x] **Step 5: Write failing supply-path, snapshot, and summary pacing tests**
- [x] **Step 6: Verify RED**
- [x] **Step 7: Implement supply-path writes, snapshot writes, metrics, and pacing status**
- [x] **Step 8: Verify GREEN**

### Task 4: Add Tenant-Aware Controller With TDD

- [x] **Step 1: Write failing controller tests for all endpoints**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `ProgrammaticDspController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Docs

- [x] **Step 1: Run focused backend tests**
- [x] **Step 2: Run P2-082 paid-media/creator/search/DSP regression tests**
- [x] **Step 3: Update P2-082Q and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=ProgrammaticDspSchemaTest,ProgrammaticDspServiceTest,ProgrammaticDspControllerTest test
```

Result: 8 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest,CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest,SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest,ProgrammaticDspSchemaTest,ProgrammaticDspServiceTest,ProgrammaticDspControllerTest test
```

Result: 31 tests passed, 0 failures, 0 errors.
