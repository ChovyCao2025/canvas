# Churn Prediction And Smart Timing Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

Status: Closed in implementation; verification evidence is recorded in Task 5.

**Goal:** Compute explainable churn probability and best send hour fields into CDP profiles so existing audience and canvas rules can consume them.

**Architecture:** Build a deterministic baseline prediction service inside the existing Java backend. Feature snapshots read from `event_log`, `message_send_record`, and `cdp_user_profile`; prediction services write auditable snapshots and merge stable fields into `cdp_user_profile.properties_json`; a small UI exposes run status and distribution.

**Tech Stack:** Java 21, Spring Boot WebFlux style controllers, MyBatis, Flyway, Jackson, existing CDP profile/event mappers, React 18, TypeScript, Ant Design, Vitest, JUnit 5, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p2-020-churn-prediction-and-smart-timing-foundation.md`
- Source: `docs/optimization/todo/2026-05-31-ai-capability-roadmap.md`

## File Structure

**Backend**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnPredictionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/SmartTimingService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/PredictionProfileWriter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPredictionRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUserPredictionSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPredictionRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUserPredictionSnapshotMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`

**Frontend**
- Create: `frontend/src/pages/ai-predictions/index.tsx`
- Create: `frontend/src/services/aiPredictionApi.ts`
- Modify: `frontend/src/App.tsx`

