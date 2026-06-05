# Spec: Frontend Canvas State

Source package: `docs/architecture/active/reviewed-packages/p1/frontend-canvas-state/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed, but old "no global state anywhere" is too broad.

## Problems

- `frontend/src/pages/canvas-editor/index.tsx` was 2037 lines at the start of this implementation pass and is 1581 lines after extracting graph, selection, history, publish/test/canary/version-history workflows, settings components, publish validation, API adapters, and graph serialization helpers.
- Canvas editor state is heavily local: many `useState`, `useEffect`, refs, casts, and workflow concerns live in one page.
- `any` remains common in the canvas editor and service layer.
- Some context providers exist, so the accurate issue is not "zero global state"; it is missing structured canvas-editor state boundaries.

## Evidence

- `frontend/src/pages/canvas-editor/index.tsx` current line count: 1581.
- Canvas editor state boundaries now include `useCanvasGraphState`, `useCanvasSelectionState`, `useCanvasHistoryState`, `useCanvasPublishWorkflow`, `useCanvasTestRunWorkflow`, `useCanvasCanaryWorkflow`, `useCanvasVersionHistoryWorkflow`, `CanvasWorkflowModals`, `CanvasVersionHistoryDrawer`, and `CanvasEditorSettingsPanel`.
- High-risk production `any` casts in canvas editor/API files are removed; remaining typed casts are React Flow `Node.data` and DOM target boundary casts documented in evidence.
- Existing contexts: `AuthContext`, `NotificationContext`, `CanvasActionsContext`.

## Acceptance Criteria

- Canvas editor is split into focused hooks/components with stable state boundaries.
- Graph state, selection, history, publish/test workflows, and settings have explicit APIs.
- Core editor interactions are covered by component or hook tests.
- High-risk `any` casts are replaced with typed models.
