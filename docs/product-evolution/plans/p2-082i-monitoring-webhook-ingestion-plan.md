# Monitoring Webhook Ingestion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add signed public webhook ingestion for marketing monitoring sources.

**Architecture:** Extend the source registry with encrypted webhook credentials, add a small source-scoped ingestion service that verifies HMAC signatures before mapping generic payloads, and reuse `MarketingMonitoringService.ingestItem` for persistence, sentiment, competitor extraction, alerts, and idempotency. Keep provider-specific mappers out of this first slice.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, SecretCipher, BCrypt, JUnit 5, Mockito, AssertJ, Reactor Test.

---

## Scope

This plan implements P2-082I:

- Product specs and indexes.
- Additive monitoring source webhook schema.
- Webhook credential fields on `MarketingMonitorSourceDO`.
- Secret rotation command/view and service behavior.
- Raw-body HMAC verification with timestamp replay guard.
- Generic social-listening payload mapper.
- Public webhook controller and SecurityConfig route permit.
- Focused backend tests.

## Files

- Create `docs/product-evolution/specs/p2-082i-monitoring-webhook-ingestion.md`.
- Create `docs/product-evolution/plans/p2-082i-monitoring-webhook-ingestion-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V314__monitoring_webhook_ingestion.sql`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorSourceDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSecretView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSignatureService.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookPayloadMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookController.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookSignatureServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookPayloadMapperTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorWebhookIngestionServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitoringControllerTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookControllerTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`.

## Tasks

### Task 1: Index P2-082I Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082I after P2-082H in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with in-progress webhook ingestion slice**
- [x] **Step 4: Verify indexability with `rg -n "P2-082I|p2-082i-monitoring-webhook-ingestion"`**

### Task 2: Add Webhook Schema With TDD

- [x] **Step 1: Write failing `MarketingMonitorWebhookIngestionSchemaTest`**
- [x] **Step 2: Verify RED with `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorWebhookIngestionSchemaTest test`**
- [x] **Step 3: Add migration and source DO fields**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Signature And Payload Helpers With TDD

- [x] **Step 1: Write failing signature and payload mapper tests**
- [x] **Step 2: Verify RED for both tests**
- [x] **Step 3: Implement HMAC verifier and generic payload mapper**
- [x] **Step 4: Verify GREEN**

### Task 4: Add Ingestion Service With TDD

- [x] **Step 1: Write failing service tests for rotation, valid webhook ingestion, stale timestamp, and invalid signature**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `MarketingMonitorWebhookIngestionService`**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Controllers And Security Route With TDD

- [x] **Step 1: Write failing controller and SecurityConfig tests**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add authenticated rotation endpoint, public webhook controller, and permit rule**
- [x] **Step 4: Verify GREEN**

### Task 6: Verify Backend Slice

- [x] **Step 1: Run focused P2-082I backend tests**
- [x] **Step 2: Run P2-082G/P2-082I related regression tests**
- [x] **Step 3: Update parent docs from in-progress to delivered if verification passes**
