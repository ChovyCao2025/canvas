# P1-003E - Canvas Import Export Package Spec

Priority: P1
Sequence: 003E
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003e-canvas-import-export-package-plan.md`

## Goal

Add a versioned canvas package format so operators can export a sanitized journey and import it as a new draft.

## Current Baseline

- `CanvasOpsService.clone` copies a draft inside the same environment.
- There is no versioned JSON package for migration, template reuse, or cross-environment review.
- Runtime graph JSON may contain snapshot IDs, idempotency keys, route state, and secret-like config keys.

## In Scope

- Package version 1 DTO with source metadata, canvas metadata, graph JSON, and export timestamp.
- Export sanitization that removes runtime-only keys and masks secret-like values using `DataMaskingUtil`.
- Import validation for `packageVersion=1` and `graph.nodes`.
- Draft canvas and draft version creation on import.
- Frontend list-page helpers and actions for export/download and import/upload-by-text.

## Out Of Scope

- Cross-tenant sharing and public templates.
- Environment credential mapping.
- Project/folder filter implementation; split into P1-003F, although package fields may preserve that metadata when present.

## Functional Requirements

1. Export returns `packageVersion=1`, `exportedAt`, `source`, `canvas`, and sanitized `graph`.
2. Exported graph does not contain `audienceSnapshotId`, `idempotencyKey`, `apiKey`, `token`, `secret`, `password`, `authorization`, `cookie`, or data source credentials.
3. Import rejects unsupported package versions and invalid graph shape.
4. Import creates a new `DRAFT` canvas with one draft `CanvasVersionDO`.
5. Import never inherits published, canary, offline, killed, or archived state.

## Technical Scope

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasImportExportService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasExportPackage.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/CanvasImportResp.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- `frontend/src/services/api.ts`
- `frontend/src/pages/canvas-list/canvasImportExport.ts`
- `frontend/src/pages/canvas-list/index.tsx`

## Acceptance Criteria

- Export sanitization tests prove runtime and secret-like keys are absent.
- Import tests prove draft canvas/version creation and unsupported version rejection.
- Frontend helper tests prove stable filename and import payload generation.
- List page exposes export for canvases with a version and import through a JSON text modal.
