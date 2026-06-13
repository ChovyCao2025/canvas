# DDD-C09T Worker Return

Date: 2026-06-13

## Worker

- Worker: Planck `019ebd81-ad92-7282-856c-e68c72de47e6`
- Dispatch: `dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400`
- Task: `DDD-C09T`
- Branch: `main`
- Worktree: `/Users/photonpay/project/canvas`
- Base commit: `01aac65697d524f4cf2e92d954db088895631004`
- Head commit: `01aac65697d524f4cf2e92d954db088895631004`

## Files Changed

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`

## Contracts Changed

- Added compact lifecycle routes in canvas-web:
  - `POST /canvas/{id}/publish`
  - `POST /canvas/{id}/offline`
  - `POST /canvas/{id}/archive`
  - `POST /canvas/{id}/kill`
- Preserved C09 envelope shape.
- `publish` returns existing `VersionResponse` fields.
- `offline`, `archive`, and `kill` return success envelopes with no data.
- `/offline` maps to final `CanvasPublishApplicationService#unpublish`.
- `kill` accepts but does not pass `mode`.
- `IllegalArgumentException` and publish `IllegalStateException` map to HTTP
  400 / `API_001`.

## TDD Evidence

- RED:
  ```bash
  cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
    mvn -pl canvas-web -am -Dtest=CanvasControllerCompatibilityTest,CanvasApiCompatibilityTest test
  ```
  Failed at test compile because `CanvasController` lacked constructor
  `(CanvasVersionApplicationService, CanvasPublishApplicationService)`.
- GREEN: same command rerun after implementation.

## Worker Verification

Worker-reported result: build success, 11 tests run, 0 failures, 0 errors, 0
skipped.

Reports under `backend/canvas-web/target/surefire-reports/`.

## Risks

- `backend/canvas-web` is untracked in this worktree, so normal `git diff` does
  not show hunks against base. Scoped status shows only the two allowed files
  in this task scope.

## Coordinator Actions Needed

- Review and verify the two scoped files.
- Move dispatch to read-only review if coordinator verification passes.

## Rollback Path

Revert only the DDD-C09T edits in:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`
