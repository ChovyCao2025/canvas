# Documentation, ADRs, And Runbooks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace broad archived review material with active ADRs, handler guidance, Redis key catalog, OpenAPI generation checks, and runbooks that operators and implementers can execute.

**Architecture:** Keep archived files under `docs/architecture/archive` as evidence. New active docs live under `docs/architecture/adr`, `docs/architecture/guides`, `docs/architecture/reference`, and `docs/architecture/runbooks`. Documentation checks use `rg`, backend OpenAPI tests, and frontend API-doc tests.

**Tech Stack:** Markdown, Java 21, Springdoc OpenAPI WebFlux, Spring Boot Actuator, Redis key helpers, JUnit 5, TypeScript, Vitest.

---

## Source Material

- Spec: `../specs/P2-03-documentation-adr-and-runbooks-spec.md`
- Source package: `../todo/p2/documentation-adr-and-runbooks/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/specs/P2-03-documentation-adr-and-runbooks-spec.md`
- Read: `docs/architecture/todo/p2/documentation-adr-and-runbooks/plan.md`
- Read: `docs/architecture/archive/evolution/production-practice-review.md`
- Read: `docs/architecture/archive/reviews/architect-checklist-report.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Create: `docs/architecture/adr/ADR-0000-template.md`
- Create: `docs/architecture/adr/ADR-0001-web-runtime-model.md`
- Create: `docs/architecture/adr/ADR-0002-node-handler-model.md`
- Create: `docs/architecture/adr/ADR-0003-groovy-expression-engine.md`
- Create: `docs/architecture/adr/ADR-0004-redis-rocketmq-mysql-choices.md`
- Create: `docs/architecture/adr/ADR-0005-data-isolation-model.md`
- Create: `docs/architecture/guides/node-handler-development.md`
- Create: `docs/architecture/reference/redis-key-catalog.md`
- Create: `docs/architecture/runbooks/dag-execution-flow.md`
- Create: `docs/architecture/runbooks/failure-triage.md`
- Create: `docs/architecture/runbooks/dlq-replay.md`
- Create: `docs/architecture/runbooks/route-rebuild.md`
- Create: `docs/architecture/runbooks/cache-invalidation.md`
- Create: `docs/architecture/runbooks/deploy-rollback.md`
- Test: `frontend/src/pages/api-docs/apiDocs.test.ts`
- Test: `frontend/src/pages/api-docs/openApiDocs.test.ts`

### Task 1: Create ADR template and initial ADR list

**Files:**
- Create: `docs/architecture/adr/ADR-0000-template.md`
- Create: `docs/architecture/adr/ADR-0001-web-runtime-model.md`
- Create: `docs/architecture/adr/ADR-0002-node-handler-model.md`
- Create: `docs/architecture/adr/ADR-0003-groovy-expression-engine.md`
- Create: `docs/architecture/adr/ADR-0004-redis-rocketmq-mysql-choices.md`
- Create: `docs/architecture/adr/ADR-0005-data-isolation-model.md`
- Read: `docs/architecture/specs/P3-00-architecture-boundary-review-spec.md`

- [x] Create an ADR template with status, context, decision, alternatives, consequences, rollback trigger, and owner.
- [x] Create initial ADRs for WebFlux/MVC, NodeHandler model, Groovy, Redis/RocketMQ/MySQL, and data isolation.
- [x] Link ADRs to the P0/P1/P3 specs that control the decision.

**Run:**
```bash
test -f docs/architecture/adr/ADR-0000-template.md
rg "Status|Context|Decision|Alternatives|Consequences|Rollback Trigger|Owner" docs/architecture/adr/ADR-0000-template.md
rg "WebFlux|NodeHandler|Groovy|RocketMQ|data isolation" docs/architecture/adr
```

**Expected:** The ADR directory exists, the template has all required sections, and the five initial ADRs cite controlling specs.

### Task 2: Write DAG execution flow and Handler development guide

**Files:**
- Create: `docs/architecture/guides/node-handler-development.md`
- Create: `docs/architecture/runbooks/dag-execution-flow.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/dag/DagParser.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/StartHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/WaitHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`

- [x] Document the execution path from trigger admission through DAG parsing, handler dispatch, wait/resume, trace writes, and completion.
- [x] Document NodeHandler registration with `@NodeHandlerType`, supported input/output contracts, blocking-call rules, trace expectations, and mapper access restrictions.
- [x] Include a checklist for adding a new handler with the exact test classes to update.

**Run:**
```bash
test -f docs/architecture/guides/node-handler-development.md
test -f docs/architecture/runbooks/dag-execution-flow.md
rg "DagParser|NodeHandler|@NodeHandlerType|wait/resume|trace|mapper access" docs/architecture/guides/node-handler-development.md docs/architecture/runbooks/dag-execution-flow.md
cd backend && mvn test -pl canvas-engine -Dtest=StartHandlerTest,WaitHandlerTest,NodeRouteResolverTest
```

**Expected:** Guide and flow docs name the concrete engine classes and focused handler tests pass.

### Task 3: Generate Redis key catalog from RedisKeyUtil and related services

**Files:**
- Create: `docs/architecture/reference/redis-key-catalog.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/ContextPersistenceService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/TriggerRouteService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/CanvasEntityCache.java`

- [x] Catalog each key prefix, owner service, TTL, payload shape, invalidation path, and operational risk.
- [x] Include Redis context, trigger route, entity cache, quota, lock, and kill-switch key families.
- [x] Link each key family to its cleanup or retention rule in `docs/architecture/capacity/retention-policy.md` when that file exists.

**Run:**
```bash
test -f docs/architecture/reference/redis-key-catalog.md
rg "prefix|owner service|TTL|payload|invalidation|ContextPersistenceService|TriggerRouteService" docs/architecture/reference/redis-key-catalog.md
cd backend && mvn test -pl canvas-engine -Dtest=CanvasEntityCacheTest,CacheConfigTest
```

**Expected:** Redis catalog traces every key family to an owner and cleanup rule; cache-focused backend tests pass.

### Task 4: Write operational runbooks for DLQ, route rebuild, cache invalidation, deploy, rollback, and incident triage

**Files:**
- Create: `docs/architecture/runbooks/failure-triage.md`
- Create: `docs/architecture/runbooks/dlq-replay.md`
- Create: `docs/architecture/runbooks/route-rebuild.md`
- Create: `docs/architecture/runbooks/cache-invalidation.md`
- Create: `docs/architecture/runbooks/deploy-rollback.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DlqController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/CanvasRouteInitializer.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/cache/RocketMqCacheInvalidationPublisher.java`

- [x] Each runbook must include symptom, severity, dashboard link placeholder, diagnostic commands, remediation commands, rollback commands, and evidence to capture.
- [x] DLQ runbook must name `DlqController`, retry limits, and replay verification.
- [x] Route/cache runbooks must name route rebuild and cache invalidation services.

**Run:**
```bash
test -f docs/architecture/runbooks/failure-triage.md
test -f docs/architecture/runbooks/dlq-replay.md
test -f docs/architecture/runbooks/route-rebuild.md
test -f docs/architecture/runbooks/cache-invalidation.md
test -f docs/architecture/runbooks/deploy-rollback.md
rg "symptom|severity|diagnostic commands|remediation commands|rollback commands|evidence" docs/architecture/runbooks
cd backend && mvn test -pl canvas-engine -Dtest=OpsControllerTemplateTest,CanvasExecutionDlqSchemaTest
```

**Expected:** Runbooks are command-oriented and backend ops/DLQ tests pass.

### Task 5: Add OpenAPI annotation and spec work to API contract implementation

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `frontend/src/pages/api-docs/openApiDocs.ts`
- Modify: `frontend/src/pages/api-docs/apiDocs.ts`
- Test: `frontend/src/pages/api-docs/apiDocs.test.ts`
- Test: `frontend/src/pages/api-docs/openApiDocs.test.ts`

- [x] Add OpenAPI tags, operation names, request schemas, response schemas, and auth notes for core Canvas, Execution, and Audience routes.
- [x] Ensure frontend API docs merge generated OpenAPI with local overrides without losing route metadata.
- [x] Add tests that fail when a documented route loses method, path, auth, or response metadata.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasStatsControllerTest,ExecutionControllerTest,AudienceControllerTest
cd frontend && npm test -- api-docs
```

