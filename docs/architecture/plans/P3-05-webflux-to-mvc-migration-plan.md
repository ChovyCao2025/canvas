# WebFlux To MVC Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make and prepare the WebFlux versus MVC architecture decision with evidence.

**Architecture:** Use measurement and dependency inventory before changing runtime architecture. If migration is selected, move endpoint groups incrementally with compatibility and rollback.

**Tech Stack:** Spring Boot 3.2, WebFlux, Spring MVC, Reactor, MyBatis-Plus, JUnit 5, benchmark scripts.

---

## Source Material

- Spec: `../specs/P3-05-webflux-to-mvc-migration-spec.md`
- Evolution doc: `../archive/evolution/webflux-to-mvc-migration.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/adr/webflux-vs-mvc.md`
- Create: `docs/architecture/webflux-mvc-migration-inventory.md`
- Test: focused controller and transaction tests before endpoint migration

### Task 1: Build The Runtime Inventory

- [ ] **Step 1: Inventory reactive and blocking usage**

Run `rg -n "\\.block\\(|\\.subscribe\\(|Thread\\.sleep\\(|@Transactional|Mono<|Flux<" backend/canvas-engine/src/main/java`.

- [ ] **Step 2: Create the inventory**

Write `docs/architecture/webflux-mvc-migration-inventory.md` with endpoint group, blocking dependencies, transaction needs, and risk.

- [ ] **Step 3: Verify coverage**

Run `rg -n "Controller|block|transaction|Redis|RocketMQ|MyBatis" docs/architecture/webflux-mvc-migration-inventory.md`. Expected: all categories appear.

### Task 2: Write The Architecture Decision

- [ ] **Step 1: Compare options**

Document WebFlux hardening, MVC migration, and hybrid containment in `docs/architecture/adr/webflux-vs-mvc.md`.

- [ ] **Step 2: Add acceptance gates**

Add benchmark, test, rollback, and team-readiness gates to the ADR.

- [ ] **Step 3: Verify ADR completeness**

Run `rg -n "Decision|Options|Benchmark|Rollback|Team" docs/architecture/adr/webflux-vs-mvc.md`. Expected: all sections exist.

### Task 3: Prepare Incremental Migration If Approved

- [ ] **Step 1: Choose first endpoint group**

Select the lowest-risk endpoint group from the inventory.

- [ ] **Step 2: Add characterization tests**

Add tests for current request, response, error, auth, and transaction behavior.

- [ ] **Step 3: Review diff**

Run `git diff -- .`. Expected: migration preparation is isolated to the selected endpoint group and docs.
