# P2-082R - Monitoring Anomaly Detection Foundation Spec

Priority: P2
Sequence: 082R
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082r-monitoring-anomaly-detection-foundation-plan.md`

## Goal

Add the first production-grade anomaly detection foundation for marketing monitoring: configurable anomaly rules, rolling-baseline detection from existing trend snapshots, auditable anomaly events, and tenant-scoped operator APIs.

## Delivery Status

Delivered backend first slice on 2026-06-06:

- Tenant-scoped anomaly rule registry for metric, source, brand, competitor, direction, baseline window, threshold, and metadata.
- Rolling-baseline detector that reads existing monitoring trend snapshots and compares a target bucket against historical buckets.
- Robust statistical evidence persisted with each anomaly event: actual value, baseline median, baseline MAD, robust z-score, delta, direction, severity, and evidence JSON.
- Optional alert-row creation for detected anomalies so existing monitoring alert workbench can surface the signal.
- Tenant-scoped APIs for rule upsert, manual detection, anomaly event listing, and event resolution.
- Additive Flyway migration is `V325__monitoring_anomaly_detection.sql`.

## Current Baseline

P2-082G created monitoring sources, mentions, deterministic sentiment, competitor matches, and basic alert rows. P2-082M/N added polling and trend snapshots. The platform can see trend buckets but cannot yet detect statistically unusual spikes or drops across mention volume, negative sentiment, competitor mentions, alert volume, or sentiment score.

## Research Inputs

- Datadog anomaly monitors model anomaly detection as identifying when a metric behaves differently from its historical pattern, including trend and seasonality:
  - https://docs.datadoghq.com/monitors/types/anomaly/
- Google Cloud BigQuery anomaly detection describes anomaly detection as identifying deviations from a baseline in a dataset:
  - https://docs.cloud.google.com/bigquery/docs/anomaly-detection-overview
- Sprinklr Listening Smart Alerts are an operator pattern for turning social-listening changes into configurable smart alerts:
  - https://www.sprinklr.com/help/articles/listening-use-case/create-smart-alerts-for-a-listening-use-case/641568e97517d84a3ab073aa

The production pattern for this first slice is connector-neutral and auditable: detect deviations from already persisted monitoring evidence, write why an event was considered anomalous, and leave model-serving/seasonality engines as later replacements behind the same rule/event contract.

## Product Design

Backend adds two additive tables:

- `marketing_monitor_anomaly_rule` stores tenant-scoped rule configuration.
- `marketing_monitor_anomaly_event` stores detected anomaly events and statistical evidence.

Backend adds `MarketingMonitorAnomalyDetectionService`:

- `upsertRule` normalizes metric/direction/grain, validates tenant-owned source when `sourceId` is present, and persists rule metadata.
- `detect` loads the target trend snapshot and historical baseline snapshots for the rule scope.
- The first detector uses median and median absolute deviation (MAD). If MAD is zero, it still supports absolute delta via `min_delta`.
- `events` lists tenant-scoped anomaly events by rule/status with bounded limits.
- `resolveEvent` marks an event resolved with actor and timestamp.

Backend adds `MarketingMonitorAnomalyController`:

- `POST /canvas/marketing-monitoring/anomaly-rules`
- `POST /canvas/marketing-monitoring/anomalies/detect`
- `GET /canvas/marketing-monitoring/anomalies`
- `POST /canvas/marketing-monitoring/anomalies/{eventId}/resolve`

## Functional Requirements

1. Schema must be additive only and must not edit applied migrations.
2. Rule identity must be unique per tenant and rule key.
3. Event identity must be unique per tenant, rule, metric, source, brand, competitor, and target bucket.
4. Rule writes must reject source ids that do not belong to the tenant.
5. Detection must require an enabled tenant-owned rule.
6. Detection must require enough historical baseline buckets before creating an anomaly event.
7. Detection must support metrics: `MENTION_COUNT`, `NEGATIVE_COUNT`, `COMPETITOR_COUNT`, `ALERT_COUNT`, and `AVG_SENTIMENT_SCORE`.
8. Detection must support directions: `SPIKE`, `DROP`, and `BOTH`.
9. Detection must remain tenant-bound and re-filter mapper results in-memory.
10. Detection must persist actual value, baseline median, MAD, robust z-score, delta, direction, severity, and evidence JSON.
11. Detection must avoid duplicate events for the same rule and bucket by upserting through the unique identity.
12. Event query and resolution must reject cross-tenant event ids.
13. Local tests must not require Datadog, Sprinklr, BigQuery ML, social listening providers, or external ML services.

## Out Of Scope

- Real Datadog/Sprinklr/BigQuery ML integration.
- Seasonal decomposition, forecasting, or model training.
- Automatic scheduler execution for anomaly detection.
- Frontend anomaly workbench changes.
- LLM sentiment inference or entity extraction.

## Acceptance Criteria

- P2-082R docs are indexed after P2-082Q.
- Schema test proves anomaly rule/event tables and uniqueness/indexes exist.
- Service tests prove rule upsert, tenant guards, anomaly detection math, no-event threshold handling, event querying, and resolution.
- Controller tests prove tenant/operator propagation for rule writes, detection, event reads, and resolution.
- Focused backend tests pass with Java 21.
