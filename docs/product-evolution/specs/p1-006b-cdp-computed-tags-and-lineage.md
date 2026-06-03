# P1-006B - CDP Computed Tags And Lineage Spec

Priority: P1
Sequence: 006B
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006b-cdp-computed-tags-and-lineage-plan.md`

## Goal

Add computed tags with dependency validation and lineage so operators can create behavior-based tags and understand downstream impact before changing them.

## Current Baseline

- `CdpTagService` writes manual/API/import tags and tag history.
- `TagDefinitionDO` stores tag metadata and value type.
- Canvas and audience rules can reference tags, but there is no reverse lineage index or delete-impact check.
- There is no computed tag definition, dependency graph, cycle validation, preview, or run history.

## In Scope

- Computed tag definitions with RULE, SQL, and EXPR metadata.
- Dependency edges between computed tags.
- Cycle detection with returned cycle path.
- Preview count and sample users.
- Activate, pause, run-now, and run-history operations.
- Tag writeback through `CdpTagService` using deterministic idempotency keys.
- Lineage checks from tag/profile attribute to audiences and canvases.

## Out Of Scope

- Computed profile attribute implementation; P1-006.
- Realtime audience membership; P1-006C.
- Large SQL execution against external warehouses.

## Functional Requirements

1. Dependency cycles must be rejected before activation with a path such as `tag_a -> tag_b -> tag_a`.
2. Computed tag preview must not write current tags or tag history.
3. Run-now must write current tags and history through `CdpTagService`.
4. Lineage must find references in tag dependencies, audience rules, and canvas graph JSON.
5. Delete or incompatible type change must return an impact response rather than silently mutating.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V100__cdp_computed_tags_lineage.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpLineageService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java`
- `frontend/src/pages/cdp-computed-tags/index.tsx`
- `frontend/src/services/cdpApi.ts`

## Acceptance Criteria

- Computed tag definitions can be created, previewed, activated, paused, run, and audited.
- Cycle validation tests return the exact cycle path.
- Lineage tests show tag to audience and tag to canvas impact.
- Run-now writes tags idempotently via `CdpTagService`.

