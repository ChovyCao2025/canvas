# DDD-C09R Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09R-canvas-project-folder-metadata-controller-20260613-032637`
reserves a compact production Canvas project-folder metadata controller seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasProjectFolderMetadataController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasProjectFolderMetadataControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09Q closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 8 controllers / 33 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- The exact reserved files were absent before reservation.
- Read-only selector Plato `019ebd48-f02c-7732-87ef-abdfc7d6624c`
  recommended this slice as a compact `family:Canvas` production seed backed
  by final `CanvasProjectFolderApplicationService`.

## Scope Boundary

Implement only final `CanvasProjectFolderApplicationService`-backed production
seed routes:

- `GET /canvas/{id}/project-folder-metadata`
- `PUT /canvas/{id}/project-folder-metadata`

Preserve the compatibility envelope and response fields already used by the old
`CanvasController`: `canvasId`, `projectId`, `projectKey`, `projectName`,
`folderKey`, and `folderName`.

Out of scope:

- Canvas create/update/publish/import/export/canary/rollback route parity
- Old permission plumbing not owned by the final application service
- CDP, BI, marketing, risk, admin, meta, AI, and warehouse realtime route parity
- Editing old `canvas-engine`

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CanvasProjectFolderMetadataControllerCompatibilityTest,CanvasApiCompatibilityTest test
```
