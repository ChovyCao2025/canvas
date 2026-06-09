# P2-020 - Churn Prediction And Smart Timing Foundation Spec

Priority: P2
Sequence: 020
Status: Closed
Source: `docs/optimization/todo/2026-05-31-ai-capability-roadmap.md`, `docs/optimization/todo/competitor-analysis-report.md`, `docs/optimization/todo/market-research-report.md`
Implementation plan: `../plans/p2-020-churn-prediction-and-smart-timing-foundation-plan.md`

## Goal

Add an explainable prediction foundation that computes `churn_probability`, `churn_risk_band`, and `best_send_hour` into CDP profiles for use by existing audience rules and canvas conditions.

## User And Business Value

Operators gain the smallest useful predictive capability without waiting for a full ML platform. Churn rescue and smart timing campaigns can be built with existing IF_CONDITION, SCORING, AUDIENCE, and DELAY nodes.

## Evidence From Optimization

- AI roadmap identifies churn probability as the minimum viable AI capability that changes product positioning.
- Competitor analysis shows Klaviyo-style churn, next purchase, CLV, and channel affinity predictions as a major gap.
- Original gap: CDP profiles, tags, event logs, message send records, and canvas examples existed without a prediction service or profile prediction fields.

## In Scope

- Add deterministic feature extraction from `event_log`, `message_send_record`, and `cdp_user_profile`.
- Add an explainable baseline churn scoring service using configurable weights, not a custom ML training pipeline.
- Add smart timing computation from user event timestamps and send history.
- Write prediction outputs into `cdp_user_profile.properties_json`.
- Store prediction run summaries and per-user prediction snapshots for audit and debugging.
- Add an operator API and small UI for latest run status, distribution, top at-risk users, and manual recompute.
- Add a template canvas or example metadata note showing how `churn_probability` and `best_send_hour` are consumed.

## Out Of Scope

- Predicted CLV, next purchase date, channel affinity ranking, and automated channel choice.
- Model training pipelines, Python services, notebooks, feature stores, or online inference infrastructure.
- Autonomous suppression, autonomous offer selection, or automatic A/B winner promotion.
- Claiming causal uplift without attribution and experiment support.

## Functional Requirements

1. Prediction output fields must be written into CDP profile JSON with stable names: `churn_probability`, `churn_risk_band`, `best_send_hour`, and `prediction_updated_at`.
2. Churn probability must be explainable by stored feature values and contribution fields.
3. Smart timing must return a local hour from `0` to `23`; users without enough history get the tenant default hour.
4. The run must be idempotent by tenant, model version, and run date.
5. The service must cap batch size and expose status so it can be scheduled safely.
6. Audience and canvas condition consumers must be able to read the new profile fields without a new node type.
7. Manual recompute must be gated by `canvas.ai.prediction.enabled`; P2-020 does not register an automatic scheduler.

## Scheduler And Sample Metadata Note

P2-020 intentionally ships the foundation with operator-triggered recompute only. `canvas.ai.prediction.enabled=false` keeps recompute closed by default; a future scheduler should call the same recompute path only after the flag is enabled and should preserve the configured batch cap.

Sample canvas metadata for consuming the prediction fields with existing nodes:

```json
{
  "exampleKey": "churn_rescue_smart_timing",
  "usesProfileFields": ["churn_probability", "churn_risk_band", "best_send_hour"],
  "ifCondition": {
    "source": "profile",
    "field": "churn_probability",
    "operator": ">",
    "value": 0.7
  },
  "delay": {
    "mode": "UNTIL_PROFILE_HOUR",
    "profileField": "best_send_hour",
    "fallbackHour": 20
  }
}
```

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/ChurnPredictionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/SmartTimingService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/PredictionProfileWriter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/CdpAudienceSourceService.java`

### Frontend Touchpoints

- `frontend/src/pages/ai-predictions/index.tsx`
- `frontend/src/services/aiPredictionApi.ts`
- `frontend/src/App.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V165__churn_prediction_smart_timing.sql`
- `backend/canvas-engine/src/main/resources/application.yml`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnFeatureSnapshotServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/ChurnPredictionServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/SmartTimingServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiPredictionControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/audience/CdpAudienceSourceServiceTest.java`
- `frontend/src/pages/ai-predictions/aiPredictions.test.tsx`

## Dependencies

- P1-005 improves event ingestion quality and should be implemented first for production-grade data collection.
- P2-016 improves event schema and retention; this spec can start with existing `event_log` but should adopt the richer schema when available.
- P2-019 is not a hard dependency; this prediction foundation does not require LLM calls.

## Risks And Controls

- False precision: label the first version as an explainable baseline and store model version `baseline_v1`.
- Sparse data: require minimum event counts and use default output with low confidence when history is insufficient.
- Privacy risk: use aggregate behavioral features and do not expose raw event payloads in UI.
- Operational load: process capped batches and expose run status, duration, processed count, and failure count.
- Product misuse: do not auto-suppress or auto-discount users in this release.

## Acceptance Criteria

- Backend tests prove deterministic feature extraction, churn score bands, smart hour selection, sparse-data fallback, profile JSON writes, and idempotent run behavior.
- Audience source tests prove `churn_probability`, `churn_risk_band`, and `best_send_hour` can be used in profile rule matching.
- Frontend tests prove latest run, distribution, top-risk table, empty state, and recompute action states.
- Manual verification can recompute predictions for a small tenant, inspect profile JSON, and build an IF_CONDITION route on `churn_probability > 0.7`.
- CLV, next purchase date, channel affinity, and autonomous optimization remain deferred to follow-up specs.
