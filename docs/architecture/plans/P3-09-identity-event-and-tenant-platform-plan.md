# Identity, Event, And Tenant Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define shared platform primitives required before larger service and data-platform evolution work.

**Architecture:** Establish canonical identity, event, and tenant contracts before extracting services or building analytics pipelines. Keep contracts versioned, testable, and backward compatible.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, RocketMQ, OpenAPI, JSON schema or equivalent event schema tooling, JUnit 5.

---

## Source Material

- Spec: `../specs/P3-09-identity-event-and-tenant-platform-spec.md`
- Evolution doc: `../archive/evolution/architect-critical-review.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/platform-primitives.md`
- Create: `docs/architecture/event-schema-governance.md`
- Modify: implementation only after primitive contracts are reviewed
- Test: contract tests for identity mapping, event compatibility, tenant quota, and degradation behavior

### Task 1: Define OneID And Tenant Contracts

- [ ] **Step 1: Create platform primitives document**

Write `docs/architecture/platform-primitives.md` with OneID, source identities, merge/split, tenant visibility, and quota sections.

- [ ] **Step 2: Define auditability**

Add audit events for identity merge/split, tenant scope changes, and quota changes.

- [ ] **Step 3: Verify sections**

Run `rg -n "OneID|source identity|merge|split|tenant|quota|audit" docs/architecture/platform-primitives.md`. Expected: all sections exist.

### Task 2: Define Event Schema Governance

- [ ] **Step 1: Create event governance document**

Write `docs/architecture/event-schema-governance.md` with schema owner, versioning, compatibility, replay, ordering, and deprecation rules.

- [ ] **Step 2: Pick initial event families**

Document canvas lifecycle, execution lifecycle, customer identity, reach delivery, and ops events.

- [ ] **Step 3: Verify event families**

Run `rg -n "canvas lifecycle|execution lifecycle|customer identity|reach delivery|ops" docs/architecture/event-schema-governance.md`. Expected: all families exist.

### Task 3: Define Degradation And Engine/Web Split

- [ ] **Step 1: Add degradation matrix**

Document fail-open/fail-closed behavior for Redis, RocketMQ, datasource, WeCom, and analytics dependencies.

- [ ] **Step 2: Add engine/web boundary**

Document API, events, data ownership, deployment, and rollback boundaries between web/admin and execution engine.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture/platform-primitives.md docs/architecture/event-schema-governance.md`. Expected: only platform primitive docs are changed before implementation starts.
