# OSG-W10 Quality Fix

status: READY_FOR_REREVIEW
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815
actor: coordinator

## Review Findings Addressed

- Block unsupported graph edge semantics during Canvas DSL export. Non-empty
  edge `condition` or `conditionJson` now returns `exportable=false`,
  `document=null`, preserved `rawGraphJson`, and
  `UNSUPPORTED_GRAPH_EDGE_SEMANTICS`.
- Convert projection failures to the same non-exportable raw graph envelope
  with `UNSUPPORTED_GRAPH_JSON`.

## Files Changed

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMappingService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapper.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## RED Evidence

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` failed with:
  - `exportEndpointDoesNotDropUnsupportedEdgeSemantics`: expected
    `exportable=false` but was `true`.
  - `exportEndpointReturnsRawGraphEnvelopeWhenProjectionFails`: threw
    `IllegalArgumentException: Expected JSON array`.

## GREEN Evidence

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest` -> BUILD SUCCESS, 8 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn install -pl canvas-context-canvas -DskipTests` -> BUILD SUCCESS.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` -> BUILD SUCCESS, 9 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs` -> `{ "ok": true }`.
- `node tools/program-coordination/check-dispatch-state.mjs .` -> `{ "ok": true }`.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` -> passed.

## Re-Review

Arendt `019eb4e5-280f-7913-bacf-138b46f01a13` was asked to re-review the
quality fixes.
