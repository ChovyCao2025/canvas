# BI / Analytics Module Quality Progress

Date: 2026-06-16
Branch: main
Workspace: current local branch and current workspace; no worktree or branch created.

## Scope

- Frontend reviewed: `frontend/src/pages/bi`, `frontend/src/pages/analytics`, `frontend/src/services/biApi.ts`, `frontend/src/services/analyticsApi.ts`.
- Backend/API reviewed: legacy runtime BI controllers under `backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi`, analytics controller, BI context resource key normalization, and related tests. Current runtime module references were identified under `backend/canvas-context-bi` and `backend/canvas-web`.
- Requested Browser scope: `/bi` and `/analytics` only.

## Worktree Guardrail

First command run:

```text
git status --short
```

The workspace already had unrelated modified and untracked files. Later status also showed additional unrelated changes in `backend/canvas-web`, `frontend/src/services/systemOptions.ts`, and several progress/audit files. I did not revert or edit those files.

Files intentionally written in this pass:

- `tmp/module-quality-bi-analytics-progress.md`
- `docs/e2e-browser-audits/bi-analytics.md`

## Review Findings

1. Medium: `/analytics` initializes filter state from URL parameters but never writes user changes back to the URL.
   Evidence: `frontend/src/pages/analytics/index.tsx` reads `startDate`, `endDate`, `eventCode`, `userId`, and `attribute` from `useSearchParams()` at lines 31-35, while the RangePicker and Inputs only call local state setters at lines 135-142, 162-169, and 214-221. Refreshing after changing filters loses the current query/filter state unless those values were already in the original URL. This directly affects query/filter state and refresh behavior.

2. Medium: BI dashboard widget and control query failures are silently converted to missing results.
   Evidence: `frontend/src/pages/bi/index.tsx` catches every widget query failure and maps it to `[widgetKey, null]` at lines 2799-2803, then filters nulls out at lines 2804-2807. Control option queries do the same at lines 2825-2838. Users see empty chart/table/control states with no per-widget error contract, making API failures indistinguishable from valid empty datasets.

3. Low / coverage risk: analytics has API-client and presentation-helper tests, but no component/page test covering route-loaded filter controls, error rendering, or refresh persistence.
   Evidence: `frontend/src/services/analyticsApi.test.ts` and `frontend/src/pages/analytics/analyticsPresentation.test.ts` passed, but there is no `frontend/src/pages/analytics/index.test.tsx`. This leaves the finding above uncovered by frontend route/component tests.

## Positive Evidence

- Route wiring exists for `/bi` and `/analytics` behind the authenticated app layout in `frontend/src/App.tsx` lines 164-174.
- BI runtime route parsing and resource selection are unit-covered and implemented in `frontend/src/pages/bi/biWorkbench.ts` lines 3804-3843 and 7225-7241.
- BI runtime parameter precedence is explicit: URL, remembered runtime state, defaults, and global parameter mirroring in `frontend/src/pages/bi/biWorkbench.ts` lines 2216-2307.
- Subscription/export side-effect APIs are centralized in `frontend/src/services/biApi.ts` lines 2039-2081.
- BI resource keys are normalized to URL-safe slugs in `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceKey.java`, reducing path-segment risk for BI resource paths.

## Verification

Passed:

```text
cd frontend
npm run test -- --run src/services/analyticsApi.test.ts src/services/biApi.test.ts src/pages/analytics/analyticsPresentation.test.ts src/pages/bi/biWorkbench.test.ts src/pages/bi/index.test.tsx src/pages/bi/embed.test.tsx
```

Result: 6 test files passed, 214 tests passed, duration 362.81s.

Passed after selecting the installed Java 21 runtime:

```text
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine -Dtest=AnalyticsControllerTest,BiDashboardControllerTest,BiQueryControllerTest,BiSelfServiceControllerTest,BiSubscriptionControllerTest test
```

Result: 71 tests passed, 0 failures, 0 errors, 0 skipped.

Environment note: the default shell still uses Corretto 8, which fails this Maven slice before tests:

```text
openjdk version "1.8.0_482"
Fatal error compiling: invalid flag: --release
```

Repository guideline requires Java 21, so backend verification should pin `JAVA_HOME` or update the shell default.

Browser verification:

- Fresh port checks across continuations: no process was listening on `127.0.0.1:3000` or `:8080`.
- Vite proxies `/canvas`, `/auth`, `/admin`, `/meta`, and `/v3` API traffic to `http://localhost:8080` when the frontend server is running.
- Retried the required Codex in-app Browser using the Browser plugin across repeated goal continuations. The runtime consistently returned: `Browser is not available: iab`.
- Because the requested Browser surface is unavailable, `/bi` and `/analytics` Browser route checks remain blocked.

## Bugs Fixed

None. The objective restricted writes to the two audit/progress files, so product code was not changed.

## Needs Coordination

- Start frontend on `localhost:3000` and backend on `localhost:8080` before Browser-regressing API-backed BI/analytics behavior.
- Provide an available Codex in-app Browser `iab` session for the requested E2E page testing. This is now the repeated hard blocker for completing the objective.
- Use Java 21 for backend verification; Java 21 is installed at `/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`.
- If product fixes are approved, address the two findings with code changes and focused tests:
  - synchronize `/analytics` filter state to URL/search params,
  - surface per-widget/control BI query errors instead of silently dropping failed results.

## Remaining Risks

- Browser-only checks for blank screen, ErrorBoundary behavior, console/network errors, layout overflow, and refresh behavior are not complete because the in-app Browser remains unavailable after repeated attempts.
- API-backed route behavior could differ once backend services are running, especially around auth, tenant context, and BI query error payloads.
