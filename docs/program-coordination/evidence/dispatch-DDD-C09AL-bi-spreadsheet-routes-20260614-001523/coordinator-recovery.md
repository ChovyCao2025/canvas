# DDD-C09AL Coordinator Recovery

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AL-bi-spreadsheet-routes-20260614-001523`
Task: `DDD-C09AL`

## Worker State

`multi_agent_v1.spawn_agent` created real code-writing worker
`Schrodinger 019ec1ca-a93c-70b2-833f-d5fa3b704b42` before the dispatch was
moved to `RUNNING`.

The coordinator waited once. `multi_agent_v1.wait_agent` timed out with no
normal worker return packet. Per protocol, the coordinator inspected changed
paths, evidence, and focused tests instead of repeatedly waiting.

`multi_agent_v1.close_agent 019ec1ca-a93c-70b2-833f-d5fa3b704b42` returned
previous status `running`.

## Recovered Issues

Focused verification initially exposed two exact-scope issues:

- `BiCatalogApplicationServiceTest.spreadsheetResourceLifecycleScopesByTenantNormalizesKeysAndRestoresVersions`
  failed because nested spreadsheet sheet metadata copied `"sheetKey"` with
  surrounding whitespace.
- `BiCatalogControllerCompatibilityTest.spreadsheetLifecycleRoutesPreserveEnvelopeDefaultsAndPathKeys`
  failed with `404 NOT_FOUND` for
  `POST /canvas/bi/spreadsheets/resources/revenue-sheet/draft` because the
  spreadsheet route mappings were not present in `BiCatalogController`.

Coordinator recovery:

- normalized nested spreadsheet sheet `sheetKey` values in
  `BiPresentationResourceCatalog` while preserving other sheet metadata.
- added the seven spreadsheet lifecycle mappings to `BiCatalogController`.
- added `SpreadsheetDraftRequest` to adapt request bodies into
  `BiSpreadsheetResourceCommand`.

## Verification

Passed:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
```

Surefire summaries:

```text
BiCatalogApplicationServiceTest: Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
BiApiCompatibilityTest: Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
BiCatalogControllerCompatibilityTest: Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
```

Passed:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result summary:

```text
current canvas-web: 15 controllers / 105 endpoints
route:/canvas/bi: 1 current controller / 65 current endpoints
cutoverReady: false
blockers remain global route parity, not DDD-C09AL-specific failures
```

Strict old-coupling scan passed with no matches:

```bash
rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO|BiSpreadsheetResourceService|BiSpreadsheetVersionView" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
```

The literal packet scan that also includes `BiSpreadsheetResource` reports only
the new final-module API names added by this dispatch. It does not report old
service/domain/persistence coupling.

Scoped diff whitespace check passed:

```bash
git diff --check -- backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSpreadsheetResourceCommand.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSpreadsheetResourceView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceVersionView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java docs/program-coordination/dispatch-state.json docs/program-coordination/progress-ledger.md docs/program-coordination/subagent-worker-packets.md
```

## Review State

Read-only reviewer `Arendt 019ec1e4-0dfa-75c1-a8af-e0d6e2f15c4b` was spawned
after coordinator verification. The reviewer is intentionally treated as
background work; the coordinator continued non-overlapping evidence and state
maintenance instead of idling on a long wait.

Arendt returned `PASS_WITH_CONCERNS`. The two concerns were resolved before
closeout:

- aggregate `BiApiCompatibilityTest` now exercises the spreadsheet lifecycle
  routes.
- `GET /canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions` accepts
  optional `limit` and limits the returned compatibility response.

Post-resolution focused verification passed 68/68:

```text
BiCatalogApplicationServiceTest: 30/30
BiApiCompatibilityTest: 14/14
BiCatalogControllerCompatibilityTest: 24/24
```

## Risks

- No normal Schrodinger worker return packet exists.
- The spreadsheet lifecycle remains a compact in-memory compatibility seed, not
  durable persistence or full legacy spreadsheet-service parity.
- Global DDD-C09 final cutover remains blocked by broader route parity.

## Rollback

Revert only the exact DDD-C09AL reserved BI API/domain/application/controller and
BI test files listed in the worker packet.
