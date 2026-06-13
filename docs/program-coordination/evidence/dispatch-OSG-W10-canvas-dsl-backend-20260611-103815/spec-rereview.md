# OSG-W10 Spec Re-Review

review status: PASS
review id: review-OSG-W10-spec-rereview-20260611-1209
reviewer: Hubble 019eb4de-725f-7672-8ff7-62d0550aa2bf
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815

## Review Scope

Post-fix spec compliance review focused on the two prior blockers and reserved
scope compliance.

## Files Reviewed

- `docs/program-coordination/evidence/dispatch-OSG-W10-canvas-dsl-backend-20260611-103815/spec-review.md`
- `docs/program-coordination/evidence/dispatch-OSG-W10-canvas-dsl-backend-20260611-103815/worker-return-fix.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## Requirements Checked

- DSL metadata now exposes and preserves `metadata.title`; mapper writes
  `title`, and only reads `displayName` as legacy graph fallback.
- Export validates the projected DSL before returning it; unsupported graph
  nodes return `exportable=false`, `document=null`, violations, and preserved
  `rawGraphJson`.
- OSG-W10 stayed within the reserved DSL/backend controller file scope.
- No old `backend/canvas-engine` edits were detected.
- No direct DB writes or execution-runtime concrete imports were detected in
  reviewed files.

## Commands Inspected Or Run

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest` -> PASS, 8 tests.
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` -> PASS, 7 tests.
- `node tools/open-source-growth/guardrail-verifier.mjs` -> `{ "ok": true }`.
- Scoped `git status --short -- backend/canvas-engine` -> no output.
- Scoped `git status` over reviewed files -> only OSG-W10 allowed paths shown
  as untracked.
- Scoped `rg` checks for DB writes, old engine/runtime bindings, and
  `displayName`.

## Findings

None.

## Required Fixes

None.

## Residual Risks

- The broader workspace remains dirty/untracked, so attribution still relies on
  the dispatch reservation plus scoped checks.
- `CanvasDslMapper.fromGraphJson` remains a projection primitive; the export
  endpoint correctly wraps it with validation before returning public output.

## Ledger Update

OSG-W10 post-fix spec review PASS: Goodall fixed both prior blockers.
`metadata.title` is the public DSL field, unsupported graph export no longer
returns a valid-looking DSL document, focused Maven tests and OSG guardrail
pass, and scoped checks found no old engine, DB-write, or execution-runtime
scope violation.
