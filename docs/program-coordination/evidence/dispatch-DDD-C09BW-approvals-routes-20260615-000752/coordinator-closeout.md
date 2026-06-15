# DDD-C09BW Coordinator Closeout

Date: 2026-06-15
Dispatch: `dispatch-DDD-C09BW-approvals-routes-20260615-000752`
Status: DONE_WITH_CONCERNS

## Changes

- Added `canvas-platform` approval facade/application/catalog seed for legacy `/approvals` route compatibility.
- Added `canvas-web` `ApprovalController` with the six legacy approval endpoints and compatibility envelopes.
- Kept tests behavior-focused: route/envelope compatibility, default/header mapping, state transitions, tenant-scoped id collision, admin-only Lark sync, and forbidden/bad-request envelopes.
- Fixed meaningful review findings: tenant-scoped duplicate id lookup, same-role decision authorization, instance `id` alias, and `AUTH_003` forbidden envelope.

## Verification

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=ApprovalApplicationServiceTest
```

Passed: `ApprovalApplicationServiceTest` 3/3.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ApprovalControllerCompatibilityTest test
```

Passed: `ApprovalControllerCompatibilityTest` 3/3.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
```

Passed: reactor production compile through `canvas-web`.

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Passed as a diagnostic command; global cutover remains false. Current `canvas-web` is 42 controllers / 610 endpoints, and `route:/approvals` is no longer in the reported top gaps. Next top gap is `route:/canvas/marketing-forms`.

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain|TenantContext|ApprovalWorkflowService|AccessDeniedException" <DDD-C09BW production files> || true
git diff --check -- <DDD-C09BW scoped files plus coordination files>
node tools/program-coordination/check-dispatch-state.mjs .
```

Passed: strict old-coupling scan had no matches, scoped diff check had no whitespace errors, and dispatch-state validation passed.

## Accepted Concerns

- Compact deterministic approval compatibility seed only; durable workflow/audit/external-provider parity remains out of scope.
- Legacy tenant-context resolver behavior is not fully replicated in this route seed.
- DDD-C09 final cutover remains blocked by global route parity.
