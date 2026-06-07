# AI Decision Models Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add governed AI decision model runs, user recommendations, feedback, and tenant-scoped APIs for LTV, next-best-action, next-best-offer, and channel affinity.

**Architecture:** Extend the existing `domain/ai` module with a deterministic decisioning service that consumes CDP profiles, churn snapshots, feature snapshots, smart timing, and consent. Persist all model outputs in new run/recommendation/feedback tables so future external models can replace the scorer without changing the audit contract.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

This plan implements the P2-082F backend first slice:

- Schema and MyBatis data objects/mappers.
- Domain command/query/view records.
- Deterministic decision scoring service with feature/explanation JSON.
- Feedback write path and bounded recommendation reads.
- Controller APIs under `/ai/decisions`.
- Focused schema, service, and controller tests.

External AI serving, autonomous execution, frontend pages, and social/competitor monitoring are deferred.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V309__ai_decision_models.sql`.
- Create data objects:
  - `AiDecisionRunDO`
  - `AiUserDecisionRecommendationDO`
  - `AiDecisionFeedbackDO`
- Create mappers:
  - `AiDecisionRunMapper`
  - `AiUserDecisionRecommendationMapper`
  - `AiDecisionFeedbackMapper`
- Create domain records and service under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiDecisionController.java`.
- Add tests:
  - `AiDecisionModelSchemaTest`
  - `AiDecisionModelServiceTest`
  - `AiDecisionControllerTest`
- Update P2-082 parent docs and indexes.

## Tasks

### Task 1: Index P2-082F Docs

**Files:**
- Create: `docs/product-evolution/specs/p2-082f-ai-decision-models.md`
- Create: `docs/product-evolution/plans/p2-082f-ai-decision-models-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- Modify: `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`
- Modify: `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082F index rows after P2-082E**
- [x] **Step 3: Update parent P2-082 status from pending to in progress**
- [x] **Step 4: Verify indexability**

Run:

```bash
rg -n "P2-082F|p2-082f-ai-decision-models" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082f-ai-decision-models.md docs/product-evolution/plans/p2-082f-ai-decision-models-plan.md docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md
```

Expected: every file has a P2-082F or slug match.

### Task 2: Add AI Decision Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/AiDecisionModelSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V309__ai_decision_models.sql`

- [x] **Step 1: Write failing schema test**

Assert the migration contains:

- `ai_decision_run`
- `ai_user_decision_recommendation`
- `ai_decision_feedback`
- `uk_ai_decision_run`
- `idx_ai_decision_reco_type_rank`
- `idx_ai_decision_feedback_reco`

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AiDecisionModelSchemaTest test
```

Expected before migration: FAIL because the migration is missing.

- [x] **Step 3: Add migration**

Create the three tables with tenant-scoped unique keys, JSON feature/explanation columns, counters, actor fields, and query indexes.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 3: Add Data Objects And Mappers

**Files:**
- Create data objects and mappers listed in the Files section.

- [x] **Step 1: Add Lombok/MyBatis data objects**
- [x] **Step 2: Add mapper interfaces**
- [x] **Step 3: Compile schema test**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AiDecisionModelSchemaTest test
```

Expected: PASS.

### Task 4: Implement Decision Service With TDD

**Files:**
- Create: `AiDecisionModelServiceTest.java`
- Create: `AiDecisionModelService.java`
- Create domain records for commands, queries, feedback, and views.

- [x] **Step 1: Write failing service tests**

Cover:

- recompute creates one run and four recommendation types per processed user;
- recommendation rows include feature JSON, explanation JSON, model version, confidence, score, rank, and fallback reason;
- budget caps mark expensive offers ineligible without deleting their audit rows;
- recommendation queries are tenant-scoped and bounded;
- feedback rejects cross-tenant recommendations and writes actor/outcome metadata for owned rows.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AiDecisionModelServiceTest test
```

Expected: FAIL because service/records do not exist.

- [x] **Step 3: Implement minimal service**

Use existing `ChurnFeatureSnapshotService`, `SmartTimingService`, `AiUserPredictionSnapshotMapper`, `CdpUserProfileMapper`, and `MarketingConsentMapper`. Serialize features and explanations with Jackson. Keep the scorer deterministic and local.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 5: Add Controller

**Files:**
- Create: `AiDecisionControllerTest.java`
- Create: `AiDecisionController.java`

- [x] **Step 1: Write failing controller tests**

Cover:

- recompute requires admin and passes tenant/actor/request;
- latest run passes tenant and decision scope;
- recommendations endpoint passes filters and bounded limits;
- feedback endpoint passes tenant, actor, recommendation id, and command.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AiDecisionControllerTest test
```

Expected: FAIL because controller does not exist.

- [x] **Step 3: Implement controller**

Follow existing AI prediction controller patterns: admin access via `TenantContextResolver.current()`, `R.ok`, bounded limits, actor from tenant context, and `Schedulers.boundedElastic()`.

- [x] **Step 4: Verify GREEN**

Run controller tests and expect PASS.

### Task 6: Verify P2-082F Focused Suite

- [x] **Step 1: Verify focused suite**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=AiDecisionModelSchemaTest,AiDecisionModelServiceTest,AiDecisionControllerTest,ChurnPredictionServiceTest,SmartTimingServiceTest,PredictionProfileWriterTest test
```

Expected: all tests pass.
