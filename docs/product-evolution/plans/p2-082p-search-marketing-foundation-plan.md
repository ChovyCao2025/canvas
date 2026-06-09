# Search Marketing Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend first slice for SEO/SEM search marketing management with sources, keywords, daily snapshots, opportunities, and summaries.

**Architecture:** Use additive Flyway tables and a focused domain service under `org.chovy.canvas.domain.search`. Keep the slice connector-neutral: external providers later write normalized source, keyword, and snapshot evidence into these tables.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor `StepVerifier`.

**Implementation status (2026-06-06):** Completed. The actual Flyway migration is `V322__search_marketing_foundation.sql`. Focused search tests passed 8/8, paid-media + creator + search regression passed 23/23, and conversation adapter compatibility tests passed 41/41 after fixing a provider-attribute function invocation compile issue.

Status: Completed on 2026-06-06.

---

## Scope

This plan implements P2-082P:

- Product docs and indexes.
- Additive schema for search-marketing sources, keywords, daily snapshots, and opportunities.
- Backend DOs/mappers.
- Domain commands, views, queries, and service.
- Tenant-aware controller.
- Focused schema/service/controller tests.

## Files

- Create `docs/product-evolution/specs/p2-082p-search-marketing-foundation.md`.
- Create `docs/product-evolution/plans/p2-082p-search-marketing-foundation-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V322__search_marketing_foundation.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSourceDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingKeywordDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingSnapshotDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/SearchMarketingOpportunityDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSourceMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingKeywordMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingSnapshotMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/SearchMarketingOpportunityMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSourceCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingKeywordCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSnapshotCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityEvaluationCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSummaryQuery.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSourceView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingKeywordView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSnapshotView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingOpportunityView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/search/SearchMarketingSummaryView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/search/SearchMarketingServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/SearchMarketingControllerTest.java`.

## Tasks

### Task 1: Index P2-082P Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082P after P2-082O in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with search marketing slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082P|p2-082p-search-marketing-foundation"`**

### Task 2: Add Search Marketing Schema With TDD

- [x] **Step 1: Write failing `SearchMarketingSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add `V322__search_marketing_foundation.sql`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Domain Service With TDD

- [x] **Step 1: Write failing service tests for source/keyword upserts and tenant guards**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DOs, mappers, commands, views, and service**
- [x] **Step 4: Verify GREEN**
- [x] **Step 5: Write failing summary and opportunity evaluation tests**
- [x] **Step 6: Verify RED**
- [x] **Step 7: Implement summary metric math and deterministic opportunity creation**
- [x] **Step 8: Verify GREEN**

### Task 4: Add Tenant-Aware Controller With TDD

- [x] **Step 1: Write failing controller tests for all endpoints**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `SearchMarketingController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Docs

- [x] **Step 1: Run focused backend tests**
- [x] **Step 2: Run P2-082 paid-media/creator/search regression tests**
- [x] **Step 3: Update P2-082P and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest test
```

Result: 8 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest,CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest,SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest test
```

Result: 23 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AbstractProviderConversationReplyAdapterTest,ConversationAdapterContractMatrixTest test
```

Result: 41 tests passed, 0 failures, 0 errors.
