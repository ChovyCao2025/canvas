# Codex Module Review + Browser E2E Local Window Goals

Date: 2026-06-16

## 用法

每个 Codex 窗口只复制一个 `Goal:` 块。所有窗口都直接在当前本地分支和当前工作区执行，不创建 git worktree，不创建新分支，不要求用户手动 `cd`。

每个窗口按项目模块独立工作，流程固定为：

1. 先审核本模块代码。
2. 再用 Codex in-app Browser 测本模块页面。
3. 发现问题后，只修本模块范围内、证据充分的问题。
4. 运行最小必要验证。
5. 用 Browser 回归对应页面。
6. 把代码审核、端测、修复、验证结果写入同一个模块报告。

隔离规则：

- 每个窗口只测试自己的模块路由。
- 每个窗口只写自己的报告文件。
- 每个窗口只修改自己的模块范围。
- 不要同时让多个窗口修改同一个文件。
- 遇到共享文件或跨模块问题，记录为 `needs coordination`，不要抢改。

断点和报告文件：

- 总入口：`docs/e2e-browser-audit.md`
- 每个窗口报告：`docs/e2e-browser-audits/<module-slug>.md`
- 每个窗口本地进度：`tmp/module-quality-<module-slug>-progress.md`

## 模块边界

| Module | Routes | Primary frontend scope | Primary backend/API scope |
| --- | --- | --- | --- |
| public-auth | `/login`, `/public/forms/:publicKey`, `/bi/embed/:resourceType/:resourceKey` | `frontend/src/pages/login`, `frontend/src/pages/public-marketing-form`, `frontend/src/pages/bi/embed.tsx`, `frontend/src/auth`, `frontend/src/context/AuthContext.tsx` | auth/session/public form/embed ticket APIs in `backend/canvas-boot` and `backend/canvas-web` |
| home-ops-collaboration | `/home`, `/ops`, `/approvals`, `/conversations` | `frontend/src/pages/home`, `frontend/src/pages/ops-dashboard`, `frontend/src/pages/approvals`, `frontend/src/pages/conversations` | ops, approval, conversation, notification APIs |
| canvas-journey | `/canvas`, `/canvas/:id/edit`, `/canvas/:id/stats`, `/canvas/:id/users` | `frontend/src/pages/canvas-list`, `frontend/src/pages/canvas-editor`, `frontend/src/pages/canvas-stats`, `frontend/src/pages/canvas-users`, `frontend/src/components/canvas`, `frontend/src/components/node-panel`, `frontend/src/components/config-panel`, canvas types/hooks/services | canvas, execution, stats, user journey APIs |
| marketing-suite | `/marketing-monitoring`, `/mautic-insights`, `/marketing-platform`, `/search-marketing`, `/risk`, `/growth-activities`, `/marketing-preferences`, `/marketing-forms`, `/content-hub`, `/message-templates`, `/message-deliveries`, `/channel-connectors`, `/demo-sandbox` | corresponding `frontend/src/pages/*` directories and matching marketing/risk/channel/message/content/form services | marketing, risk, content, forms, message, delivery, channel connector, monitoring, search marketing, growth activity APIs |
| bi-analytics | `/bi`, `/analytics` | `frontend/src/pages/bi`, `frontend/src/pages/analytics`, BI/analytics services | BI and analytics APIs |
| cdp-audience | `/cdp/users`, `/cdp/users/:userId`, `/audiences`, `/audiences/new`, `/audiences/:id/edit`, `/cdp/computed-profile`, `/cdp/computed-tags`, `/cdp/realtime-audiences` | CDP, audience, computed profile/tags, realtime audience pages and services | CDP, audience, profile/tag APIs |
| admin-config | `/admin/users`, `/admin/projects`, `/api-config`, `/data-source-config`, `/ab-experiments`, `/tag-config`, `/identity-types`, `/tag-import`, `/mq-config`, `/event-config`, `/webhook-subscriptions`, `/api-docs`, `/system-options`, `/test-users`, `/ai-predictions` | admin/config/project/tag/event/system/test-user/AI prediction pages and services | admin, config, project, tag, event, system option, test user APIs |
| tenant-admin | `/admin/tenants` | `frontend/src/pages/tenant-admin` | tenant admin APIs |

