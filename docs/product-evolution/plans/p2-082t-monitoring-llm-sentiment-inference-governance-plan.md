# Monitoring LLM Sentiment Inference Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a tenant-scoped monitoring inference ledger and APIs for governed LLM-style sentiment, entity, topic, and risk analysis over existing monitor items.

**Architecture:** Keep deterministic ingest-time sentiment unchanged. Add an independent inference service that reads monitor items, runs an injectable generator with LLM-gateway and deterministic fallback behavior, persists a governance ledger row, and exposes bounded tenant-scoped APIs through the existing monitoring controller.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor test.

**Implementation status (2026-06-06):** Completed. Focused P2-082T tests passed 20/20; monitoring regression passed 70/70.

Status: Completed on 2026-06-06.

---

## Scope

This plan implements P2-082T backend first slice:

- Product docs and indexes.
- Additive Flyway migration for `marketing_monitor_inference`.
- Data object and mapper.
- Command/query/view records and generator result contracts.
- Inference service with tenant guard, deterministic fallback, LLM output normalization, hashes, and bounded queries.
- Monitoring controller endpoints and focused tests.

## Files

- Create `docs/product-evolution/specs/p2-082t-monitoring-llm-sentiment-inference-governance.md`.
- Create `docs/product-evolution/plans/p2-082t-monitoring-llm-sentiment-inference-governance-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V328__monitoring_llm_sentiment_inference.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorInferenceDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorInferenceMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceQuery.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerationContext.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerationResult.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceGenerator.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/LlmMarketingMonitorInferenceGenerator.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorInferenceServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitoringControllerTest.java`.

## Tasks

### Task 1: Index P2-082T Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082T after P2-082S in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with inference-governance status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082T|p2-082t-monitoring-llm-sentiment-inference-governance"`**

### Task 2: Add Schema Red Test And Migration

- [x] **Step 1: Write failing `MarketingMonitorInferenceSchemaTest` for `V328__monitoring_llm_sentiment_inference.sql`**
- [x] **Step 2: Verify RED with Java 21 Maven**
- [x] **Step 3: Add the migration with tenant/item/model/status indexes, hash fields, JSON output fields, and fallback/provider status columns**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Service Red Tests And Implementation

- [x] **Step 1: Write failing service tests for tenant guard, fallback ledger creation, model-output parsing, bounded queries, and no lexicon sentiment mutation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DO/mapper, command/query/view records, generator contracts, default generator, prompt template, and inference service**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Controller Red Tests And Endpoints

- [x] **Step 1: Write failing controller tests for `POST /items/{itemId}/inferences` and `GET /inferences` service delegation**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Inject `MarketingMonitorInferenceService` into `MarketingMonitoringController` and add endpoints**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Status

- [x] **Step 1: Run focused P2-082T tests**
- [x] **Step 2: Run monitoring regression including P2-082G through P2-082T tests**
- [x] **Step 3: Update P2-082T plan and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest,MarketingMonitoringControllerTest test
```

Expected: P2-082T focused schema/service/controller tests pass.

Result: 20 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookIngestionServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorAnomalyDetectionSchemaTest,MarketingMonitorAnomalyDetectionServiceTest,MarketingMonitorAnomalyControllerTest,MarketingMonitorProviderPollClientTest,MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest,MarketingMonitoringControllerTest test
```

Expected: monitoring regression plus inference-governance tests pass.

Actual command:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookIngestionServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorAnomalyDetectionSchemaTest,MarketingMonitorAnomalyDetectionServiceTest,MarketingMonitorAnomalyControllerTest,MarketingMonitorProviderPollClientTest,MarketingMonitorInferenceSchemaTest,MarketingMonitorInferenceServiceTest,MarketingMonitoringControllerTest test
```

Result: 70 tests passed, 0 failures, 0 errors.