**Expected:** Backend controller tests and frontend API-doc tests pass with OpenAPI metadata preserved.

### Task 6: Keep archive docs as historical evidence, not active specs

**Files:**
- Modify: `docs/architecture/specs/README.md`
- Modify: `docs/architecture/plans/README.md`
- Read: `docs/architecture/archive/reviews/architect-checklist-report.md`
- Read: `docs/architecture/archive/evolution/production-practice-review.md`

- [x] Add a short policy that active decisions live in specs, plans, ADRs, guides, reference docs, and runbooks.
- [x] State that archived reviews are source evidence and must be cited by active docs before implementation.
- [x] Link the policy from both specs and plans README files.

**Run:**
```bash
rg "archive|historical evidence|active decisions|ADRs|runbooks" docs/architecture/specs/README.md docs/architecture/plans/README.md
```

**Expected:** Specs and plans README files explain how archived docs feed active architecture decisions.

### Task 7: Handoff scoped documentation, ADR, and runbook changes

**Files:**
- Modify: `docs/architecture/plans/P2-03-documentation-adr-and-runbooks-plan.md`
- Modify: `docs/architecture/specs/README.md`
- Modify: `docs/architecture/plans/README.md`
- Create: `docs/architecture/adr/`
- Create: `docs/architecture/guides/node-handler-development.md`
- Create: `docs/architecture/reference/redis-key-catalog.md`
- Create: `docs/architecture/runbooks/`

- [x] Review only docs and tests named in this plan.
- [x] Record evidence for active docs, ADRs, runbooks, OpenAPI changes, and tests.
- [x] Leave staging and commit to the user-controlled handoff because the current worktree contains unrelated active changes.

**Run:**
```bash
git diff -- docs/architecture/plans/P2-03-documentation-adr-and-runbooks-plan.md docs/architecture/specs/P2-03-documentation-adr-and-runbooks-spec.md docs/architecture/specs/README.md docs/architecture/plans/README.md docs/architecture/evidence/P2-03-documentation-adr-and-runbooks.md docs/architecture/adr docs/architecture/guides docs/architecture/reference docs/architecture/runbooks backend/canvas-engine/src/main/java/org/chovy/canvas/config/OpenApiSecurityConfig.java backend/canvas-engine/src/main/java/org/chovy/canvas/web frontend/src/pages/api-docs backend/canvas-engine/src/test/java/org/chovy/canvas/domain/analytics/AudienceMaterializationScheduleServiceTest.java
```

**Expected:** The handoff contains active architecture documentation, OpenAPI metadata changes, related tests, and evidence scoped to this package; no commit is created automatically.
