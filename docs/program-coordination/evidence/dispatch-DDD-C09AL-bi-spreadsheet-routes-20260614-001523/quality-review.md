# DDD-C09AL Quality Review

Date: 2026-06-14
Reviewer: `Arendt 019ec1e4-0dfa-75c1-a8af-e0d6e2f15c4b`
Review status: `PASS_WITH_CONCERNS`

## Scope

Read-only review of recovered DDD-C09AL spreadsheet lifecycle output.

## Findings

Arendt found no required fixes for the stated DDD-C09AL scope.

Concerns:

- `BiApiCompatibilityTest` did not directly assert spreadsheet routes.
- Legacy `GET /canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions`
  accepted a `limit` query parameter; the recovered route initially returned
  all versions.

## Coordinator Resolution

Both concerns were addressed before closeout:

- `BiApiCompatibilityTest` now covers the spreadsheet lifecycle route family
  through its aggregate compatibility adapter.
- `GET /canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions` now accepts
  optional `limit` and limits the returned compatibility response.

Residual accepted risks:

- Spreadsheet lifecycle remains compact in-memory final-module compatibility
  behavior, not durable old-engine persistence parity.
- Version snapshot semantics remain compact seed behavior and are not claimed as
  full old spreadsheet service parity.

## Verification After Resolution

Passed:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
```

Result summary:

```text
BiCatalogApplicationServiceTest: 30/30
BiApiCompatibilityTest: 14/14
BiCatalogControllerCompatibilityTest: 24/24
Total focused tests: 68/68
```

Passed:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result summary:

```text
current canvas-web: 15 controllers / 105 endpoints
route:/canvas/bi: 1 current controller / 65 endpoints
cutoverReady: false
```

Strict old-coupling scan returned no matches:

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO|BiSpreadsheetResourceService|BiSpreadsheetVersionView" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
```
