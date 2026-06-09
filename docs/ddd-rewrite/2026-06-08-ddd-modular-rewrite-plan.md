# DDD Modular Rewrite Implementation and Collaboration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> `superpowers:subagent-driven-development` for task execution after this plan
> is approved. Use `superpowers:using-git-worktrees` before implementation to
> isolate the rewrite. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the backend as a DDD-style modular monolith while preserving
observable behavior, API compatibility, and database compatibility for the first
cutover.

**Architecture:** Build new Maven modules in parallel with the existing
`canvas-engine`. The new `canvas-boot` module becomes the runtime assembly only
after context modules, web adapters, architecture tests, and contract tests pass.
The old `canvas-engine` remains a behavior reference until cutover.

**Tech Stack:** Java 21, Maven 3.9+, Spring Boot 3.2.5, WebFlux, MyBatis-Plus,
Flyway, Redis, RocketMQ, JUnit 5, AssertJ, ArchUnit or Spring Modulith.

---

## 1. Collaboration Model

All agents must read the companion conventions file before implementation:

```text
docs/ddd-rewrite/2026-06-08-ddd-rewrite-conventions-and-examples.md
```

All agents must also read the guardrail index:

```text
docs/ddd-rewrite/guardrails/README.md
```

The conventions file is the operational rulebook for class placement, naming,
migration examples, anti-patterns, and worker output format. The guardrail files
define failure modes, automated checks, review gates, and worker enforcement.

### 1.1 Roles

```text
Main Coordinator
  Owns architecture decisions, task decomposition, cross-module contracts,
  integration, final verification, and cutover.

Explorer Agents
  Read-only agents that map old code, controllers, data objects, mappers,
  tests, and coupling points by context.

Worker Agents
  Code-writing agents. Each worker owns exactly one bounded context or one
  infrastructure slice. Workers must not edit files outside their assigned
  write scope.

Reviewer Agents
  Review spec compliance, code quality, architecture rule compliance, and
  integration risks after each worker task.
```

### 1.2 Parallelization Rules

Parallelize only when write scopes do not overlap.

Allowed parallel work:

```text
Explorer A: map marketing old code
Explorer B: map risk old code
Explorer C: map CDP old code
Explorer D: map BI old code

Worker A: backend/canvas-context-marketing/**
Worker B: backend/canvas-context-risk/**
Worker C: backend/canvas-platform/**
```

Not allowed:

```text
Two workers editing backend/pom.xml at the same time
Two workers editing canvas-common at the same time
Two workers editing canvas-web controllers at the same time
Two workers changing the same cross-context API
Workers changing another worker's context module
```

### 1.3 Worktree Strategy

Use one isolated rewrite workspace for the main coordinator. If the platform
supports forked subagent workspaces, each worker edits in its forked workspace
and returns changed paths. If workers share one workspace, do not run code-writing
workers in parallel; use explorers in parallel and workers sequentially.

Recommended branch:

```bash
git checkout -b feat/ddd-modular-rewrite
```

Recommended local worktree path:

```text
.worktrees/feat-ddd-modular-rewrite
```

### 1.4 Worker Prompt Template

Use this template for code-writing workers:

```text
You are not alone in the codebase. Other agents may be editing other modules.
Do not revert edits you did not make. Do not modify files outside your assigned
write scope.

Assigned context:
  <context-name>

Allowed write scope:
  <exact directories>

Forbidden write scope:
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/pom.xml
  any other canvas-context-* module

Goal:
  Rebuild this context using api/application/domain/adapter package layout.
  Preserve existing behavior needed by current HTTP APIs and tests.

Rules:
  - domain must not depend on Spring Web, MyBatis, Redis, RocketMQ, or WebClient.
  - data objects and mappers belong in adapter.persistence.
  - application services own transactions and use-case orchestration.
  - cross-context references must use api types or ports.
  - do not invent new database tables in the first milestone.

Return:
  status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT, or BLOCKED
  task id:
  dispatch id:
  branch:
  worktree:
  base commit:
  head commit:
  files changed:
  contracts changed:
  old classes migrated:
  tests run:
  verification result:
  verification output summary/path:
  evidence artifact paths:
  open risks:
  coordinator actions needed:
  ledger update:
  rollback path:
```

