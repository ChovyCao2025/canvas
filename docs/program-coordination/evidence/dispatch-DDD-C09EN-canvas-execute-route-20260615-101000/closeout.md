# DDD-C09EN Canvas Execute Route Closeout

Date: 2026-06-15

## Scope

Investigated preflight candidate `route:/canvas/execute` by comparing old
`backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
with final execution web routes.

## Findings

Old `ExecutionController` exposed two `/canvas/execute` routes:

- `POST /canvas/execute/direct/{canvasId}`
- `POST /canvas/execute/dry-run/{canvasId}`

Final `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java`
already exposed `POST /canvas/execute/direct/{canvasId}` with compatibility
coverage. The remaining preflight candidate was a real gap for
`POST /canvas/execute/dry-run/{canvasId}`, not a false positive for the direct
route.

## Changes

- Added `POST /canvas/execute/dry-run/{canvasId}` to the final execution
  controller.
- The route accepts the old request shape including `inputParams` and
  `graphJson`, preserves the old compatibility envelope, defaults missing
  dry-run user identity to `system`, and delegates to the final
  `CanvasExecutionFacade` with `dryRun=true`.
- Added focused compatibility coverage proving the old dry-run path maps to a
  dry-run final command.

## Verification

Red check before implementation:

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ExecutionControllerCompatibilityTest test
=> failed as expected: POST /canvas/execute/dry-run/42 returned 404 NOT_FOUND
```

Green check after implementation:

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ExecutionControllerCompatibilityTest test
=> BUILD SUCCESS; Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

Later rerun of the same focused Maven command was blocked before test execution
by unrelated dirty BI compatibility test work:

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ExecutionControllerCompatibilityTest test
=> failed during canvas-web testCompile:
   BiCatalogControllerCompatibilityTest.RecordingBiCatalogFacade does not
   implement cleanupDatasetAcceleration(Long, String, int, String)
```

Production compile remained clean:

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -DskipTests compile
=> BUILD SUCCESS
```

Static route scan:

```text
rg -n "@PostMapping\\(\"/canvas/execute/(direct|dry-run)/\\{canvasId\\}\"\\)|uri\\(\"/canvas/execute/(direct|dry-run)/42\"\\)" \
  backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java \
  backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/ExecutionControllerCompatibilityTest.java
=> found final direct and dry-run mappings plus focused test requests
```

Preflight:

```text
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
=> current canvas-web 104 controllers / 825 endpoints
=> routeGapSummary candidates: route:/warehouse/readiness, route:/canvas/bi
=> route:/canvas/execute no longer reported
=> cutoverReady=false due unrelated controller count blocker
```

Old-coupling scan over touched final files:

```text
rg -n "backend/canvas-engine|org\\.chovy\\.canvas\\.(dal|domain|engine|security)|canvas-engine" \
  backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionController.java \
  backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/ExecutionControllerCompatibilityTest.java
=> no matches
```

## Result

Status: `DONE_WITH_CONCERNS`

`route:/canvas/execute` is no longer a real preflight route gap after this work.

Concern: the route-level compatibility gap is closed, but the final
`CanvasExecutionFacade` command model only exposes a `dryRun` flag; it does not
currently provide old engine `triggerDryRun(canvasId, userId, inputParams,
graphJson)` graph-json execution semantics through this web adapter.
