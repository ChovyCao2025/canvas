# BI / Analytics E2E Browser Audit

Date: 2026-06-16
Branch: main
Routes requested: `/bi`, `/analytics`

## Environment

- Frontend dev server: no listener on `127.0.0.1:3000` during repeated fresh continuation checks.
- Backend API server: no listener on `localhost:8080`.
- Vite proxy sends `/canvas`, `/auth`, `/admin`, `/meta`, and `/v3` to `http://localhost:8080`; API-backed checks are blocked without that backend.
- Codex in-app Browser: retried Browser plugin setup for `iab` across repeated goal continuations; result remains `Browser is not available: iab`.

## Routes Tested

None with the Codex in-app Browser. The required Browser instance remains unavailable after repeated attempts, so I did not claim Browser coverage via another tool.

## Routes Blocked

- `/bi`: blocked by unavailable in-app Browser. Full API-backed route behavior also requires frontend on `:3000` and backend on `:8080`.
- `/analytics`: blocked by unavailable in-app Browser. Analytics API calls require frontend on `:3000` and backend on `:8080`.

## Requested Browser Checks Status

- Workbench loading: blocked.
- Dashboard/resource selection: blocked.
- Query/filter controls: blocked.
- Chart/table rendering: blocked.
- Runtime route behavior: blocked.
- Blank screen: blocked.
- ErrorBoundary: blocked.
- Console/network errors: blocked.
- Layout overflow: blocked.
- Refresh behavior: blocked.

## Code Review Findings Relevant to E2E

1. `/analytics` refresh persistence risk.
   The page reads initial filters from search params but does not update the URL after user changes. A Browser refresh after changing date range, event code, user ID, or attribute will revert to the original URL-backed state.

2. `/bi` silent query failure risk.
   Dashboard widget and control-option query failures are caught and mapped to null results, then removed from rendering state. Browser users may see empty charts/tables/controls without an actionable error.

## Verification Evidence

Frontend focused tests passed:

```text
6 test files passed
214 tests passed
```

Command:

```text
cd frontend
npm run test -- --run src/services/analyticsApi.test.ts src/services/biApi.test.ts src/pages/analytics/analyticsPresentation.test.ts src/pages/bi/biWorkbench.test.ts src/pages/bi/index.test.tsx src/pages/bi/embed.test.tsx
```

Backend focused Maven slice passed after explicitly selecting Java 21:

```text
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=AnalyticsControllerTest,BiDashboardControllerTest,BiQueryControllerTest,BiSelfServiceControllerTest,BiSubscriptionControllerTest test
```

Result:

```text
Tests run: 71, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Fixes

No product fixes were applied in this pass. The workspace instruction limited writes to this audit file and the progress file.

## Next Browser Regression Entry Criteria

- Codex in-app Browser `iab` is available.
- Frontend is running on `localhost:3000`.
- Backend is running on `localhost:8080` with a usable local auth/session path or seeded local user state.
- Re-run only `/bi` and `/analytics`, capturing route load, refresh, console errors, failed network requests, overflow, ErrorBoundary state, and the two review findings above.
