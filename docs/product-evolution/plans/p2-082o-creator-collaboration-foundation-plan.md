# Creator Collaboration Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend first slice for KOL/KOC creator collaboration management with creator profiles, campaigns, collaborations, deliverables, and performance summaries.

**Architecture:** Use additive Flyway tables and a focused domain service under `org.chovy.canvas.domain.creator`. Keep this slice connector-neutral: external platforms later write normalized creator and deliverable evidence into these tables.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor `StepVerifier`.

**Implementation status (2026-06-06):** Completed. The actual Flyway migration is `V321__creator_collaboration_foundation.sql` because `V320` is already allocated to BI Quick Engine capacity policy in the current workspace. Focused creator tests passed 7/7, paid-media + creator regression passed 15/15, and conversation adapter compatibility tests passed 37/37 after restoring shared provider adapter testCompile compatibility.

Status: Completed on 2026-06-06.

---

## Scope

This plan implements P2-082O:

- Product docs and indexes.
- Additive schema for creators, campaigns, collaborations, and deliverables.
- Backend DOs/mappers.
- Domain commands, views, queries, and service.
- Tenant-aware controller.
- Focused schema/service/controller tests.

## Files

- Create `docs/product-evolution/specs/p2-082o-creator-collaboration-foundation.md`.
- Create `docs/product-evolution/plans/p2-082o-creator-collaboration-foundation-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V321__creator_collaboration_foundation.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorProfileDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorCampaignDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorCollaborationDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CreatorDeliverableDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorProfileMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorCampaignMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorCollaborationMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CreatorDeliverableMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProfileCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCampaignCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorDeliverableCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorPerformanceSummaryQuery.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorProfileView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCampaignView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorCollaborationView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorDeliverableView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/creator/CreatorPerformanceSummaryView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorCollaborationSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/creator/CreatorCollaborationServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CreatorCollaborationControllerTest.java`.

## Tasks

### Task 1: Index P2-082O Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082O after P2-082N in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with creator collaboration slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082O|p2-082o-creator-collaboration-foundation"`**

### Task 2: Add Creator Collaboration Schema With TDD

- [x] **Step 1: Write failing `CreatorCollaborationSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add `V321__creator_collaboration_foundation.sql`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Domain Service With TDD

- [x] **Step 1: Write failing service tests for upserts and tenant guards**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DOs, mappers, commands, views, and service**
- [x] **Step 4: Verify GREEN**
- [x] **Step 5: Write failing summary aggregation test**
- [x] **Step 6: Verify RED**
- [x] **Step 7: Implement summary aggregation, commission, ROI, and overdue counts**
- [x] **Step 8: Verify GREEN**

### Task 4: Add Tenant-Aware Controller With TDD

- [x] **Step 1: Write failing controller tests for all endpoints**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `CreatorCollaborationController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Docs

- [x] **Step 1: Run focused backend tests**
- [x] **Step 2: Run P2-082 paid-media/creator regression tests**
- [x] **Step 3: Update P2-082O and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest test
```

Result: 7 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest,CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest test
```

Result: 15 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AbstractProviderConversationReplyAdapterTest,ConversationAdapterContractMatrixTest test
```

Result: 37 tests passed, 0 failures, 0 errors.
