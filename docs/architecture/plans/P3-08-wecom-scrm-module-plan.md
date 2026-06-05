# WeCom SCRM Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define the first approved WeCom SCRM implementation slice without coupling WeCom-specific behavior into generic canvas execution boundaries.

**Architecture:** Keep WeCom API calls in external adapters and expose stable domain services to DAG handlers and frontend APIs. Treat callbacks and sync as idempotent event ingestion flows with signature verification, replay protection, retry, and DLQ behavior. Per `../adr/ADR-0006-service-extraction-gate.md`, WeCom starts as the Integration bounded context inside the modular monolith unless service-extraction gates are satisfied.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, RocketMQ, external WeCom API adapter, React, TypeScript, Vitest, JUnit 5.

---

## Source Material

- Spec: `../specs/P3-08-wecom-scrm-module-spec.md`
- Evolution doc: `../archive/evolution/wecom-scrm-module-design.md`
- Boundary spec: `../specs/P3-00-architecture-boundary-review-spec.md`
- Boundary evidence: `../evidence/p3-00-architecture-boundary-review.md`
- Service extraction gate: `../adr/ADR-0006-service-extraction-gate.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-08-wecom-scrm.md`
- Create: `docs/architecture/wecom-scrm-implementation-slice.md`
- Create: `docs/architecture/wecom-scrm-integration-boundary.md`
- Create: `docs/architecture/wecom-scrm-test-plan.md`
- Read: `docs/architecture/archive/evolution/wecom-scrm-module-design.md`

### Task 1: Confirm Product Slice

**Files:**
- Create: `docs/architecture/wecom-scrm-implementation-slice.md`
- Create: `docs/architecture/evidence/p3-08-wecom-scrm.md`
- Read: `docs/architecture/archive/evolution/wecom-scrm-module-design.md`

- [x] List candidate capabilities: callback ingestion, customer sync, group operations, journey send node, session tracking, and frontend configuration.
- [x] Select one first slice or explicitly defer implementation if product/compliance ownership is missing.
- [x] Define scope, out of scope, API, data, consent/compliance, rollback, and owner.

Run:

```bash
test -f docs/architecture/wecom-scrm-implementation-slice.md
rg -n "Scope|Out of scope|API|Data|Consent|Compliance|Callback|Rollback|Owner|Deferred" docs/architecture/wecom-scrm-implementation-slice.md
```

Expected: slice document names first slice or deferral reason and covers scope, API, data, compliance, rollback, and owner.

### Task 2: Define Integration Boundary

**Files:**
- Create: `docs/architecture/wecom-scrm-integration-boundary.md`
- Modify: `docs/architecture/evidence/p3-08-wecom-scrm.md`
- Read: `docs/architecture/wecom-scrm-implementation-slice.md`

- [x] Define adapter interfaces, domain services, handler contracts, callback endpoint contract, frontend API contract, and data ownership.
- [x] Define signature verification, replay protection, idempotency key, retry classification, DLQ, and reconciliation command.
- [x] State how generic DAG handlers avoid direct WeCom client dependencies.

Run:

```bash
test -f docs/architecture/wecom-scrm-integration-boundary.md
rg -n "adapter|domain service|handler contract|callback|frontend API|signature|replay|idempotency|retry|DLQ|reconciliation|generic DAG" docs/architecture/wecom-scrm-integration-boundary.md
```

Expected: integration boundary isolates WeCom adapters and covers callback safety mechanisms.

### Task 3: Plan Tests Before Implementation

**Files:**
- Create: `docs/architecture/wecom-scrm-test-plan.md`
- Modify: `docs/architecture/evidence/p3-08-wecom-scrm.md`
- Modify: `docs/architecture/plans/P3-08-wecom-scrm-module-plan.md`

- [x] List backend tests for callback signature, replay, duplicate event idempotency, retry, adapter failure, DLQ, and handler output.
- [x] List frontend tests for config form payload, validation, disabled state, and typed API response.
- [x] Define manual verification for sandbox WeCom tenant credentials without committing secrets.

Run:

```bash
test -f docs/architecture/wecom-scrm-test-plan.md
rg -n "signature|replay|idempotency|retry|adapter failure|DLQ|handler output|form payload|disabled state|sandbox|secrets" docs/architecture/wecom-scrm-test-plan.md
git diff -- docs/architecture/evidence/p3-08-wecom-scrm.md docs/architecture/wecom-scrm-implementation-slice.md docs/architecture/wecom-scrm-integration-boundary.md docs/architecture/wecom-scrm-test-plan.md docs/architecture/plans/P3-08-wecom-scrm-module-plan.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: documentation diff contains only WeCom evidence, slice, boundary, test-plan, and plan documents; no files are staged or committed.
