# Frontend Accessibility And Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add frontend error isolation and accessibility checks for the main canvas, admin, and editor workflows without destabilizing existing editor state refactors.

**Architecture:** Keep route wiring in `frontend/src/App.tsx`, shared error UI in `frontend/src/components/layout`, canvas-editor local isolation in `frontend/src/pages/canvas-editor`, and static accessibility helpers/tests beside the components they check. Browser-level manual evidence is recorded under `docs/architecture/evidence/frontend`.

**Tech Stack:** React 18, TypeScript, Vite, Vitest, Ant Design, React Router, React Flow, server-rendered component tests, optional axe-core for static accessibility checks.

---

## Source Material

- Spec: `../specs/P2-06-frontend-accessibility-and-quality-spec.md`
- Source package: `../../../reviewed-packages/p2/frontend-accessibility-and-quality/`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/completed/specs/P2-06-frontend-accessibility-and-quality-spec.md`
- Read: `docs/architecture/active/reviewed-packages/p2/frontend-accessibility-and-quality/plan.md`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`
- Modify: `frontend/package.json`
- Create: `frontend/src/components/layout/ErrorBoundary.tsx`
- Create: `frontend/src/components/layout/ErrorBoundary.test.tsx`
- Create: `frontend/src/pages/canvas-editor/CanvasEditorErrorBoundary.tsx`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.ts`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.test.ts`
- Create: `frontend/src/test/accessibilityChecks.ts`
- Create: `docs/architecture/evidence/frontend/accessibility-audit.md`
- Test: `frontend/src/context/AuthContext.test.tsx`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Test: `frontend/src/components/config-panel/controlChrome.test.ts`
- Test: `frontend/src/components/notifications/notificationPresentation.test.ts`

### Task 1: Add global and route-level ErrorBoundaries

**Files:**
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`
- Create: `frontend/src/components/layout/ErrorBoundary.tsx`
- Create: `frontend/src/components/layout/ErrorBoundary.test.tsx`
- Test: `frontend/src/context/AuthContext.test.tsx`

- [x] Create a reusable `ErrorBoundary` class component with route name, reset key, and accessible fallback action.
- [x] Wrap the app shell and route groups in `App.tsx` with named boundaries for authenticated layout, admin routes, and canvas routes.
- [x] Ensure fallback content does not expose tokens, raw stack traces, or localStorage values.
- [x] Add tests for render success, thrown child fallback, reset behavior, and secret redaction.

**Run:**
```bash
cd frontend && npm test -- ErrorBoundary AuthContext
cd frontend && npm run build
```

**Expected:** ErrorBoundary tests pass, auth secret-redaction tests still pass, and the production frontend build succeeds.

### Task 2: Add canvas-editor local error isolation

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/CanvasEditorErrorBoundary.tsx`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Test: `frontend/src/pages/canvas-editor/settingsPresentation.test.ts`
- Test: `frontend/src/components/config-panel/presentation.test.ts`

- [x] Wrap graph canvas, inspector/config panel, execution trace panel, and node library with local editor boundaries.
- [x] Add reset keys for canvas id, graph reload key, and selected node id so a panel failure can recover without remounting the full editor.
- [x] Add tests that prove graph hydration and config-panel presentation helpers keep their existing behavior.

**Run:**
```bash
cd frontend && npm test -- graphHydration settingsPresentation presentation
```

**Expected:** Existing canvas-editor and config-panel tests pass after local boundary wiring.

### Task 3: Audit keyboard and focus behavior for core pages

**Files:**
- Create: `docs/architecture/evidence/frontend/accessibility-audit.md`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.ts`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.test.ts`
- Modify: `frontend/src/components/canvas/HoverEdge.tsx`
- Modify: `frontend/src/components/node-panel/NodeLibraryItem.tsx`
- Modify: `frontend/src/components/notifications/NotificationBell.tsx`

- [x] Audit login, home, canvas list, canvas editor, API docs, tenant admin, and notification bell workflows.
- [x] Record keyboard order, focus target, visible focus, label source, screen-reader name, and remediation path for each workflow.
- [x] Add static helper tests for canvas-editor keyboard labels, ARIA names, and focusable controls.

**Run:**
```bash
cd frontend && npm test -- canvasEditorAccessibility notificationPresentation nodeLibrary
test -f docs/architecture/evidence/frontend/accessibility-audit.md
rg "keyboard order|visible focus|screen-reader name|canvas editor|notification bell" docs/architecture/evidence/frontend/accessibility-audit.md
```

**Expected:** Accessibility audit has concrete workflow rows, and frontend static accessibility tests pass.

