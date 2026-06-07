# P2-082N - Monitoring Scheduler And Trend Workbench Spec

Priority: P2
Sequence: 082N
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082n-monitoring-scheduler-and-trend-workbench-plan.md`

## Goal

Move monitoring from manually triggered polling to production-style always-on listening by adding a due-source polling scheduler and exposing trend snapshots in the operator workbench.

## Delivery Status

Delivered backend and frontend slice:

- Backend due-source scheduler service that scans enabled polling sources and executes due polls.
- Spring scheduled wrapper with disabled-by-default configuration, overlap guard, optional distributed lease, tenant scope, limit, and operator identity.
- Frontend API coverage for polling config, manual poll, trend snapshot build, and trend snapshot query.
- Monitoring workbench trend panel that shows persisted trend snapshots and lets operators build a bucketed snapshot.

## Current Baseline

P2-082M delivered source polling configuration, manual poll execution, poll-run evidence, cursor tracking, and persisted trend snapshots. P2-082N adds the always-on due-source scheduler and operator trend surface, so monitoring data is no longer limited to manual polling and raw backend APIs.

## Research Inputs

- Brandwatch Listen positions listening around real-time trend discovery, mention monitoring, sentiment, and smart alerts for unusual data changes:
  - https://www.brandwatch.com/products/listen/
  - https://social-media-management-help.brandwatch.com/hc/en-us/articles/4555758894877-Viewing-and-Analyzing-Listen-Searches
- Meltwater social listening emphasizes always-current executive dashboards, real-time alerts, volume spikes, sentiment shifts, historical trend analysis, and share-of-voice tracking:
  - https://www.meltwater.com/en/capabilities/social-listening
  - https://www.meltwater.com/social-media-monitoring/
- Sprinklr social listening emphasizes real-time crisis alerts, automated workflow management, dashboards, trend detection, and benchmarked social intelligence:
  - https://www.sprinklr.com/products/consumer-intelligence/social-listening/
  - https://dev.sprinklr.com/listening

The production pattern is always-on collection feeding persisted dashboards and alerts. P2-082N applies that pattern without binding the platform to any single provider.

## Product Design

Backend adds `MarketingMonitorPollingScheduleService`:

- Accepts tenant id, evaluation time, limit, and operator.
- Finds tenant-owned sources where `enabled = 1`, `poll_enabled = 1`, and `next_poll_at` is null or due.
- Orders due sources by `next_poll_at` so overdue sources are handled first.
- Calls `MarketingMonitorPollingService.pollSource` for each candidate with `force=false`.
- Returns a cycle summary with candidate, due, succeeded, failed, skipped, and evaluated-at metrics.
- Catches per-source failures so one bad provider does not stop the cycle.

Backend adds `MarketingMonitorPollingScheduler`:

- Uses `@Scheduled(fixedDelayString = "${canvas.monitoring.polling-scheduler.fixed-delay-ms:60000}")`.
- Is disabled by default with `canvas.monitoring.polling-scheduler.enabled=false`.
- Supports tenant id, limit, operator, and lease TTL configuration.
- Uses `CdpWarehouseJobLeaseService` when available to prevent cross-instance duplicate cycles.
- Uses an `AtomicBoolean` overlap guard to prevent nested cycles inside one JVM.

Frontend extends monitoring APIs and workbench:

- Add API methods for polling config, manual poll, trend build, and trend query.
- Add normalized trend types and helper functions for compact sentiment/alert display.
- Add a trend snapshot panel with source/brand/competitor filters, a build form, and a table of bucketed snapshots.
- Keep charts simple for this slice: persisted trend rows are shown as sortable operational evidence rather than a custom charting dependency.

## Functional Requirements

1. Add `MarketingMonitorPollingScheduleService` with due-source scanning and per-source execution summary.
2. Add `MarketingMonitorPollingScheduler` with disabled-by-default scheduling, overlap protection, optional lease, tenant/limit/operator config, and testable `runCycle`.
3. Due-source scanning must not poll disabled sources, polling-disabled sources, future sources, or cross-tenant sources.
4. Scheduler must never require real provider credentials in local tests.
5. Frontend `marketingMonitoringApi` must expose P2-082M polling/trend endpoints.
6. Frontend workbench must query trend snapshots and display mention, sentiment, competitor, alert, average sentiment, bucket, source, brand, and competitor fields.
7. Operators must be able to build a trend snapshot from the workbench for a source/window/scope.
8. Existing monitoring ingestion, alert, webhook, fanout, and polling tests must continue passing.

## Out Of Scope

- Real Brandwatch, Meltwater, Sprinklr, Reddit, TikTok, X, Google, or review-site API clients.
- Automatic trend snapshot generation on every poll.
- Spike/anomaly ML detection.
- Custom chart rendering dependency.
- Multi-tenant global scheduler fan-out beyond one configured tenant id per scheduler instance.

## Acceptance Criteria

- P2-082N docs are indexed after P2-082M.
- Scheduler service tests prove due-source selection, per-source failure isolation, and tenant isolation.
- Scheduler wrapper tests prove disabled mode, enabled execution, lease denial, and overlap guard.
- Frontend API tests cover all polling/trend endpoints.
- Frontend page tests prove trend snapshots load and a snapshot build action calls the correct API.
- Focused backend and frontend tests pass.

## Verification

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitoringControllerTest test` - 26 tests passed.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=MarketingMonitoringSchemaTest,MarketingMonitoringServiceTest,MarketingMonitoringControllerTest,MarketingMonitorWebhookIngestionSchemaTest,MarketingMonitorWebhookSignatureServiceTest,MarketingMonitorWebhookPayloadMapperTest,MarketingMonitorWebhookIngestionServiceTest,PublicMarketingMonitoringWebhookControllerTest,MarketingMonitorAlertFanoutSchemaTest,MarketingMonitorAlertFanoutServiceTest,MarketingMonitorPollingSchemaTest,MarketingMonitorPollingServiceTest,MarketingMonitorPollingScheduleServiceTest,MarketingMonitorPollingSchedulerTest test` - 49 tests passed.
- `npm run test -- src/services/marketingMonitoringApi.test.ts src/pages/marketing-monitoring/monitoringWorkbench.test.ts src/pages/marketing-monitoring/index.test.tsx` - 7 tests passed.
- `npm run build` - TypeScript and Vite production build passed.
