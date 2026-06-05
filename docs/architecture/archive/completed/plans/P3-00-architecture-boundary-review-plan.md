# Architecture Boundary Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `../specs/P3-00-architecture-boundary-review-spec.md` the enforced gate for every P3 service split, data platform, WeCom, Kubernetes, and platform-component proposal.

**Architecture:** Keep the current canvas application as a modular monolith until a downstream P3 item proves data ownership, API/event contracts, tenant propagation, observability, rollout, rollback, and characterization tests. Use the seven bounded contexts from the review as the default boundary model.

**Tech Stack:** Markdown evidence docs, ADRs, Java 21, Spring Boot 3.2, MyBatis-Plus, Redis, RocketMQ, React 18, TypeScript, JUnit 5, Vitest.

---

## Source Material

- Spec: `../specs/P3-00-architecture-boundary-review-spec.md`
- Code verification: `../specs/P3-00-architecture-boundary-code-verification.md`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`
- Related specs: `../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`, `../specs/P3-03-data-platform-architecture-spec.md`, `../specs/P3-08-wecom-scrm-module-spec.md`

## File Structure

- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-code-verification.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Create: `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
- Modify: `docs/architecture/decisions/adr/ADR-0000-template.md`
- Create: `docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`

### Task 1: Refresh the boundary evidence gate

**Files:**
- Create: `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-code-verification.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`

- [x] Re-run package, controller, mapper, and migration inventory commands against the current repo.
- [x] Record the seven bounded contexts, current code anchors, direct cross-context imports, shared mapper access, and tenant propagation gaps.
- [x] Record the recommendation: modular-monolith boundary cleanup before physical service extraction.

**Run:**
```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 2 -type d | sort
rg "Mapper|StringRedisTemplate|RocketMQTemplate|WebClient|@Transactional" backend/canvas-engine/src/main/java/org/chovy/canvas/domain backend/canvas-engine/src/main/java/org/chovy/canvas/engine backend/canvas-engine/src/main/java/org/chovy/canvas/web
test -f docs/architecture/evidence/p3-00-architecture-boundary-review.md
rg "Canvas Authoring|Execution Runtime|CDP / Audience|Reach / Notification|Integration|Platform|Data Platform / Analytics|modular monolith" docs/architecture/evidence/p3-00-architecture-boundary-review.md
```

**Expected:** Evidence file exists and confirms the seven-context boundary model with current code anchors and current blockers.

### Task 2: Apply the boundary review to downstream P3 specs and plans

**Files:**
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-03-data-platform-architecture-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-06-k8s-deployment-platform-spec.md`
- Read: `docs/architecture/archive/completed/specs/P3-08-wecom-scrm-module-spec.md`
- Modify: `docs/architecture/archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`
- Modify: `docs/architecture/archive/completed/plans/P3-03-data-platform-architecture-plan.md`
- Modify: `docs/architecture/archive/completed/plans/P3-08-wecom-scrm-module-plan.md`

- [x] Check each P3 plan for immediate physical service extraction language.
- [x] Require every service, data platform, and WeCom proposal to reference the boundary review and evidence file.
- [x] Ensure K8s and production-component plans are platform decisions, not domain-boundary drivers.

**Run:**
```bash
rg "P3-00-architecture-boundary-review-spec.md|p3-00-architecture-boundary-review.md|service extraction|bounded context" docs/architecture/archive/completed/plans/P3-*.md docs/architecture/archive/completed/specs/P3-*.md
rg "immediate physical service|split all services|big-bang" docs/architecture/archive/completed/plans/P3-*.md docs/architecture/archive/completed/specs/P3-*.md --glob '!P3-00-architecture-boundary-review-plan.md'
```

**Expected:** Downstream P3 plans reference the boundary review gate, and no plan requires an immediate broad service split.

### Task 3: Gate service extraction with ADRs

**Files:**
- Modify: `docs/architecture/decisions/adr/ADR-0000-template.md`
- Create: `docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md`
- Read: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/evidence/p3-00-architecture-boundary-review.md`

- [x] Ensure the ADR template includes context, decision, alternatives, data ownership, API contracts, event contracts, rollout, rollback, observability, tenant propagation, idempotency, and exit criteria.
- [x] Create a service-extraction gate ADR that blocks extraction until every trigger in the review spec is satisfied.
- [x] Link the gate ADR from downstream P3 plans that propose service extraction.

**Run:**
```bash
test -f docs/architecture/decisions/adr/ADR-0000-template.md
test -f docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md
rg "Data Ownership|API Contracts|Event Contracts|Rollback|Observability|Tenant Propagation|Idempotency|Exit Criteria" docs/architecture/decisions/adr/ADR-0000-template.md docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md
```

**Expected:** ADR template and service-extraction gate ADR contain every gate required by the boundary review.

### Task 4: Add characterization tests before extraction

**Files:**
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CanvasUserQueryServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/NotificationServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/tenant/TenantServiceTest.java`

- [x] For each extraction candidate, name the context and current code anchors from the seven-context table.
- [x] Add characterization tests for current API behavior, mapper reads/writes, tenant behavior, failure behavior, and event/notification behavior.
- [x] Run the characterization tests before moving packages, tables, or APIs.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=CanvasUserQueryServiceTest,NotificationServiceTest,TenantServiceTest
```

**Expected:** Candidate-context characterization tests pass against current monolith behavior before any extraction begins.

### Task 5: Commit scoped boundary-review gate changes

**Files:**
- Modify: `docs/architecture/archive/completed/plans/P3-00-architecture-boundary-review-plan.md`
- Create: `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
- Modify: `docs/architecture/decisions/adr/ADR-0000-template.md`
- Create: `docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md`

- [x] Review only boundary evidence, ADR, and P3 plan files named in this plan.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record verification commands and remaining follow-ups in evidence.

**Run:**
```bash
git diff -- docs/architecture/archive/completed/plans/P3-00-architecture-boundary-review-plan.md docs/architecture/evidence/p3-00-architecture-boundary-review.md docs/architecture/decisions/adr/ADR-0000-template.md docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md docs/architecture/archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md docs/architecture/archive/completed/plans/P3-03-data-platform-architecture-plan.md docs/architecture/archive/completed/plans/P3-06-k8s-deployment-platform-plan.md docs/architecture/archive/completed/plans/P3-07-production-platform-components-plan.md docs/architecture/archive/completed/plans/P3-08-wecom-scrm-module-plan.md
```

**Expected:** The diff contains only the boundary-review plan, evidence file, ADR gate files, and downstream P3 gate references. No commit is created by default.
