# Sentiment And Competitor Monitoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped monitoring sources, mention ingestion, deterministic sentiment analysis, competitor extraction, and alert workflow APIs.

**Architecture:** Keep external crawlers out of scope. Implement a connector-neutral persistence and analysis layer under `domain/monitoring`, with thin WebFlux controller endpoints and deterministic local scoring.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

**Implementation Status:** Current workspace record: delivered backend first slice. Verification results are recorded below.

---

## Scope

This plan implements the P2-082G backend first slice:

- Schema and MyBatis data objects/mappers.
- Domain command/query/view records.
- Monitoring service with source upsert, item ingestion, lexicon sentiment, competitor extraction, alert creation, reads, and alert resolution.
- Controller APIs under `/canvas/marketing-monitoring`.
- Focused schema, service, and controller tests.

Real external crawling, LLM inference, alert fanout, and frontend pages are deferred.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V311__sentiment_competitor_monitoring.sql`.
- Create data objects and mappers for:
  - `MarketingMonitorSource`
  - `MarketingMonitorItem`
  - `MarketingSentimentAnalysis`
  - `MarketingCompetitorMention`
  - `MarketingMonitorAlert`
- Create domain records and service under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Add tests:
  - `MarketingMonitoringSchemaTest`
  - `MarketingMonitoringServiceTest`
  - `MarketingMonitoringControllerTest`
- Update P2-082 parent docs and indexes.

## Tasks

### Task 1: Index P2-082G Docs

**Files:**
- Create: `docs/product-evolution/specs/p2-082g-sentiment-competitor-monitoring.md`
- Create: `docs/product-evolution/plans/p2-082g-sentiment-competitor-monitoring-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- Modify: `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`
- Modify: `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082G index rows after P2-082F**
- [x] **Step 3: Update parent P2-082 status from pending to in progress**
- [x] **Step 4: Verify indexability**

Run:

```bash
rg -n "P2-082G|p2-082g-sentiment-competitor-monitoring" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082g-sentiment-competitor-monitoring.md docs/product-evolution/plans/p2-082g-sentiment-competitor-monitoring-plan.md docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md
```

Expected: every file has a P2-082G or slug match.

### Task 2: Add Monitoring Schema

**Files:**
- Create: `MarketingMonitoringSchemaTest.java`
- Create: `V311__sentiment_competitor_monitoring.sql`

- [x] **Step 1: Write failing schema test**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Data Objects And Mappers

- [x] **Step 1: Add Lombok/MyBatis data objects**
- [x] **Step 2: Add mapper interfaces**
- [x] **Step 3: Compile schema test**

### Task 4: Implement Monitoring Service With TDD

- [x] **Step 1: Write failing service tests**

Cover source upsert, item ingestion sentiment, competitor extraction, alert creation, tenant-scoped reads, bounded limits, and alert resolution.

- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement minimal service**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Controller

- [x] **Step 1: Write failing controller tests**

Cover source upsert tenant/actor propagation, item ingestion tenant/actor propagation, item and alert bounded reads, and alert resolution actor propagation.

- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement controller**
- [x] **Step 4: Verify GREEN**

### Task 6: Verify P2-082G Focused Suite

- [x] **Step 1: Verify focused suite**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitoringControllerTest test
```

Expected: all tests pass.
