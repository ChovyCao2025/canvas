# P2-016C - Bounded Analytics Query APIs Spec

Priority: P2
Sequence: 016C
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/todo/cdp_gap_analysis.md`
Implementation plan: `../plans/p2-016c-bounded-analytics-query-apis-plan.md`

## Goal

Add tenant-safe, date-bounded analytics query APIs for event analysis, funnels, user timelines, attribute distribution, alerts, and exports.

## Current Baseline

- Existing stats endpoints are execution-counter oriented.
- P2-016 provides richer event/trace fields.
- There is no query service that enforces tenant/date bounds across analytics reports.

## In Scope

- Migration `V129__analytics_query_definitions.sql` for funnel definitions, alert rules, and export jobs.
- `AnalyticsQueryService` and `AnalyticsController`.
- Date range, tenant predicate, max range, and row-limit enforcement.
- Async or row-limited export creation.

## Out Of Scope

- Frontend analytics views; split into P2-016D.
- Heatmaps, session replay, app click analytics, and predictive LTV.

## Acceptance Criteria

- Backend tests prove bounded query rejection, event grouping, funnel version lookup, timeline pagination, attribute distribution, alert preview, and export creation.
