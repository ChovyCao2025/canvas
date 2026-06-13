# DDD-C09V Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09V-warehouse-realtime-cutover-readiness-20260613-052500`
reserves a compact production `/warehouse/realtime/cutover-readiness` seed.

## Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeCutoverReadinessController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeCutoverReadinessControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09U closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 11 controllers / 43 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- Exact target files were absent before reservation.
- Read-only selector Maxwell `019ebda4-c55f-7712-962a-84cfcd17a49c`
  recommended this slice as a compact `/warehouse/realtime` production seed
  backed by final CDP `CdpWarehouseReadinessFacade`.

## Scope Boundary

Implement only:

- `GET /warehouse/realtime/cutover-readiness`

Use final CDP APIs only:

- `org.chovy.canvas.cdp.api.CdpWarehouseReadinessFacade`
- `org.chovy.canvas.cdp.api.CdpWarehouseReadinessView`

Preserve the old query parameters at the web boundary:

- `targetMode`, default `FLINK_FIRST`
- repeated `pipelineKey`
- repeated `contractKey`
- `certificationMode`, default `HYBRID`
- `maxCertificationAgeMinutes`, default `60`

Derive the compatibility decision from final aggregate warehouse readiness.
Do not depend on old `CdpWarehouseRealtimeCutoverReadinessService`,
`canvas-engine`, DAL mappers, or old DO types.

Out of scope:

- `/warehouse/realtime/status`
- `/warehouse/realtime/jobs/**`
- `/warehouse/realtime/job-probes/**`
- `/canvas/bi/**`
- `/canvas/marketing-monitoring/**`
- `/canvas/growth-activities/**`
- `/canvas/search-marketing/**`
- `/ai/**`
- `/admin/**`

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web,canvas-context-cdp -am \
  -Dtest=CdpWarehouseRealtimeCutoverReadinessControllerCompatibilityTest,CdpWarehouseReadinessApplicationServiceTest test
```
