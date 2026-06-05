# P0-002 - Frontend Resilience And A11y Spec

Priority: P0
Sequence: 002
Source: `todo/p0/frontend-resilience-and-a11y-stopgaps.md`
Implementation plan: `../plans/p0-002-frontend-resilience-and-a11y-plan.md`

## Implementation Status

Implemented and verified on 2026-06-05. Verification evidence is recorded in `../plans/p0-002-frontend-resilience-and-a11y-plan.md`.

## Goal

Prevent white screens, dead routes, silent request failures, and baseline accessibility failures in the React app shell.

## User And Business Value

Operators can recover from UI failures, understand request failures, avoid losing unsaved canvas changes, and complete core navigation with keyboard or screen-reader tooling.

## In Scope

- Add a jsdom-capable component test harness for frontend resilience tests while keeping existing pure Vitest tests working.
- Add a reusable `AppErrorBoundary` and route-level fallback so lazy-route or render failures show a recoverable error panel.
- Add styled 403 and 404 pages and route guards that render the 403 page for authenticated users without the required role.
- Add request timeout and normalized error classification in the shared API client.
- Add offline detection and a non-visual `aria-live` announcement surface for route and network status changes.
- Add skip link, `main` landmark, route focus management, and stable menu navigation labels in `AppLayout`.
- Add a browser unload guard for dirty canvas-editor drafts so refresh or tab close warns before data loss.

## Out Of Scope

- Full WCAG AA/AAA audit.
- Dark mode, motion-reduction system, design-system migration, or visual redesign.
- Backend authorization changes; those are implemented by `p0-001-production-safety-and-compliance`.
- Advanced request dedupe, React Query adoption, or global data cache replacement.

## Functional Requirements

1. A component render error must show a page-level alert with retry and home navigation actions.
2. An authenticated user without the required role must see the 403 page instead of plain text.
3. Unknown routes must render a 404 page under the normal app shell when the user is authenticated.
4. The shared Axios client must time out after 15 seconds and must reject with a classified error object containing `kind`, `status`, `message`, and `retryable`.
5. 401 responses must keep the existing logout-and-login redirect behavior.
6. 403, timeout, offline, canceled, server, and unknown errors must be distinguishable by frontend callers.
7. App layout must expose a skip link, a `role="navigation"` side menu label, a focusable `main` landmark, and a route-change announcement.
8. Dirty canvas editor state must register `beforeunload`; clean or read-only editor state must not register the warning.
9. New component tests must run with `npm test -- <test-file>` and existing pure tests must continue to run with the default `node` test environment.

## Technical Scope

### Frontend Touchpoints

- `frontend/package.json`
- `frontend/package-lock.json`
- `frontend/vite.config.ts`
- `frontend/src/test/setupTests.ts`
- `frontend/src/App.tsx`
- `frontend/src/auth/guards.tsx`
- `frontend/src/components/layout/AppLayout.tsx`
- `frontend/src/components/errors/AppErrorBoundary.tsx`
- `frontend/src/components/errors/ForbiddenPage.tsx`
- `frontend/src/components/errors/NotFoundPage.tsx`
- `frontend/src/components/accessibility/LiveRegion.tsx`
- `frontend/src/components/accessibility/RouteA11y.tsx`
- `frontend/src/services/api.ts`
- `frontend/src/services/apiError.ts`
- `frontend/src/pages/canvas-editor/unsavedChangeGuard.ts`
- `frontend/src/pages/canvas-editor/index.tsx`

### Test Touchpoints

- `frontend/src/components/errors/AppErrorBoundary.test.tsx`
- `frontend/src/components/errors/routeFallbacks.test.tsx`
- `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- `frontend/src/services/apiResilience.test.ts`
- `frontend/src/pages/canvas-editor/unsavedChangeGuard.test.ts`

## Dependencies

- React 18 and React Router 6.23 are already present.
- Ant Design is already present for buttons and result states.
- New frontend dev dependencies are `jsdom`, `@testing-library/react`, `@testing-library/jest-dom`, and `@testing-library/user-event`.

## Risks And Controls

- Component tests can slow down pure helper tests. Keep global Vitest environment as `node`; use `/* @vitest-environment jsdom */` only in component test files.
- Error handling can accidentally change the 401 login redirect contract. Preserve the existing localStorage cleanup and `window.location.href = '/login'` behavior in `api.ts`.
- Route focus can fail when `main` is not mounted yet. `RouteA11y` should query `#main-content` after location changes and ignore missing elements.
- Unload warnings can become noisy. Register the listener only when `isDirty === true` and `readonly === false`.

## Acceptance Criteria

- `AppErrorBoundary.test.tsx` proves a throwing child renders a recoverable alert and a reset action can re-render children.
- `routeFallbacks.test.tsx` proves 403 and 404 pages expose stable roles, headings, and navigation actions.
- `apiResilience.test.ts` proves timeout, canceled, offline, 401, 403, server, and unknown errors classify to distinct `kind` values.
- `AppLayout.a11y.test.tsx` proves skip link, `main` landmark, route announcement, and navigation labels exist.
- `unsavedChangeGuard.test.ts` proves dirty writable drafts register unload warnings while clean or read-only drafts do not.
- `cd frontend && npm test -- AppErrorBoundary.test.tsx routeFallbacks.test.tsx apiResilience.test.ts AppLayout.a11y.test.tsx unsavedChangeGuard.test.ts` passes.
- `cd frontend && npm run build` passes.
