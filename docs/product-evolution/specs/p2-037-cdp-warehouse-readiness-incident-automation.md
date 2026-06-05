# P2-037 - CDP Warehouse Readiness Incident Automation Spec

Priority: P2
Sequence: 037
Source: `docs/product-evolution/specs/p2-036-cdp-warehouse-readiness-and-slo-summary.md`, `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`, `docs/product-evolution/specs/p2-033-cdp-warehouse-realtime-pipeline-incidents.md`
Implementation plan: `../plans/p2-037-cdp-warehouse-readiness-incident-automation-plan.md`

## Goal

Turn the tenant-scoped warehouse readiness summary into an actionable incident loop so operators do not need to manually inspect every readiness section after a WARN or FAIL result.

## Current Baseline

- P2-036 exposes `/warehouse/readiness` as a read-only summary across offline sync, realtime pipelines, warehouse incidents, BI datasources, and audience materialization.
- P2-030 and P2-033 already persist warehouse incidents in `cdp_warehouse_incident` with stable incident keys and open-incident upsert behavior.
- Readiness WARN/FAIL states are visible but do not automatically open or refresh incidents.

## In Scope

- Add a readiness incident scanner that reads `CdpWarehouseReadinessService`.
- Reuse `CdpWarehouseIncidentService` and the existing `cdp_warehouse_incident` table.
- Open or refresh one stable incident per non-PASS readiness section.
- Skip the readiness `incidents` section to avoid creating recursive incidents about existing incidents.
- Add a tenant-scoped API to run the scan.
- Add service, controller, and incident mapping tests.

## Out Of Scope

- New incident schema.
- Alert routing to Slack, PagerDuty, email, or notification center.
- Automatic scheduled scan.
- Incident auto-resolution.
- Recomputing readiness source checks.

## Runtime Semantics

1. The scanner calls `readiness(tenantId)` and uses the returned section list as the source of truth.
2. Sections with `PASS` are skipped.
3. The `incidents` section is skipped because it already reflects open incidents.
4. Non-PASS sections create or refresh `READINESS:{SECTION_KEY}` incidents.
5. Section `FAIL` creates `CRITICAL` severity; section `WARN` creates `WARN` severity.
6. Incident descriptions include readiness status, section key, section status, reason, and generated time.
7. Scanner write failures are counted and do not stop the rest of the scan.

## Functional Requirements

1. Operators can run a tenant-scoped readiness incident scan.
2. The scan returns total sections, opened/refreshed count, skipped count, and failed write count.
3. Readiness incidents use stable keys and reuse the existing upsert behavior.
4. PASS sections do not open incidents.
5. The `incidents` section does not open a recursive readiness incident.
6. Tests prove scanner behavior, tenant scoping, and incident payload construction.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentService.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseReadinessIncidentServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpWarehouseReadinessIncidentControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseIncidentServiceTest.java`

## Acceptance Criteria

- No Flyway migration is added.
- Readiness WARN and FAIL sections create stable readiness incidents.
- PASS sections and the `incidents` section are skipped.
- Incident write failures are counted.
- Controller tests prove current tenant is passed to the scanner.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy the manual scan API.
2. Add a guarded operator action or scheduler hook to run it after readiness checks.
3. Use incident list as the single warehouse issue queue.

## Rollback

- Stop calling the readiness incident scan API.
- Existing readiness and incident APIs continue to work independently.
