# Canvas Testing Layer Map

## Layer Ownership

| Layer | Purpose | Current file | Owner |
| --- | --- | --- | --- |
| Backend unit | Pure domain, handler, parser, and policy logic with mocked collaborators. | `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasStateTransitionPolicyTest.java` | Backend domain |
| Backend integration | MySQL/Redis-backed side effects with Testcontainers and Flyway. | `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyIntegrationTest.java` | Runtime platform |
| Migration/schema | Flyway filename, schema, and SQL compatibility checks. | `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java` | Backend/DBA |
| Controller/API | WebFlux controller, auth, validation, and route behavior. | `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java` | Backend API |
| Frontend behavior | Editor graph, config panel, services, hooks, and presentation behavior. | `frontend/src/pages/canvas-editor/graphHydration.test.ts` | Frontend |

## Critical Coverage Map

| Coverage area | Concrete test class/file | Layer | Status | Owner |
| --- | --- | --- | --- | --- |
| canvas state machine | `CanvasStateTransitionPolicyTest`, `CanvasOpsServiceStateTest`, `CanvasTransactionServiceStateTest` | Backend unit | Covered | Backend domain |
| Redis/DB side effects | `CanvasExecutionRequestServiceIdempotencyIntegrationTest`, `CanvasEntityCacheTest` | Backend integration/unit | Covered for MySQL idempotency; cache remains in-memory | Runtime platform |
| trigger admission | `TriggerAdmissionServiceTest`, `ExecutionLifecycleGateTest` | Backend unit | Covered | Runtime platform |
| scheduler races | `SpecialNodeTimerRaceTest`, `CanvasSchedulerServiceTest`, `NodeTimeoutCoordinatorTest` | Backend unit/concurrency | Covered | Runtime platform |
| circuit breaker races | `CircuitBreakerRegistryTest`, `InFlightExecutionRegistryConcurrencyTest` | Backend unit/concurrency | Covered | Runtime platform |
| direct execution auth | `ExecutionControllerMachineAuthTest`, `InternalApiAuthFilterTest`, `SecurityConfigRouteTest` | Controller/API | Covered | Backend API |
| end-to-end canvas execution | `DagEngineLifecycleTest`, `CanvasExecutionServiceCdpTest`, capacity runbooks in `docs/stressTest/` | Backend unit plus manual | Partially covered; full multi-service E2E stays manual until RocketMQ harness is automated | Runtime platform |
| frontend graph hydration | `frontend/src/pages/canvas-editor/graphHydration.test.ts` | Frontend behavior | Covered | Frontend |
| frontend config form values | `frontend/src/components/config-panel/formValues.test.ts` | Frontend behavior | Covered | Frontend |
| frontend local drafts | `frontend/src/pages/canvas-editor/localDraft.test.ts` | Frontend behavior | Covered | Frontend |

## P0 Package Coverage

| P0 package | Required behavior | Test file | Layer | Status |
| --- | --- | --- | --- | --- |
| P0-01 security hardening | Route auth, production guardrails, internal API auth. | `SecurityConfigRouteTest`, `ProductionSecurityValidatorTest`, `InternalApiAuthFilterTest` | Controller/API | Covered |
| P0-02 reactive threading and transactions | Blocking boundaries and scheduler isolation. | `BlockingHandlerAssemblyTest`, `BlockingWorkSchedulerTest`, `TrackedReactiveTaskRegistryTest` | Backend unit | Covered |
| P0-03 canvas state data consistency | Transaction annotation and state transitions. | `CanvasTransactionAnnotationTest`, `CanvasStateTransitionPolicyTest`, `CanvasTransactionSideEffectTest` | Backend unit | Covered |
| P0-04 execution concurrency safety | In-flight registry and idempotent execution requests. | `InFlightExecutionRegistryConcurrencyTest`, `CanvasExecutionRequestServiceIdempotencyTest`, `CanvasExecutionRequestServiceIdempotencyIntegrationTest` | Backend unit/integration | Covered |
| P0-05 production resilience and DR | DLQ, route recovery, shutdown, runtime metrics. | `ExecutionDlqWriterTest`, `TriggerRouteRecoveryServiceTest`, `ApplicationShutdownConfigTest`, `CanvasRuntimeMetricsTest` | Backend unit | Covered |
| P0-06 data security and tenant isolation | Tenant context, tenant fields, tenant-scoped canvas behavior. | `TenantContextResolverTest`, `CoreTenantFieldMappingTest`, `CanvasTenantIsolationTest` | Backend unit/schema | Covered |

## Integration Harness

| Harness | Dependencies | Command | Notes |
| --- | --- | --- | --- |
| `CanvasIntegrationTestBase` | MySQL 8, Redis 7, Flyway | `cd backend && mvn -pl canvas-engine -am -P integration-tests -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test` | Starts Testcontainers, applies Flyway migrations, exposes Spring datasource and Redis properties. |
| `CanvasRocketMqTestSupport` | Local RocketMQ substitute | `docker compose -f docker-compose.local.yml up -d rocketmq-namesrv rocketmq-broker` | Exposes name-server properties and an assumption gate until a dedicated RocketMQ Testcontainer is stable. |

## Promotion Rules

| Unit test symptom | Promote to integration when | Required dependency | Owner |
| --- | --- | --- | --- |
| Mapper, SQL, or Flyway behavior is mocked. | The assertion depends on insert/update semantics, generated columns, JSON type handling, or unique constraints. | MySQL Testcontainer | Backend/DBA |
| Redis behavior is represented by an in-memory fake. | The assertion depends on TTL, pub/sub, distributed locks, Lua, serialization compatibility, or connection failure behavior. | Redis Testcontainer | Runtime platform |
| RocketMQ behavior is represented by a mocked `RocketMQTemplate`. | The assertion depends on broker acknowledgements, consumer group offsets, retry topics, or tag filtering. | RocketMQ substitute or Testcontainer | Runtime platform |
| Controller tests bypass WebFlux filters. | The assertion depends on route matching, filter order, headers, or machine/user auth. | WebTestClient slice or Spring context | Backend API |
| Frontend helper tests bypass user workflow state. | The assertion depends on React state, effects, autosave, or async service calls. | Vitest component/hook test | Frontend |

## Local Commands

```bash
cd backend && mvn test
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
cd frontend && npm test
cd frontend && npm run build
```
