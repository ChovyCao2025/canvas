# Identity, Event, And Tenant Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define shared identity, event, and tenant platform contracts required before service extraction and data-platform evolution.

**Architecture:** Establish versioned contracts before runtime implementation. OneID, event schema, tenant quota/visibility, degradation modes, and engine/web boundaries must be documented, testable, and backward compatible before services or analytics pipelines depend on them.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, RocketMQ, OpenAPI, JSON Schema, JUnit 5, Markdown contract docs.

---

## Source Material

- Spec: `../specs/P3-09-identity-event-and-tenant-platform-spec.md`
- Evolution doc: `../archive/evolution/architect-critical-review.md`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-09-platform-primitives.md`
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/platform-primitives.md`
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/event-schema-governance.md`
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/tenant-platform-contract.md`
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/engine-web-boundary.md`

### Task 1: Define OneID And Tenant Contracts

**Files:**
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/platform-primitives.md`
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/tenant-platform-contract.md`
- Create: `docs/architecture/evidence/p3-09-platform-primitives.md`

- [x] Define canonical OneID, source identity, merge, split, confidence, conflict, and audit rules.
- [x] Define tenant visibility, tenant quota, tenant-scoped query, and tenant-scope-change audit contracts.
- [x] Define compatibility rules for existing CDP and canvas tenant fields.

Run:

```bash
test -f docs/architecture/decisions/work-products/p3-09-platform-primitives/platform-primitives.md
test -f docs/architecture/decisions/work-products/p3-09-platform-primitives/tenant-platform-contract.md
rg -n "OneID|source identity|merge|split|confidence|conflict|audit|tenant visibility|quota|tenant-scoped|compatibility" docs/architecture/decisions/work-products/p3-09-platform-primitives/platform-primitives.md docs/architecture/decisions/work-products/p3-09-platform-primitives/tenant-platform-contract.md
```

Expected: identity and tenant contracts cover OneID lifecycle, auditability, quota, tenant visibility, and compatibility.

### Task 2: Define Event Schema Governance

**Files:**
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/event-schema-governance.md`
- Modify: `docs/architecture/evidence/p3-09-platform-primitives.md`

- [x] Define schema owner, versioning, compatibility, replay, ordering, idempotency, deprecation, and retention rules.
- [x] Define initial event families: canvas lifecycle, execution lifecycle, customer identity, reach delivery, CDP profile, and ops.
- [x] Include one example event envelope with tenant, correlation, schema version, event ID, idempotency key, and occurred-at timestamp fields.

Run:

```bash
test -f docs/architecture/decisions/work-products/p3-09-platform-primitives/event-schema-governance.md
rg -n "schema owner|versioning|compatibility|replay|ordering|idempotency|deprecation|retention|canvas lifecycle|execution lifecycle|customer identity|reach delivery|ops|schemaVersion|eventId" docs/architecture/decisions/work-products/p3-09-platform-primitives/event-schema-governance.md
```

Expected: event governance covers lifecycle rules, initial families, and a concrete event-envelope example.

### Task 3: Define Degradation And Engine/Web Split

**Files:**
- Create: `docs/architecture/decisions/work-products/p3-09-platform-primitives/engine-web-boundary.md`
- Modify: `docs/architecture/evidence/p3-09-platform-primitives.md`
- Modify: `docs/architecture/archive/completed/plans/P3-09-identity-event-and-tenant-platform-plan.md`

- [x] Define fail-open/fail-closed behavior for Redis, RocketMQ, datasource, WeCom, analytics, and AI dependencies.
- [x] Define API, event, data ownership, deployment, rollback, and observability boundaries between web/admin and execution engine.
- [x] Define contract tests required before any service extraction consumes these primitives.

Run:

```bash
test -f docs/architecture/decisions/work-products/p3-09-platform-primitives/engine-web-boundary.md
rg -n "fail-open|fail-closed|Redis|RocketMQ|datasource|WeCom|analytics|AI|API boundary|event boundary|data ownership|deployment|rollback|observability|contract tests" docs/architecture/decisions/work-products/p3-09-platform-primitives/engine-web-boundary.md
git diff -- docs/architecture/evidence/p3-09-platform-primitives.md docs/architecture/decisions/work-products/p3-09-platform-primitives/platform-primitives.md docs/architecture/decisions/work-products/p3-09-platform-primitives/event-schema-governance.md docs/architecture/decisions/work-products/p3-09-platform-primitives/tenant-platform-contract.md docs/architecture/decisions/work-products/p3-09-platform-primitives/engine-web-boundary.md docs/architecture/archive/completed/plans/P3-09-identity-event-and-tenant-platform-plan.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: documentation diff contains only platform primitive evidence, identity/tenant/event/boundary docs, and plan changes; no files are staged or committed.
