# home-ops-collaboration Audit

Date: 2026-06-16
Branch: `main`

## Requested Routes

- `/home`
- `/ops`
- `/approvals`
- `/conversations`

## Code Review Findings

### High: Ops emergency APIs trust a missing client-controlled role header as `TENANT_ADMIN`

Evidence:

- `frontend/src/services/api.ts:39`-`44` only injects `Authorization`; it does not send `X-Role`.
- `frontend/src/services/opsApi.ts:41`-`46` calls `/ops/runtime/status`, `/ops/audit-events`, and `/ops/canvas/{id}/{action}` without role or actor headers.
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java:28` defaults missing role to `TENANT_ADMIN`.
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java:67`-`114` accepts optional `X-Role` for pause/offline/resume/kill/rollback.
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java:123`-`127` passes `roleOrDefault(role)` into `facade.emergencyAction`.
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java:143`-`145` returns `DEFAULT_ROLE` when `X-Role` is absent.

Impact:

The `/ops` page computes role behavior from `runtimeStatus.role`, but the backend can report/admin-authorize `TENANT_ADMIN` whenever `X-Role` is absent. Unless another verified layer injects trusted `X-Role`, a normal authenticated browser request can be treated as emergency-authorized. This undermines expected role behavior and affects high-impact actions.

Recommended fix:

Derive tenant, actor, and role from the authenticated security context or a trusted server-side principal. Do not default emergency role to `TENANT_ADMIN`, and do not trust browser-supplied `X-Role` for authorization.

### Medium: Conversation workspace async failures have no user-visible error state

Evidence:

- `frontend/src/pages/conversations/index.tsx:87`-`97` catches no errors in inbox load; failures only clear loading.
- `frontend/src/pages/conversations/index.tsx:119`-`138` catches no errors in timeline and AI suggestion load; the drawer can remain open with empty/stale content.
- `frontend/src/pages/conversations/index.tsx:164`-`260` wraps assign/status/task/AI mutations in `try/finally` but no `catch`; failed actions clear spinners without a toast or persistent error.

Impact:

Network or backend failures on `/conversations` can look like empty data or no-op actions. This directly affects conversation inspection, filters/actions/modals, loading/error/empty states, refresh behavior, and operator confidence.

Recommended fix:

Add route-level error state for inbox and drawer loads, and catch mutation failures with actionable messages while preserving the selected work item.

### Medium: Approval task load failures are toast-only and leave an indistinguishable empty table

Evidence:

- `frontend/src/pages/approvals/index.tsx:20`-`29` catches load errors with `message.error` but stores no persistent error.
- `frontend/src/pages/approvals/index.tsx:128`-`134` renders the same table surface after failure, with no error banner or retry affordance beyond the generic refresh button.

Impact:

After the toast disappears, `/approvals` cannot distinguish "no pending approvals" from "approvals failed to load." This weakens loading/error/empty-state behavior and approval action readiness.

Recommended fix:

Track load errors in component state and render a persistent `Alert` with retry. Keep the empty table state only for confirmed successful empty results.

### Medium: Ops emergency action failures are not caught

Evidence:

- `frontend/src/pages/ops-dashboard/index.tsx:93`-`106` submits emergency actions in `try/finally` without a `catch`.

Impact:

If an emergency action fails, the spinner clears but the user gets no local failure message from the page. Depending on global promise handling, this can appear as a no-op after a high-impact action.

Recommended fix:

Catch action failures and show a persistent or toast error with enough detail to distinguish permission denial, validation failure, and transient backend failure.

### Medium: Notification provider can keep stale reconnect state after user changes

Evidence:

- `frontend/src/context/NotificationContext.tsx:84` stores reconnect attempt count in a ref.
- `frontend/src/context/NotificationContext.tsx:172`-`177` starts fallback polling.
- `frontend/src/context/NotificationContext.tsx:180`-`195` schedules reconnect based on the existing ref.
- `frontend/src/context/NotificationContext.tsx:209`-`250` updates connection state, but the user-change effect does not visibly reset `reconnectAttemptRef` before reconnecting.

Impact:

After one user exhausts reconnect attempts, a later user in the same tab can inherit stale reconnect backoff state and remain on polling longer than expected. This is a notification side-effect risk around role/user switching.

Recommended fix:

Reset reconnect attempt state on user changes and on explicit logout cleanup.

## Verification Run

Frontend focused tests passed:

```text
Test Files  16 passed (16)
Tests       63 passed (63)
```

Backend focused tests passed with Java 21:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Browser E2E Status

Vite served the frontend at:

```text
http://127.0.0.1:3000/
```

Codex in-app Browser setup still cannot proceed because no `iab` target is registered. Runtime target query returned:

```json
{
  "browsers": []
}
```

No Browser route testing was completed in this session, and no alternate browser was used.

This same Browser availability blocker has repeated across consecutive goal continuations. The audit cannot be completed further without an available Codex in-app Browser `iab` target.

## Route Results

| Route | Status | Notes |
| --- | --- | --- |
| `/home` | Blocked | In-app Browser unavailable. Code review found no blocking route-specific bug beyond needing Browser verification. |
| `/ops` | Blocked | In-app Browser unavailable. Code review found role/authorization and action-error risks. |
| `/approvals` | Blocked | In-app Browser unavailable. Code review found toast-only load failure state. |
| `/conversations` | Blocked | In-app Browser unavailable. Code review found missing load/action error handling. |

## Fixes

No source fixes were applied in this turn because writes were restricted to this progress file and audit file only.

## Needs Coordination

- Provide or enable the Codex in-app Browser `iab` target for route checks.
- Approve source edits if the documented bugs should be fixed in this workspace.

## Remaining Risks

- Browser evidence is missing for blank screen, ErrorBoundary, console errors, network errors, content rendering, filters/actions/modals, conversation panels, refresh behavior, and expected role behavior.
- Source bugs were documented but not fixed due the write restriction.
