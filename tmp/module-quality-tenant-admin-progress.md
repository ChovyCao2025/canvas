# Tenant Admin Module Quality Progress

Date: 2026-06-16
Branch: `main`
Workspace: `/Users/photonpay/project/canvas`

## Git Status Baseline

`git status --short` was run first. Existing unrelated changes were present and were not reverted or edited:

- Modified: `.github/workflows/canvas-ci.yml`, `.github/workflows/ci.yml`, `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`, Helm/deploy docs/scripts/tools files.
- Untracked: `docs/e2e-browser-audit.md`, `docs/prompt/`.

This pass only wrote:

- `tmp/module-quality-tenant-admin-progress.md`
- `docs/e2e-browser-audits/tenant-admin.md`

## Scope Reviewed

Frontend:

- `frontend/src/pages/tenant-admin/index.tsx`
- `frontend/src/services/api.ts`
- `frontend/src/auth/guards.tsx`
- `frontend/src/auth/roles.ts`
- `frontend/src/context/AuthContext.tsx`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/AppLayout.tsx`

Backend/API:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AdminPlatformApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/admin/AdminPlatformControllerCompatibilityTest.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AdminPlatformApplicationServiceTest.java`

## Review Findings

### High: Boot tenant-admin APIs lack an API-side super-admin guard

Evidence:

- `frontend/src/App.tsx:223` wraps `/admin/tenants` in `RequireSuperAdmin`, and `frontend/src/auth/roles.ts:15` limits `canManageTenants` to super-admin roles.
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java:171` through `199` expose list/create/disable/activate/usage without role headers, JWT context, or a permission check.
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/admin/AdminPlatformControllerCompatibilityTest.java:44` through `48` assert successful route shape, but there is no permission-failure test for tenant-admin or operator roles in this module.

Impact: A caller that can reach the boot API directly can invoke tenant control-plane operations if no upstream security filter blocks it. This contradicts the frontend route contract and the legacy engine route-test intent that tenant admins cannot access tenant administration.

Recommended fix when code edits are allowed: add a boot-runtime API authorization layer for `/admin/tenants/**` that requires `SUPER_ADMIN` or rollout-compatible `ADMIN`, then add controller/filter tests proving `TENANT_ADMIN`, `OPERATOR`, missing auth, and malformed auth are denied.

### High: Tenant create validation is incomplete

Evidence:

- UI requires `name` and `tenantKey` at `frontend/src/pages/tenant-admin/index.tsx:132` and `135`.
- Backend validates only `name` at `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java:165` through `169`.
- Missing `tenantKey` is silently replaced with `tenant-<size>`, duplicates are not rejected, and `planCode` is uppercased without allow-list validation.

Impact: API callers can create tenants that violate the UI contract, collide by key, or carry unsupported plans.

Recommended fix when code edits are allowed: validate required `tenantKey`, normalize and enforce a stable key pattern, reject duplicate keys case-insensitively, and validate `planCode` against supported plan codes.

### Medium: `quotaJson` is accepted and returned without JSON validation or sensitive-field controls

Evidence:

- Create stores raw `quotaJson` at `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java:170`.
- Tenant list returns full tenant maps at `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java:161`.
- UI accepts free-form quota JSON at `frontend/src/pages/tenant-admin/index.tsx:141`.

Impact: Invalid quota data can be persisted in the compatibility catalog. If future quota payloads include credentials or provider-specific secrets, the list API will echo them back.

Recommended fix when code edits are allowed: parse `quotaJson` as JSON, store quota as structured data, reject invalid JSON with a 400 envelope, and explicitly redact or reject sensitive keys.

### Medium: Tenant usage API response does not match frontend contract

Evidence:

- Frontend displays `publishedCanvasCount`, `executionCount`, and `dlqCount` at `frontend/src/pages/tenant-admin/index.tsx:76` through `78`.
- Backend returns only `tenantId`, `userCount`, `projectCount`, and `canvasCount` at `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java:190` through `193`.