**Data And Config**
- Existing: `backend/canvas-engine/src/main/resources/db/migration/V165__churn_prediction_smart_timing.sql`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/SmartTimingServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiPredictionControllerTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`
- Create: `frontend/src/pages/ai-predictions/aiPredictions.test.tsx`

### Task 1: Data Model And Red Tests

**Files:**
- Existing: `backend/canvas-engine/src/main/resources/db/migration/V165__churn_prediction_smart_timing.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/SmartTimingServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`

- [x] **Step 1: Add additive Flyway migration**

Create:

```sql
CREATE TABLE ai_prediction_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    model_key VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    run_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    processed_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL,
    error_message VARCHAR(500) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_prediction_run (tenant_id, model_key, model_version, run_date),
    KEY idx_ai_prediction_run_status (tenant_id, status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_user_prediction_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    model_key VARCHAR(80) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    churn_probability DECIMAL(6,5) NULL,
    churn_risk_band VARCHAR(20) NULL,
    best_send_hour TINYINT NULL,
    confidence DECIMAL(6,5) NOT NULL DEFAULT 0.50000,
    feature_json JSON NOT NULL,
    contribution_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_user_prediction_snapshot (tenant_id, run_id, user_id, model_key),
    KEY idx_ai_prediction_user_latest (tenant_id, user_id, created_at),
    KEY idx_ai_prediction_band (tenant_id, churn_risk_band, churn_probability)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [x] **Step 2: Write feature snapshot tests**

`ChurnFeatureSnapshotServiceTest` covers:

```java
@Test void extractsDaysSinceLastEventAndThirtyDayEventCount()
@Test void extractsThirtyDaySendCountAndFailureRate()
@Test void marksSparseHistoryWhenUserHasFewerThanThreeEvents()
@Test void capsBatchByConfiguredLimit()
```

- [x] **Step 3: Write prediction tests**

`ChurnPredictionServiceTest` covers deterministic score values:

```java
@Test void highIdleUserGetsHighChurnRisk()
@Test void recentlyActiveUserGetsLowChurnRisk()
@Test void sparseHistoryGetsDefaultMediumRiskAndLowConfidence()
@Test void predictionStoresFeatureAndContributionJson()
@Test void runIsIdempotentForTenantModelVersionAndRunDate()
```

Use `baseline_v1` and these bands: `HIGH` for probability `>= 0.70`, `MEDIUM` for `>= 0.40`, otherwise `LOW`.

- [x] **Step 4: Write smart timing tests**

`SmartTimingServiceTest` covers most active hour, timezone fallback, sparse data default hour, and hour range validation:

```java
@Test void selectsMostFrequentEventHour()
@Test void usesTenantDefaultHourWhenHistoryIsSparse()
@Test void neverReturnsHourOutsideZeroToTwentyThree()
```

- [x] **Step 5: Extend audience rule tests**

Add a test to `CdpAudienceSourceServiceTest` proving a CDP profile rule can match `churn_probability > 0.7` and `best_send_hour == 20` from `properties_json`.

- [x] **Step 6: Run red tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChurnFeatureSnapshotServiceTest,ChurnPredictionServiceTest,SmartTimingServiceTest,CdpAudienceSourceServiceTest
```

Historical expectation: FAIL before the implementation existed. Current closeout red coverage also verifies that disabled recompute is blocked before enabling production code changes.

### Task 2: Feature Extraction And Baseline Prediction

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnPredictionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPredictionRunDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUserPredictionSnapshotDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPredictionRunMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUserPredictionSnapshotMapper.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionServiceTest.java`

- [x] **Step 1: Add prediction config**

Add under `canvas.ai.prediction`:

```yaml
canvas:
  ai:
    prediction:
      enabled: false
      batch-size: 500
      default-best-send-hour: 20
      sparse-history-min-events: 3
      model-version: baseline_v1
```

- [x] **Step 2: Implement feature snapshot DTO and extraction**

`ChurnFeatureSnapshotService.FeatureSnapshot` contains `userId`, `daysSinceLastEvent`, `eventCount30d`, `sendCount30d`, `deliveryFailureRate30d`, `goalCount30d`, `profileAgeDays`, and `sparseHistory`. Use mapper queries against `event_log`, `message_send_record`, and `cdp_user_profile`; keep query methods package-private if custom XML is not needed.

- [x] **Step 3: Implement baseline scoring formula**

Use this deterministic formula:

```java
double raw = 0.20
        + Math.min(snapshot.daysSinceLastEvent(), 60) * 0.012
        + snapshot.deliveryFailureRate30d() * 0.20
        - Math.min(snapshot.eventCount30d(), 30) * 0.006
        - Math.min(snapshot.goalCount30d(), 10) * 0.025;
double probability = snapshot.sparseHistory() ? 0.50 : Math.max(0.05, Math.min(0.95, raw));
String band = probability >= 0.70 ? "HIGH" : probability >= 0.40 ? "MEDIUM" : "LOW";
```

Store contributions for idle days, failures, engagement, and goals in `contribution_json`.

- [x] **Step 4: Implement idempotent run creation**

Before computing, select or insert `ai_prediction_run` by `(tenant_id, model_key='churn_prediction', model_version, run_date)`. If status is `SUCCESS`, return the existing summary unless the request includes `force=true`.

- [x] **Step 5: Persist snapshots**

Insert one `ai_user_prediction_snapshot` per processed user with `model_key='churn_prediction'`, `model_version='baseline_v1'`, probability, band, feature JSON, and contribution JSON.

- [x] **Step 6: Run feature and prediction tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ChurnFeatureSnapshotServiceTest,ChurnPredictionServiceTest
```

Expected: PASS for extraction, deterministic score, sparse history, contributions, and run idempotency.

### Task 3: Smart Timing And Profile Writes

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/SmartTimingService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/PredictionProfileWriter.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/SmartTimingServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`

- [x] **Step 1: Implement smart timing**

For each user, count events by local hour over the last 30 days. Return the highest-count hour; ties choose the earliest hour. If fewer than `sparse-history-min-events` events exist, return `default-best-send-hour`.

- [x] **Step 2: Implement profile writer**

Merge these fields into `CdpUserProfileDO.propertiesJson` without deleting existing keys:

```json
{
  "churn_probability": 0.82,
  "churn_risk_band": "HIGH",
  "best_send_hour": 20,
  "prediction_updated_at": "2026-06-03T20:00:00"
}
```

- [x] **Step 3: Expose computed fields to audience rules**

Ensure `CdpAudienceSourceService.profileSourceFields()` includes `churn_probability`, `churn_risk_band`, and `best_send_hour`, and `matchesProfile` reads them from parsed `properties_json`.

- [x] **Step 4: Run smart timing and audience tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SmartTimingServiceTest,CdpAudienceSourceServiceTest
```

Expected: PASS for hour selection, profile field merge, and audience rule matching.

### Task 4: Operator API And UI

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiPredictionControllerTest.java`
- Create: `frontend/src/pages/ai-predictions/index.tsx`
- Create: `frontend/src/pages/ai-predictions/aiPredictions.test.tsx`
- Create: `frontend/src/services/aiPredictionApi.ts`
- Modify: `frontend/src/App.tsx`

- [x] **Step 1: Write controller tests**

`AiPredictionControllerTest` covers latest run, distribution, top-risk users, manual recompute, unauthorized rejection, and tenant isolation.

- [x] **Step 2: Implement API endpoints**

Add:

```text
GET  /ai/predictions/latest-run
GET  /ai/predictions/churn-distribution
GET  /ai/predictions/top-risk-users?limit=100
POST /ai/predictions/recompute
```

`POST /recompute` returns run id, status, processed count, skipped count, and failed count.

- [x] **Step 3: Write frontend tests**

`aiPredictions.test.tsx` covers loading, empty run, distribution chart data, top-risk table, recompute disabled while running, recompute success, and server error.

- [x] **Step 4: Implement UI and service**

`aiPredictionApi.ts` exports typed calls for the endpoints. The page renders latest run status, distribution by risk band, top-risk table with probability and best hour, and a recompute button.

- [x] **Step 5: Run API and UI tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AiPredictionControllerTest
cd frontend && npm test -- aiPredictions.test.tsx
```

Expected: PASS for controller and page behavior.

### Task 5: Verification And Closeout

**Files:**
- Modify: `docs/product-evolution/specs/p2-020-churn-prediction-and-smart-timing-foundation.md`
- Modify: `docs/product-evolution/plans/p2-020-churn-prediction-and-smart-timing-foundation-plan.md`

- [x] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=ChurnFeatureSnapshotServiceTest,ChurnPredictionServiceTest,SmartTimingServiceTest,AiPredictionControllerTest,CdpAudienceSourceServiceTest,PredictionProfileWriterTest,ChurnPredictionSmartTimingSchemaTest
```

Result: PASS, 33 tests, 0 failures, 0 errors.

- [x] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- aiPredictionApi aiPredictions
cd frontend && npm run build
```

Result: PASS, 2 test files and 9 tests; build completed with `tsc && vite build`.

- [x] **Step 3: Run affected regression**

Run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpUserServiceTest,CdpUserDirectoryServiceTest,GoalCheckHandlerTest
```

Result: PASS for the CDP tests Maven found, 7 tests, 0 failures, 0 errors. `GoalCheckHandlerTest` is not present in this worktree. Full frontend `npm test -- --run` was not run; focused page/API tests plus `npm run build` were used for this slice.

- [x] **Step 4: Do not commit without explicit instruction**

No commit was created because the task explicitly says not to commit unless instructed.
