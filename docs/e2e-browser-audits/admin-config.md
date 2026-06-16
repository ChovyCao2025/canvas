# Admin Config E2E Browser Audit

Date: 2026-06-16
Branch: main
Dev server: `http://127.0.0.1:3001/` (`:3000` was already in use)

## Scope

Requested in-app Browser-only routes:
- `/admin/users`
- `/admin/projects`
- `/api-config`
- `/data-source-config`
- `/ab-experiments`
- `/tag-config`
- `/identity-types`
- `/tag-import`
- `/mq-config`
- `/event-config`
- `/webhook-subscriptions`
- `/api-docs`
- `/system-options`
- `/test-users`
- `/ai-predictions`

Requested checks:
- Admin route access
- List/table loading
- Filter/search
- Form validation
- Modal open/close
- Safe create/edit previews where available
- Blank screen
- ErrorBoundary
- Console/network errors
- Layout overflow
- Refresh behavior

## Browser Availability

Blocked.

Codex in-app Browser setup failed before route navigation:
- Required Browser target: `iab`
- Setup result: `Browser is not available: iab`
- Browser target list: `[]`
- Retry on 2026-06-16: setup still returned `Browser is not available: iab`; browser target list still returned `[]`.
- Third consecutive goal-turn retry on 2026-06-16: setup still returned `Browser is not available: iab` with `browserTargets: []`.

No route reached an admin-account or admin-role blocker. The blocker is the missing in-app Browser target in this session.

This is now a repeated environment blocker across three consecutive goal turns. The route audit cannot proceed until the Codex in-app Browser exposes the `iab` target.

## Route Results

| Route | In-App Browser Status | Notes |
| --- | --- | --- |
| `/admin/users` | Blocked | `iab` unavailable before navigation |
| `/admin/projects` | Blocked | `iab` unavailable before navigation |
| `/api-config` | Blocked | `iab` unavailable before navigation |
| `/data-source-config` | Blocked | `iab` unavailable before navigation |
| `/ab-experiments` | Blocked | `iab` unavailable before navigation |
| `/tag-config` | Blocked | `iab` unavailable before navigation |
| `/identity-types` | Blocked | `iab` unavailable before navigation |
| `/tag-import` | Blocked | `iab` unavailable before navigation |
| `/mq-config` | Blocked | `iab` unavailable before navigation |
| `/event-config` | Blocked | `iab` unavailable before navigation |
| `/webhook-subscriptions` | Blocked | `iab` unavailable before navigation |
| `/api-docs` | Blocked | `iab` unavailable before navigation |
| `/system-options` | Blocked | `iab` unavailable before navigation |
| `/test-users` | Blocked | `iab` unavailable before navigation |
| `/ai-predictions` | Blocked | `iab` unavailable before navigation |

## Bugs Found Outside Browser

### Fixed: System Options Runtime Contract

Finding:
- `SystemOptionsPage` consumes `res.data.list`.
- `systemOptionsApi.adminList` was typed as `R<PageResult<SystemOption>>`.
- Current `canvas-web` runtime returns `/admin/system-options` as `R<SystemOption[]>`.
- Result: the page could receive an array and set table data from `undefined`.

Fix:
- `frontend/src/services/systemOptions.ts` now normalizes list responses to `{ total, list }`.
- `frontend/src/services/systemOptions.test.ts` covers the current runtime response shape.

Verification:
- Focused frontend tests: 9 files, 38 tests passed.
- `npm run build` passed.
- Focused backend admin/config tests: 77 tests passed with Java 21.

## Review Findings To Track

1. Current `canvas-boot` does not depend on the legacy security module where JWT/security rules live, so direct admin/config API protection is not proven in the current boot runtime.
2. Legacy security rules do not explicitly tenant-admin gate all requested admin-config endpoints. Missing examples include MQ definitions, event definitions, tag definitions, identity types, tag import batch/API upload, test users, and AI prediction recompute.
3. Compatibility tests cover route shape and happy-path forwarding but not non-admin, unauthenticated, tenant-spoof, or permission-denied paths.

## Required Follow-Up

- Re-run this audit with the Codex in-app Browser once `iab` is available.
- Add/verify direct API authorization tests before treating admin-config permission coverage as complete.
