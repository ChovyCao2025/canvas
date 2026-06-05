# Multi Datasource Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define a verified, reversible path from the current single datasource to isolated datasource groups for control, runtime, CDP/customer, analytics, and ops data.

**Architecture:** Keep this P3 slice evidence-first. Map table ownership and transaction flows before introducing routing code; any runtime split must use outbox, saga, or repairable reconciliation rather than implicit distributed transactions.

**Tech Stack:** Spring Boot 3.2, MyBatis-Plus, HikariCP, Flyway, MySQL, Redis, RocketMQ, Micrometer, Markdown evidence docs.

---

## Source Material

- Spec: `../specs/P3-04-multi-datasource-isolation-spec.md`
- Evolution doc: `../archive/evolution/multi-datasource-isolation.md`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-04-multi-datasource-isolation.md`
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-ownership-map.md`
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md`
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-migration-plan.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration/`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/`

### Task 1: Map Table Ownership

**Files:**
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-ownership-map.md`
- Create: `docs/architecture/evidence/p3-04-multi-datasource-isolation.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration/`

- [x] List every table created by Flyway migrations and assign it to one owner group: control, runtime, CDP/customer, analytics, or ops.
- [x] For each group, record owner package, tenant-scope rule, PII class, retention expectation, and backup owner.
- [x] Record tables that cannot be moved until P0 data-security and tenant-isolation criteria are met.

Run:

```bash
rg -n "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration > /tmp/canvas_tables.txt
test -s /tmp/canvas_tables.txt
test -f docs/architecture/work-products/p3-04-multi-datasource/datasource-ownership-map.md
rg -n "control|runtime|CDP/customer|analytics|ops|tenant|PII|retention|backup" docs/architecture/work-products/p3-04-multi-datasource/datasource-ownership-map.md
```

Expected: ownership map exists and names all five datasource groups plus tenant, PII, retention, and backup fields.

### Task 2: Identify Cross-Datasource Transactions

**Files:**
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md`
- Modify: `docs/architecture/evidence/p3-04-multi-datasource-isolation.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/`

- [x] Inventory current `@Transactional` methods and repository writes that touch multiple future datasource groups.
- [x] Classify each flow as same-datasource, outbox, saga, reconciliation, or blocked.
- [x] For every cross-group write, name the event, idempotency key, reconciliation command, and rollback decision owner.

Run:

```bash
rg -n "@Transactional|insert\\(|update\\(|delete\\(" backend/canvas-engine/src/main/java/org/chovy/canvas > /tmp/canvas_write_flows.txt
test -s /tmp/canvas_write_flows.txt
test -f docs/architecture/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md
rg -n "outbox|saga|reconciliation|blocked|idempotency|rollback owner" docs/architecture/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md
```

Expected: boundary map names a safe replacement pattern for each cross-datasource write and does not depend on distributed transactions.

### Task 3: Define Migration, Rollback, And Monitoring

**Files:**
- Create: `docs/architecture/work-products/p3-04-multi-datasource/datasource-migration-plan.md`
- Modify: `docs/architecture/evidence/p3-04-multi-datasource-isolation.md`
- Modify: `docs/architecture/archive/completed/plans/P3-04-multi-datasource-isolation-plan.md`

- [x] Define Flyway ownership conventions for each datasource group, including directory naming and startup order.
- [x] Define rollback for schema, data copy, datasource routing, application deployment, and reconciliation state.
- [x] Define monitoring for pool health, migration status, replication lag, event backlog, and reconciliation failures.

Run:

```bash
test -f docs/architecture/work-products/p3-04-multi-datasource/datasource-migration-plan.md
rg -n "Flyway|startup order|schema rollback|data copy|routing rollback|pool health|migration status|replication lag|reconciliation failure" docs/architecture/work-products/p3-04-multi-datasource/datasource-migration-plan.md
git diff -- docs/architecture/evidence/p3-04-multi-datasource-isolation.md docs/architecture/work-products/p3-04-multi-datasource/datasource-ownership-map.md docs/architecture/work-products/p3-04-multi-datasource/datasource-transaction-boundary-map.md docs/architecture/work-products/p3-04-multi-datasource/datasource-migration-plan.md docs/architecture/archive/completed/plans/P3-04-multi-datasource-isolation-plan.md
```

Expected: diff contains only datasource isolation evidence, ownership, transaction, migration, and plan docs. No commit is created by default.
