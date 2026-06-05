# P2-030 - CDP Warehouse Quality Incident Loop Spec

Priority: P2
Sequence: 030
Source: `docs/product-evolution/specs/p2-026-cdp-warehouse-quality-and-reconciliation.md`, `docs/product-evolution/specs/p2-029-cdp-warehouse-realtime-checkpoint-and-lag.md`
Implementation plan: `../plans/p2-030-cdp-warehouse-quality-incident-loop-plan.md`

## Goal

Add a warehouse quality incident loop so failed or warning warehouse checks become tenant-scoped operational incidents that can be listed, acknowledged, and resolved.

## Current Baseline

- P2-026 records durable warehouse quality check rows.
- Quality checks can return `WARN`, but there is no incident state for operators to triage.
- P2-029 exposes realtime status and retry backlog, but it does not provide a quality alert lifecycle.

## In Scope

- A MySQL warehouse incident table.
- A service that opens or updates incidents from quality check warnings.
- Operator APIs to list, acknowledge, and resolve incidents.
- Integration with ODS count reconciliation and aggregate lag checks.
- Tests for schema, service behavior, quality integration, and controller delegation.

## Out Of Scope

- External alert routing to Slack, PagerDuty, or Feishu.
- Incident assignment workflows.
- SLA escalation policies.
- Replacing the quality check ledger.

## Runtime Semantics

1. Incident rows are tenant scoped.
2. Incidents are deduplicated by `(tenant_id, incident_key)`.
3. Repeated warnings for the same check reopen an incident if resolved, update `last_seen_at`, and increment `occurrence_count`.
4. `PASS` and `SKIPPED` checks do not open incidents in this slice.
5. Operators can acknowledge and resolve incidents through APIs.

## Functional Requirements

1. Incident table must preserve check source, source id, severity, status, title, description, first/last seen times, occurrence count, acknowledgement, and resolution metadata.
2. ODS count warnings must open/update a `QUALITY:ODS_COUNT` incident.
3. Aggregate lag warnings must open/update a `QUALITY:AGGREGATE_LAG` incident.
4. Incident listing must be tenant scoped and bounded.
5. Acknowledge and resolve operations must be tenant scoped.
6. Incident creation must never break quality check recording.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V197__cdp_warehouse_quality_incident.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseIncidentDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseIncidentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseIncidentController.java`
- Integration in `CdpWarehouseQualityService`.

## Acceptance Criteria

- Schema test proves incident table, unique incident key, and status indexes exist.
- Service tests prove warnings open/update incidents while pass/skipped checks are ignored.
- Service tests prove list, acknowledge, and resolve are tenant scoped.
- Quality service tests prove warning checks invoke incident recording and incident failures do not fail quality checks.
- Controller tests prove tenant-scoped incident APIs.
- Focused warehouse backend tests pass.

## Rollout

1. Deploy the additive incident migration.
2. Enable quality scheduler in staging.
3. Verify warning checks appear under `/warehouse/incidents`.
4. Train operators to acknowledge and resolve after remediation.

## Rollback

- Stop calling incident APIs.
- Leave the incident table in place; quality checks can continue writing their existing ledger.
