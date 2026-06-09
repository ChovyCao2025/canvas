# P2-039 - CDP Warehouse SLO Policy Gates Spec

Priority: P2
Sequence: 039
Source: `docs/product-evolution/specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`, `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`, `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`
Implementation plan: `../plans/p2-039-cdp-warehouse-slo-policy-gates-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Make warehouse readiness freshness thresholds explicit and tenant-governed instead of relying only on fixed readiness heuristics.

## Current Baseline

- Dataset catalog already stores `freshness_sla_minutes` as asset metadata.
- Realtime pipeline contracts already own per-pipeline `maxLagMs` and `maxCheckpointAgeSeconds`.
- Readiness still uses fixed behavior for missing offline runs, missing watermarks, and audience materialization recency.
- Operators cannot view or change readiness freshness thresholds per tenant.

## In Scope

- Add a tenant-scoped warehouse SLO policy table.
- Seed a global default readiness policy.
- Add service and API to list, upsert, and resolve the effective policy.
- Integrate the effective policy into readiness checks for:
  - offline sync run gap;
  - offline watermark lag;
  - audience materialization run gap.
- Keep realtime lag thresholds in existing realtime pipeline contracts.
- Add service, controller, schema, and readiness tests.

## Out Of Scope

- Replacing dataset catalog `freshness_sla_minutes`.
- Replacing realtime pipeline `maxLagMs` or checkpoint age checks.
- Alert routing.
- Multi-tenant fan-out.
- UI.

## Runtime Semantics

1. Global tenant `0` provides `WAREHOUSE_READINESS_DEFAULT`.
2. A tenant-specific policy with the same key overrides the global policy.
3. Missing policy rows fall back to in-code defaults so readiness remains available during migration or partial rollout.
4. Offline sync fails when recent runs contain failures or latest run/watermark age exceeds fail thresholds.
5. Offline sync warns when evidence is missing or latest run/watermark age exceeds warn thresholds.
6. Audience materialization fails on failed runs or run age above fail threshold.
7. Audience materialization warns when there are no runs or run age above warn threshold.

## Functional Requirements

1. Operators can list SLO policies for the current tenant scope.
2. Operators can upsert a tenant-scoped policy.
3. Operators can fetch the effective readiness policy.
4. Readiness uses policy thresholds in offline and audience materialization sections.
5. Tests prove tenant override, validation, API tenant scoping, schema, and readiness policy behavior.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V201__cdp_warehouse_slo_policy.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseSloPolicyDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseSloPolicyMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseSloPolicyService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseSloPolicyController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessService.java`
- Tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web`.

## Acceptance Criteria

- Migration creates and seeds the policy table.
- Policy service merges global default and tenant override.
- Invalid warn/fail threshold ordering is rejected.
- Readiness fails/warns based on policy thresholds.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy schema and default policy.
2. Keep default thresholds conservative.
3. Allow tenant overrides for stricter production customers.
4. Observe readiness incident volume after P2-037/P2-038 automation.

## Rollback

- Stop calling the SLO policy API and remove tenant overrides.
- Readiness falls back to in-code defaults if the policy service cannot return a row.