## 通用 Goal 模板

把 `ASSIGNED_MODULE`、`MODULE_SLUG`、`MODULE_ROUTES`、`FRONTEND_SCOPE`、`BACKEND_SCOPE` 替换成具体模块即可。

```text
Goal: For ASSIGNED_MODULE, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for MODULE_ROUTES. Record findings, fix only bugs owned by ASSIGNED_MODULE when evidence is sufficient, verify, and continue until this module is completed or genuinely blocked.

First confirm the current workspace state with `git status --short`. Do not revert user changes. Do not ask me to run `cd`, create a worktree, or create a branch. Stay in the current repository workspace.

Keep this window isolated. Only write these progress/report files: `tmp/module-quality-MODULE_SLUG-progress.md` and `docs/e2e-browser-audits/MODULE_SLUG.md`. Do not edit other module reports. Do not test routes outside MODULE_ROUTES except for login/navigation steps required to reach MODULE_ROUTES. Do not modify code outside FRONTEND_SCOPE and BACKEND_SCOPE unless a bug cannot be fixed without crossing the boundary; in that case, record it as `needs coordination` instead of editing. If a required fix touches shared files such as `frontend/src/App.tsx`, shared layout, shared auth, shared HTTP client, shared backend security, or shared migrations, record it as `needs coordination` unless this goal explicitly gives ownership of that shared file.

Read `docs/prompt/codex-browser-e2e-prompt.md`, `docs/e2e-browser-audit.md`, and `frontend/src/App.tsx`. Confirm MODULE_ROUTES still exist.

Code review phase:
1. Review current uncommitted diff inside FRONTEND_SCOPE and BACKEND_SCOPE. If there is no relevant diff, review the current implementation for those scopes.
2. Trace necessary callers and tests with `rg`; do not expand into unrelated modules.
3. Prioritize real bugs, behavior regressions, permission risks, tenant/data isolation risks, API contract breaks, error handling gaps, and missing tests.
4. Every finding must include file, line, risk, trigger, impact, evidence, recommendation, confidence, and status.
5. Style preferences and broad refactors are not findings unless they create a concrete risk.

Browser E2E phase:
1. Start or reuse local services as needed: frontend Vite, backend Spring Boot, and local docker dependencies.
2. If ports are busy, use the actual available ports and record them.
3. For every route in MODULE_ROUTES, verify with Browser: page opens, no blank screen, no unexpected ErrorBoundary, no unexpected console error, no unexpected 4xx/5xx network error, main content renders, important controls can be used, layout has no obvious overlap/overflow, and refresh still works.
4. For dynamic route params, first find real test data through existing UI, API, seed data, or local database state. If real data is unavailable, mark that route `blocked` and write exactly what data or permission is missing. Do not fake a route parameter and count 404/empty state as passed.

Fixing rules:
1. Fix only findings owned by ASSIGNED_MODULE and inside FRONTEND_SCOPE/BACKEND_SCOPE.
2. Do not do unrelated refactors.
3. Do not modify applied Flyway migrations.
4. Do not revert user changes.
5. Add or update focused tests when the fix changes behavior, compatibility contracts, shared logic, persistence, migrations, regressions, or non-obvious validation.
6. Run the smallest useful verification, then use Browser to regress the affected route.

Report file `docs/e2e-browser-audits/MODULE_SLUG.md` must contain:
- module, current branch, status
- reviewed files and commands
- code review findings
- route progress
- Browser evidence
- bugs found and fixed
- files changed
- verification commands and results
- Browser regression result
- blocked routes
- needs coordination
- remaining risks

Report final status with: module, current branch, routes tested, routes passed, routes blocked, review findings, bugs fixed, files changed, verification commands, Browser regression result, needs coordination, and remaining risks.
```

## 可复制 Goals

### public-auth

