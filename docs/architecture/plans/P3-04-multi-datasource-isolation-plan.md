# Multi Datasource Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define and validate a safe path from one database/pool to isolated datasource groups.

**Architecture:** Start by mapping table ownership and transaction flows. Introduce datasource separation only behind explicit repositories, migration ownership, and reconciliation paths.

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, HikariCP, Flyway, MySQL, Redis, RocketMQ, Micrometer.

---

## Source Material

- Spec: `../specs/P3-04-multi-datasource-isolation-spec.md`
- Evolution doc: `../archive/evolution/multi-datasource-isolation.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/datasource-ownership-map.md`
- Create: `docs/architecture/datasource-migration-plan.md`
- Test: datasource routing, transaction boundary, and reconciliation tests before runtime split

### Task 1: Map Table Ownership

- [ ] **Step 1: Extract schema tables**

Run `rg -n "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration`.

- [ ] **Step 2: Create ownership map**

Write `docs/architecture/datasource-ownership-map.md` grouping tables into control, runtime, CDP/customer, analytics, and ops.

- [ ] **Step 3: Verify required groups**

Run `rg -n "control|runtime|CDP|analytics|ops" docs/architecture/datasource-ownership-map.md`. Expected: all groups appear.

### Task 2: Identify Cross-Datasource Transactions

- [ ] **Step 1: Find transactional methods**

Run `rg -n "@Transactional" backend/canvas-engine/src/main/java`.

- [ ] **Step 2: Classify each cross-domain write**

Add a table to `docs/architecture/datasource-migration-plan.md` listing current transaction, target datasource groups, and replacement pattern.

- [ ] **Step 3: Verify no implicit distributed transaction plan**

Run `rg -n "outbox|saga|reconciliation|repair" docs/architecture/datasource-migration-plan.md`. Expected: each cross-datasource flow names one of these patterns.

### Task 3: Define Migration And Rollback

- [ ] **Step 1: Document Flyway ownership**

Add datasource-specific migration directories or ownership conventions to the migration plan.

- [ ] **Step 2: Document rollback**

Define rollback for schema, data copy, routing, and application deployment.

- [ ] **Step 3: Review diff**

Run `git diff -- docs/architecture`. Expected: only datasource isolation docs are changed before implementation starts.
