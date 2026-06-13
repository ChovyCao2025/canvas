status: DONE

worker: Euclid 019ebddd-148a-7171-9614-35b2e91f7746

Files changed:
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskListFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/governance/RiskListRepository.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskListApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskListRepository.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskListApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskListController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskListControllerCompatibilityTest.java`

RED evidence:
- Ran the required Maven command after adding tests first.
- It failed in `canvas-context-risk:testCompile` with missing final list API classes:
  - `cannot find symbol: class MybatisRiskListRepository`
  - `cannot find symbol: class RiskListRepository`

GREEN evidence:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-risk -am -Dtest=RiskListControllerCompatibilityTest,RiskListApplicationServiceTest,RiskPersistenceMappingTest test
```

Result summary:
- `BUILD SUCCESS`
- `RiskPersistenceMappingTest`: 2 run, 0 failures, 0 errors
- `RiskListApplicationServiceTest`: 3 run, 0 failures, 0 errors
- `RiskListControllerCompatibilityTest`: 2 run, 0 failures, 0 errors

Forbidden-coupling search result:
- No matches for old coupling terms: `canvas-engine`, `RiskListService`, `org.chovy.canvas.domain.risk`, `org.chovy.canvas.dal`.
- `RiskListMapper` / `RiskListDO` matches are only final-context allowed usages in `MybatisRiskListRepository` and `RiskListApplicationServiceTest`.

Concerns/risks:
- None blocking. Maven emitted existing dependency/runtime warnings, but no test failures or compile errors.