Impact: Clicking "查看用量" renders `undefined` for published/execution/DLQ counters instead of useful values.

Recommended fix when code edits are allowed: align `TenantUsage` and backend data. Either return the expected counters with zero defaults or adjust the frontend to display the current backend fields.

### Medium: API errors can leave tenant-admin actions without user feedback

Evidence:

- `fetchTenants` uses `try/finally` but no `catch` message or local error state at `frontend/src/pages/tenant-admin/index.tsx:18` through `25`.
- `handleCreate`, `changeStatus`, and `loadUsage` await API calls without user-facing error handling at `frontend/src/pages/tenant-admin/index.tsx:30` through `55`.

Impact: list failures can clear loading and leave an empty table with no explanation; create/status/usage failures depend on global console behavior rather than a page-level message.

Recommended fix when code edits are allowed: catch `ApiBusinessError`/classified HTTP errors, show `message.error`, and preserve existing table data when refresh fails.

## Fixes Applied

No production code fixes were applied because this goal explicitly constrained writes to the two report files above. Tenant-admin-owned fixes remain recommended.

## Verification

Passed:

- `cd frontend && npm run test -- --run src/components/layout/AppLayout.a11y.test.tsx src/components/layout/AppLayout.responsive.test.tsx src/services/api.test.ts`
  - 3 files passed, 7 tests passed.
- `cd frontend && npm run build`
  - TypeScript and Vite production build passed.
- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-platform -am -Dtest=AdminPlatformControllerCompatibilityTest,AdminPlatformApplicationServiceTest test`
  - 8 backend tests passed.

Initial backend verification environment issue:

- The same Maven command failed under default Java 8 with `无效的标记: --release`.
- `java -version` showed `openjdk version "1.8.0_482"`.
- JDK 21 is available at `/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`, and the rerun passed with that `JAVA_HOME`.

## Browser E2E Status

Blocked in this session: Codex in-app Browser runtime reported `Browser is not available: iab` during setup. No substitute browser was used because the objective requires the Codex in-app Browser.

Continuation retry on 2026-06-16: repeated the mandated in-app Browser setup and received the same result:

```text
Browser is not available: iab
```

Second continuation retry on 2026-06-16: repeated the mandated in-app Browser setup again and received the same result:

```text
Browser is not available: iab
```

Blocked-goal status: this is the third consecutive goal turn with the same missing Codex in-app Browser `iab` surface. The goal is now marked blocked because no meaningful Browser E2E progress can be made without that external Browser surface becoming available or the Browser requirement being changed.

Routes tested with Browser: none.

Routes blocked:

- `/admin/tenants`

Requested Browser checks not completed because of the unavailable in-app Browser:

- Super admin route access.
- Tenant list/table loading.
- Forms/modals where safe.
- Blank screen and ErrorBoundary behavior.
- Console/network errors.
- Layout overflow.
- Refresh behavior.

Super-admin account/role availability was not reached; the blocker is the missing in-app Browser surface, not a confirmed missing account.

## Needs Coordination

- Enable or expose the Codex in-app Browser `iab` surface for this session, or provide an alternate instruction allowing non-in-app Playwright.
- Provide or confirm a safe local super-admin session fixture if Browser testing should use real login instead of localStorage seeding.
- Decide whether the write constraint should be relaxed so tenant-admin-owned code and tests can be fixed.
- Retry Browser audit once the Codex in-app Browser `iab` surface is available, or provide alternate instructions allowing a non-in-app Playwright/browser path. The strict blocked-goal threshold has now been met on the Browser availability blocker.

## Remaining Risks

- API-side tenant administration authorization remains unproven and appears missing in boot-runtime code.
- Permission-failure tests for `/admin/tenants` are missing in the reviewed boot module.
- Tenant creation accepts weak/inconsistent data outside the UI path.
- Usage counters are incompatible between frontend and backend.
- Page-level API failure UX is thin.
