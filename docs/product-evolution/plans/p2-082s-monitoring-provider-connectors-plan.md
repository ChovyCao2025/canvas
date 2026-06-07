# Monitoring Provider Connectors Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add provider-specific monitoring poll clients for X recent search, YouTube search, Google Business reviews, and TikTok Research video query.

**Architecture:** Reuse the existing `MarketingMonitorPollClient` extension point. Add a provider connector that delegates HTTP calls through an injectable transport and resolves credentials through explicit environment/system-property refs, then normalizes provider JSON into `MarketingMonitorPollItem` records for the existing polling service.

**Tech Stack:** Java 21, Spring Boot, Jackson, Java `HttpClient`, JUnit 5, Mockito, AssertJ.

**Implementation status (2026-06-06):** Completed. Focused provider connector tests passed 6/6, and monitoring regression passed 50/50.

---

## Scope

This plan implements P2-082S:

- Product docs and indexes.
- Credential-safe provider connector support for four monitoring source types.
- Provider HTTP request/response abstraction for deterministic tests and production HTTP calls.
- Focused unit tests and polling regression.

## Files

- Create `docs/product-evolution/specs/p2-082s-monitoring-provider-connectors.md`.
- Create `docs/product-evolution/plans/p2-082s-monitoring-provider-connectors-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderCredentialResolver.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/EnvironmentMarketingMonitorProviderCredentialResolver.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpRequest.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpResponse.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderHttpTransport.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/JavaMarketingMonitorProviderHttpTransport.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderPollClient.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorProviderPollClientTest.java`.

## Tasks

### Task 1: Index P2-082S Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082S after P2-082R in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with provider connector status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082S|p2-082s-monitoring-provider-connectors"`**

### Task 2: Add Provider Connector Tests With TDD

- [x] **Step 1: Write failing `MarketingMonitorProviderPollClientTest` for source support and missing credentials**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add credential resolver, HTTP request/response/transport records, and provider client shell**
- [x] **Step 4: Verify GREEN**
- [x] **Step 5: Write failing tests for X, YouTube, Google reviews, and TikTok request/mapping/cursor behavior**
- [x] **Step 6: Verify RED**
- [x] **Step 7: Implement provider request builders and response mappers**
- [x] **Step 8: Verify GREEN**

### Task 3: Verify Slice And Update Docs

- [x] **Step 1: Run focused provider connector tests**
- [x] **Step 2: Run monitoring regression tests including polling and provider connector tests**
- [x] **Step 3: Update P2-082S and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorProviderPollClientTest test
```

Expected: provider connector tests pass.

Result: 6 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitorPollingServiceTest,MarketingMonitorProviderPollClientTest test
```

Expected: monitoring ingestion/polling regression plus provider connector tests pass.

Actual command:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookIngestionServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorAnomalyDetectionSchemaTest,MarketingMonitorAnomalyDetectionServiceTest,MarketingMonitorAnomalyControllerTest,MarketingMonitorProviderPollClientTest test
```

Result: 50 tests passed, 0 failures, 0 errors.