---

## 2. Phase Overview

```text
Phase 0: Baseline and inventory
Phase 1: Module skeleton and architecture guardrails
Phase 2: Common, boot, and web shell
Phase 3: Low-coupling context rewrites in parallel
Phase 4: Medium-coupling context rewrites in parallel
Phase 5: Canvas and execution contract split
Phase 6: Canvas rewrite
Phase 7: Execution rewrite
Phase 8: Web adapter cutover
Phase 9: Runtime assembly and migration cleanup
Phase 10: Verification, cutover, and deletion of old engine
```

The main coordinator should commit after each phase and after each context
integration.

---

## 3. Phase 0: Baseline and Inventory

### Task 0.0: Capture Backup And Rollback Point

**Files:**

- Read: `docs/program-coordination/backup-and-rollback-runbook.md`
- Create: `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`
- Modify: `docs/program-coordination/progress-ledger.md`
- Modify: `docs/program-coordination/dispatch-state.json`

- [ ] Run the pre-rewrite backup procedure from
      `docs/program-coordination/backup-and-rollback-runbook.md`.

- [ ] Create
      `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` with
      the backup tag, bundle path, dirty diff path, untracked manifest path,
      data backup decision, and restore notes.

- [ ] Run G0B:

```bash
git status --short
git branch --show-current
git rev-parse HEAD
git worktree list
test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md
```

Expected: commands complete and the manifest exists before any code-writing
dispatch or coordinator code task starts.

- [ ] Record the manifest path and backup tag in
      `docs/program-coordination/progress-ledger.md` and
      `docs/program-coordination/dispatch-state.json`.

### Task 0.1: Capture Clean Baseline

**Files:**

- Read: `backend/pom.xml`
- Read: `backend/canvas-engine/pom.xml`
- Read: `frontend/package.json`
- Create: `docs/ddd-rewrite/evidence/baseline-2026-06-08.md`

- [ ] Run backend baseline:

```bash
cd backend
mvn clean install
```

Expected: pass, or record pre-existing failures in
`docs/ddd-rewrite/evidence/baseline-2026-06-08.md`.

- [ ] Run frontend baseline:

```bash
cd frontend
npm run build
npm run test
```

Expected: pass, or record pre-existing failures in the baseline evidence file.

- [ ] Record active dirty worktree state:

```bash
git status --short > docs/ddd-rewrite/evidence/git-status-before-rewrite.txt
```

Expected: file records user-existing changes so rewrite work does not silently
overwrite them.

### Task 0.2: Generate API Inventory

**Files:**

- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/**/*.java`
- Create: `docs/ddd-rewrite/inventory/http-api-inventory.md`

- [ ] List every controller, request mapping, HTTP method, path, request type,
      response type, and owning target context.

- [ ] Mark API compatibility level:

```text
MUST_KEEP_EXACT
MAY_ADD_FIELDS
MAY_DEPRECATE_LATER
INTERNAL_ONLY
```

### Task 0.3: Generate Persistence Inventory

**Files:**

- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/**/*.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/**/*.java`
- Create: `docs/ddd-rewrite/inventory/persistence-ownership.md`

- [ ] Map every `*DO` and `*Mapper` to one target context.

- [ ] Mark ambiguous ownership entries with an explicit coordinator decision.

### Task 0.4: Generate Service Inventory

**Files:**

- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/**/*.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/**/*.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/platform/**/*.java`
- Create: `docs/ddd-rewrite/inventory/service-ownership.md`

- [ ] Map every service to:

```text
domain model
application service
persistence adapter
external adapter
runtime infrastructure
delete or merge candidate
```

### Task 0.5: Commit Inventory

```bash
git add docs/ddd-rewrite/evidence docs/ddd-rewrite/inventory
git commit -m "docs: inventory ddd rewrite baseline"
```

---

## 4. Phase 1: Module Skeleton and Architecture Guardrails

### Task 1.1: Create Maven Module Skeletons

**Files:**

- Modify: `backend/pom.xml`
- Create:
  - `backend/canvas-common/pom.xml`
  - `backend/canvas-context-canvas/pom.xml`
  - `backend/canvas-context-execution/pom.xml`
  - `backend/canvas-context-marketing/pom.xml`
  - `backend/canvas-context-cdp/pom.xml`
  - `backend/canvas-context-bi/pom.xml`
  - `backend/canvas-context-risk/pom.xml`
  - `backend/canvas-context-conversation/pom.xml`
  - `backend/canvas-platform/pom.xml`
  - `backend/canvas-web/pom.xml`
  - `backend/canvas-boot/pom.xml`

- [ ] Add modules to `backend/pom.xml` in this order:

```xml
<modules>
    <module>canvas-common</module>
    <module>canvas-cache-sdk</module>
    <module>canvas-context-canvas</module>
    <module>canvas-context-execution</module>
    <module>canvas-context-marketing</module>
    <module>canvas-context-cdp</module>
    <module>canvas-context-bi</module>
    <module>canvas-context-risk</module>
    <module>canvas-context-conversation</module>
    <module>canvas-platform</module>
    <module>canvas-web</module>
    <module>canvas-boot</module>
    <module>canvas-flink-jobs</module>
</modules>
```

- [ ] Each new module POM uses parent:

```xml
<parent>
    <groupId>org.chovy</groupId>
    <artifactId>canvas-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

- [ ] Run:

```bash
cd backend
mvn -q -DskipTests install
```

Expected: all new empty modules compile.

### Task 1.2: Add Package Markers

**Files:**

- Create one `package-info.java` per package root:

```text
backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/package-info.java
backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/package-info.java
backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/package-info.java
backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/package-info.java
```

Repeat for each context.

- [ ] Package Javadoc states allowed responsibilities for that package.

### Task 1.3: Add Architecture Test Module

**Files:**

- Modify: `backend/canvas-boot/pom.xml`
- Create: `backend/canvas-boot/src/test/java/org/chovy/canvas/architecture/ModularArchitectureTest.java`

- [ ] Add ArchUnit test dependency to `canvas-boot`.

- [ ] Implement rules:

```text
classes in ..domain.. must not depend on Spring Web
classes in ..domain.. must not depend on MyBatis
classes in ..domain.. must not depend on Redis
classes in ..domain.. must not depend on RocketMQ
classes in ..web.. must not depend on ..adapter.persistence..
classes named *Controller must not depend on classes named *Mapper
classes named *Controller must not depend on classes named *DO
```

- [ ] Run:

```bash
cd backend
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

Expected: passes for new modules; old `canvas-engine` may be excluded from the
first guardrail run until migration begins.

### Task 1.4: Commit Skeleton

```bash
git add backend/pom.xml backend/canvas-*/pom.xml backend/canvas-*/src
git commit -m "refactor: add ddd modular backend skeleton"
```

---

## 5. Phase 2: Common, Boot, and Web Shell

### Task 2.1: Move Minimal Common Types

**Files:**

- Create:
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/R.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/PageResult.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/ErrorCode.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/tenant/TenantContext.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/tenant/TenantScopeSupport.java`
  - `backend/canvas-common/src/main/java/org/chovy/canvas/common/validation/ApiRequestValidation.java`

- [ ] Copy behavior from old `canvas-engine` common classes.

- [ ] Do not move business enums into common unless the coordinator approves.

- [ ] Run:

```bash
cd backend
mvn test -pl canvas-common
```

Expected: compile/test pass.

### Task 2.2: Create Boot Runtime Shell

**Files:**

- Create: `backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasEngineApplication.java`
- Create: `backend/canvas-boot/src/main/resources/application.yml`
- Move later: Flyway resources from `canvas-engine` after context migration is
  integrated.

- [ ] `CanvasEngineApplication` contains only Spring Boot startup and global
      configuration property enablement.

- [ ] Run:

```bash
cd backend
mvn test -pl canvas-boot
```

Expected: compile/test pass.

### Task 2.3: Create Web Shell

**Files:**

- Create:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/package-info.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/support/ReactiveTenantSupport.java`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/support/ResponseSupport.java`

- [ ] Web shell depends on `canvas-common`.

- [ ] No controller migration yet.

### Task 2.4: Commit Shell

```bash
git add backend/canvas-common backend/canvas-boot backend/canvas-web
git commit -m "refactor: add common boot and web shells"
```

---

## 6. Phase 3: Low-Coupling Context Rewrites