```text
Goal: For public-auth, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/login`, `/public/forms/:publicKey`, and `/bi/embed/:resourceType/:resourceKey`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-public-auth-progress.md` and `docs/e2e-browser-audits/public-auth.md`. Primary frontend scope: `frontend/src/pages/login`, `frontend/src/pages/public-marketing-form`, `frontend/src/pages/bi/embed.tsx`, `frontend/src/auth`, `frontend/src/context/AuthContext.tsx`, and directly related services/types. Primary backend/API scope: auth/session/public form/embed ticket APIs in `backend/canvas-boot` and `backend/canvas-web`.

Code review focus: auth redirects, anonymous access, public form validation, BI embed ticket handling, token/cookie handling, API error handling, and tests for failed auth or missing data. Browser checks: blank screen, ErrorBoundary, console errors, network errors, content rendering, form/embed behavior, refresh behavior, and expected auth behavior. For `:publicKey`, `:resourceType`, and `:resourceKey`, find real local test data; if unavailable, mark blocked with the exact missing data. Fix only bugs owned by public-auth, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### home-ops-collaboration

```text
Goal: For home-ops-collaboration, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/home`, `/ops`, `/approvals`, and `/conversations`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-home-ops-collaboration-progress.md` and `docs/e2e-browser-audits/home-ops-collaboration.md`. Primary frontend scope: `frontend/src/pages/home`, `frontend/src/pages/ops-dashboard`, `frontend/src/pages/approvals`, `frontend/src/pages/conversations`, plus shared notification/context/layout code only when a bug on these routes proves it is necessary. Primary backend/API scope: ops, approval, conversation, notification, and collaboration APIs.

Code review focus: dashboard data loading, approval actions, conversation inspection, notification side effects, loading/error/empty states, and role behavior. Browser checks: blank screen, ErrorBoundary, console errors, network errors, content rendering, filters/actions/modals, conversation panels, refresh behavior, and expected role behavior. Fix only bugs owned by home-ops-collaboration, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### canvas-journey

```text
Goal: For canvas-journey, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/canvas`, `/canvas/:id/edit`, `/canvas/:id/stats`, and `/canvas/:id/users`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-canvas-journey-progress.md` and `docs/e2e-browser-audits/canvas-journey.md`. Primary frontend scope: `frontend/src/pages/canvas-list`, `frontend/src/pages/canvas-editor`, `frontend/src/pages/canvas-stats`, `frontend/src/pages/canvas-users`, `frontend/src/components/canvas`, `frontend/src/components/node-panel`, `frontend/src/components/config-panel`, canvas hooks/types/services. Primary backend/API scope: canvas, execution, canvas stats, and canvas user APIs.

Code review focus: canvas list/project filters, editor graph state, autosave/import/export, node config validation, stats/user detail data loading, execution APIs, and tests around graph helpers and route data. Browser checks: list loading, editor loading, canvas rendering, node/config panel basics, stats rendering, user list rendering, blank screen, ErrorBoundary, console/network errors, layout overlap, refresh behavior, and navigation back to list. For `:id`, find a real local canvas id through list/API/seed data; if unavailable, mark blocked with the exact missing data. Fix only bugs owned by canvas-journey, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### marketing-suite

```text
Goal: For marketing-suite, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/marketing-monitoring`, `/mautic-insights`, `/marketing-platform`, `/search-marketing`, `/risk`, `/growth-activities`, `/marketing-preferences`, `/marketing-forms`, `/content-hub`, `/message-templates`, `/message-deliveries`, `/channel-connectors`, and `/demo-sandbox`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-marketing-suite-progress.md` and `docs/e2e-browser-audits/marketing-suite.md`. Primary frontend scope: corresponding `frontend/src/pages/*` directories for the assigned routes and matching marketing/risk/channel/message/content/form services. Primary backend/API scope: marketing, risk, content, form, message, delivery, channel connector, monitoring, search marketing, growth activity, and demo sandbox APIs.

Code review focus: production operation flows, filters/search, create/edit forms, delivery/channel state transitions, risk decision handling, content/form validation, API errors, and tests for failure paths. Browser checks: blank screen, ErrorBoundary, console/network errors, main tables/cards/forms, filter/search behavior, modal open/close, create/edit forms where safe, layout overflow, and refresh behavior. Fix only bugs owned by marketing-suite, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### bi-analytics

