# DDD-C09Q Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09Q-cdp-warehouse-readiness-controller-20260613-025451`
reserves a compact production CDP warehouse readiness controller seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseReadinessControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09P recovery closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 7 controllers / 32 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- The exact reserved files were absent before reservation.
- Read-only selector Kierkegaard `019ebd2b-ff34-77b1-aac1-5470d1e2ca38`
  recommended this slice as the clearest final-facade production seed.

## Scope Boundary

Implement only final `CdpWarehouseReadinessFacade`-backed production seed route:

- `GET /warehouse/readiness`

Preserve the compatibility view shape already exercised by
`CdpApiCompatibilityTest`: `tenantId`, `status`, `generatedAt`, `sections`,
`productionReady`, `blockerCount`, and `blockers`.

Out of scope:

- `/warehouse/realtime/**`
- `/warehouse/status`, `/warehouse/backfill`, `/warehouse/aggregate`, offline
  cycle, and retention routes
- `/cdp/events/track` and write-key auth
- `/warehouse/audiences/**`
- broader BI, marketing-monitoring, growth, search-marketing, meta, AI, risk,
  and admin route parity

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web -am -Dtest=CdpWarehouseReadinessControllerCompatibilityTest,CdpApiCompatibilityTest test
```
