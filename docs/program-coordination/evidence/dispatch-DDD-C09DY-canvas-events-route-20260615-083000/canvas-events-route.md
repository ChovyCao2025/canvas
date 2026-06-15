# DDD-C09DY Canvas Events Route

## Scope

- Added final-context compatibility route `POST /canvas/events/report`.
- Kept `backend/canvas-engine/**` and all `pom.xml` files untouched.
- Added focused behavior compatibility coverage for raw JSON delegation, legacy envelope fields, deterministic report fields, and validation failure envelope.

## Files

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasEventReportFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasEventReportApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasEventReportController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasEventReportControllerCompatibilityTest.java`

## RED

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasEventReportControllerCompatibilityTest test
```

Result: failed at test compilation because `CanvasEventReportFacade` and `CanvasEventReportController` were missing.

## GREEN

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasEventReportControllerCompatibilityTest test
```

Result: passed. `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.

## Verification

```text
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
```

Result: passed. Reactor ended with `BUILD SUCCESS`.

```text
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result: command exited 0. Overall `cutoverReady=false` remains because repository-wide controller and endpoint counts are still below old `canvas-engine` counts; the reported top route gaps did not include `route:/canvas/events`.

```text
rg "org\.chovy\.canvas\.(engine|service|mapper|entity|common)" <touched final files>
```

Result: no matches.

```text
git diff --check -- <touched files>
```

Result: no whitespace errors reported.