### Task 4: Add accessibility checks to frontend test tooling where practical

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/test/accessibilityChecks.ts`
- Modify: `frontend/src/components/config-panel/controlChrome.test.ts`
- Modify: `frontend/src/components/notifications/notificationPresentation.test.ts`
- Modify: `frontend/src/pages/api-docs/apiDocs.test.ts`

- [x] Add a small accessibility helper that checks accessible names, roles, missing labels, and focusable-control metadata from rendered strings or presentation models.
- [x] Add `axe-core` only if the Node test environment can run it without a browser dependency.
- [x] Wire checks into config-panel, notification, and API-doc tests.

**Run:**
```bash
cd frontend && npm test -- controlChrome notificationPresentation apiDocs
cd frontend && npm run build
```

**Expected:** Accessibility helper tests pass, no browser-only test dependency breaks the Node Vitest run, and frontend build succeeds.

### Task 5: Fold layout and style cleanup into frontend state refactors to avoid duplicate churn

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/settingsPanel.css`
- Modify: `frontend/src/components/config-panel/controlChrome.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/controlChrome.test.ts`
- Test: `frontend/src/components/config-panel/formValues.test.ts`
- Test: `frontend/src/pages/canvas-editor/localDraft.test.ts`

- [x] Keep cleanup limited to editor layout, focus order, stable control dimensions, and config-panel readability.
- [x] Preserve existing local draft, graph hydration, autosave, and config-panel presentation behavior.
- [x] Add tests for any helper extracted from the large editor component.

**Run:**
```bash
cd frontend && npm test -- controlChrome formValues localDraft graphHydration canvasEditorAutosave
```

**Expected:** Focused editor and config-panel tests pass, and style cleanup does not change saved draft or graph behavior.

### Task 6: Handoff scoped frontend accessibility and quality changes

**Files:**
- Modify: `docs/architecture/archive/completed/plans/P2-06-frontend-accessibility-and-quality-plan.md`
- Modify: `frontend/package.json`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/main.tsx`
- Create: `frontend/src/components/layout/ErrorBoundary.tsx`
- Create: `frontend/src/components/layout/ErrorBoundary.test.tsx`
- Create: `frontend/src/pages/canvas-editor/CanvasEditorErrorBoundary.tsx`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.ts`
- Create: `frontend/src/pages/canvas-editor/canvasEditorAccessibility.test.ts`
- Create: `frontend/src/test/accessibilityChecks.ts`
- Create: `docs/architecture/evidence/frontend/accessibility-audit.md`

- [x] Review only files named in this plan.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record verification commands and any remaining follow-ups in evidence.

**Run:**
```bash
git diff -- docs/architecture/archive/completed/plans/P2-06-frontend-accessibility-and-quality-plan.md frontend/package.json frontend/src/App.tsx frontend/src/main.tsx frontend/src/components/layout frontend/src/pages/canvas-editor frontend/src/test/accessibilityChecks.ts docs/architecture/evidence/frontend/accessibility-audit.md
git diff -- docs/architecture/archive/completed/plans/P2-06-frontend-accessibility-and-quality-plan.md docs/architecture/evidence/P2-06-frontend-accessibility-and-quality.md docs/architecture/evidence/frontend/accessibility-audit.md frontend/src/App.tsx frontend/src/components/errors/AppErrorBoundary.tsx frontend/src/components/layout/ErrorBoundary.tsx frontend/src/components/layout/ErrorBoundary.test.tsx frontend/src/components/canvas/HoverEdge.tsx frontend/src/components/node-panel/NodeLibraryItem.tsx frontend/src/components/notifications/NotificationBell.tsx frontend/src/components/notifications/notificationPresentation.test.ts frontend/src/components/config-panel/controlChrome.ts frontend/src/components/config-panel/controlChrome.test.ts frontend/src/components/config-panel/index.tsx frontend/src/pages/api-docs/index.tsx frontend/src/pages/api-docs/apiDocs.test.ts frontend/src/pages/canvas-editor/CanvasEditorErrorBoundary.tsx frontend/src/pages/canvas-editor/CanvasEditorErrorBoundary.test.tsx frontend/src/pages/canvas-editor/canvasEditorAccessibility.ts frontend/src/pages/canvas-editor/canvasEditorAccessibility.test.ts frontend/src/pages/canvas-editor/editorLayout.ts frontend/src/pages/canvas-editor/editorLayout.test.ts frontend/src/pages/canvas-editor/index.tsx frontend/src/pages/canvas-editor/settingsPanel.css frontend/src/test/accessibilityChecks.ts
```

**Expected:** The diff contains only frontend error isolation, accessibility checks, related docs, and focused tests. No commit is created by default.
