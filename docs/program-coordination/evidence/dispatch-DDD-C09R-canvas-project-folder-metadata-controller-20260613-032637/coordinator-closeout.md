# DDD-C09R Coordinator Closeout

Date: 2026-06-13

Status: DONE_WITH_CONCERNS

## Result

`canvas-web` now has a compact production Canvas project-folder metadata
controller seed backed by final `CanvasProjectFolderApplicationService`.

Implemented routes:

- `GET /canvas/{id}/project-folder-metadata`
- `PUT /canvas/{id}/project-folder-metadata`

The controller preserves the C09 compatibility envelope, defaults absent tenant
to `7L`, maps actor/operator data into `SaveProjectFolderCommand`, and exposes
only `canvasId`, `projectId`, `projectKey`, `projectName`, `folderKey`, and
`folderName` in response data.

## Verification

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web -am -Dtest=CanvasProjectFolderMetadataControllerCompatibilityTest,CanvasApiCompatibilityTest test
```

Result: BUILD SUCCESS; 8 tests, 0 failures, 0 errors.

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result: current canvas-web 9 controllers / 35 endpoints; compatibility
presentCount 7 / missingCount 0; global `cutoverReady=false`.

```bash
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
```

Result: both passed.

## Review

Laplace `019ebd57-fd62-7fd1-97da-bdefaf96e122` returned PASS with no findings.

## Accepted Concerns

- Broader `CanvasController` route parity remains out of scope.
- Old tenant/project permission plumbing is not recreated in `canvas-web`; the
  slice is intentionally limited to final application-service ownership.
- The cutover preflight route-gap grouping still reports `family:Canvas` as
  0/0 because this seed uses a narrow controller name rather than a direct
  `CanvasController` class-family replacement, even though total canvas-web
  controller/endpoint counts increased.

## Rollback

Remove only:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasProjectFolderMetadataController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasProjectFolderMetadataControllerCompatibilityTest.java`
