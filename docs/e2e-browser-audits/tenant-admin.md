# Tenant Admin Browser Audit

Date: 2026-06-16
Route: `/admin/tenants`
Branch: `main`

## Status

Blocked. The Codex in-app Browser could not be attached in this session.

Setup result:

```text
Browser is not available: iab
```

Continuation retry on 2026-06-16 produced the same setup result:

```text
Browser is not available: iab
```

Second continuation retry on 2026-06-16 produced the same setup result:

```text
Browser is not available: iab
```

This is the third consecutive goal turn with the same missing in-app Browser `iab` surface.

No substitute browser was used because the requested verification was specifically "Codex in-app Browser" E2E testing only for `/admin/tenants`.

## Routes Tested

None.

## Routes Blocked

- `/admin/tenants`

## Requested Checks Not Completed

- Super admin route access.
- Tenant list/table loading.
- Forms/modals where safe.
- Blank screen detection.
- ErrorBoundary detection.
- Console errors.
- Network errors.
- Layout overflow.
- Refresh behavior.

## Super Admin Permission

Not reached. The blocker is the unavailable in-app Browser surface, not a confirmed missing super-admin account or role.

Exact missing account/role: not determined.

## Static Review Signals Relevant To E2E

- Frontend route guard exists: `/admin/tenants` is mounted under `RequireSuperAdmin` in `frontend/src/App.tsx:223`.
- Tenant menu visibility is limited by `canManageTenants`, which maps to super-admin-compatible roles in `frontend/src/auth/roles.ts:15`.
- Backend boot controller exposes `/admin/tenants` routes without an observed API-side role guard in `backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java:171`.
- Usage UI expects fields that the backend does not return, so a completed Browser usage check would likely show `undefined` for published/execution/DLQ counters after clicking "查看用量".

## Verification Commands Completed Outside Browser

- `cd frontend && npm run test -- --run src/components/layout/AppLayout.a11y.test.tsx src/components/layout/AppLayout.responsive.test.tsx src/services/api.test.ts`
- `cd frontend && npm run build`
- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-platform -am -Dtest=AdminPlatformControllerCompatibilityTest,AdminPlatformApplicationServiceTest test`

All three completed successfully.

## Follow-Up Needed

Re-run this audit after the in-app Browser `iab` surface is available. Use a super-admin-compatible session (`SUPER_ADMIN` or rollout-compatible `ADMIN`) and a tenant-admin or operator session for negative route access checks.

Blocked-goal audit note: the strict three-turn threshold has now been met for the same in-app Browser availability blocker. The persistent goal is blocked until the Codex in-app Browser `iab` surface is available or the Browser requirement is changed.
