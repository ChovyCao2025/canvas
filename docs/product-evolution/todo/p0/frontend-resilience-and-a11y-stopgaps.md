# P0 - Frontend Resilience And A11y Stopgaps

## Sources

- `product-interaction-directions-2026-06-01.md`
- `product-interaction-directions-2026-06-02.md`
- `product-strategy-supplementary-dimensions-2026-05-31.md`

## Why P0

This work prevents white screens, unreachable routes, silent request failures, and basic accessibility failures. The sources propose many interaction improvements; only the baseline resilience items are P0.

## Work Items

- Add global, route-level, and widget-level ErrorBoundary coverage.
- Add 404 route fallback and replace bare 403 text with a styled page.
- Configure request timeout and cancellation for long-running or abandoned requests.
- Add offline detection and a clear offline banner.
- Classify error notifications into network, permission, server, and business errors.
- Add semantic `header`, `nav`, `main`, and skip-link structure to the application layout.
- Add screen-reader utility classes and route-change focus management.
- Add `aria-live` announcements for success, failure, and urgent errors.
- Fix the documented dependency inconsistency: user preference infrastructure is phase 2, not a phase 0 dependency.

## Deferred From Sources

- Page transitions, full motion system, report builder, mobile editor, CRDT, and dark mode are not P0.
- The 154-item interaction configuration inventory is not executable scope.

## Dependencies

- Error reporting can start with console logging and later connect to backend reporting.
- Focus management depends on current router structure.

## Acceptance Criteria

- A broken page component does not blank the whole app.
- Route misses and permission failures render recoverable pages.
- Timed-out or cancelled requests do not leave components stuck in loading state.
- Keyboard and screen-reader users have a main-content target and dynamic status announcements.
