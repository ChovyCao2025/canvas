# P2-082F - AI Decision Models Spec

Priority: P2
Sequence: 082F
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082f-ai-decision-models-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add a governed AI decisioning layer for daily marketing operations: LTV scoring, next-best-action, next-best-offer, channel affinity, and budget-aware ranking. The first slice must be deterministic, explainable, tenant-scoped, and auditable so operators can trust and review recommendations before later model integrations.

## Current Baseline

The platform already has a churn prediction and smart-timing foundation:

- `ai_prediction_run` and `ai_user_prediction_snapshot` persist churn risk, confidence, feature JSON, contribution JSON, and best send hour.
- CDP profiles store operational properties such as churn probability, churn band, and best send hour.
- Event logs, send records, consent records, audiences, journeys, paid-media sync, and BI evidence already provide the core marketing data plane.

What is still missing is the decision layer that turns these signals into ranked recommendations with model governance, action eligibility, budget cost, feature snapshots, explanations, and feedback.

## Product Design

The first slice uses deterministic baseline models instead of external black-box calls. This keeps local tests stable, avoids credential and policy dependency, and establishes the audit contract future ML services must obey.

Data model:

- `ai_decision_run`: one run ledger per tenant, model key/version, decision scope, run date, status, counters, actor, and metadata.
- `ai_user_decision_recommendation`: one row per user and decision type, with rank, score, confidence, model version, feature JSON, explanation JSON, fallback reason, budget cost, and eligibility status.
- `ai_decision_feedback`: operator or downstream outcome feedback tied to a recommendation.

Decision types:

- `LTV`: estimated customer value tier and score.
- `NEXT_BEST_ACTION`: recommended operational action such as retention, winback, cross-sell, loyalty nurture, or paid-media suppression.
- `NEXT_BEST_OFFER`: recommended offer family such as discount, points bonus, bundle, service benefit, or no-offer.
- `CHANNEL_AFFINITY`: preferred reachable channel based on consent, profile contactability, smart timing, and engagement signals.

## API Contract

### Recompute Decisions

`POST /ai/decisions/recompute`

```json
{
  "runDate": "2026-06-06",
  "decisionScope": "DAILY_MARKETING",
  "userIds": ["u-1", "u-2"],
  "force": true,
  "budgetCap": 1000,
  "metadata": { "source": "manual" }
}
```

The service creates one run and recommendation rows for each requested or candidate user. The command deduplicates user ids, bounds work to the configured batch limit, and reuses an existing successful run unless `force=true`.

### Latest Run

`GET /ai/decisions/latest-run?decisionScope=DAILY_MARKETING`

### Recommendations

`GET /ai/decisions/recommendations?decisionType=NEXT_BEST_ACTION&eligibilityStatus=ELIGIBLE&limit=50`

The response is tenant-scoped and sorted by rank and score.

### Feedback

`POST /ai/decisions/recommendations/{recommendationId}/feedback`

```json
{
  "feedbackType": "ACCEPTED",
  "outcomeValue": 99.90,
  "metadata": { "canvasId": 123 }
}
```

Feedback is tenant-scoped and preserves actor, timestamp, outcome value, and metadata.

## Functional Requirements

1. All writes and reads must be tenant-scoped.
2. Recompute must persist a run with status, counters, actor, timestamps, and metadata.
3. Recompute must create recommendations for LTV, next-best-action, next-best-offer, and channel affinity for every processed user.
4. Every recommendation must include `model_key`, `model_version`, `decision_type`, `score`, `confidence`, `rank`, `feature_json`, and `explanation_json`.
5. Sparse or missing history must produce deterministic fallback recommendations with low confidence and explicit `fallback_reason`.
6. LTV scoring must use recent engagement, goal/purchase signals, profile age, and churn risk when available.
7. Next-best-action ranking must account for churn risk, LTV, engagement, sparse history, and budget cost.
8. Next-best-offer must avoid unnecessary discounting for low-risk users and mark budget-constrained offers as ineligible when budget cost exceeds the run cap.
9. Channel affinity must prefer reachable channels with marketing consent and profile contactability, falling back to in-app or manual follow-up when consent is missing.
10. Query limits must be bounded to 1..100 rows.
11. Feedback must reject cross-tenant recommendation ids.
12. The service must not call external AI, advertising, or messaging providers in this slice.

## Scoring Baseline

The baseline model uses transparent heuristic contributions:

- Churn risk raises retention, winback, and service-benefit scores.
- Goal/purchase event counts and profile age raise LTV.
- Delivery failures lower channel confidence.
- Explicit consent and profile identifiers raise channel eligibility.
- Sparse history caps confidence and emits fallback reasons.
- Budget cost is persisted and used to mark expensive offers ineligible when a budget cap is present.

## Out Of Scope

- Live model-serving integration or feature store dependency.
- Autonomous campaign execution from recommendations.
- Reinforcement learning or bid optimization.
- Frontend AI decision workbench.
- Sentiment, competitor monitoring, or social-web ingestion.

## Acceptance Criteria

- This spec and plan are indexed after P2-082E.
- Migration `V309__ai_decision_models.sql` creates run, recommendation, and feedback tables with tenant indexes.
- Schema test proves table and index names exist.
- Service tests prove recompute, explanations/fallbacks, budget eligibility, tenant-scoped reads, and feedback writes.
- Controller tests prove admin access, tenant propagation, bounded limits, and feedback actor propagation.
- Focused backend tests pass with Java 21.
