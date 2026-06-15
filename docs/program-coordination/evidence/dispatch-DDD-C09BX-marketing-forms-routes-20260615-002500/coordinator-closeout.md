# DDD-C09BX Coordinator Closeout

Date: 2026-06-15
Dispatch: `dispatch-DDD-C09BX-marketing-forms-routes-20260615-002500`
Status: DONE_WITH_CONCERNS

## Changes

- Added marketing-context `MarketingFormFacade`, `MarketingFormApplicationService`, and deterministic `MarketingFormCatalog`.
- Added final-module `MarketingFormController` for the six legacy `/canvas/marketing-forms` management endpoints.
- Left `/public/marketing-forms/**` out of scope because `PublicIngressController` already owns those routes.
- Kept tests behavior-focused: six route shapes, compatibility envelope, header/default mapping, payload forwarding, tenant-scoped ids, create/update/status behavior, submission filtering/limits, JSON validation, and bad-request envelope.
- Resolved worker/coordinator same-file API-shape conflict by stopping the worker and aligning all six files to the final source contract.

## Verification

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingFormApplicationServiceTest
```

Passed: `MarketingFormApplicationServiceTest` 4/4.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingFormControllerCompatibilityTest test
```

Passed: `MarketingFormControllerCompatibilityTest` 4/4.

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
```

Passed: reactor production compile through `canvas-web`.

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Passed as a diagnostic command; global cutover remains false. Current `canvas-web` is 43 controllers / 616 endpoints, and `route:/canvas/marketing-forms` is no longer in the reported top gaps. Next top gap is `route:/canvas/mautic-insights`.

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain|TenantContext|MarketingFormService|AccessDeniedException" <DDD-C09BX production files> || true
git diff --check -- <DDD-C09BX scoped files plus coordination files>
node tools/program-coordination/check-dispatch-state.mjs .
```

Passed: strict old-coupling scan had no matches, scoped diff check had no whitespace errors, and dispatch-state validation passed.

## Accepted Concerns

- Compact deterministic route compatibility seed only; durable old `MarketingFormService` persistence and submit-action behavior remain out of scope.
- Public marketing form routes remain delegated to `PublicIngressController`.
- DDD-C09 final cutover remains blocked by global route parity.
