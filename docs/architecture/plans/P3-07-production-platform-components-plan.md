# Production Platform Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decide and stage production platform components with ADRs and proof-of-value gates.

**Architecture:** Evaluate one component at a time against a concrete capability gap. Keep core business code protected by local interfaces where component churn is likely.

**Tech Stack:** Spring Boot, Redis/Redisson, RocketMQ, Nacos, Sentinel, Feign or WebClient, Spring Boot Admin, ClickHouse, logging/metrics stack.

---

## Source Material

- Spec: `../specs/P3-07-production-platform-components-spec.md`
- Evolution doc: `../archive/evolution/production-practice-review.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/platform-component-decision-matrix.md`
- Create: ADR files under `docs/architecture/adr/`
- Modify: implementation only after a component ADR is approved
- Test: proof-of-value tests for selected components

### Task 1: Build The Decision Matrix

- [ ] **Step 1: List candidate components**

Extract candidates from `../archive/evolution/production-practice-review.md`.

- [ ] **Step 2: Create matrix**

Write `docs/architecture/platform-component-decision-matrix.md` with problem, alternative, operational owner, cost, failure mode, and decision.

- [ ] **Step 3: Verify candidates**

Run `rg -n "XXL|Redisson|Nacos|Knife4j|Sentinel|Spring Boot Admin|ClickHouse" docs/architecture/platform-component-decision-matrix.md`. Expected: each candidate appears.

### Task 2: Pick One Component For Proof Of Value

- [ ] **Step 1: Select the highest value component**

Choose the candidate that closes a confirmed P0/P1/P2 gap with the smallest rollout risk.

- [ ] **Step 2: Write ADR**

Create an ADR with problem, selected option, alternatives, rollout, rollback, and owner.

- [ ] **Step 3: Verify ADR**

Run `rg -n "Problem|Decision|Alternatives|Rollout|Rollback|Owner" docs/architecture/adr`. Expected: the ADR includes each section.

### Task 3: Plan The Implementation Boundary

- [ ] **Step 1: Define local abstraction**

Document the interface that isolates business code from the component.

- [ ] **Step 2: Define proof-of-value test**

Specify the test, metric, or operational drill that proves the component solves the stated problem.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture`. Expected: only platform decision docs are changed before implementation starts.