Run the three context workers in parallel only if they have isolated workspaces
or forked subagent workspaces.

### Task 3.1: Rewrite Platform Module

**Worker:** `platform-worker`

**Allowed write scope:**

```text
backend/canvas-platform/**
```

**Read scope:**

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/platform/**
backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/**
backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/**
backend/canvas-engine/src/test/java/org/chovy/canvas/platform/**
```

**Required package result:**

```text
org.chovy.canvas.platform.api
org.chovy.canvas.platform.application
org.chovy.canvas.platform.domain
org.chovy.canvas.platform.adapter.persistence
```

**Acceptance:**

- [ ] `PlatformWorkstreamService` becomes an application service or facade.
- [ ] `WorkstreamRepository` is a domain/application port.
- [ ] JDBC/MyBatis implementation lives in `adapter.persistence`.
- [ ] Existing platform tests are ported or equivalent tests are added.

### Task 3.2: Rewrite Risk Module

**Worker:** `risk-worker`

**Allowed write scope:**

```text
backend/canvas-context-risk/**
```

**Read scope:**

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/**
backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Risk*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Risk*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/**
backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/**
```

**Required package result:**

```text
org.chovy.canvas.risk.api
org.chovy.canvas.risk.application
org.chovy.canvas.risk.domain
org.chovy.canvas.risk.adapter.persistence
```

**Acceptance:**

- [ ] `RiskDecisionFacade` exists in `api`.
- [ ] Risk strategies/lists/decisions are not exposed as DOs outside the module.
- [ ] Execution can call risk through `api`, not through mappers.
- [ ] Risk domain tests cover strategy status, list matching, and decision result
      construction.

### Task 3.3: Rewrite Marketing Module

**Worker:** `marketing-worker`

**Allowed write scope:**

```text
backend/canvas-context-marketing/**
```

**Read scope:**

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/**
backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Marketing*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Marketing*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/**
```

**Required package result:**

```text
org.chovy.canvas.marketing.api
org.chovy.canvas.marketing.application
org.chovy.canvas.marketing.domain
org.chovy.canvas.marketing.adapter.persistence
org.chovy.canvas.marketing.adapter.external
```

**Acceptance:**

- [ ] `MarketingCampaignService` is split into application service, domain
      model/policy, repository interface, and MyBatis repository implementation.
- [ ] Campaign command/view types remain compatible with current controller
      behavior.
- [ ] Integration contract probe client lives in `adapter.external`.
- [ ] Domain does not depend on `ObjectMapper`, MyBatis, or WebFlux.

### Task 3.4: Integrate Phase 3

**Coordinator only.**

- [ ] Review worker changed files and reject out-of-scope edits.
- [ ] Add required module dependencies in `backend/pom.xml` and module POMs.
- [ ] Run:

```bash
cd backend
mvn test -pl canvas-platform,canvas-context-risk,canvas-context-marketing
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

- [ ] Commit:

```bash
git add backend/canvas-platform backend/canvas-context-risk backend/canvas-context-marketing backend/pom.xml
git commit -m "refactor: rewrite low-coupling ddd contexts"
```

---

## 7. Phase 4: Medium-Coupling Context Rewrites

### Task 4.1: Rewrite CDP Module

**Worker:** `cdp-worker`

**Allowed write scope:**

```text
backend/canvas-context-cdp/**
```

**Acceptance:**

- [ ] CDP owns `Cdp*`, `Audience*`, `Tag*`, and warehouse-related persistence
      classes assigned by inventory.
- [ ] `AudienceSnapshotFacade` or equivalent exists in `api` for execution.
- [ ] Doris/warehouse adapters live in `adapter.external` or
      `adapter.persistence` depending on whether they are internal persistence
      or external analytical storage.

### Task 4.2: Rewrite BI Module

**Worker:** `bi-worker`

**Allowed write scope:**

```text
backend/canvas-context-bi/**
```

**Acceptance:**

- [ ] BI owns `Bi*` persistence classes.
- [ ] Dashboard/chart/dataset/portal application services expose stable API
      views for `canvas-web`.
- [ ] BI collaboration/permission rules are not implemented in controllers.

### Task 4.3: Rewrite Conversation Module

**Worker:** `conversation-worker`

**Allowed write scope:**

```text
backend/canvas-context-conversation/**
```

**Acceptance:**

- [ ] Conversation owns `Conversation*` persistence classes.
- [ ] Public webhook input is represented as API/application command, not as
      persistence data object.
- [ ] AI reply and routing external calls live in adapters.

### Task 4.4: Integrate Phase 4

**Coordinator only.**

- [ ] Review worker changed files.
- [ ] Resolve API type conflicts.
- [ ] Run:

```bash
cd backend
mvn test -pl canvas-context-cdp,canvas-context-bi,canvas-context-conversation
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

- [ ] Commit:

```bash
git add backend/canvas-context-cdp backend/canvas-context-bi backend/canvas-context-conversation
git commit -m "refactor: rewrite cdp bi and conversation contexts"
```

---

## 8. Phase 5: Canvas and Execution Contract Split

This phase must be coordinated before either canvas or execution is rewritten.

### Task 5.1: Define Canvas/Execution API Contracts

**Files:**

- Create:
  - `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinition.java`
  - `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionProvider.java`
  - `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ExecutionPublicationPort.java`
  - `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/ExecutionRequestCommand.java`
  - `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/ExecutionResultView.java`
  - `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java`

**Contract rules:**

- `PublishedCanvasDefinition` is immutable.
- It contains enough information for execution runtime to run without loading
  `CanvasDO` or `CanvasVersionDO`.
- Execution runtime may use canvas API, not canvas persistence.

### Task 5.2: Add Contract Tests

**Files:**

- Create:
  - `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionTest.java`
  - `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/ExecutionRequestCommandTest.java`

- [ ] Tests verify null/blank rejection for required IDs and graph payload.
- [ ] Tests verify immutability for collection fields.

### Task 5.3: Commit Contract Split

```bash
git add backend/canvas-context-canvas backend/canvas-context-execution
git commit -m "refactor: define canvas execution contracts"
```

---

## 9. Phase 6: Canvas Rewrite

### Task 6.1: Rewrite Canvas Domain and Application

**Worker:** `canvas-worker`

**Allowed write scope:**

```text
backend/canvas-context-canvas/**
```

**Read scope:**

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Canvas*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Canvas*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/**
```

**Required split:**

```text
CanvasDraftApplicationService
CanvasVersionApplicationService
CanvasPublishApplicationService
CanvasQueryApplicationService
CanvasProjectFolderApplicationService
```

**Acceptance:**

- [ ] Publish workflow uses `ExecutionPublicationPort`.
- [ ] Canvas domain does not depend on Redis, scheduler, execution service, or
      MyBatis.
- [ ] Canvas persistence classes live in `adapter.persistence`.
- [ ] State transition policy is a domain policy.
- [ ] Existing canvas tests are ported or equivalent tests exist.

### Task 6.2: Integrate Canvas

**Coordinator only.**

- [ ] Run:

```bash
cd backend
mvn test -pl canvas-context-canvas
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

- [ ] Commit:

```bash
git add backend/canvas-context-canvas
git commit -m "refactor: rewrite canvas context"
```

---

## 10. Phase 7: Execution Rewrite

### Task 7.1: Rewrite Execution Domain and Runtime

**Worker:** `execution-worker`

**Allowed write scope:**

```text
backend/canvas-context-execution/**
```

**Read scope:**

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/engine/**
backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/**
backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/*Execution*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/*Execution*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/engine/**
```

**Required split:**

```text
CanvasExecutionApplicationService
CanvasTriggerApplicationService
CanvasSchedulerApplicationService
ExecutionRecoveryApplicationService
DagRuntimeService
NodeHandlerRegistry
ExecutionTraceService
```

**Acceptance:**

- [ ] Node handlers remain registered through the local execution module.
- [ ] Risk decision node calls `risk.api`, not risk persistence.
- [ ] Audience/CDP nodes call `cdp.api`, not CDP persistence.
- [ ] MQ and Redis code live in adapters.
- [ ] Execution domain/runtime does not depend on canvas persistence.

### Task 7.2: Integrate Execution

**Coordinator only.**

- [ ] Run:

```bash
cd backend
mvn test -pl canvas-context-execution
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

- [ ] Commit:

```bash
git add backend/canvas-context-execution
git commit -m "refactor: rewrite execution context"
```

---

## 11. Phase 8: Web Adapter Cutover

### Task 8.1: Migrate Controllers to `canvas-web`

**Worker model:** sequential, coordinator-owned, because controllers integrate
all contexts.

**Files:**

- Move/recreate controllers from:

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/web/**
```

- Into:

```text
backend/canvas-web/src/main/java/org/chovy/canvas/web/**
```

**Rules:**

- Controllers depend on application facade or `api` types.
- Controllers do not depend on `*Mapper`, `*DO`, or `adapter.persistence`.
- Existing request paths remain compatible.
- Existing response envelope `R<T>` remains compatible.

### Task 8.2: Port Controller Tests

**Files:**

- Move/recreate controller tests from:

```text
backend/canvas-engine/src/test/java/org/chovy/canvas/web/**
backend/canvas-engine/src/test/java/org/chovy/canvas/controller/**
```

- Into:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/**
```

### Task 8.3: Run Web Tests

```bash
cd backend
mvn test -pl canvas-web
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

### Task 8.4: Commit Web Cutover

```bash
git add backend/canvas-web
git commit -m "refactor: migrate controllers to web module"
```

---

## 12. Phase 9: Runtime Assembly

### Task 9.1: Wire `canvas-boot`

**Files:**

- Modify: `backend/canvas-boot/pom.xml`
- Modify: `backend/canvas-boot/src/main/java/org/chovy/canvas/boot/CanvasEngineApplication.java`
- Move/copy resources:
  - `backend/canvas-engine/src/main/resources/application*.yml`
  - `backend/canvas-engine/src/main/resources/db/migration/**`

**Rules:**

- `canvas-boot` depends on `canvas-web` and all context modules.
- Flyway migration files keep their existing names and content unless a
  separate migration repair spec authorizes a change.
- Mapper scan covers each context's `adapter.persistence` package.

### Task 9.2: Update Local Run Commands

**Files:**

- Modify: `README.md`
- Modify: `docs/INDEX.md` if it references old backend run module.

Command becomes:

```bash
cd backend
mvn -f canvas-boot/pom.xml spring-boot:run
```

### Task 9.3: Runtime Smoke Test

```bash
docker compose -f docker-compose.local.yml up -d
cd backend
mvn -f canvas-boot/pom.xml spring-boot:run
```

Smoke flow:

```text
GET /actuator/health
login or create test auth context
create canvas
save draft version
publish canvas
trigger execution
query execution trace
list marketing campaigns
query risk strategy/decision endpoint
query BI dashboard endpoint
query CDP audience endpoint
```

### Task 9.4: Commit Runtime Assembly

```bash
git add backend/canvas-boot README.md docs/INDEX.md
git commit -m "refactor: assemble ddd modular runtime"
```

---

## 13. Phase 10: Verification and Cutover

### Task 10.1: Full Backend Verification

```bash
cd backend
mvn clean install
```

Expected: pass.

### Task 10.2: Full Frontend Verification

```bash
cd frontend
npm run build
npm run test
```

Expected: pass against compatible API contracts.

### Task 10.3: Architecture Verification

```bash
cd backend
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
```

Expected: pass.

### Task 10.4: Contract Verification

Create or run contract tests that compare old expected shapes to new responses
for critical routes:

```text
canvas CRUD and versioning
canvas publish
execution trigger and trace
marketing campaign/readiness
CDP audience/tag/profile read path
BI dashboard/dataset/chart read path
risk decision path
conversation session/webhook path
```

### Task 10.5: Remove Old Runtime Dependency

**Files:**

- Modify: `backend/pom.xml`
- Delete or archive: `backend/canvas-engine/**` only after all tests pass and
  coordinator approves deletion.

Rule:

- Do not delete `canvas-engine` while any module still depends on it.
- Do not delete old tests until equivalent new tests exist.

### Task 10.6: Final Commit

```bash
git add backend docs README.md
git commit -m "refactor: complete ddd modular backend rewrite"
```

---

## 14. Review Gates

Every worker delivery must pass two reviews before integration.

### 14.1 Spec Compliance Review

Reviewer checks:

- Assigned files only.
- Package layout matches the spec.
- Public API types are in `api`.
- Application services own transactions and orchestration.
- Domain does not depend on framework/infrastructure.
- Persistence code lives in `adapter.persistence`.
- Behavior required by old tests is preserved.
- No failure mode listed in
  `docs/ddd-rewrite/guardrails/llm-drift-and-failure-modes.md` is present.

### 14.2 Code Quality Review

Reviewer checks:

- No large service was copied unchanged when it needed splitting.
- No new global utility package was introduced without approval.
- No business enum was dumped into common without approval.
- Tests are focused and meaningful.
- Cross-context dependencies are explicit and minimal.
- Naming reflects business language.
- The reviewer has run or explicitly justified not running the checks in
  `docs/ddd-rewrite/guardrails/automated-guardrail-checks.md`.

### 14.3 Integration Review

Coordinator checks:

- No dependency cycles.
- No duplicate API concepts across contexts.
- No controller uses DO/Mapper.
- No module reaches into another module's adapter package.
- All module POM dependencies are necessary.
- Worker changed-file lists match the allowed write scopes in the task packs.
- Any temporary bridge has an owner, removal phase, and cutover blocker.

---

## 15. Subagent Dispatch Schedule

### Wave 1: Explorers

Spawn in parallel:

```text
explorer-marketing
explorer-risk
explorer-cdp
explorer-bi
explorer-conversation
explorer-canvas-execution
```

Outputs:

```text
owned controllers
owned services
owned DO/Mapper classes
owned tests
cross-context dependencies
recommended migration order
```

### Wave 2: Low-Coupling Workers

Spawn in parallel only with isolated write scopes:

```text
worker-platform
worker-risk
worker-marketing
```

### Wave 3: Medium-Coupling Workers

Spawn in parallel only after Wave 2 integration:

```text
worker-cdp
worker-bi
worker-conversation
```

### Wave 4: Contract Workers

Do not code in parallel until API contracts are frozen:

```text
coordinator defines canvas/execution contracts
reviewer checks contracts
```

### Wave 5: Core Runtime Workers

Run with stricter coordination:

```text
worker-canvas
worker-execution
```

These may explore in parallel, but implementation should be integrated
sequentially because the contract is shared.

### Wave 6: Web and Boot

Coordinator-owned sequential integration:

```text
canvas-web
canvas-boot
runtime verification
old engine deletion
```

---

## 16. Risk Register

| Risk | Mitigation |
| --- | --- |
| Multiple workers edit shared files | Coordinator owns shared files; workers get disjoint write scopes. |
| `canvas-common` becomes a dumping ground | Common admission rule: only primitives used by at least three contexts and not business-specific. |
| API compatibility breaks frontend | Contract inventory and controller tests before cutover. |
| Domain remains anemic copy of old services | Review gate requires service splitting where old service mixed persistence, infrastructure, and business rules. |
| Canvas/execution coupling blocks migration | Define published canvas and execution ports before rewriting either side. |
| Flyway history breaks | First milestone preserves migration filenames/content; schema changes need separate spec. |
| Old and new runtime drift | Old engine remains behavior reference until cutover; contract tests pin critical behavior. |
| Architecture tests added too late | Add guardrails before context rewrites. |

---

## 17. Definition of Done

The rewrite is done when:

- `backend/canvas-boot` is the only backend application runtime.
- `backend/canvas-engine` is deleted or no longer part of the active Maven
  reactor.
- All context modules compile independently.
- Architecture tests pass.
- Backend full build passes.
- Frontend build and tests pass.
- Critical smoke flows pass locally.
- Controllers do not depend on persistence classes.
- Domain packages do not depend on framework/infrastructure.
- Each table has exactly one owning context.
- Cross-context collaboration uses API packages, ports, or events.

---

## 18. Execution Choice

Recommended execution mode:

```text
Subagent-driven execution
```

Reason:

- Explorers can map independent contexts in parallel.
- Workers can rewrite disjoint modules in parallel.
- Reviewers can check each worker delivery before integration.
- The main coordinator keeps architectural consistency.

Fallback execution mode:

```text
Inline sequential execution
```

Use this if workers cannot get isolated workspaces or if parallel writes are not
supported by the active tool environment.
