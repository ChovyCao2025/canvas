# Monitoring Alert Fanout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add external fanout for monitoring alerts with tenant-scoped channels, encrypted secrets, delivery logs, provider payloads, and manual resend.

**Architecture:** Add monitoring-owned alert channel and delivery tables. `MarketingMonitorAlertFanoutService` owns channel configuration, payload creation, HTTP send, retry classification, and delivery views. `MarketingMonitoringService` calls fanout after alert persistence and never lets fanout failures fail mention ingestion.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, SecretCipher, BCrypt, WebClient, JUnit 5, Mockito, AssertJ, Reactor Test.

---

## Scope

This plan implements P2-082J:

- Product specs and indexes.
- Additive schema for monitoring alert channels and delivery logs.
- Channel command/view and delivery views.
- Alert fanout service and provider payload formatting for generic webhook, Slack, Feishu, and Teams.
- Automatic dispatch after alert creation and manual dispatch API.
- Focused backend tests.

## Files

- Create `docs/product-evolution/specs/p2-082j-monitoring-alert-fanout.md`.
- Create `docs/product-evolution/plans/p2-082j-monitoring-alert-fanout-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V315__monitoring_alert_fanout.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAlertChannelDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAlertDeliveryDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAlertChannelMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAlertDeliveryMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertChannelCommand.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertChannelView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertDeliveryView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertDispatchView.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitoringService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAlertFanoutServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitoringServiceTest.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitoringControllerTest.java`.

## Tasks

### Task 1: Index P2-082J Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082J after P2-082I in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with alert fanout slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082J|p2-082j-monitoring-alert-fanout"`**

### Task 2: Add Alert Fanout Schema With TDD

- [x] **Step 1: Write failing `MarketingMonitorAlertFanoutSchemaTest`**
- [x] **Step 2: Verify RED with `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorAlertFanoutSchemaTest test`**
- [x] **Step 3: Add migration, DOs, and mappers**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Fanout Service With TDD

- [x] **Step 1: Write failing service tests for channel upsert, generic HMAC, Feishu payload, filtering, and failure logs**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `MarketingMonitorAlertFanoutService`**
- [x] **Step 4: Verify GREEN**

### Task 4: Auto Fanout From Monitoring Ingestion With TDD

- [x] **Step 1: Write failing monitoring service test that negative alert creation dispatches matching alerts**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Inject optional fanout service and call it after alert insert**
- [x] **Step 4: Verify GREEN**

### Task 5: Add Controller APIs With TDD

- [x] **Step 1: Write failing controller tests for channel upsert, manual dispatch, and delivery query**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add endpoints to `MarketingMonitoringController`**
- [x] **Step 4: Verify GREEN**

### Task 6: Verify Backend Slice And Update Parent Docs

- [x] **Step 1: Run focused P2-082J backend tests**
- [x] **Step 2: Run P2-082G/P2-082I/P2-082J monitoring regression tests**
- [x] **Step 3: Update P2-082J and parent docs to delivered after verification passes**
