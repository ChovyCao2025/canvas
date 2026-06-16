# Admin Config Module Quality Progress

Date: 2026-06-16
Branch: main
Workspace: current local branch/workspace directly; no git worktree and no new branch.

## Initial State

- Ran `git status --short` first.
- Existing worktree had unrelated modified/untracked files outside admin-config scope. They were not reverted or edited.
- Requested output files did not exist before this pass:
  - `tmp/module-quality-admin-config-progress.md`
  - `docs/e2e-browser-audits/admin-config.md`

## Review Scope Covered

Frontend:
- `frontend/src/App.tsx`
- `frontend/src/auth/guards.tsx`
- `frontend/src/context/AuthContext.tsx`
- `frontend/src/auth/roles.ts`
- `frontend/src/services/api.ts`
- `frontend/src/services/systemOptions.ts`
- `frontend/src/services/aiPredictionApi.ts`
- Admin/config route directories for projects, API config, data source config, AB experiments, tags, identity types, imports, MQ, events, webhooks, API docs, system options, test users, and AI predictions by route/service search.

Backend/API:
- `backend/canvas-boot/pom.xml`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/platform/TestUserController.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java`
- Matching `canvas-web` compatibility controllers for API/data-source/MQ/event/tag/identity/import/webhook/AB paths.
- Matching `canvas-platform`, `canvas-context-canvas`, `canvas-context-marketing`, and `canvas-context-cdp` facades/application/domain modules.
- Legacy security reference: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`.

## Findings

1. High: current boot runtime does not include the legacy Spring Security/JWT module.
   - Evidence: `backend/canvas-boot/pom.xml` depends on `canvas-web`, context modules, platform, cache, starter, actuator, Flyway, and MySQL, but not `canvas-engine` or a shared security module.
   - Impact: admin/config API protection is not proven in the current runtime. The frontend routes are guarded, but direct API calls may depend only on whether another deployment layer blocks them.

2. High: even the legacy security matcher does not explicitly tenant-admin gate several requested admin-config APIs.
   - Legacy protected examples: `/canvas/data-sources/**`, `/canvas/api-definitions/**`, `/canvas/tag-import-sources/**`, `/cdp/webhooks/**` writes, `/canvas/ab-experiments/**` writes, `/admin/**`.
   - Missing from the inspected matcher: `/canvas/mq-definitions/**`, `/canvas/event-definitions/**`, `/canvas/tag-definitions/**`, `/canvas/identity-types/**`, `/canvas/tag-imports/**`, `/test-users/**`, and `/ai/predictions/recompute`.
   - Impact: if the legacy security config is restored as-is, these config/admin operations appear to fall through to authenticated-user access rather than admin-level access.

3. Medium: controller compatibility tests cover happy paths and default headers, but do not assert unauthenticated, non-admin, tenant-spoof, or failure permission paths for the requested admin/config APIs.
   - Evidence: `AdminPlatformControllerCompatibilityTest` asserts route shapes, default tenant/actor headers, explicit header forwarding, and bad request mapping.
   - Impact: permission regressions and header spoofing would not be caught by the focused compatibility tests.

4. Fixed: `/admin/system-options` frontend/runtime response contract mismatch.
   - Root cause: `SystemOptionsPage` consumes `res.data.list`, and `systemOptionsApi.adminList` was typed as `R<PageResult<SystemOption>>`, while the current `canvas-web` controller/facade returns `R<SystemOption[]>`.
   - Fix: `systemOptionsApi.adminList` now normalizes current list responses into `{ total, list }` while preserving future paged responses.
   - Regression: `frontend/src/services/systemOptions.test.ts`.

## Fixes Applied

- `frontend/src/services/systemOptions.ts`
  - Added response normalization for current `R<SystemOption[]>` admin-list responses.
- `frontend/src/services/systemOptions.test.ts`
  - Added regression proving current runtime list responses become page results for the page layer.

## Verification

