# P2-04 Dependency Abstraction And Vendor Lock-In Evidence

Status date: 2026-06-05

## Scope Completed

- Added dependency inventory and vendor alternatives docs.
- Added `CanvasMessageBus` and RocketMQ adapter for send-node and cache-invalidation publishing.
- Added `DistributedRateLimiter` and Redis adapter for execution replay rate limiting.
- Added `ExpressionEngine` and Groovy adapter for Groovy node execution and expression evaluation.
- Added `ExternalHttpClient` and WebClient adapter for reach delivery `/send` calls.
- Added `reactFlowAdapter.ts` as a thin React Flow graph helper boundary.
- Updated P2-04 plan/spec to reflect implementation and handoff state.

## TDD / Red-Green Notes

- `SendMqHandlerTest` first required send-node behavior to use `CanvasMessageBus` instead of direct `RocketMQTemplate`.
- `CanvasExecutionReplayRateLimiterTest` first failed on missing `DistributedRateLimiter` / `RedisDistributedRateLimiter`.
- `GroovyHandlerValidationTest` first failed to compile because `ExpressionEngine` did not exist.
- `RuntimeMigrationEvidenceTest` first failed because it still expected Groovy runtime details in `GroovyHandler`; it now records the adapter boundary.
- `ReachDeliveryServicePolicyTest` first failed to compile because `ExternalHttpClient` did not exist.
- `connectionInteraction.test.ts` first failed because `reactFlowAdapter.ts` did not exist.

## Verification Commands

Backend focused verification:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn test -pl canvas-engine -Dtest=SendMqHandlerTest,RocketMqCacheInvalidationTest,CanvasExecutionReplayRateLimiterTest,CanvasExecutionRequestServiceIdempotencyTest,CanvasExecutionRequestBacklogMetricsTest,GroovyHandlerValidationTest,NotificationServiceTest,ReachDeliveryServicePolicyTest,SendMessageHandlerTest,SendMessageHandlerOutboxRoutingTest,RuntimeMigrationEvidenceTest
```

Result: 43 tests, 0 failures, 0 errors, 0 skipped.

Frontend focused verification:

```bash
cd frontend
npm test -- graphHydration connectionInteraction
```

Result: 2 test files, 8 tests passed.

Documentation gates:

```bash
rg "contract now|adapter only|leave direct|RocketMQTemplate|StringRedisTemplate|@xyflow/react" docs/architecture/evidence/dependencies/dependency-inventory.md
rg "RocketMQ|Redisson|Groovy|Aviator|QLExpress|WebClient|Feign|React Flow|non-goals" docs/architecture/evidence/dependencies/vendor-alternatives.md
test -f docs/architecture/evidence/dependencies/dependency-inventory.md
test -f docs/architecture/evidence/dependencies/vendor-alternatives.md
```

Result: all exited 0.

Source containment checks:

```bash
rg "RocketMQTemplate" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq
```

Result: `RocketMQTemplate` appears only in `infrastructure/mq/RocketMqCanvasMessageBus.java` for the migrated send/cache path.

```bash
rg "StringRedisTemplate" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis
```

Result: replay limiter no longer imports `StringRedisTemplate`; Redis direct usage remains in Redis infrastructure components including `RedisDistributedRateLimiter`.

```bash
rg "GroovyShell|SecureASTCustomizer|WebClient" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http
```

Result: Groovy runtime details are in `GroovyExpressionEngine` and `GroovyScriptCache`; reach delivery WebClient is in `WebClientExternalHttpClient`. `CouponHandler` and `ApiCallHandler` still use WebClient directly and are documented as `leave direct` because they are dynamic or provider-specific handler integrations outside this package's migrated reach-delivery path.

```bash
rg "React Flow|replacement trigger|migration cost|reactFlowAdapter" docs/architecture/evidence/dependencies/vendor-alternatives.md frontend/src/pages/canvas-editor
```

Result: exits 0; React Flow runtime remains intentionally direct, with helper boundary documented through `reactFlowAdapter.ts`.

## Verification Blockers Resolved

During Maven testCompile, unrelated in-progress BI/warehouse tests initially blocked focused backend verification:

- BI dataset/portal version methods and controller endpoints were already present in source after the interrupted run; rerunning testCompile picked them up.
- `CdpWarehouseProductionReadinessProofServiceTest` ambiguous `null` constructor calls are now typed as `CdpWarehouseConsumerAvailabilityService` null.

These changes were required only to restore Maven focused-test execution and are not part of the P2-04 dependency abstraction design.

## Handoff Notes

- No commit was created.
- Remaining direct dependency usages are intentionally classified in the inventory, not silently treated as completed migrations.
- MyBatis abstraction and React Flow runtime replacement remain explicit non-goals for this package.
