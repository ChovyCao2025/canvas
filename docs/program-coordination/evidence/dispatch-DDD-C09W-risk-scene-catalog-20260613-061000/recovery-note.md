# DDD-C09W Recovery Note

Date: 2026-06-13

Dispatch `dispatch-DDD-C09W-risk-scene-catalog-20260613-061000`
reserves a compact production `/canvas/risk/scenes` seed.

## Scope

- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskSceneFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskSceneView.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/governance/RiskSceneRepository.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskSceneApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskSceneRepository.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskSceneApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskSceneController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskSceneControllerCompatibilityTest.java`

## Gate Evidence Before Reservation

- Active dispatch registry was empty after DDD-C09V closeout.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed.
- G0B backup manifest exists at
  `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
  branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with current canvas-web 12 controllers / 44 endpoints,
  compatibility presentCount 7 / missingCount 0, and global
  `cutoverReady=false`.
- Exact target files were absent before reservation.
- Read-only selector James `019ebdb7-910f-7300-8188-928c9610ca08`
  recommended this slice as a compact `/canvas/risk` production seed backed by
  final risk context scene catalog APIs.

## Scope Boundary

Implement only:

- `GET /canvas/risk/scenes`

Use final risk module APIs and persistence types only:

- `org.chovy.canvas.risk.api.RiskSceneFacade`
- `org.chovy.canvas.risk.api.RiskSceneView`
- final `RiskSceneDO` / `RiskSceneMapper` through a final-context repository
  adapter

Preserve the old default scene seed behavior by re-homing the small default
catalog in the final risk context. Do not depend on old
`org.chovy.canvas.domain.risk.governance.RiskSceneService`,
`canvas-engine`, old DAL classes, old DO packages, or old mappers.

Out of scope:

- `/canvas/risk/decisions/traces`
- `/canvas/risk/lab/**`
- `/canvas/risk/lists/**`
- `/canvas/risk/strategies/**`
- `/canvas/bi/**`
- `/canvas/marketing-monitoring/**`
- `/canvas/growth-activities/**`
- `/canvas/search-marketing/**`
- `/ai/**`
- `/admin/**`
- `/warehouse/realtime/**`

## Required Verification

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
  mvn -pl canvas-web,canvas-context-risk -am \
  -Dtest=RiskSceneControllerCompatibilityTest,RiskSceneApplicationServiceTest,RiskPersistenceMappingTest test
```
