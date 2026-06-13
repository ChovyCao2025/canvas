reviewer: Kant 019ebdc5-1e89-7ff1-b55f-9d3c0b69641d
status: FAIL

Critical:
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskSceneRepository.java`: `toRow()` did not set `createdAt` or `updatedAt`, while `risk_scene.created_at` and `risk_scene.updated_at` are `NOT NULL`.

Important:
- `RiskSceneApplicationService` plus `MybatisRiskSceneRepository`: select-empty-then-plain-insert seed flow was not idempotent under concurrent first requests and could fail on `(tenant_id, scene_key)` duplicate.
- `MybatisRiskSceneRepository`: `listScenes()` did not filter `status = ACTIVE`.
- `RiskSceneController`: MyBatis-backed work was wrapped in `Mono.fromCallable()` without `boundedElastic`.

Minor:
- `RiskSceneApplicationServiceTest`: default catalog test checked all keys but only the full field set for the first scene.

Recovery:
- Added failing tests for duplicate seed handling, audit timestamp population, active-only query, all six default scene field sets, and boundedElastic execution.
- Updated `MybatisRiskSceneRepository` to set `createdAt` and `updatedAt`, ignore duplicate scene-key inserts during seed, and filter active scenes.
- Updated `RiskSceneController` to subscribe blocking facade work on `Schedulers.boundedElastic()`.
- Required Maven selector passed after recovery:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-risk -am -Dtest=RiskSceneControllerCompatibilityTest,RiskSceneApplicationServiceTest,RiskPersistenceMappingTest test
```

Result:
- `RiskPersistenceMappingTest`: 2/2
- `RiskSceneApplicationServiceTest`: 4/4
- `RiskSceneControllerCompatibilityTest`: 2/2
- Total selected tests: 8/8, 0 failures, 0 errors

Re-review:
- Reviewer: Kant 019ebdc5-1e89-7ff1-b55f-9d3c0b69641d
- Status: PASS
- Critical: none
- Important: none
- Minor: none

Resolved findings:
- Seeded rows now set `createdAt` and `updatedAt`.
- Duplicate first-read seed races are swallowed per insert.
- Scene listing filters `ACTIVE`.
- Blocking facade work is scheduled on `boundedElastic`.
- Tests now cover the full six-scene default catalog, timestamp and duplicate handling, active query predicate, and scheduler behavior.

Remaining required fixes:
- None.
