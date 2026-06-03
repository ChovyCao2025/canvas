# Data Platform Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce an implementable data platform design and proof-of-concept path for Marketing Canvas analytics and governance.

**Architecture:** Separate OLTP execution from analytical workloads. Start with source inventory and event/CDC contracts, then validate one thin vertical slice before selecting or scaling platform components.

**Tech Stack:** MySQL, Redis, RocketMQ, CDC tooling, Flink or batch ingestion, ClickHouse or equivalent OLAP store, Spring Boot APIs, data governance docs.

---

## Source Material

- Spec: `../specs/P3-03-data-platform-architecture-spec.md`
- Evolution doc: `../archive/evolution/data-platform-architecture.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/data-platform-source-inventory.md`
- Create: `docs/architecture/data-platform-poc-plan.md`
- Modify: implementation only after ADR approval
- Test: ingestion contract tests and replay tests for the selected proof of concept

### Task 1: Inventory Source Data

- [ ] **Step 1: List source tables and events**

Run `rg -n "CREATE TABLE|RocketMQ|event" backend/canvas-engine/src/main/resources backend/canvas-engine/src/main/java` and list candidate sources.

- [ ] **Step 2: Create the source inventory**

Write `docs/architecture/data-platform-source-inventory.md` with source owner, freshness, retention, PII class, and consumers.

- [ ] **Step 3: Verify source coverage**

Run `rg -n "canvas|execution|event|cdp|notification|reach" docs/architecture/data-platform-source-inventory.md`. Expected: each core source group appears.

### Task 2: Define The Thin Vertical Slice

- [ ] **Step 1: Pick one use case**

Choose one report or audience computation that exercises ingestion, storage, query, and governance.

- [ ] **Step 2: Create the proof-of-concept plan**

Write `docs/architecture/data-platform-poc-plan.md` with input, transform, serving model, SLA, rollback, and cost assumptions.

- [ ] **Step 3: Verify decision readiness**

Run `rg -n "Input|Transform|Serving|SLA|Rollback|Cost" docs/architecture/data-platform-poc-plan.md`. Expected: all sections exist.

### Task 3: Define Contracts And Governance

- [ ] **Step 1: Document schema versioning**

Add event or CDC schema versioning rules to the proof-of-concept plan.

- [ ] **Step 2: Document PII handling**

Add masking, deletion, retention, and lineage requirements for the selected slice.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture`. Expected: only data platform docs are changed before implementation starts.
