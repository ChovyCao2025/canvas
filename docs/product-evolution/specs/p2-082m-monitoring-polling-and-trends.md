# P2-082M - Monitoring Polling And Trends Spec

Priority: P2
Sequence: 082M
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082m-monitoring-polling-and-trends-plan.md`

## Goal

Make the marketing monitoring data plane production-ready for social/search/review listening by adding source polling configuration, provider polling runs, cursor tracking, ingestion evidence, and trend snapshots on top of the existing mention, sentiment, competitor, alert, webhook, and fanout layers.

## Delivery Status

Delivered backend first slice:

- Additive monitoring polling and trend migration.
- Tenant-scoped polling configuration on monitoring sources.
- Poll run ledger with cursor, window, item counts, duplicate counts, alert counts, status, and error evidence.
- Pluggable provider poll client interface plus sandbox metadata-backed client.
- Trend snapshot builder for mention volume, sentiment mix, competitor count, alert count, and average sentiment.
- Controller APIs for polling configuration, manual source polling, trend snapshot build, and trend query.
- Verified with focused P2-082M backend tests and P2-082G/I/J/M monitoring regression.

## Baseline Before This Slice

P2-082G stores normalized monitored items, deterministic sentiment, competitor mentions, and alert workflow. P2-082H provides an operator workbench. P2-082I adds signed public webhook ingestion. P2-082J adds external alert fanout. Before P2-082M, operators could ingest mentions manually or by webhook, but the product could not poll configured sources, record cursor advancement, explain collection outcomes, or persist trend buckets for dashboards.

## Research Inputs

- Brandwatch Listen exposes mentions, metrics, insights, and alerts; its developer docs model listening around keyword-based queries that retrieve matching mentions from online sources:
  - https://social-media-management-help.brandwatch.com/en/articles/12767960-introduction-to-listen
  - https://developers.brandwatch.com/docs/creating-queries
- Sprinklr social listening emphasizes real-time multi-channel monitoring, trends, sentiment, entity extraction, and proactive anomaly/smart alerts:
  - https://www.sprinklr.com/products/consumer-intelligence/social-listening/
  - https://www.sprinklr.com/help/articles/listening-smart-alerts/smart-alerts-capabilities-within-listening/645e6d1d0104980882a5b698
- Meltwater positions social listening around cross-channel data, AI-powered analysis, dashboards, alerts, and analytics dimensions/measures:
  - https://www.meltwater.com/en/capabilities/social-listening
  - https://developer.meltwater.com/docs/meltwater-api/listening/analytics-options/

These products converge on a production pattern: configured listening sources continuously collect mentions, runs are observable, and dashboards need persisted trend metrics rather than raw mention lists only.

## Product Design

Extend `marketing_monitor_source` with polling state:

- `poll_enabled`: whether scheduled/manual polling is allowed.
- `poll_interval_minutes`: intended cadence.
- `poll_cursor`: provider cursor for incremental collection.
- `last_polled_at`, `next_poll_at`, `last_poll_status`: operational state.

Add `marketing_monitor_poll_run`:

- Stores one row per manual or scheduled collection attempt.
- Captures source id/key/type, status, requested window, cursor before/after, item count, inserted count, duplicate count, alert count, error, metadata, actor, start, and finish times.
- A failed run must not mutate source cursor as successful.

Add `marketing_monitor_trend_snapshot`:

- Stores time-bucketed metrics for a source/brand/competitor scope.
- Metrics include total mentions, sentiment counts, competitor mention count, alert count, average sentiment score, metadata, and bucket boundaries.
- Unique key lets rebuilding the same bucket replace the prior snapshot.

Add a provider-client contract:

- `MarketingMonitorPollClient` declares `supports(sourceType)` and `fetch(request)`.
- The first production slice includes a sandbox metadata-backed client so integration tests and demo environments can run without real external credentials.
- Real Brandwatch/Meltwater/Sprinklr clients can be added behind the same interface later.

## Functional Requirements

1. Add additive migration `V319__monitoring_polling_trends.sql`.
2. Extend `MarketingMonitorSourceDO` with polling state fields.
3. Add poll run and trend snapshot DOs/mappers.
4. Add command/view records for polling config, poll execution, trend build, and trend query.
5. Add `MarketingMonitorPollingService` for polling config, manual poll, trend snapshot build, and trend query.
6. Polling must validate tenant ownership, source enabled state, and `poll_enabled` unless forced.
7. Polling must select a provider client by source type, pass cursor/window/max item context, ingest new items through `MarketingMonitoringService.ingestItem`, count duplicates before ingestion, and update source cursor/status after successful completion.
8. Polling failures must persist a failed run with error message and leave source cursor unchanged.
9. Trend snapshot build must aggregate tenant-scoped items, sentiment rows, competitor rows, and alerts inside the requested bucket.
10. Add controller APIs for source polling config, manual poll, trend snapshot build, and trend query.
11. Focused backend tests must cover schema, config update, successful poll, duplicate accounting, failure ledger, trend aggregation, tenant isolation, and controller tenant/operator wiring.

## Out Of Scope

- Real Brandwatch, Meltwater, Sprinklr, Reddit, Google, TikTok, X, or review-site credentials and API calls.
- Scheduler wiring for automatic periodic polling.
- Frontend trend dashboard charts.
- LLM sentiment/entity extraction beyond existing deterministic monitoring analysis.
- Historical backfill orchestration beyond a single explicit poll window.

## Acceptance Criteria

- This spec and plan are indexed after P2-082L.
- A tenant can configure a source for polling and run a manual poll that records a completed run.
- Duplicate provider items are counted and skipped before ingestion.
- Provider failures create failed run evidence and do not advance source cursor.
- A trend snapshot can be built and queried for a source/window/scope.
- Existing monitoring tests still pass.
- Focused backend tests pass with Java 21.

## Verification

Executed on 2026-06-06 with Java 21:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitoringControllerTest test`
  - Result: 18 tests, 0 failures, 0 errors, 0 skipped.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitoringControllerTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookIngestionServiceTest,PublicMarketingMonitoringWebhookControllerTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest test`
  - Result: 41 tests, 0 failures, 0 errors, 0 skipped.
