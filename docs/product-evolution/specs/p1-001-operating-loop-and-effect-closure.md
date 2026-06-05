# P1-001 - Operating Loop And Effect Closure Spec

Priority: P1
Sequence: 001
Source: `todo/p1/operating-loop-and-effect-closure.md`
Implementation plan: `../plans/p1-001-operating-loop-and-effect-closure-plan.md`

## Implementation Status

Implemented and verified on 2026-06-05. Verification evidence is recorded in `../plans/p1-001-operating-loop-and-effect-closure-plan.md`.

## Goal

Make the core operator loop measurable and reversible from template selection through pre-publish checks, publish, control-group holdout, conversion attribution, receipt tracking, version comparison, and rollback.

## User And Business Value

Operators can start from a proven template, understand publish risk before launch, measure incremental effect after launch, inspect delivery records, and recover from bad releases without asking engineering to query the database.

## In Scope

- Template browsing and one-click clone from existing `CanvasTemplateDO` records.
- Pre-publish check endpoint and editor panel that blocks publish on structural errors and warns on risky configuration.
- Canvas-level control group percentage with deterministic user holdout and auditable holdout records.
- Last-touch conversion attribution from `event_log` conversion events to the latest successful `message_send_record` for the same user and canvas.
- Canvas stats endpoints for send receipts and attribution summary.
- Frontend stats additions for delivery status counts, conversions, conversion amount, and attributed send records.
- Version diff access from the editor version drawer and existing draft revert flow.

## Out Of Scope

- Multi-touch attribution, path-based attribution, predictive optimization, or AI journey creation.
- Provider receipt webhooks and reconciliation jobs; those belong to `p0-003-delivery-outbox-receipts-and-reconciliation`.
- Advanced report builder and arbitrary metric formulas.
- Collaboration review comments; those belong to `p2-001-collaboration-personalization-and-reporting`.

## Functional Requirements

1. The canvas list page must expose a template catalog action that lists enabled templates and creates a draft canvas from a selected template.
2. Publishing from the editor must first call `GET /canvas/{id}/pre-publish-checks`.
3. A pre-publish result with any `ERROR` item must prevent publish and display the blocking item codes.
4. A canvas may define `controlGroupPercent` from 0 to 50 and `controlGroupSalt`; held-out users must not execute the canvas and must be recorded in a holdout table.
5. Event reporting must attribute conversion events whose `eventCode` matches a canvas `conversionEventCode` to the most recent sent touch for the same user and canvas within the configured attribution window.
6. Attribution must be idempotent by event log and canvas so event retries do not double-count conversion value.
7. Canvas stats must return delivery status counts from `message_send_record` and conversion summary from attribution records.
8. The editor version drawer must expose a diff action that calls the existing backend diff endpoint and a revert action that keeps the current published version unchanged.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasControlGroupService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasAttributionService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/TriggerPreCheckService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/service/impl/EventDefinitionServiceImpl.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasControlGroupHoldoutDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasConversionAttributionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasControlGroupHoldoutMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasConversionAttributionMapper.java`
- `backend/canvas-engine/src/main/resources/db/migration/V96__operating_loop_effect_closure.sql`

### Frontend Touchpoints

- `frontend/src/pages/canvas-list/index.tsx`
- `frontend/src/pages/canvas-list/templateCatalog.ts`
- `frontend/src/pages/canvas-editor/index.tsx`
- `frontend/src/pages/canvas-editor/prePublishChecks.ts`
- `frontend/src/pages/canvas-stats/index.tsx`
- `frontend/src/pages/canvas-stats/effectClosure.ts`
- `frontend/src/services/api.ts`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/OpsControllerTemplateTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasPrePublishCheckServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasControlGroupServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasAttributionServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CanvasStatsControllerEffectClosureTest.java`
- `frontend/src/pages/canvas-list/templateCloneFlow.test.ts`
- `frontend/src/pages/canvas-editor/prePublishChecks.test.ts`
- `frontend/src/pages/canvas-stats/effectClosure.test.ts`

## Dependencies

- P0-001 must protect tenant and send-policy paths before attribution and receipt views are considered production-safe.
- P0-002 must provide the frontend jsdom/component test harness if UI rendering tests are added during execution.
- P0-003 can later replace the simple receipt-count read model with provider-webhook reconciliation.

## Risks And Controls

- Control-group holdout changes who receives messages. Keep the initial percentage bounded to 50, default it to 0, and record every holdout decision.
- Last-touch attribution can overclaim effect. Label the metric as last-touch and keep the algorithm deterministic, documented, and idempotent.
- Pre-publish checks can block urgent launches. Use `ERROR` only for structural failures that would break execution; use `WARNING` for optimization suggestions.
- Template cloning can duplicate stale examples. Filter the catalog to enabled templates and keep created canvases in `DRAFT`.

## Acceptance Criteria

- Template catalog frontend helper tests pass and the backend template controller test proves clone creates a draft version from template graph JSON.
- Pre-publish check tests prove invalid graph and missing trigger produce `ERROR`, while a valid event-trigger graph can publish.
- Control-group tests prove the same canvas/user/salt always yields the same holdout decision and records held-out users once.
- Attribution tests prove a conversion event maps to the latest prior sent record and duplicate event logs do not double-count.
- Stats controller tests prove receipt counts and attribution summary are returned for a canvas.
- Frontend helper tests prove receipt and attribution summaries produce stable KPI display models.
