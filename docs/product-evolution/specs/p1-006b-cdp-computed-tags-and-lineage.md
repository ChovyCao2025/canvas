# P1-006B - CDP Computed Tags And Lineage Spec

Priority: P1
Sequence: 006B
Source: `docs/optimization/todo/cdp_gap_analysis.md`, `docs/optimization/todo/2026-05-30-cdp-roadmap.md`
Implementation plan: `../plans/p1-006b-cdp-computed-tags-and-lineage-plan.md`

## Goal

Add computed tags with dependency validation and lineage so operators can create behavior-based tags and understand downstream impact before changing them.

## Current Baseline

- Implemented in the current workspace record on 2026-06-05; commit and merge status was not verified in this docs-only audit.
- `CdpTagService` writes manual/API/import/computed tags and tag history.
- `TagDefinitionDO` stores tag metadata and value type.
- Computed tag definition, dependency graph, cycle validation, preview, run-now, run history, and lineage impact checks are available.
- Operator UI is available at `/cdp/computed-tags` for administrators.

## In Scope

- Computed tag definitions with RULE, SQL, and EXPR metadata.
- RULE and EXPR first-slice execution against `cdp_user_profile.properties_json`; SQL is stored/validated as metadata in this slice.
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

- `backend/canvas-engine/src/main/resources/db/migration/V105__cdp_computed_tags_lineage.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/ComputedTagService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpLineageService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java`
- `frontend/src/pages/cdp-computed-tags/index.tsx`
- `frontend/src/pages/cdp-computed-tags/computedTagPresentation.ts`
- `frontend/src/services/cdpApi.ts`

## Acceptance Criteria

- Computed tag definitions can be created, previewed, activated, paused, run, and audited.
- Cycle validation tests return the exact cycle path.
- Lineage tests show tag to audience and tag to canvas impact.
- Run-now writes tags idempotently via `CdpTagService`.

## Implementation Status

- Status: implemented in the current workspace record on 2026-06-05; commit and merge status was not verified in this docs-only audit.
- Backend: added `ComputedTagService`, `CdpLineageService`, and `CdpComputedTagController` under `/cdp/computed-tags`; actual migration is `V105__cdp_computed_tags_lineage.sql` using `cdp_computed_tag_*` tables.
- Computed tag execution supports RULE/EXPR against active user profile JSON, rejects dependency cycles with an exact path, and writes matched tags through `CdpTagService.setTag(tenantId, userId, req)` with deterministic `computed-tag:{runId}:{userId}:{tagCode}` idempotency keys.
- Lineage impact checks scan computed tag dependencies, audience rule JSON, and published canvas graph JSON.
- Frontend: added typed `cdpApi.computedTags`, presentation helpers, `/cdp/computed-tags` admin page, route, and side-nav entry.
- Verification: focused Java 21 backend verification passed on 2026-06-08 with `ComputedTagSchemaTest`, `ComputedTagServiceTest`, `CdpLineageServiceTest`, and `CdpComputedTagControllerTest` covering 8 tests. Focused frontend verification passed on 2026-06-08 with `computedTagPresentation.test.ts` and `cdpApi.test.ts` covering 7 tests.
