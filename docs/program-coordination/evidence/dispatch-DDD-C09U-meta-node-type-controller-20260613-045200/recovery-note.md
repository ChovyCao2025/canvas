# DDD-C09U Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09U-meta-node-type-controller-20260613-045200`
reserves a compact production `/meta` node-type catalog seed.

## Scope

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/node/NodeMetadataApplicationService.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/meta/MetaNodeTypeController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/meta/MetaNodeTypeControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09T closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 10 controllers / 41 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- Exact target files were absent before reservation.
- Read-only selector Nash `019ebd8e-b5cd-7772-9a49-4363f7079f7c`
  recommended this slice as a compact `/meta` production seed backed by final
  execution `NodeHandlerRegistry`.

## Scope Boundary

Implement only final execution-module-backed node-type catalog routes:

- `GET /meta/node-types`
- `GET /meta/node-types/{typeKey}/schema`

Use final execution module APIs. Add `NodeMetadataFacade` and
`NodeMetadataApplicationService` to adapt `NodeHandlerRegistry#metadata()` into
immutable `NodeMetadataView` records. Do not depend on old `MetaService`,
`NodeTypeRegistryDO`, old DAL mappers, or `canvas-engine`.

Out of scope:

- Other `/meta/*` routes: options, AI providers/templates/models, MQ, coupons,
  AB experiments, context fields, tagger, biz lines, identity/event APIs
- `/canvas/marketing-monitoring`
- `/canvas/growth-activities`
- `/canvas/search-marketing`
- `/ai/*`
- `/admin/*`
- `/warehouse/realtime/*` beyond already seeded readiness route

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web,canvas-context-execution -am \
  -Dtest=MetaNodeTypeControllerCompatibilityTest,NodeMetadataContractTest test
```
