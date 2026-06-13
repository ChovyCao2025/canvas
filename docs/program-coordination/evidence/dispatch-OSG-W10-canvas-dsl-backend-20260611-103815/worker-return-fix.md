# OSG-W10 Worker Fix Return

status: DONE_WITH_CONCERNS
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815
worker: Goodall 019eb491-4c8a-7201-8165-7bf0ac56b1b8
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004

## Files Changed

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/CanvasDslDocument.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapper.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMappingService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidator.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## Contracts Changed

- DSL API metadata surface now exposes and preserves `metadata.title`, not
  `displayName`.
- Mapper writes `metadata.title`; it tolerates legacy `displayName` only when
  reading graph JSON.
- DSL export now validates the projected document. Unsupported graph semantics
  return `exportable=false`, `document=null`, validation violations, and
  preserved `rawGraphJson`.

## Tests Run

- RED: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest` failed on missing `Metadata.title()`.
- RED: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` failed on missing `title()`, `exportable()`, `rawGraphJson()`, and `violations()`.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest` -> BUILD SUCCESS, 8 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -pl canvas-context-canvas -DskipTests` -> BUILD SUCCESS.
- GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` -> BUILD SUCCESS, 7 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs` -> `{ "ok": true }`.

## Verification Result

PASS with accepted concern: the single-module `canvas-web` command needs the
updated `canvas-context-canvas` artifact installed first because it resolves the
local Maven artifact.

## Verification Output Summary

- `backend/canvas-context-canvas/target/surefire-reports/`
- `backend/canvas-web/target/surefire-reports/`

## Evidence Artifact Paths

- `backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.application.dsl.CanvasDslMapperTest.xml`
- `backend/canvas-context-canvas/target/surefire-reports/TEST-org.chovy.canvas.canvas.application.dsl.CanvasDslValidatorTest.xml`
- `backend/canvas-web/target/surefire-reports/TEST-org.chovy.canvas.web.canvas.CanvasDslControllerCompatibilityTest.xml`

## Risks

- Publish route remains out of OSG-W10 scope.
- Reserved module paths are still untracked relative to base SHA, so scoped
  status reports `??`.

## Coordinator Actions Needed

- Record the accepted Maven local artifact caveat with the verification sequence
  above.

## Ledger Update

OSG-W10 RETURNED / DONE_WITH_CONCERNS: Banach blockers fixed;
`metadata.title` contract is preserved, and unsupported graph JSON export no
longer presents unsupported nodes as valid DSL v1.

## Rollback Path

Revert assigned DSL backend/controller files and tests only.
