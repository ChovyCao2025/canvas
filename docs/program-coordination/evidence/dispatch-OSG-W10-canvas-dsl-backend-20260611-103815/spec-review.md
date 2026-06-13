# OSG-W10 Spec Review

status: FAIL
review id: review-OSG-W10-spec-20260611-1107
reviewer: Banach 019eb4a6-cd72-7892-affa-b463826f458b
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815

## Files Reviewed

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/CanvasDslDocument.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidator.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapper.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMappingService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/DslJsonSupport.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidationResult.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## Commands Run

- `git status --short -- <reviewed scopes> backend/canvas-engine`
- `rg` checks for old `canvas-engine`, direct DB, execution runtime, publish, persistence/runtime imports
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
- `node tools/open-source-growth/guardrail-verifier.mjs`

## Findings

1. Blocker: DSL metadata field violates Canvas DSL v1 contract. The contract
   defines `metadata.title`, but the API model exposes `Metadata(String name,
   String displayName)`. Mapping writes `displayName` into graph JSON and tests
   assert `displayName`, so exported DSL does not match the v1 YAML/API shape.

2. Blocker: export can project unsupported graph JSON into an invalid DSL v1
   document. `fromGraphJson` copies every graph node into
   `CanvasDslDocument.Node` without filtering, and `exportDsl` returns that
   document directly without validation or an explicit raw-graph fallback. This
   conflicts with the v1 compatibility rule that unsupported nodes remain in
   graph JSON and must not be forced into the DSL contract.

## Scope / Forbidden Work

- No reviewed changes in old `backend/canvas-engine`.
- No direct DB writes found in the reviewed worker scope.
- No execution runtime files or forbidden execution/runtime concrete imports
  found in the reviewed worker scope.
- The implementation stayed inside the packet's allowed file scope, based on
  scoped `git status`.

## Required Fixes

- Change the DSL API contract surface to use/preserve `metadata.title` as the
  v1 field, with tests proving contract-shaped import/export.
- Add export behavior for unsupported graph JSON semantics: either
  reject/return validation for non-v1-expressible graphs, or return an explicit
  partial projection that preserves raw graph JSON without presenting
  unsupported nodes as valid DSL v1.

## Accepted Concerns

- Publish route remaining out of scope is acceptable for OSG-W10.
- Maven local artifact caveat is acceptable as a verification concern; the
  focused web test passed locally in this workspace, but the coordinator may
  still prefer reactor/aggregator verification before integration.

## Recommended Ledger Update

OSG-W10 REVIEW FAIL: scoped tests and guardrail pass, and write scope is clean,
but Canvas DSL v1 contract blockers remain for `metadata.title` compatibility
and unsupported graph JSON export semantics. Return to worker for required fixes
before `DONE` or `DONE_WITH_CONCERNS`.
