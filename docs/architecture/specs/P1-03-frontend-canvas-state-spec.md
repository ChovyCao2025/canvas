# Spec: Frontend Canvas State

Source package: `docs/architecture/todo/p1/frontend-canvas-state/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Confirmed, but old "no global state anywhere" is too broad.

## Problems

- `frontend/src/pages/canvas-editor/index.tsx` is 2084 lines.
- Canvas editor state is heavily local: many `useState`, `useEffect`, refs, casts, and workflow concerns live in one page.
- `any` remains common in the canvas editor and service layer.
- Some context providers exist, so the accurate issue is not "zero global state"; it is missing structured canvas-editor state boundaries.

## Evidence

- `frontend/src/pages/canvas-editor/index.tsx` line count: 2084.
- Canvas editor local state starts around `index.tsx:284` and continues through workflow modals, history, settings, test execution, canary, and graph state.
- `frontend/src/services/api.ts` has many `any` definitions.
- Existing contexts: `AuthContext`, `NotificationContext`, `CanvasActionsContext`.

## Acceptance Criteria

- Canvas editor is split into focused hooks/components with stable state boundaries.
- Graph state, selection, history, publish/test workflows, and settings have explicit APIs.
- Core editor interactions are covered by component or hook tests.
- High-risk `any` casts are replaced with typed models.
