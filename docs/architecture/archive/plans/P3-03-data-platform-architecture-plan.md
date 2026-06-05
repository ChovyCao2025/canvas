# Data Platform Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Define a data platform proof-of-concept that serves one measurable analytics or audience use case without coupling OLTP canvas execution to OLAP workloads.

**Architecture:** Start from source ownership, event/CDC contracts, retention, PII classification, and replay rules. Select a thin vertical slice before choosing Flink, ClickHouse, warehouse layers, or lakehouse components. The online execution path must not require the data platform to complete. Per `../adr/ADR-0006-service-extraction-gate.md`, data-platform work is not a domain-service extraction driver unless the gate is satisfied.

**Tech Stack:** MySQL/Flyway source tables, Redis key catalog, RocketMQ/event contracts, optional CDC, optional batch ingestion, optional ClickHouse/Flink after ADR approval, Java 21 contract tests, Markdown evidence docs.

---

## Source Material

- Spec: `../specs/P3-03-data-platform-architecture-spec.md`
- Boundary review: `../specs/P3-00-architecture-boundary-review-spec.md`
- Boundary evidence: `../evidence/p3-00-architecture-boundary-review.md`
- Service extraction gate: `../adr/ADR-0006-service-extraction-gate.md`
- Source docs: `../archive/evolution/data-platform-architecture.md`, `../archive/evolution/target-architecture-overview.md`, `../archive/evolution/architect-critical-review.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/specs/P3-03-data-platform-architecture-spec.md`
- Read: `docs/architecture/archive/evolution/data-platform-architecture.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V39__audience_definition.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V62__message_send_record.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V72__perf_run_tracking.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V73__audience_compute_run_tracking.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V74__cdp_core.sql`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventLogDO.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Create: `docs/architecture/evidence/p3-03-data-platform.md`
- Create: `docs/architecture/data-platform-source-inventory.md`
- Create: `docs/architecture/data-platform-poc-plan.md`
- Create: `docs/architecture/data-platform-contract-governance.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceComputeRunTrackingSchemaTest.java`

### Task 1: Inventory source data

**Files:**
- Create: `docs/architecture/evidence/p3-03-data-platform.md`
- Create: `docs/architecture/data-platform-source-inventory.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq`

- [x] List source tables and events for canvas, execution, trace, DLQ, CDP profile/identity/tag, audience, notification/reach, consent/suppression, and event log data.
- [x] For each source, record owner context, ingestion method, freshness target, retention, PII class, deletion behavior, and downstream consumers.
- [x] Identify which sources are excluded from the first proof of concept.

**Run:**
```bash
rg "CREATE TABLE" backend/canvas-engine/src/main/resources/db/migration
rg "EventLogDO|RocketMQ|MqTriggerMessage|MessageSendRecordDO|CdpUserIdentityDO" backend/canvas-engine/src/main/java/org/chovy/canvas
test -f docs/architecture/data-platform-source-inventory.md
rg "canvas|execution|trace|CDP|audience|notification|consent|PII|freshness|retention" docs/architecture/data-platform-source-inventory.md
```

**Expected:** Source inventory covers every core source group and records ownership, ingestion, freshness, retention, PII, deletion, and consumers.

### Task 2: Define the thin vertical slice

**Files:**
- Create: `docs/architecture/data-platform-poc-plan.md`
- Modify: `docs/architecture/evidence/p3-03-data-platform.md`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBatchComputeService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- Read: `frontend/src/pages/canvas-stats/index.tsx`

- [x] Choose one vertical slice such as execution-funnel analytics, audience compute history, or reach delivery report.
- [x] Define input sources, transform steps, storage target, serving API, freshness SLA, retention, PII masking, cost estimate, rollback, and success metric.
- [x] State why a full data platform is deferred until the slice is measured.

**Run:**
```bash
test -f docs/architecture/data-platform-poc-plan.md
rg "Input sources|Transform|Storage|Serving API|SLA|Retention|PII|Cost|Rollback|Success metric|full data platform is deferred" docs/architecture/data-platform-poc-plan.md
```

**Expected:** Proof-of-concept plan defines one measurable vertical slice and defers broad platform selection.

### Task 3: Define contracts and governance

**Files:**
- Create: `docs/architecture/data-platform-contract-governance.md`
- Modify: `docs/architecture/data-platform-poc-plan.md`
- Read: `docs/architecture/compliance/data-inventory.md`
- Read: `docs/architecture/capacity/retention-policy.md`

- [x] Document schema versioning, ownership, compatibility, replay, ordering, late-arriving data, backfill, deletion propagation, and lineage rules.
- [x] Include CDC/event contract examples for the selected slice.
- [x] Link PII and retention rules to compliance and capacity docs when those docs exist.

**Run:**
```bash
test -f docs/architecture/data-platform-contract-governance.md
rg "schema versioning|compatibility|replay|ordering|backfill|deletion propagation|lineage|CDC|event contract" docs/architecture/data-platform-contract-governance.md
cd backend && mvn test -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,AudienceComputeRunTrackingSchemaTest
```

**Expected:** Governance doc covers schema lifecycle and replay behavior, and source schema tests pass.

### Task 4: Define proof-of-concept tests and operating evidence

**Files:**
- Modify: `docs/architecture/data-platform-poc-plan.md`
- Modify: `docs/architecture/evidence/p3-03-data-platform.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/execution/PerfRunTrackingSchemaTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/audience/AudienceComputeRunTrackingSchemaTest.java`

- [x] Define contract tests for source schema, event envelope, replay idempotency, deletion propagation, and serving query shape.
- [x] Define operating evidence for freshness, dropped records, backfill duration, query latency, storage growth, and cost.
- [x] Record stop criteria for rejecting the selected platform component.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=PerfRunTrackingSchemaTest,AudienceComputeRunTrackingSchemaTest
rg "contract test|freshness|dropped records|backfill duration|query latency|storage growth|stop criteria" docs/architecture/data-platform-poc-plan.md docs/architecture/evidence/p3-03-data-platform.md
```

**Expected:** Proof-of-concept has testable contracts, operating evidence, and rejection criteria.

### Task 5: Review scoped data platform architecture changes

**Files:**
- Modify: `docs/architecture/archive/plans/P3-03-data-platform-architecture-plan.md`
- Create: `docs/architecture/evidence/p3-03-data-platform.md`
- Create: `docs/architecture/data-platform-source-inventory.md`
- Create: `docs/architecture/data-platform-poc-plan.md`
- Create: `docs/architecture/data-platform-contract-governance.md`

- [x] Review only files named in this plan.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record verification commands and remaining follow-ups in evidence.

**Run:**
```bash
git diff -- docs/architecture/archive/plans/P3-03-data-platform-architecture-plan.md docs/architecture/evidence/p3-03-data-platform.md docs/architecture/data-platform-source-inventory.md docs/architecture/data-platform-poc-plan.md docs/architecture/data-platform-contract-governance.md
```

**Expected:** The diff contains only data platform evidence, architecture docs, governance docs, and plan changes. No commit is created by default.
