# P2-079 - CDP Warehouse Privacy Audience Bitmap Rebuild Automation Run History Spec

Priority: P2
Sequence: 079
Source: `docs/product-evolution/specs/p2-077-cdp-warehouse-privacy-audience-bitmap-rebuild-automation.md`, `docs/product-evolution/specs/p2-078-cdp-warehouse-privacy-audience-bitmap-rebuild-automation-operations-api.md`
Implementation plan: `../plans/p2-079-cdp-warehouse-privacy-audience-bitmap-rebuild-automation-run-history-plan.md`

## Goal

Persist manual and scheduled privacy audience bitmap rebuild automation cycles as tenant-scoped run history so production operators can audit what ran, when it ran, which source triggered it, and what P2-077 returned.

## Current Baseline

- P2-077 returns an in-memory automation summary and can run from a scheduler.
- P2-078 exposes a manual operations API for one automation cycle.
- P2-078 explicitly leaves persisted automation run history out of scope.
- Operators can see a manual response immediately, but cannot later audit scheduler outcomes or previous manual runs without logs.

## In Scope

- Add a Flyway migration for `cdp_warehouse_privacy_audience_rebuild_automation_run`.
- Add a DO and mapper for run history.
- Add a run history service that wraps the existing P2-077 automation service.
- Persist trigger source, actor, limits, retry flag, status counters, result JSON, error message, and timestamps.
- Route P2-078 manual runs through the run history service while preserving the existing `AutomationResult` response.
- Route P2-077 scheduler runs through the same run history service when configured.
- Add recent and get APIs for run history under the existing privacy erasure controller.
- Add focused schema, service, controller, and scheduler tests.

## Out Of Scope

- Changing P2-077 candidate selection or rebuild behavior.
- Async queues or retrying failed automation cycles from history.
- Persisting individual per-request rows outside the result JSON.
- UI.

## Runtime Semantics

1. Manual runs are recorded with `triggerSource=MANUAL`.
2. Scheduled runs are recorded with `triggerSource=SCHEDULED`.
3. The run service inserts a `RUNNING` row before delegating to P2-077.
4. On success, it stores P2-077 counters and result JSON, then marks the row with the P2-077 status.
5. On failure, it marks the row `FAIL`, stores a bounded error message, and rethrows so existing caller behavior remains fail-closed.
6. P2-078 `POST /warehouse/privacy/erasure/audience-rebuild/automation/run` still returns the same `AutomationResult` payload.
7. Operators can query recent runs and one run by id for the current tenant.

## Functional Requirements

1. Manual automation runs must be persisted with tenant, actor, command limits, retry flag, status counters, and result JSON.
2. Scheduled automation runs must be persisted with `SCHEDULED` source.
3. Failed automation execution must leave a `FAIL` run row with error evidence.
4. Run history queries must be tenant scoped.
5. Existing P2-077 service tests and P2-078 manual response semantics must continue to pass.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V261__cdp_warehouse_privacy_audience_rebuild_automation_run.sql`.
- Add `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO.java`.
- Add `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper.java`.
- Add `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyAudienceBitmapRebuildScheduler.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java`.
- Add or modify focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.
- Update product-evolution indexes.

## Acceptance Criteria

- P2-079 spec and plan are indexed.
- Schema test proves the run history table and tenant/status/source indexes exist.
- Run service tests prove success rows, failure rows, recent listing, and tenant-scoped get behavior.
- Controller tests prove manual run is recorded and recent/get endpoints use current tenant.
- Scheduler tests prove scheduled runs use the run history service when present.
- Focused backend tests pass.
- Warehouse/CDP regression passes.

## Rollout

1. Deploy migration and code.
2. In staging, call P2-078 manual run and verify the run appears in recent history.
3. Enable P2-077 scheduler and verify scheduled runs appear with `SCHEDULED` source.
4. Use run history alongside P2-073 erasure proof backlog during privacy operations.

## Rollback

- Stop calling the run history APIs.
- P2-077 automation and P2-078 manual response behavior remain available.
