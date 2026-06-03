# P1-003F - Canvas Project Folder Metadata Spec

Priority: P1
Sequence: 003F
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003f-canvas-project-folder-metadata-plan.md`

## Goal

Add flat project/folder metadata and list filters so operators can scan a growing canvas list without a full campaign hierarchy.

## Current Baseline

- `CanvasDO`, `CanvasCreateReq`, `CanvasUpdateReq`, and `CanvasListQuery` have no project/folder fields.
- Canvas list supports status/name-style filtering but not operational grouping.
- There is no project table, folder table, nested tree, or project permission model.

## In Scope

- Add nullable `project_key`, `project_name`, `folder_key`, and `folder_name` to `canvas`.
- Preserve metadata on create, update, list, import, and export when the caller supplies it.
- Canvas list filters by `projectKey` and `folderKey`.
- Frontend helper, create/edit payload fields, table column, and list filters.

## Out Of Scope

- Project ACLs, quotas, billing, lifecycle, nested folder trees, and campaign object model.
- Collaboration comments and reporting workflow; covered by P2-001.
- Design-system guided experience; covered by P2-014.

## Functional Requirements

1. Canvas create/update stores flat project and folder metadata.
2. Canvas list returns metadata and filters by exact `projectKey` and `folderKey`.
3. Import/export package preserves project/folder metadata when present.
4. Frontend list table shows project/folder label and sends selected filter parameters.
5. Blank filter values are not sent to the backend.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V96_1__canvas_project_folder_metadata.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- `frontend/src/types/index.ts`
- `frontend/src/services/api.ts`
- `frontend/src/pages/canvas-list/canvasProjectFilters.ts`
- `frontend/src/pages/canvas-list/index.tsx`

## Acceptance Criteria

- Migration test proves the four metadata columns and list index exist.
- Backend list filter test proves project/folder parameters are applied.
- Frontend tests prove query param generation and table label formatting.
- Existing canvas create/update/list behavior remains compatible when metadata is absent.
