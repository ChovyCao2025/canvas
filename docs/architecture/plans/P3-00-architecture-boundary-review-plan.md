# Architecture Boundary Review Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Use `../specs/P3-00-architecture-boundary-review-spec.md` as the decision gate before executing any service split, data platform, WeCom, K8s, or production component plan.

**Architecture:** Keep the review as a governance and design-control plan, not an immediate code migration. Each downstream P3 plan must prove that it follows the bounded-context model, service extraction triggers, and dependency rules in the review spec.

**Tech Stack:** Markdown, ADRs, Java 21, Spring Boot 3.2, MyBatis-Plus, Redis, RocketMQ, React, TypeScript.

---

## Source Material

- Spec: `../specs/P3-00-architecture-boundary-review-spec.md`
- Coverage matrix: `../todo/coverage-matrix.md`
- Related specs: `../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`, `../specs/P3-03-data-platform-architecture-spec.md`, `../specs/P3-08-wecom-scrm-module-spec.md`

## File Structure

- Read: `../specs/P3-00-architecture-boundary-review-spec.md`
- Modify: downstream P3 specs and plans only when they contradict the boundary review
- Create: ADRs under `docs/architecture/adr/` when a physical service extraction is proposed
- Test: documentation checks plus characterization tests for the context being extracted

### Task 1: Apply The Boundary Review To P3 Specs

**Files:**
- Read: `../specs/P3-00-architecture-boundary-review-spec.md`
- Modify: `../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Modify: `../specs/P3-03-data-platform-architecture-spec.md`
- Modify: `../specs/P3-08-wecom-scrm-module-spec.md`

- [ ] **Step 1: Check each P3 spec against the seven-context model**

Run `rg -n "service|bounded context|data platform|WeCom|K8s|component" ../specs/P3-*.md`.

- [ ] **Step 2: Remove direct service-extraction assumptions**

Replace any statement that implies immediate physical service extraction with a requirement to satisfy the extraction triggers in the review spec.

- [ ] **Step 3: Verify references**

Run `rg -n "P3-00-architecture-boundary-review-spec.md|service extraction triggers|bounded context" ../specs/P3-*.md`. Expected: downstream P3 specs reference the boundary review or its rules.

### Task 2: Gate Service Extraction With ADRs

**Files:**
- Create: `docs/architecture/adr/`
- Read: `../specs/P3-00-architecture-boundary-review-spec.md`

- [ ] **Step 1: Create ADR template if absent**

Create a template with sections for context, decision, alternatives, data ownership, API/event contracts, rollout, rollback, observability, tenant propagation, and idempotency.

- [ ] **Step 2: Require ADR for each physical service proposal**

Before extracting a service, create one ADR under `docs/architecture/adr/` and link it from the relevant P3 plan.

- [ ] **Step 3: Verify ADR readiness**

Run `rg -n "Data ownership|API|Event|Rollback|Observability|Tenant|Idempotency" docs/architecture/adr`. Expected: every physical service ADR contains these sections.

### Task 3: Add Characterization Tests Before Extraction

**Files:**
- Read: affected context code under `backend/canvas-engine/src/main/java/org/chovy/canvas`
- Test: affected context tests under `backend/canvas-engine/src/test/java/org/chovy/canvas`

- [ ] **Step 1: Identify the context being extracted**

Use the context table in `../specs/P3-00-architecture-boundary-review-spec.md` to name the context and current code anchors.

- [ ] **Step 2: Add characterization tests**

Add tests for current API behavior, data ownership behavior, event behavior, tenant propagation, and failure behavior.

- [ ] **Step 3: Run focused tests**

Run the relevant backend or frontend test command. Expected before refactor: tests pass against current behavior and protect the migration.

### Task 4: Re-Review After Each Boundary Change

**Files:**
- Read: `../specs/P3-00-architecture-boundary-review-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: `../specs/README.md`, `../plans/README.md`, and `../todo/coverage-matrix.md` when package scope changes

- [ ] **Step 1: Re-run scope check**

Confirm the proposed change does not create a new deployable without data ownership, observability, rollback, and tenant propagation.

- [ ] **Step 2: Update traceability**

Update specs, plans, and the coverage matrix if the architecture boundary changes.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture`. Expected: boundary changes are explicit in spec, plan, ADR, and coverage docs.