```text
Goal: For bi-analytics, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/bi` and `/analytics`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-bi-analytics-progress.md` and `docs/e2e-browser-audits/bi-analytics.md`. Primary frontend scope: `frontend/src/pages/bi`, `frontend/src/pages/analytics`, BI/analytics services and related types. Primary backend/API scope: BI and analytics APIs.

Code review focus: dashboard/resource selection, runtime route parameters, query/filter state, chart/table rendering contracts, subscription/export side effects, API error handling, and BI/analytics test coverage. Browser checks: workbench loading, dashboard/resource selection, query/filter controls, chart/table rendering, runtime route behavior, blank screen, ErrorBoundary, console/network errors, layout overflow, and refresh behavior. Fix only bugs owned by bi-analytics, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### cdp-audience

```text
Goal: For cdp-audience, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/cdp/users`, `/cdp/users/:userId`, `/audiences`, `/audiences/new`, `/audiences/:id/edit`, `/cdp/computed-profile`, `/cdp/computed-tags`, and `/cdp/realtime-audiences`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-cdp-audience-progress.md` and `docs/e2e-browser-audits/cdp-audience.md`. Primary frontend scope: `frontend/src/pages/cdp-users`, `frontend/src/pages/cdp-user-detail`, `frontend/src/pages/audience-list`, `frontend/src/pages/audience-edit`, `frontend/src/pages/cdp-computed-profile`, `frontend/src/pages/cdp-computed-tags`, `frontend/src/pages/realtime-audiences`, CDP/audience services and types. Primary backend/API scope: CDP, user profile, audience, computed profile/tag, and realtime audience APIs.

Code review focus: tenant/user isolation, audience rule construction, snapshot mode, computed profile/tag data loading, realtime audience operations, form validation, dynamic id handling, and tests for boundary values. Browser checks: list/detail loading, audience creation/editing basics, filters, tables, forms, dynamic id routes, blank screen, ErrorBoundary, console/network errors, layout overflow, and refresh behavior. For `:userId` and `:id`, find real local test data; if unavailable, mark blocked with the exact missing data. Fix only bugs owned by cdp-audience, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### admin-config

```text
Goal: For admin-config, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/admin/users`, `/admin/projects`, `/api-config`, `/data-source-config`, `/ab-experiments`, `/tag-config`, `/identity-types`, `/tag-import`, `/mq-config`, `/event-config`, `/webhook-subscriptions`, `/api-docs`, `/system-options`, `/test-users`, and `/ai-predictions`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-admin-config-progress.md` and `docs/e2e-browser-audits/admin-config.md`. Primary frontend scope: corresponding admin/config/project/tag/event/system/test-user/AI prediction page directories and matching services. Primary backend/API scope: admin user, project, API config, data source config, AB experiment, tag, identity, import, MQ, event, webhook, API docs, system option, test user, and AI prediction APIs.

Code review focus: admin role checks, config validation, project and tag data contracts, import flows, event/MQ schemas, webhook safety, system option editing, API docs accuracy, test-user flows, and tests for permission/failure paths. Browser checks: admin route access, list/table loading, filter/search, form validation, modal open/close, safe create/edit previews where available, blank screen, ErrorBoundary, console/network errors, layout overflow, and refresh behavior. If admin permission is unavailable, mark affected routes blocked with the exact missing account/role. Fix only bugs owned by admin-config, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

### tenant-admin

```text
Goal: For tenant-admin, use the current local branch and current workspace directly. Do not create a git worktree. Do not create a new branch. First perform code review for this module, then use the Codex in-app Browser to run E2E page testing only for `/admin/tenants`.

First run `git status --short` and do not revert user changes. Only write `tmp/module-quality-tenant-admin-progress.md` and `docs/e2e-browser-audits/tenant-admin.md`. Primary frontend scope: `frontend/src/pages/tenant-admin` and directly related admin/auth services. Primary backend/API scope: tenant admin APIs.

Code review focus: super admin guard, tenant isolation, tenant create/update validation, sensitive config handling, API errors, and tests for permission failures. Browser checks: super admin route access, tenant list/table loading, forms/modals where safe, blank screen, ErrorBoundary, console/network errors, layout overflow, and refresh behavior. If super admin permission is unavailable, mark blocked with the exact missing account/role. Fix only bugs owned by tenant-admin, run focused verification, Browser-regress the fixed route, and report status, current branch, review findings, routes tested, routes blocked, bugs, fixes, verification, needs coordination, and remaining risks.
```

