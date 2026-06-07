# QuickBI Full Closure Plan

**Goal:** Close the remaining production gaps listed in `docs/superpowers/specs/2026-06-05-quickbi-platform-design.md` until QuickBI can be claimed as functionally complete for the local product scope.

**Source Of Truth:** The authoritative scope is the local QuickBI design document's "后续仍需补齐" and "仍未完成的生产级能力" sections. Passing `scripts/verify-quickbi-focus.sh` is necessary but not sufficient.

## Closure Workstreams

1. Designer and runtime completion
   - Keyboard operations for dashboard widgets.
   - Full runtime-state editor for dashboard controls.
   - Runtime parameter visibility, reset, persistence, and embed reuse evidence.

2. Dataset and chart authoring completion
   - Full dataset editor UI.
   - Field folders, batch movement/transfer, copy, permission request UI.
   - Chart editor forms, field drag-to-query, chart copy, and reference impact analysis.

3. Datasource production hardening
   - Deeper connector driver/auth capability constraints.
   - Health governance beyond current preview/connectivity/SLO foundation.

4. Permission closure
   - Full permission editor forms.
   - Unified permission path for export, subscription delivery, embed, and AI agents.

5. Portal completion
   - Portal editor, layout configuration, menu drag/drop, default homepage, search, fullscreen, mobile portal, preview, and embed rendering.

6. Big-screen completion
   - In-canvas drag/resize, multi-select alignment, snap guides, mobile layout variants, richer component library.

7. Spreadsheet completion
   - Formula evaluation, batch fill, pivot/crosstab editing, cell style editing, mobile adaptation.

8. Self-service/export and delivery evidence closure
   - Finer object restore operations.
   - Cross-provider partition recovery drills.
   - Holiday-aware and natural-boundary period-over-period alert hardening.

## Verification Gates

- Focused frontend tests for each UI slice.
- Focused backend tests for each backend/domain slice.
- `scripts/verify-quickbi-focus.sh --backend-all` after a workstream closes.
- `scripts/verify-quickbi-focus.sh` after each merged slice.
- A final requirement-by-requirement audit against the design document before claiming full completion.

## First Slice

Start with dashboard designer keyboard operations because the design document explicitly lists this as missing, the existing designer already has move/resize/copy/remove/undo/redo primitives, and the closure can be verified without touching non-QuickBI modules.
