# Dependency Abstraction And Vendor Lock-In Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce migration and testing risk by moving direct RocketMQ, Redis rate-limit/lock, Groovy expression, WebClient, and React Flow usage behind local contracts where the code already has multiple call sites or clear test value.

**Architecture:** Keep framework-specific adapters in `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure` and keep business-facing contracts close to the domain that consumes them. Migrate one dependency family at a time and prove each adapter with existing handler, cache, and frontend tests. Do not add a contract when the inventory shows only one stable call site and no test or migration gain.

**Tech Stack:** Java 21, Spring Boot 3.2, Reactor, RocketMQ Spring, Spring Data Redis, WebClient, Groovy 4, JUnit 5, React 18, TypeScript, React Flow, Vitest.

---

## Source Material

- Spec: `../specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md`
- Source package: `../../../reviewed-packages/p2/dependency-abstraction-and-vendor-lock-in/`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/completed/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md`
- Read: `docs/architecture/reviewed-packages/p2/dependency-abstraction-and-vendor-lock-in/plan.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/MqTriggerConsumer.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/RocketMqCacheInvalidationPublisher.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionReplayRateLimiter.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Read: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `docs/architecture/dependencies/dependency-inventory.md`
- Create: `docs/architecture/dependencies/vendor-alternatives.md`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/CanvasMessageBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/RocketMqCanvasMessageBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/DistributedRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDistributedRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/ExpressionEngine.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/GroovyExpressionEngine.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/ExternalHttpClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/WebClientExternalHttpClient.java`
- Create: `frontend/src/pages/canvas-editor/reactFlowAdapter.ts`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMqHandlerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/GroovyHandlerValidationTest.java`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`

### Task 1: Inventory direct usages of Redis, RocketMQ, Groovy, WebClient, and React Flow APIs

**Files:**
- Create: `docs/architecture/dependencies/dependency-inventory.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas`
- Read: `frontend/src`

- [x] Inventory direct `RocketMQTemplate`, `StringRedisTemplate`, `WebClient`, `GroovyShell`, `SecureASTCustomizer`, and `@xyflow/react` imports.
- [x] For each usage, record package, class/file, dependency, behavior, replacement value, and migration priority.
- [x] Mark each candidate as `contract now`, `adapter only`, or `leave direct` with one sentence of rationale.

**Run:**
```bash
rg "RocketMQTemplate|StringRedisTemplate|WebClient|GroovyShell|SecureASTCustomizer" backend/canvas-engine/src/main/java
rg "@xyflow/react|ReactFlow|useReactFlow" frontend/src
test -f docs/architecture/dependencies/dependency-inventory.md
rg "contract now|adapter only|leave direct|RocketMQTemplate|StringRedisTemplate|@xyflow/react" docs/architecture/dependencies/dependency-inventory.md
```

**Expected:** The inventory names every direct framework usage and classifies each one before code migration starts.

### Task 2: Identify high-value abstractions: message bus, distributed lock, rate limiter, expression engine, external client

**Files:**
- Modify: `docs/architecture/dependencies/dependency-inventory.md`
- Create: `docs/architecture/dependencies/vendor-alternatives.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/RocketMqCacheInvalidationPublisher.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionReplayRateLimiter.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`

- [x] Define contracts only for message bus, distributed rate limiter, expression engine, and external HTTP client.
- [x] Document alternatives for RocketMQ, Redis/Redisson, Groovy/Aviator/QLExpress, WebClient/Feign, and React Flow.
- [x] Record non-goals for MyBatis and React Flow when the current migration value is documentation rather than code movement.

**Run:**
```bash
test -f docs/architecture/dependencies/vendor-alternatives.md
rg "RocketMQ|Redisson|Groovy|Aviator|QLExpress|WebClient|Feign|React Flow|non-goals" docs/architecture/dependencies/vendor-alternatives.md
```

**Expected:** The alternatives document names the chosen contracts, candidate replacements, operational cost, and non-goals.

### Task 3: Add message bus interfaces only where there are multiple call sites or clear migration/testing value

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/CanvasMessageBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/RocketMqCanvasMessageBus.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMqHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/RocketMqCacheInvalidationPublisher.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/RocketMqCacheInvalidationConsumer.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMqHandlerTest.java`

