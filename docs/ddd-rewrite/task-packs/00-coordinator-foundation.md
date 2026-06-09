# Task Pack 00: Coordinator Foundation

**Owner:** Main coordinator

**Program:** DDD modular rewrite

**Task id:** DDD-C00

**Readiness level:** R1 foundation

**Target backend state:** DDD_FINAL_MODULE skeleton only

**Goal:** Create the rewrite skeleton, guardrails, inventory outputs, and
subagent-ready execution boundaries before any worker rewrites a context.

---

## Allowed Write Scope

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-boot/**
backend/canvas-web/**
backend/canvas-context-*/**
backend/canvas-platform/**
docs/ddd-rewrite/**
```

Workers must not edit this scope unless the coordinator explicitly assigns it.

## Forbidden Changes

```text
business behavior rewrites inside context modules
controller migration from canvas-engine to canvas-web
runtime plugin implementations
template import implementation
DSL import/export implementation
AI journey backend implementation
old canvas-engine removal
production or staging profile semantic changes
```

## Run-With Constraints

Can run with:

```text
read-only DDD explorers
OSG docs-only workers
OSG frontend-only workers
OSG local CLI validate/diff worker
```

Must not run with:

```text
any DDD code-writing worker
DDD-C07
DDD-C09
any worker editing backend/pom.xml or module pom.xml files
```

Foundation is the only task allowed to create or pre-seed module POM files
before worker dispatch. Later module POM edits require explicit coordinator
handoff in `subagent-worker-packets.md`.

---

## Required Inputs

```text
docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-spec.md
docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-plan.md
docs/ddd-rewrite/inventory/context-ownership-seed.md
```

---

## Required Outputs

```text
backend/canvas-common/pom.xml
backend/canvas-context-canvas/pom.xml
backend/canvas-context-execution/pom.xml
backend/canvas-context-marketing/pom.xml
backend/canvas-context-cdp/pom.xml
backend/canvas-context-bi/pom.xml
backend/canvas-context-risk/pom.xml
backend/canvas-context-conversation/pom.xml
backend/canvas-platform/pom.xml
backend/canvas-web/pom.xml
backend/canvas-boot/pom.xml
backend/canvas-boot/src/test/java/org/chovy/canvas/architecture/ModularArchitectureTest.java
docs/ddd-rewrite/inventory/http-api-inventory.md
docs/ddd-rewrite/inventory/persistence-ownership.md
docs/ddd-rewrite/inventory/service-ownership.md
docs/ddd-rewrite/inventory/test-ownership.md
docs/ddd-rewrite/inventory/cross-context-dependencies.md
```

---

## Steps

- [ ] Create Maven modules listed in the spec.
- [ ] Add minimal package markers for `api`, `application`, `domain`,
      `adapter.persistence`, `adapter.messaging`, `adapter.external`, and
      `config`.
- [ ] Pre-seed each module POM with the baseline dependencies needed by its
      task pack tests. Later workers may edit their own module POM only; root
      `backend/pom.xml` remains coordinator-owned.
- [ ] Move only business-neutral common types to `canvas-common`.
- [ ] Create `canvas-boot` startup shell and architecture test shell.
- [ ] Create `canvas-web` support shell without migrating controllers.
- [ ] Generate inventory files using the commands in
      `docs/ddd-rewrite/inventory/README.md`.
- [ ] Assign ambiguous ownership entries in the inventory before worker packs
      use them.
- [ ] Run `cd backend && mvn -q -DskipTests install`.
- [ ] Run `cd backend && mvn test -pl canvas-boot -Dtest=ModularArchitectureTest`.
- [ ] Run `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`.

---

## Review Gates

- [ ] No worker context code has been moved before module skeleton passes.
- [ ] Architecture tests exist before context migration starts.
- [ ] `canvas-common` contains no business enum dumping.
- [ ] Inventory files exist and are specific enough for worker handoff.
- [ ] Dirty user worktree state is recorded before rewrite code begins.
- [ ] Guardrail check script runs without syntax/runtime error.

## Verification

```bash
cd backend
mvn -q -DskipTests install
mvn test -pl canvas-boot -Dtest=ModularArchitectureTest
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
test -f docs/ddd-rewrite/inventory/http-api-inventory.md
test -f docs/ddd-rewrite/inventory/persistence-ownership.md
test -f docs/ddd-rewrite/inventory/service-ownership.md
test -f docs/ddd-rewrite/inventory/test-ownership.md
test -f docs/ddd-rewrite/inventory/cross-context-dependencies.md
```

## Rollback

Revert only module skeletons, package markers, architecture test shell, and
generated inventory files created by this task. Do not revert unrelated user
work or prior Open Source Growth docs.

## Coordinator Response

Return:

```text
status:
files changed:
modules created:
inventory files created:
tests run:
guardrail checks:
open risks:
worker dispatch constraints:
```
