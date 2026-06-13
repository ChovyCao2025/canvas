status: DONE

worker: Darwin 019ebdbe-ddef-7770-bb7d-35b037b01f0d

Files changed:
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskSceneFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskSceneView.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/governance/RiskSceneRepository.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskSceneApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskSceneRepository.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskSceneApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskSceneController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskSceneControllerCompatibilityTest.java`

TDD RED evidence:
- Ran required Maven selector after adding tests first.
- Failed in `canvas-context-risk` test compile because `RiskSceneView`, `RiskSceneRepository`, and related final scene API/service classes did not exist.

GREEN evidence:
- Fresh final run passed with `BUILD SUCCESS`.
- `RiskPersistenceMappingTest`: 2 tests, 0 failures/errors.
- `RiskSceneApplicationServiceTest`: 2 tests, 0 failures/errors.
- `RiskSceneControllerCompatibilityTest`: 2 tests, 0 failures/errors.

Exact test command run:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-risk -am -Dtest=RiskSceneControllerCompatibilityTest,RiskSceneApplicationServiceTest,RiskPersistenceMappingTest test
```

Concerns/risks:
- Maven still emits existing dependency/logging warnings (`javassist` effective model warning, commons-logging discovery warning). The required tests pass.

Confirmed:
- No files outside the exact write scope were edited.
- The touched files contain no references to old `canvas-engine`, old `RiskSceneService`, old DAL DOs, or old mappers.
