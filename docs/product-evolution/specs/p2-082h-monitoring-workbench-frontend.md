# P2-082H - Monitoring Workbench Frontend Spec

Priority: P2
Sequence: 082H
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082h-monitoring-workbench-frontend-plan.md`

## Goal

Turn the P2-082G monitoring backend into an operator-facing workbench where marketing teams can register monitoring sources, review sentiment and competitor mentions, filter open alerts, and resolve alert workflow rows without using raw API calls.

## Implementation Status

Delivered frontend first slice:

- Typed `marketingMonitoringApi` endpoint wrapper for monitoring source upsert, item ingestion/query, alert query, and alert resolution.
- Pure workbench helpers for bounded filters, JSON parsing, sentiment/status/severity display, date formatting, and KPI calculation.
- Authenticated `/marketing-monitoring` Ant Design workbench with source setup, manual mention ingest, mention review, open-alert triage, and resolve action.
- Operations navigation integration under `运营值班` with route highlighting.
- App shell responsive guardrail for mobile viewport width, verified against the monitoring route.

## Research Inputs

Production social-listening products consistently expose three operator surfaces: monitored-source setup, mention/sentiment review, and alert triage. Brand monitoring and social-listening vendors such as Brandwatch, Meltwater, and Sprinklr document alerting, sentiment, competitor/keyword tracking, and dashboard workflows as core operating loops. This slice implements the same product pattern on top of the connector-neutral backend already delivered in P2-082G.

## Current Baseline

P2-082G delivered tenant-scoped backend APIs under `/canvas/marketing-monitoring`:

- `POST /sources` for source upsert.
- `POST /items` for manual/sandbox item ingestion.
- `GET /items` for filtered mention reads.
- `GET /alerts` for filtered alert reads.
- `POST /alerts/{alertId}/resolve` for alert resolution.

The frontend has no route, navigation item, service wrapper, presentation helpers, or workbench UI for this backend. Operators still need API clients to review negative mentions and resolve alerts.

## Product Design

Add a dense, work-focused workbench under `/marketing-monitoring`, grouped in the existing “运营值班” navigation section:

- Top KPI row: total visible mentions, negative mentions, competitor-linked mentions, open alerts.
- Source card: upsert manual/sandbox/source registry entries with key, type, display name, enabled flag, and metadata JSON.
- Mention ingestion card: manually add a monitored item with source id, external id, brand key, text, competitor term map JSON, and raw payload JSON.
- Mention table: show item id, brand/source, sentiment tag, competitor keys, text, URL, and published/ingested time. Filters include sentiment, competitor key, and bounded limit.
- Alert table: show alert type, severity, status, scope, reason, created time, and a resolve action for open alerts. Filters include status and bounded limit.

The page should be useful without live social credentials. Manual ingestion stays first-class so QA, sales demos, and local development can exercise the complete workflow.

## Functional Requirements

1. Add a typed frontend API wrapper for every P2-082G backend endpoint.
2. Normalize mention and alert query limits to 1..100 before calling the backend.
3. Provide presentation helpers for sentiment labels, alert status, severity color, JSON object parsing, competitor map parsing, KPI calculation, and stable date formatting.
4. Add `/marketing-monitoring` route behind the existing authenticated app layout.
5. Add a navigation item under “运营值班” named `监测工作台`.
6. The workbench must load mentions and open alerts on first render.
7. Operators must be able to upsert a source, ingest a manual monitored item, refresh filtered mention/alert lists, and resolve an open alert.
8. Invalid metadata, competitor map, or raw payload JSON must fail locally before calling the backend.
9. UI copy must stay operational and compact; no marketing landing page or explanatory hero.
10. Frontend tests must cover API endpoint wiring, presentation helpers, initial loading, and resolve action wiring.

## Out Of Scope

- Real public crawler or social API connector configuration.
- Charting beyond summary KPIs.
- Push alert fanout to Feishu, Slack, PagerDuty, email, SMS, or push.
- LLM sentiment inference or generated replies.

## Acceptance Criteria

- This spec and plan are indexed after P2-082G.
- `marketingMonitoringApi` covers source upsert, item ingestion, item query, alert query, and alert resolution.
- `monitoringWorkbench` helpers have focused Vitest coverage.
- `MonitoringWorkbenchPage` renders initial mentions/alerts and can call resolve.
- `App.tsx` exposes `/marketing-monitoring`.
- `AppLayout.tsx` highlights and navigates the monitoring workbench under operations.
- Focused frontend tests pass.
- Desktop and mobile route inspection renders the workbench without page-level horizontal overflow.
