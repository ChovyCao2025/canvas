# DDD-C09T Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09T-canvas-lifecycle-controller-20260613-042400`
reserves a compact production Canvas lifecycle controller seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09S closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 10 controllers / 37 endpoints,
  `family:Canvas` current 1 controller / 2 endpoints, compatibility
  presentCount 7 / missingCount 0, and global `cutoverReady=false`.
- Read-only selector Lovelace `019ebd7b-6fd9-7b90-8b05-50e1eaad56fc`
  recommended this slice as a compact `family:Canvas` production seed backed
  by final `CanvasPublishApplicationService`.

## Scope Boundary

Implement only final `CanvasPublishApplicationService`-backed production seed
routes:

- `POST /canvas/{id}/publish`
- `POST /canvas/{id}/offline`
- `POST /canvas/{id}/archive`
- `POST /canvas/{id}/kill`

Preserve the C09 compatibility envelope. Publish should return stable version
fields derived from final `CanvasVersion`; offline/archive/kill should return a
success envelope with no data. Map old `offline` route semantics to final
`CanvasPublishApplicationService#unpublish(Long canvasId)` explicitly.

Out of scope:

- Canvas create/update/detail/list/review/pre-publish/canary/revert/import/export
- Old tenant/project permission plumbing
- Old `canvas-engine` edits
- BI, marketing-monitoring, growth-activities, search-marketing, meta, AI, risk
  list/lab, warehouse realtime, and admin route parity

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CanvasControllerCompatibilityTest,CanvasApiCompatibilityTest test
```
