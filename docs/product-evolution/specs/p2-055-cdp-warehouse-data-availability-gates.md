# P2-055 - CDP Warehouse Data Availability Gates Spec

Priority: P2
Sequence: 055
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`, `docs/product-evolution/specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`, `docs/product-evolution/specs/p2-039-cdp-warehouse-slo-policy-gates.md`
Implementation plan: `../plans/p2-055-cdp-warehouse-data-availability-gates-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add tenant-scoped data availability gates so BI queries, audience segmentation, and audience materialization can ask whether a requested time window is covered by offline aggregate watermarks and realtime pipeline watermarks before consuming warehouse data.

## Current Baseline

- P2-022 writes offline backfill and aggregate watermarks.
- P2-032 exposes realtime pipeline runtime status and checkpoint watermarks.
- P2-036 summarizes warehouse readiness for operators.
- P2-039 stores SLO thresholds for readiness freshness.
- The platform still lacks a concrete window-level availability answer for downstream consumers.

## In Scope

- Add an availability service that evaluates a requested `[from, to]` window.
- Support `OFFLINE`, `REALTIME`, and `HYBRID` modes.
- Reuse `CDP_EVENT_AGGREGATE/WINDOW_END` as the offline aggregate availability watermark.
- Reuse realtime pipeline runtime `lastWatermarkTime` as realtime availability evidence.
- Return per-gate status, reason, available-until timestamp, lag minutes, and evidence counts.
- Add a tenant-scoped API endpoint and focused tests.

## Out Of Scope

- New Flyway tables.
- Blocking BI or audience APIs automatically.
- Table-level or metric-level availability calendars.
- UI.
- Cross-tenant fanout.

## Runtime Semantics

1. The API is tenant scoped.
2. Missing `from` defaults to one hour before `to`; missing `to` defaults to now.
3. `from` must not be after `to`.
4. `OFFLINE` evaluates only the aggregate watermark.
5. `REALTIME` evaluates active realtime pipelines and their minimum watermark.
6. `HYBRID` evaluates both offline and realtime gates.
7. A gate is `PASS` when the requested `to` is covered by its available-until timestamp and runtime health is good.
8. A gate is `WARN` when the requested window extends past availability but remains within tolerance, or realtime runtime has warnings.
9. A gate is `FAIL` when evidence is missing, runtime has failures, or the availability gap breaches fail thresholds.
10. Overall status is the worst gate status.

## Functional Requirements

1. Operators can query warehouse availability for a time window and mode.
2. Downstream consumers receive a deterministic `PASS`, `WARN`, or `FAIL`.
3. Offline availability uses existing watermarks rather than recomputing aggregation.
4. Realtime availability uses existing runtime checkpoints rather than reading Doris.
5. Existing readiness, SLO, and warehouse status APIs continue to work.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseAvailabilityService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityController.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Acceptance Criteria

- P2-055 spec and plan are indexed.
- No Flyway migration is added.
- Offline PASS/WARN/FAIL availability is derived from aggregate watermark evidence.
- Realtime PASS/WARN/FAIL availability is derived from pipeline runtime watermarks and health.
- HYBRID returns worst-gate overall status.
- Controller tests prove tenant scoping and request delegation.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy availability API.
2. Use the API in staging before BI dashboards and audience materialization runs.
3. Compare availability verdicts with readiness and actual query results.
4. Later slices can wire hard gates into BI and audience workflows once evidence is trusted.

## Rollback

- Stop calling the availability endpoint.
- Existing warehouse readiness, status, and realtime APIs continue independently.
