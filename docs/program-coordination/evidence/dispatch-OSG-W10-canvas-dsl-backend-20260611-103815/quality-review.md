# OSG-W10 Quality Review

review status: FAIL
review id: review-OSG-W10-quality-20260611-1214
reviewer: Arendt 019eb4e5-280f-7913-bacf-138b46f01a13
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815

## Strengths

- `metadata.title` is now the public DSL field and is preserved through mapper
  round trips.
- The controller stays in the DDD-final surface, using `CanvasDslValidator`,
  `CanvasDslMappingService`, and `PublishedCanvasDefinitionProvider` without old
  engine, DB-write, or runtime coupling.
- Focused tests pass, and `guardrail-verifier` returns `{ "ok": true }`.

## Issues

### Critical

- `CanvasDslMapper.fromGraphJson` maps edges only to `from`/`to`, dropping
  behavior-bearing fields such as `condition` / `conditionJson`; then
  `CanvasDslController.exportDsl` validates only the projected DSL. A graph with
  supported node types plus conditional edge semantics can return
  `exportable=true` while losing edge semantics not present in the v1 contract.
  Fix by detecting non-empty unsupported edge semantics during export and
  returning `exportable=false` with `rawGraphJson` and violations, or formally
  extend DSL v1 and the mapper to preserve them.

### Important

- Projection failures are not converted to the non-exportable envelope.
  Malformed or structurally unsupported graph JSON can throw before
  `rawGraphJson` is returned. Fix by catching projection/parse failures in
  export and returning `exportable=false`, `document=null`, raw graph, and a
  stable violation code.

### Minor

- Missing `apiVersion` or `kind` throws during record construction, so
  validation endpoints may not return the stable violation envelope for
  incomplete input. Consider normalizing these like other fields and letting
  `CanvasDslValidator` report violations.

## Recommendations

Add a regression test for export of a graph with supported nodes and a non-empty
edge `condition` / `conditionJson`; expected result should be
`exportable=false` with raw graph preserved. Keep unsupported raw graph detection
close to export so `fromGraphJson` can remain a projection primitive.

## Assessment

**Ready to merge?** No

**Reasoning:** The prior unsupported-node blocker is fixed, but unsupported
edge semantics can still be silently downgraded into a valid-looking DSL v1
export.

## Ledger Update

OSG-W10 quality review FAIL: required fix is to block or preserve unsupported
edge semantics during export, especially non-empty `condition` /
`conditionJson`, and return the non-exportable raw graph envelope.
