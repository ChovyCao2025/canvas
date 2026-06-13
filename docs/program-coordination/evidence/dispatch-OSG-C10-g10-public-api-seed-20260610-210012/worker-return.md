# OSG-C10 Coordinator Return

status: DONE
task id: OSG-C10
dispatch id: dispatch-OSG-C10-g10-public-api-seed-20260610-210012
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 with uncommitted coordinator worktree changes

## Files Changed

- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/CanvasDslDocument.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMappingService.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapper.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidationResult.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidator.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/DslJsonSupport.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/TemplateImportRequest.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/TemplateImportResult.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/TemplateImportService.java
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/template/TemplateImportServiceTest.java
- backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java

## Contracts Changed

- Added `canvas/v1` public DSL document records under `canvas-context-canvas` API.
- Added DSL validation and graph JSON mapping application APIs.
- Added template import request/result/service surface that validates required plugins before draft creation.
- Added `canvas-web` DSL validate/map controller compatibility surface without importing mapper-named classes in web.

## Tests Run

- RED, before implementation: canvas/web G10 tests failed at test compile because `CanvasDslDocument`, `CanvasDslValidator`, `CanvasDslValidationResult`, `CanvasDslMapper`, `TemplateImportService`, `TemplateImportRequest`, `TemplateImportResult`, and `CanvasDslController` were missing.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest`
  - Result: passed; 6 tests.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasDslControllerCompatibilityTest`
  - Result: passed; 3 tests; used to confirm the isolated `-pl canvas-web` failure was stale local artifact resolution.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -pl canvas-context-canvas -DskipTests`
  - Result: passed; refreshed local canvas artifact for the exact web gate command.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
  - Result: passed; 3 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs`
  - Result: passed.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'`
  - Result: passed; `PluginEnablementContractTest` ran 1 test.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Result: passed.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: passed.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
  - Result: passed with the known pre-existing `RiskRuleValidator` TypeCompatibility advisory.
- Scoped `git diff --check` over OSG-C10 reserved paths and coordination evidence
  - Result: passed.

## Verification Result

G10 public extension/API stability evidence is now real for the seed scope:
execution plugin contract, canvas DSL/template named tests, web compatibility
test, OSG guardrail verifier, coordination checks, dispatch-state verifier, DDD
guardrails, and scoped diff checks passed.

## Risks

- Running `mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
  immediately after changing `canvas-context-canvas` requires the local
  `canvas-context-canvas` artifact to be refreshed or the command to be run with
  `-am`. The exact gate command passed after `mvn install -pl
  canvas-context-canvas -DskipTests`.
- The new APIs are intentionally minimal seed surfaces. Full import/export,
  CLI API, AI journey backend, and official plugin behavior remain assigned to
  later G10/G11-dependent workers.

## Coordinator Actions Needed

- Clear active OSG-C10 dispatch.
- Mark OSG-C10 `DONE`.
- Permit G10-dependent OSG backend ecosystem workers only after exact
  reservations are created.

## Ledger Update

OSG-C10 `DONE`; G10 public extension/API seed verified. Active dispatch
registry cleared. Backend ecosystem workers are no longer blocked by absent
canvas/web named test evidence, but still require exact dispatch reservations.

## Rollback Path

Revert/remove the OSG-C10 assigned canvas DSL/template API files, assigned tests,
and `CanvasDslController` compatibility file listed in this packet. Do not
touch unrelated dirty worktree paths.
