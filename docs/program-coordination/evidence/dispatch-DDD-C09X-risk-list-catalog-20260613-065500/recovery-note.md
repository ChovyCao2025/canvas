status: RESERVED

task: DDD-C09X Risk list catalog production controller seed

selector:
- Aristotle 019ebdd3-4b6e-7f03-9474-51ea26ae66f0 recommended a compact `GET /canvas/risk/lists` seed.

old reference:
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java`
- method: `listLists()`
- route: `GET /canvas/risk/lists`
- backing: old `org.chovy.canvas.domain.risk.governance.RiskListService#listLists`

final backing:
- Use existing final `RiskListView`, `RiskListDO`, and `RiskListMapper`.
- Add final risk context `RiskListFacade`, `RiskListRepository`, `RiskListApplicationService`, and `MybatisRiskListRepository`.
- Do not depend on old `canvas-engine`, old `RiskListService`, old DAL data objects, or old mappers.

exact reserved files:
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskListFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/governance/RiskListRepository.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskListApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskListRepository.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskListApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskListController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskListControllerCompatibilityTest.java`

pre-dispatch checks:
- G0B backup manifest present with branch `main` and HEAD `01aac65697d524f4cf2e92d954db088895631004`.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before reservation.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed before reservation.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0 before reservation with current `canvas-web` 13 controllers / 45 endpoints, compatibility presentCount 7 / missingCount 0, and global `cutoverReady=false`.
- All seven exact reserved files were absent before reservation.

required focused Maven:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-risk -am -Dtest=RiskListControllerCompatibilityTest,RiskListApplicationServiceTest,RiskPersistenceMappingTest test
```

compatibility behavior:
- Preserve success envelope shape with `code`, `message`, `errorCode`, `data`, and `traceId`.
- Success response has `code=0`, `message=success`, and no `errorCode` or `traceId`.
- Use optional `X-Tenant-Id`, defaulting to tenant `7` when absent to match current final `canvas-web` seeds.
- Return stable list fields: `tenantId`, `listKey`, `listType`, `subjectType`, `status`, `requiresApproval`, and `owner`.
- Tenant-scoped list query should order by `listKey`.
- Run blocking MyBatis-backed facade work on `Schedulers.boundedElastic()`.

out of scope:
- `POST /canvas/risk/lists`
- `GET /canvas/risk/lists/{listKey}/entries`
- `POST /canvas/risk/lists/{listKey}/entries`
- `DELETE /canvas/risk/lists/{listKey}/entries/{entryId}`
- `POST /canvas/risk/lists/{listKey}/entries/import`
- risk strategies, risk lab, risk decision trace expansion
- `/warehouse/realtime/**`, `/canvas/bi/**`, marketing-monitoring, growth-activities, search-marketing, `/ai`, `/admin`

rollback pointer:
- Remove only the seven exact reserved files and this dispatch evidence directory if abandoning the task before worker edits are integrated.
