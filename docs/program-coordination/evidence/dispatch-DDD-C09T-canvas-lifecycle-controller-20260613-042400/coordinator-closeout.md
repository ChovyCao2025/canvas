# DDD-C09T Coordinator Closeout

Date: 2026-06-13

## Dispatch

- Dispatch: `dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400`
- Task: `DDD-C09T`
- Worker: Planck `019ebd81-ad92-7282-856c-e68c72de47e6`
- Reviewer: Banach `019ebd87-b502-7e63-8bd1-045fb98c4402`
- Result: `DONE_WITH_CONCERNS`

## Closed Scope

Production canvas-web seed for compact Canvas lifecycle compatibility:

- `POST /canvas/{id}/publish`
- `POST /canvas/{id}/offline`
- `POST /canvas/{id}/archive`
- `POST /canvas/{id}/kill`

The routes are implemented through final `CanvasPublishApplicationService`.
`publish` returns stable final `CanvasVersion` response fields, `offline` maps
to final `unpublish`, and `archive` / `kill` delegate to the final lifecycle
methods.

## Evidence

- Worker return:
  `docs/program-coordination/evidence/dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400/worker-return.md`
- Quality review:
  `docs/program-coordination/evidence/dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400/quality-review.md`
- Recovery note:
  `docs/program-coordination/evidence/dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400/recovery-note.md`

## Verification

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CanvasControllerCompatibilityTest,CanvasApiCompatibilityTest test
```

Coordinator result before review: passed, 11 tests, 0 failures, 0 errors.

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Coordinator result before review: current canvas-web 10 controllers / 41
endpoints; compatibility presentCount 7 / missingCount 0; global
`cutoverReady=false`.

## Accepted Concerns

- The scoped `backend/canvas-web` module is still untracked in this worktree,
  which limits normal diff-based review against base. Coordinator verification,
  scoped file inspection, and read-only review still passed for the exact
  DDD-C09T files.
- Broader CanvasController parity remains out of scope: create/update/detail,
  list, review/pre-publish/canary/revert/import/export were deliberately not
  added.
- Global DDD-C09 cutover remains blocked by broader route parity gaps.

## Rollback Path

Revert only the DDD-C09T edits in:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`
