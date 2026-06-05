# WebFlux To MVC Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the WebFlux versus MVC architecture decision with measured evidence before any endpoint migration starts.

**Architecture:** Treat runtime migration as a gated decision. Inventory reactive/blocking boundaries, capture baseline performance and transaction behavior, write an ADR comparing hardening, MVC migration, and hybrid containment, then define the first endpoint migration only if the gates pass.

**Tech Stack:** Spring Boot 3.2, WebFlux, Spring MVC, Reactor, MyBatis-Plus, JUnit 5, Maven, Markdown ADRs.

---

## Source Material

- Spec: `../specs/P3-05-webflux-to-mvc-migration-spec.md`
- Evolution doc: `../archive/evolution/webflux-to-mvc-migration.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-05-webflux-mvc.md`
- Create: `docs/architecture/webflux-mvc-migration-inventory.md`
- Create: `docs/architecture/adr/webflux-vs-mvc.md`
- Create: `docs/architecture/webflux-mvc-first-slice.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/`
- Read: `backend/canvas-engine/src/test/java/org/chovy/canvas/`

### Task 1: Build The Runtime Inventory

**Files:**
- Create: `docs/architecture/webflux-mvc-migration-inventory.md`
- Create: `docs/architecture/evidence/p3-05-webflux-mvc.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/`

- [x] Inventory `Mono`, `Flux`, `.block()`, `.subscribe()`, `Thread.sleep`, `@Transactional`, Redis, RocketMQ, and MyBatis usage.
- [x] Group endpoints by controller, blocking dependency, transaction need, streaming/reactive benefit, and migration risk.
- [x] Record which P0 reactive hazards must be closed before a runtime decision is valid.

Run:

```bash
rg -n "\\.block\\(|\\.subscribe\\(|Thread\\.sleep\\(|@Transactional|Mono<|Flux<|Redis|RocketMQ|MyBatis" backend/canvas-engine/src/main/java/org/chovy/canvas > /tmp/webflux_mvc_inventory.txt
test -s /tmp/webflux_mvc_inventory.txt
test -f docs/architecture/webflux-mvc-migration-inventory.md
rg -n "Controller|blocking dependency|transaction|streaming|migration risk|P0 reactive" docs/architecture/webflux-mvc-migration-inventory.md
```

Expected: inventory names controller groups, blocking dependencies, transaction needs, reactive value, migration risk, and P0 prerequisites.

### Task 2: Write The Architecture Decision

**Files:**
- Create: `docs/architecture/adr/webflux-vs-mvc.md`
- Modify: `docs/architecture/evidence/p3-05-webflux-mvc.md`
- Read: `docs/architecture/webflux-mvc-migration-inventory.md`

- [x] Compare WebFlux hardening, MVC migration, and hybrid containment.
- [x] Add benchmark, team-readiness, transaction-safety, rollback, and compatibility gates.
- [x] Record the decision as `Accepted`, `Deferred`, or `Rejected` with a named owner and revisit trigger.

Run:

```bash
test -f docs/architecture/adr/webflux-vs-mvc.md
rg -n "Decision|Options|Benchmark|Team readiness|Transaction safety|Rollback|Compatibility|Revisit" docs/architecture/adr/webflux-vs-mvc.md
```

Expected: ADR contains the required decision sections and does not approve runtime migration without gates.

### Task 3: Prepare The First Migration Slice Only If Approved

**Files:**
- Create: `docs/architecture/webflux-mvc-first-slice.md`
- Modify: `docs/architecture/evidence/p3-05-webflux-mvc.md`
- Modify: `docs/architecture/archive/plans/P3-05-webflux-to-mvc-migration-plan.md`

- [x] Select the lowest-risk endpoint group from the inventory or explicitly defer selection.
- [x] Define characterization tests for request, response, error, auth, transaction, and actuator behavior.
- [x] Define rollback steps and compatibility window for the selected slice.

Run:

```bash
test -f docs/architecture/webflux-mvc-first-slice.md
rg -n "endpoint group|request|response|error|auth|transaction|actuator|rollback|compatibility window|deferred" docs/architecture/webflux-mvc-first-slice.md
git diff -- docs/architecture/evidence/p3-05-webflux-mvc.md docs/architecture/webflux-mvc-migration-inventory.md docs/architecture/adr/webflux-vs-mvc.md docs/architecture/webflux-mvc-first-slice.md docs/architecture/archive/plans/P3-05-webflux-to-mvc-migration-plan.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: documentation diff contains only WebFlux/MVC evidence, inventory, ADR, first-slice document, and plan changes; no files are staged or committed.
