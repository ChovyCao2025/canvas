# Service Decomposition And Domain Boundaries Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create an implementable service decomposition path for the Marketing Canvas platform.

**Architecture:** Start with a modular-monolith boundary map and only promote a domain to service extraction when contracts, data ownership, rollout, and rollback are clear. Use strangler migration rather than a big-bang rewrite.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, Reactor/WebFlux or MVC depending on the selected runtime path, Redis, RocketMQ, OpenAPI, ADRs.

---

## Source Material

- Spec: `../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Evolution docs: `../archive/evolution/service-architecture-design.md`, `../archive/evolution/target-architecture-overview.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/adr/`
- Create: `docs/architecture/domain-map.md`
- Modify: backend package/module boundaries after approval
- Test: characterization tests around domain contracts before extraction

### Task 1: Write The Context Map

- [ ] **Step 1: List current code ownership**

Run `find backend/canvas-engine/src/main/java/org/chovy/canvas -maxdepth 3 -type d | sort` and map packages to the candidate domains in the spec.

- [ ] **Step 2: Create `docs/architecture/domain-map.md`**

Write sections for domain purpose, owned data, public APIs, emitted events, consumed events, and forbidden dependencies.

- [ ] **Step 3: Verify coverage**

Run `rg -n "canvas authoring|execution engine|CDP|reach|tenant|observability" docs/architecture/domain-map.md`. Expected: all six domain groups are present.

### Task 2: Choose The First Extraction Candidate

- [ ] **Step 1: Score each domain**

Score coupling, data ownership clarity, traffic criticality, and operational blast radius in `docs/architecture/domain-map.md`.

- [ ] **Step 2: Write the ADR**

Create an ADR under `docs/architecture/adr/` naming the first extraction candidate and why other domains are deferred.

- [ ] **Step 3: Verify the decision**

Run `rg -n "Decision|Deferred|Rollback|Compatibility" docs/architecture/adr`. Expected: the ADR includes all four sections.

### Task 3: Define Contracts Before Moving Code

- [ ] **Step 1: Add contract inventory**

Document REST endpoints, events, Redis keys, and tables used by the chosen domain.

- [ ] **Step 2: Add characterization tests**

Create tests around existing domain behavior before package or service extraction.

- [ ] **Step 3: Run focused tests**

Run the backend test command for the new characterization tests. Expected before refactor: pass against current behavior.

### Task 4: Plan The Strangler Migration

- [ ] **Step 1: Define compatibility windows**

Document old path, new path, dual-write/read behavior if any, and rollback trigger.

- [ ] **Step 2: Define deployment order**

Document database, backend, frontend, Redis/RocketMQ, and observability deployment order.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture`. Expected: only architecture docs and ADRs are changed before implementation starts.
