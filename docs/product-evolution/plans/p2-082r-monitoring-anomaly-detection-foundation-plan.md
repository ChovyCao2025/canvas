# Monitoring Anomaly Detection Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend first slice for marketing-monitoring anomaly rules, rolling-baseline detection, anomaly events, and tenant-aware APIs.

**Architecture:** Reuse existing monitoring trend snapshots as the evidence input. Add additive Flyway tables, focused anomaly DOs/mappers, a `MarketingMonitorAnomalyDetectionService`, and a tenant-aware controller under the existing `/canvas/marketing-monitoring` route family.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, Reactor `StepVerifier`.

**Implementation status (2026-06-06):** Completed. The actual Flyway migration is `V325__monitoring_anomaly_detection.sql`. Focused anomaly tests passed 8/8, and monitoring regression passed 44/44.

---

## Scope

This plan implements P2-082R:

- Product docs and indexes.
- Additive schema for anomaly rules and anomaly events.
- Backend DOs/mappers.
- Domain commands, views, queries, and service.
- Tenant-aware controller.
- Focused schema/service/controller tests.

## Files

- Create `docs/product-evolution/specs/p2-082r-monitoring-anomaly-detection-foundation.md`.
- Create `docs/product-evolution/plans/p2-082r-monitoring-anomaly-detection-foundation-plan.md`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.
- Modify `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`.
- Modify `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`.
- Create `backend/canvas-engine/src/main/resources/db/migration/V325__monitoring_anomaly_detection.sql`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAnomalyRuleDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingMonitorAnomalyEventDO.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAnomalyRuleMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingMonitorAnomalyEventMapper.java`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionService.java`.
- Create command/view/query records under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitorAnomalyController.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionSchemaTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/monitoring/MarketingMonitorAnomalyDetectionServiceTest.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingMonitorAnomalyControllerTest.java`.

## Tasks

### Task 1: Index P2-082R Docs

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082R after P2-082Q in implementation/spec/plan indexes**
- [x] **Step 3: Update parent P2-082 docs with anomaly detection slice status**
- [x] **Step 4: Verify indexability with `rg -n "P2-082R|p2-082r-monitoring-anomaly-detection-foundation"`**

### Task 2: Add Monitoring Anomaly Schema With TDD

- [x] **Step 1: Write failing `MarketingMonitorAnomalyDetectionSchemaTest`**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add `V325__monitoring_anomaly_detection.sql`**
- [x] **Step 4: Verify GREEN**

### Task 3: Add Domain Service With TDD

- [x] **Step 1: Write failing service tests for rule upsert and tenant source guard**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Add DOs, mappers, commands, views, and service shell**
- [x] **Step 4: Verify GREEN**
- [x] **Step 5: Write failing detection tests for spike/drop math, threshold no-op, event query, and resolution**
- [x] **Step 6: Verify RED**
- [x] **Step 7: Implement rolling-baseline detection, event upsert, alert evidence, query, and resolution**
- [x] **Step 8: Verify GREEN**

### Task 4: Add Tenant-Aware Controller With TDD

- [x] **Step 1: Write failing controller tests for rule, detection, event query, and resolution endpoints**
- [x] **Step 2: Verify RED**
- [x] **Step 3: Implement `MarketingMonitorAnomalyController`**
- [x] **Step 4: Verify GREEN**

### Task 5: Verify Slice And Update Docs

- [x] **Step 1: Run focused backend tests**
- [x] **Step 2: Run monitoring regression tests across schema/service/controller slices**
- [x] **Step 3: Update P2-082R and parent docs to delivered after verification passes**
- [x] **Step 4: Re-run index/status checks**

## Verification

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitorAnomalyDetectionSchemaTest,MarketingMonitorAnomalyDetectionServiceTest,MarketingMonitorAnomalyControllerTest test
```

Result: 8 tests passed, 0 failures, 0 errors.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookIngestionServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorAnomalyDetectionSchemaTest,MarketingMonitorAnomalyDetectionServiceTest,MarketingMonitorAnomalyControllerTest test
```

Result: 44 tests passed, 0 failures, 0 errors.