Passed:
- `npm run test -- --run src/services/systemOptions.test.ts src/services/projectApi.test.ts src/services/aiPredictionApi.test.ts src/pages/api-docs/apiDocs.test.ts src/pages/api-docs/openApiDocs.test.ts src/pages/tag-config/tagConfigPayload.test.ts src/pages/event-config/eventAttributeReview.test.ts src/pages/ai-predictions/aiPredictions.test.tsx src/pages/webhook-subscriptions/webhookSubscriptions.test.ts`
  - 9 files, 38 tests passed.
- `npm run build`
  - TypeScript and Vite production build passed.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-canvas,canvas-context-marketing,canvas-context-cdp,canvas-platform,canvas-web -am -Dtest=ApiDefinitionApplicationServiceTest,DataSourceConfigApplicationServiceTest,MqDefinitionApplicationServiceTest,EventDefinitionApplicationServiceTest,AbExperimentApplicationServiceTest,TagImportApplicationServiceTest,TagImportSourceApplicationServiceTest,CdpIdentityTypeApplicationServiceTest,CdpTagDefinitionApplicationServiceTest,CdpWebhookApplicationServiceTest,AdminPlatformApplicationServiceTest,TestUserApplicationServiceTest,AiApplicationServiceTest,AdminPlatformControllerCompatibilityTest,DataSourceConfigControllerCompatibilityTest,ApiDefinitionControllerCompatibilityTest,EventDefinitionControllerCompatibilityTest,MqDefinitionControllerCompatibilityTest,AbExperimentControllerCompatibilityTest,TagImportControllerCompatibilityTest,TagImportSourceControllerCompatibilityTest,CdpIdentityTypeControllerCompatibilityTest,CdpTagDefinitionControllerCompatibilityTest,CdpWebhookControllerCompatibilityTest,AiControllerCompatibilityTest,TestUserControllerCompatibilityTest test`
  - 77 focused tests passed across admin/config context, platform, and web modules.

Known unrelated broader failure:
- Broad backend reactor command with Java 21 failed in `MauticInsightApplicationServiceTest` with 4 compatibility expectation failures. This is outside the requested admin-config scope.

Environment note:
- Default `java`/`mvn` initially used Java 8 and failed on Java 21-compiled test classes. Java 21 is installed at `/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`; focused backend verification used `JAVA_HOME=$(/usr/libexec/java_home -v 21)`.

## Browser E2E Status

Blocked for the requested Codex in-app Browser run:
- Browser setup against required `iab` target returned `Browser is not available: iab`.
- `agent.browsers.list()` returned `[]`.
- Vite dev server was started at `http://127.0.0.1:3001/` because port 3000 was already in use.
- Second continuation retry on 2026-06-16 still returned `Browser is not available: iab`; `agent.browsers.list()` still returned `[]`.
- Third consecutive goal-turn retry on 2026-06-16 still returned `Browser is not available: iab` with `browserTargets: []`.

Routes blocked by in-app Browser unavailability:
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

No admin account/role blocker was reached. The blocker is the missing in-app Browser target, not missing admin permission.

Blocked audit status:
- Same blocker has now repeated for three consecutive goal turns: the Codex in-app Browser target `iab` is unavailable and the browser target list is empty.
- Meaningful remaining progress requires the in-app Browser target to become available, because the uncompleted requirement is Browser E2E page testing for the specified routes.

## Needs Coordination

- Decide whether current `canvas-boot` should own Spring Security/JWT directly or depend on a shared security module extracted from legacy `canvas-engine`.
- Add explicit admin/tenant-admin gates and tests for all requested config/admin API paths, not only `/admin/**`.
- Confirm whether tenant and actor should come from JWT/security context instead of client-controlled `X-Tenant-Id` and `X-Actor` headers in current compatibility controllers.
- Re-run the requested in-app Browser E2E audit once the Codex Browser `iab` target is available.

## Remaining Risks

- Browser E2E coverage is not complete because the required in-app Browser target is unavailable.
- Direct API authorization remains the largest unresolved risk.
- Some admin/config pages rely on real backend shape drift; system options is fixed, but Browser testing is still needed to catch similar runtime page-contract issues.
