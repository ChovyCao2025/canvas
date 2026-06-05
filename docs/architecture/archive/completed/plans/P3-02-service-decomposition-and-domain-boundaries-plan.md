# Service Decomposition And Domain Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a reversible modular-monolith boundary plan for Marketing Canvas and block physical service extraction until ownership, contracts, tests, rollout, and rollback are proven.

**Architecture:** Use the seven-context model from `../specs/P3-00-architecture-boundary-review-spec.md`: Canvas Authoring, Execution Runtime, CDP / Audience, Reach / Notification, Integration, Platform, and Data Platform / Analytics. Start with package and data ownership inside the existing backend; use strangler-style extraction only after `../adr/ADR-0006-service-extraction-gate.md` is satisfied and the candidate ADR is approved.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, Flyway/MySQL, Redis, RocketMQ, OpenAPI, ADRs, JUnit 5, Vitest for any frontend contract impact.

---

## Source Material

- Spec: `../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Boundary review: `../specs/P3-00-architecture-boundary-review-spec.md`
- Boundary evidence: `../evidence/p3-00-architecture-boundary-review.md`
- Service extraction gate: `../adr/ADR-0006-service-extraction-gate.md`
- Source docs: `../archive/evolution/service-architecture-design.md`, `../archive/evolution/target-architecture-overview.md`, `../archive/evolution/architecture-evolution-roadmap.md`
- Coverage matrix: `../../../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
- Read: `docs/architecture/adr/ADR-0006-service-extraction-gate.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Create: `docs/architecture/evidence/p3-02-service-decomposition.md`
- Create: `docs/architecture/domain-map.md`
- Create: `docs/architecture/domain-contract-inventory.md`
- Create: `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java`

### Task 1: Write the context map

**Files:**
- Create: `docs/architecture/evidence/p3-02-service-decomposition.md`
- Create: `docs/architecture/domain-map.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas`
- Read: `backend/canvas-engine/src/main/resources/db/migration`

- [x] Map current packages, controllers, mappers, and migrations to the seven bounded contexts.
- [x] For each context, document purpose, owned data candidates, public APIs, emitted events, consumed events, forbidden dependencies, and current violations.
- [x] Record why Canvas Authoring and Execution Runtime are not first extraction candidates.

**Run:**
```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 3 -type d | sort
rg "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration
test -f docs/architecture/domain-map.md
rg "Canvas Authoring|Execution Runtime|CDP / Audience|Reach / Notification|Integration|Platform|Data Platform / Analytics|forbidden dependencies" docs/architecture/domain-map.md
```

**Expected:** The domain map names all seven contexts, current code anchors, data candidates, and forbidden dependencies.

### Task 2: Choose the first extraction candidate

**Files:**
- Modify: `docs/architecture/domain-map.md`
- Create: `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/adr/ADR-0006-service-extraction-gate.md`
- Read: `docs/architecture/evidence/p3-02-service-decomposition.md`

- [x] Score CDP / Audience, Reach / Notification, and Integration / WeCom on coupling, data ownership clarity, traffic pressure, operational noise, tenant risk, and rollback difficulty.
- [x] Create an ADR naming the first candidate or explicitly deferring extraction.
- [x] Document why other contexts are deferred.

**Run:**
```bash
test -f docs/architecture/adr/ADR-0007-first-extraction-candidate.md
rg "CDP / Audience|Reach / Notification|Integration / WeCom|coupling|data ownership|rollback|Deferred|ADR-0006" docs/architecture/domain-map.md docs/architecture/adr/ADR-0007-first-extraction-candidate.md
```

**Expected:** ADR contains a candidate decision or deferral decision with scoring and rollback rationale.

### Task 3: Define contracts before moving code

**Files:**
- Create: `docs/architecture/domain-contract-inventory.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/RedisKeyUtil.java`

- [x] Inventory REST endpoints, events, Redis keys, MQ topics/tags, tables, DTOs, and auth/tenant assumptions for the chosen context.
- [x] Classify every cross-domain call as synchronous API, event, read model, shared library, or prohibited coupling.
- [x] Name the compatibility window and rollback trigger for every contract.

**Run:**
```bash
test -f docs/architecture/domain-contract-inventory.md
rg "REST|event|Redis key|MQ|table|DTO|tenant|synchronous API|read model|prohibited coupling|compatibility window" docs/architecture/domain-contract-inventory.md
```

**Expected:** Contract inventory covers every external dependency of the chosen context.

### Task 4: Add characterization tests before extraction

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/CanvasExecutionServiceCdpTest.java`
- Test: `frontend/src/services/api.test.ts`

- [x] Add tests for current request/response shape, mapper behavior, tenant scope, idempotency, event emission, and failure behavior for the chosen context.
- [x] Keep tests passing against the existing monolith before moving code.
- [x] Add frontend API contract tests if route payloads change.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasUserQueryServiceTest,NotificationServiceTest,CanvasExecutionServiceCdpTest
cd frontend && npm test -- api
```

**Expected:** Characterization tests pass before package, table, or API movement starts.

### Task 5: Plan the strangler migration

**Files:**
- Modify: `docs/architecture/domain-contract-inventory.md`
- Modify: `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`
- Read: `docs/architecture/runbooks/deploy-rollback.md`

- [x] Document old path, new path, proxy/adapter layer, dual-read or dual-write decision, compatibility window, rollback trigger, and reconciliation flow.
- [x] Define deployment order for schema, backend, frontend, Redis/MQ, dashboards, alerts, and runbooks.
- [x] Require tenant context, trace context, and idempotency propagation across the future boundary.

**Run:**
```bash
rg "old path|new path|dual-read|dual-write|compatibility window|rollback trigger|reconciliation|tenant context|trace context|idempotency|ADR-0006" docs/architecture/domain-contract-inventory.md docs/architecture/adr/ADR-0007-first-extraction-candidate.md
```

**Expected:** Strangler migration plan is reversible and names compatibility, deployment, rollback, and reconciliation details.

### Task 6: Review scoped service decomposition changes

**Files:**
- Modify: `docs/architecture/archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`
- Create: `docs/architecture/evidence/p3-02-service-decomposition.md`
- Create: `docs/architecture/domain-map.md`
- Create: `docs/architecture/domain-contract-inventory.md`
- Create: `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`

- [x] Review only files named in this plan.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record verification commands and remaining follow-ups in evidence.

**Run:**
```bash
git diff -- docs/architecture/archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md docs/architecture/evidence/p3-02-service-decomposition.md docs/architecture/domain-map.md docs/architecture/domain-contract-inventory.md docs/architecture/adr/ADR-0007-first-extraction-candidate.md
```

**Expected:** The diff contains only service decomposition docs, evidence, ADR, and plan changes. No commit is created by default.
