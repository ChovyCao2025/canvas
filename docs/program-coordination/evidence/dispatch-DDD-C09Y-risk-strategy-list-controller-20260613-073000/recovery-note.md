# DDD-C09Y Risk Strategy List Controller Reservation

Reserved at: 2026-06-13T07:30:00+08:00

## Scope

Implement the compact read-only production controller seed for:

- `GET /canvas/risk/strategies`
- Optional query parameter: `sceneKey`
- Optional tenant header: `X-Tenant-Id`, defaulting to `7`

## Legacy Behavior Reference

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java`
- `RiskStrategyService#listStrategies(Long tenantId, String sceneKey)`
- `JdbcRiskStrategyStateStore#findByTenant(Long tenantId)`

Observed behavior:

- read current tenant strategies only
- missing or blank `sceneKey` returns all tenant strategies
- nonblank `sceneKey` is an exact filter
- result ordering is `strategyKey` ascending
- controller wraps the blocking service call on `Schedulers.boundedElastic()`
- legacy response envelope is `code=0`, `message=success`, with absent `errorCode` and `traceId`

## Exact Reserved Files

- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskStrategyFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/governance/RiskStrategyRepository.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskStrategyApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/adapter/persistence/MybatisRiskStrategyRepository.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskStrategyApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskStrategyControllerCompatibilityTest.java`

## Verification Before Reservation

- Selector Gibbs recommended `DDD-C09Y` as the next compact route-parity task.
- Exact target files were checked absent before reservation.
- Existing final risk strategy view, DO, mapper, and persistence mapping coverage were inspected.
- Coordination checks passed before reservation in the prior checkpoint.

## Out Of Scope

- `POST /canvas/risk/strategies`
- `GET /canvas/risk/strategies/{strategyKey}`
- versions, lifecycle, diff, lab, simulation, list write/entry/import, and decision trace routes
- global DDD-C09 cutover readiness
