# P2-036 - CDP Warehouse Readiness And SLO Summary Spec

Priority: P2
Sequence: 036
Source: `docs/product-evolution/specs/p2-024-cdp-warehouse-operations-api-and-scheduler.md`, `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`, `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`, `docs/product-evolution/specs/p2-035-cdp-warehouse-audience-materialization-operations.md`
Implementation plan: `../plans/p2-036-cdp-warehouse-readiness-and-slo-summary-plan.md`

## Goal

Add a tenant-scoped warehouse readiness summary that combines existing offline, realtime, incident, BI datasource, and audience materialization facts into one production operations view.

## Current Baseline

- Warehouse status APIs expose recent sync runs and watermarks.
- Quality and realtime pipeline slices already create incidents and runtime summaries.
- BI datasource health exists behind `BiDatasourceHealthProvider`.
- OLAP audience materialization has run history.
- Operators must currently call multiple APIs to decide whether the CDP warehouse is safe to use.

## In Scope

- A read-only readiness service that aggregates existing service outputs.
- A `/warehouse/readiness` API for current tenant.
- Summary sections for offline sync, realtime pipelines, incidents, BI datasources, and audience materialization runs.
- PASS/WARN/FAIL status derivation.

## Out Of Scope

- New schema or metric storage.
- Prometheus exporter implementation.
- Recomputing quality checks or realtime pipeline checkpoints.
- Mutating incidents or materialization runs.

## Runtime Semantics

1. Readiness is read-only and best-effort across already implemented warehouse control planes.
2. Any failed realtime pipeline, unavailable BI datasource, failed latest materialization run, failed sync run, or critical/open incident makes the summary `FAIL`.
3. Open warning incidents, warning realtime pipelines, stale/missing offline runs, or no recent materialization runs make the summary `WARN`.
4. If every section is healthy, the summary is `PASS`.
5. Section errors are surfaced as section `FAIL` reasons instead of hiding exceptions.

## Functional Requirements

1. Operators can request tenant-scoped readiness through one API.
2. Readiness includes offline sync run and watermark counts.
3. Readiness includes realtime pipeline pass/warn/fail counts.
4. Readiness includes open incident severity counts.
5. Readiness includes BI datasource availability counts.
6. Readiness includes recent OLAP audience materialization run counts.
7. Tests prove status derivation and controller tenant scoping.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessController.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseReadinessControllerTest.java`

## Acceptance Criteria

- Service tests prove PASS, WARN, and FAIL derivation.
- Service tests prove dependency exceptions are surfaced as failing sections.
- Controller tests prove current tenant is passed to the service.
- No migration is added.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the read-only API.
2. Add it to the operator dashboard as the first warehouse health card source.
3. Use the summary before enabling larger realtime pipelines or audience materialization schedules.

## Rollback

- Hide or stop calling `/warehouse/readiness`.
- Existing warehouse operations APIs continue to work independently.
