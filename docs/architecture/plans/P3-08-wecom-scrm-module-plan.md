# WeCom SCRM Module Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define and stage the WeCom SCRM module without polluting generic canvas execution boundaries.

**Architecture:** Keep WeCom-specific API calls in adapters and expose stable domain services to handlers and frontend APIs. Treat callbacks and sync as idempotent event ingestion flows.

**Tech Stack:** Spring Boot, MyBatis-Plus, Redis, RocketMQ, external WeCom API client, React, TypeScript, Vitest/JUnit.

---

## Source Material

- Spec: `../specs/P3-08-wecom-scrm-module-spec.md`
- Evolution doc: `../archive/evolution/wecom-scrm-module-design.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/wecom-scrm-implementation-slice.md`
- Modify: backend/frontend only after product scope is approved
- Test: callback security, idempotency, adapter retry, handler contract, and frontend config tests

### Task 1: Confirm Product Slice

- [ ] **Step 1: Extract module capabilities**

Read `../archive/evolution/wecom-scrm-module-design.md` and list candidate capabilities in `docs/architecture/wecom-scrm-implementation-slice.md`.

- [ ] **Step 2: Select the first slice**

Choose one vertical slice such as callback ingestion, customer sync, or journey send node.

- [ ] **Step 3: Verify slice clarity**

Run `rg -n "Scope|Out of scope|API|Data|Callback|Rollback" docs/architecture/wecom-scrm-implementation-slice.md`. Expected: all sections exist.

### Task 2: Define Integration Boundary

- [ ] **Step 1: Document adapters and domain services**

Name adapter interfaces, domain services, handler contracts, and frontend API contracts.

- [ ] **Step 2: Define callback security**

Document signature verification, replay protection, idempotency key, retry, and DLQ behavior.

- [ ] **Step 3: Verify security coverage**

Run `rg -n "signature|replay|idempotency|retry|DLQ" docs/architecture/wecom-scrm-implementation-slice.md`. Expected: all mechanisms appear.

### Task 3: Plan Tests Before Implementation

- [ ] **Step 1: List backend tests**

Document tests for callback verification, duplicate event handling, retry, adapter failures, and handler output.

- [ ] **Step 2: List frontend tests**

Document tests for form payload, validation, and typed API contract.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture/wecom-scrm-implementation-slice.md`. Expected: only the WeCom slice document is changed before implementation starts.
