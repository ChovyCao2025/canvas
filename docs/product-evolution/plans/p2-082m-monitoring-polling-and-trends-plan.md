# Monitoring Polling And Trends Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add source polling runs, cursor tracking, and trend snapshots to the monitoring data plane.

**Architecture:** Keep existing mention ingestion, sentiment, competitor extraction, alerts, webhooks, and fanout unchanged. Add a polling service that validates source state, delegates provider fetches behind a client interface, ingests new items through `MarketingMonitoringService`, records a poll-run ledger, updates source cursor/status, and builds persisted trend snapshots from existing normalized tables.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor Test.

**Status:** Delivered backend first slice on 2026-06-06.

---

## Scope

This plan implements P2-082M backend first slice:

- Product specs and indexes.
- Additive polling/trend schema.
- Polling source state fields.
- Poll run and trend snapshot data objects, mappers, commands, and views.
- Provider poll client interface plus sandbox metadata-backed client.
- Polling service and controller endpoints.
- Focused backend tests and monitoring regression.

## Files

- Create `docs/product-evolution/specs/p2-082m-monitoring-polling-and-trends.md`.
- Create `docs/product-evolution/plans/p2-082m-monitoring-polling-and-trends-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V319__monitoring_polling_trends.sql`.
- Modify `MarketingMonitorSourceDO`.
- Create poll run and trend snapshot data objects and mappers.
- Create monitoring polling command/view records and client contracts.
- Create `MarketingMonitorPollingService`.
- Modify `MarketingMonitoringController`.
- Create `MarketingMonitorPollingSchemaTest`.
- Create `MarketingMonitorPollingServiceTest`.
- Modify `MarketingMonitoringControllerTest`.

## Tasks

### Task 1: Index P2-082M Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082M after P2-082L in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with polling/trends slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082M|p2-082m-monitoring-polling-and-trends"`**

### Task 2: Add Polling/Trend Schema With TDD

- [x] **Step 1: Write failing `MarketingMonitorPollingSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add migration, source fields, DOs, and mappers**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Polling Service With TDD

- [x] **Step 1: Write failing service tests for config, successful poll, duplicate accounting, failure ledger, trend aggregation, and tenant isolation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement poll client contracts, sandbox client, and `MarketingMonitorPollingService`**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Controller APIs With TDD

- [x] **Step 1: Write failing controller tests for polling config, manual poll, trend build, and trend query**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints to `MarketingMonitoringController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Backend Slice And Update Parent Docs

- [x] **Step 1: Run focused P2-082M backend tests**
- [x] **Step 2: Run P2-082G/I/J/M monitoring regression tests**
- [x] **Step 3: Update P2-082M and parent docs to delivered after verification passes**

## Verification

Executed on 2026-06-06 with Java 21:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitoringControllerTest test`
  - Result: 18 tests, 0 failures, 0 errors, 0 skipped.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitoringControllerTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookIngestionServiceTest,PublicMarketingMonitoringWebhookControllerTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest test`
  - Result: 41 tests, 0 failures, 0 errors, 0 skipped.
