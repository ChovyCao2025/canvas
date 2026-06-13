# DDD-C09S Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09S-canvas-version-read-controller-20260613-035214`
reserves a compact production Canvas version-read controller seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09R closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 9 controllers / 35 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- The exact reserved files were absent before reservation.
- Read-only selector Russell `019ebd5e-7396-76c3-b45f-0a3db5b0d410`
  recommended this slice as a compact `family:Canvas` production seed backed
  by final `CanvasVersionApplicationService`.

## Scope Boundary

Implement only final `CanvasVersionApplicationService`-backed production seed
routes:

- `GET /canvas/{id}/versions`
- `GET /canvas/{id}/versions/{versionId}`

Preserve the C09 compatibility envelope and expose version read data derived
from final `CanvasVersion` records. The list route may adapt final `List` data
to a minimal compatibility page view with `total` and `list`.

Out of scope:

- Canvas create/update/detail/list/publish/offline/archive/kill/canary/rollback/clone/import/export/message-preview
- Old tenant/project permission plumbing
- Editing old `canvas-engine`
- BI, marketing, CDP, risk, admin, meta, AI, and warehouse realtime route parity

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CanvasControllerCompatibilityTest,CanvasApiCompatibilityTest test
```