- [x] Introduce `CanvasMessageBus` with publish, orderly publish, and cache-invalidation operations used by existing RocketMQ call sites.
- [x] Move RocketMQ-specific destination, tag, timeout, and send-status logic into `RocketMqCanvasMessageBus`.
- [x] Update send-node and cache invalidation code to depend on the local contract.
- [x] Assert in tests that send-node behavior does not require `RocketMQTemplate`.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=SendMqHandlerTest
rg "RocketMQTemplate" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache
```

**Expected:** `SendMqHandlerTest` passes, and direct `RocketMQTemplate` usage remains only inside RocketMQ adapter/consumer code.

### Task 4: Migrate distributed rate-limiting through a local Redis-backed contract

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/DistributedRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDistributedRateLimiter.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request/CanvasExecutionReplayRateLimiter.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestBacklogMetricsTest.java`

- [x] Define a rate-limit contract with scope, operator, cost, limit, and window.
- [x] Move Redis increment/expire code from replay limiting into the Redis adapter.
- [x] Keep in-memory fallback behavior for tests and single-instance local runs.
- [x] Add assertions for Redis-backed and in-memory replay limits.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasExecutionRequestServiceIdempotencyTest,CanvasExecutionRequestBacklogMetricsTest
rg "StringRedisTemplate" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis
```

**Expected:** Replay limiter tests pass, and direct Redis access for replay rate limiting lives in `RedisDistributedRateLimiter`.

### Task 5: Add expression engine and external client contracts

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/ExpressionEngine.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/GroovyExpressionEngine.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/ExternalHttpClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/WebClientExternalHttpClient.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/GroovyHandlerValidationTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`

- [x] Define `ExpressionEngine` with compile, execute, evict-canvas, timeout, and result-size semantics.
- [x] Move Groovy-specific shell pool, secure AST setup, and script cache coordination into `GroovyExpressionEngine`.
- [x] Define `ExternalHttpClient` for named integration calls used by delivery code.
- [x] Move WebClient base URL and response mapping into `WebClientExternalHttpClient`.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=GroovyHandlerValidationTest,NotificationServiceTest
rg "GroovyShell|SecureASTCustomizer|WebClient" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http
```

**Expected:** Groovy and notification tests pass, and framework-specific imports are contained in the new adapters.

### Task 6: Document alternatives and React Flow adapter non-goals

**Files:**
- Modify: `docs/architecture/dependencies/vendor-alternatives.md`
- Create: `frontend/src/pages/canvas-editor/reactFlowAdapter.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`
- Test: `frontend/src/pages/canvas-editor/connectionInteraction.test.ts`

- [x] Add a thin React Flow adapter only for graph conversion helpers that are reused by tests or future migrations.
- [x] Keep interactive canvas code on React Flow components until a replacement has product approval.
- [x] Document the React Flow replacement trigger, migration cost, and tests required before replacement.

**Run:**
```bash
cd frontend && npm test -- graphHydration connectionInteraction
rg "React Flow|replacement trigger|migration cost|reactFlowAdapter" docs/architecture/dependencies/vendor-alternatives.md frontend/src/pages/canvas-editor
```

**Expected:** Frontend graph tests pass, and the alternatives doc states why React Flow remains the runtime library.

### Task 7: Review scoped dependency abstraction changes

**Files:**
- Modify: `docs/architecture/archive/completed/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md`
- Create: `docs/architecture/dependencies/dependency-inventory.md`
- Create: `docs/architecture/dependencies/vendor-alternatives.md`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/CanvasMessageBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/RocketMqCanvasMessageBus.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/DistributedRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisDistributedRateLimiter.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/ExpressionEngine.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/GroovyExpressionEngine.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/ExternalHttpClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/WebClientExternalHttpClient.java`
- Create: `frontend/src/pages/canvas-editor/reactFlowAdapter.ts`

- [x] Review only files named in this plan.
- [x] Leave adapter contracts, migrated call sites, tests, and dependency docs unstaged for handoff.
- [x] Do not create a commit unless explicitly requested by the user.

**Run:**
```bash
git diff -- docs/architecture/archive/completed/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md docs/architecture/dependencies backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers backend/canvas-engine/src/main/java/org/chovy/canvas/engine/request backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery frontend/src/pages/canvas-editor
```

**Expected:** The diff contains dependency inventory, local contracts, adapter implementations, migrated call sites, and tests for this package; no commit is created during handoff.
