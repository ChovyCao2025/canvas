# Runtime Architecture Migration Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert broad runtime migration ideas into evidence-backed architecture decisions, proof commands, rollback notes, and child-spec gates before code rewrites begin.

**Architecture:** Build an ADR index and proof harness that records current-code evidence, expected benefit, cost, rollback strategy, verification command, and decision status for each migration candidate. Keep high-blast-radius changes blocked until a child implementation spec is opened with accepted evidence.

**Tech Stack:** Markdown ADRs, Java 21 architecture tests, Node.js perf scripts, existing backend runtime code, Git-tracked evidence reports.

---

## Spec Reference

- `docs/product-evolution/specs/p2-018-runtime-architecture-migration-evidence.md`
- Optimization sources: `docs/optimization/production-design-gaps.md`, `docs/optimization/todo/plan-review-findings.md`, `docs/optimization/todo/specs`, `docs/optimization/todo/plans`

## File Structure

**Architecture Decisions**
- Create: `docs/product-evolution/architecture-decisions/README.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-001-runtime-web-stack.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-002-dag-engine-execution-model.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-003-delivery-and-mq-topic-split.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-004-script-engine-sandbox.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-005-audience-bitmap-identity-mapping.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-006-trace-olap-storage.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-007-service-split-boundaries.md`

**Proofs**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/RuntimeMigrationEvidenceTest.java`
- Create: `tools/perf/runtime-migration-baseline.mjs`
- Create: `tools/perf/runtime-migration-baseline.test.mjs`

**Source Review Inputs**
- Read: `backend/canvas-engine/pom.xml`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`

### Task 1: ADR Index And Status Model

**Files:**
- Create: `docs/product-evolution/architecture-decisions/README.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-001-runtime-web-stack.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-002-dag-engine-execution-model.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-003-delivery-and-mq-topic-split.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-004-script-engine-sandbox.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-005-audience-bitmap-identity-mapping.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-006-trace-olap-storage.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-007-service-split-boundaries.md`

- [ ] **Step 1: Create ADR template in README**

Define required fields: status, owner, source evidence, current-code evidence, decision, expected benefit, cost, rollback, proof command, accepted evidence, child spec, and dependency notes.

- [ ] **Step 2: Add migration dependency graph**

List dependencies: production safety before rewrites, delivery outbox before MQ split rollout, trace sink before OLAP migration, CDP identity evidence before bitmap mapping changes, and 3000/4000 hardening before service split.

- [ ] **Step 3: Add ADR status table**

Use statuses `Draft`, `Proof Required`, `Accepted For Child Spec`, `Deferred`, `Rejected`, and `Merged Into Existing Spec`. No ADR can be `Accepted For Child Spec` without a proof command and evidence link.

- [ ] **Step 4: Commit ADR skeletons**

Run:

```bash
git add docs/product-evolution/architecture-decisions
git commit -m "docs: add runtime migration ADR index"
```

Expected: commit contains only ADR index and skeleton files.

### Task 2: Baseline Proof Harness

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/RuntimeMigrationEvidenceTest.java`
- Create: `tools/perf/runtime-migration-baseline.mjs`
- Create: `tools/perf/runtime-migration-baseline.test.mjs`

- [ ] **Step 1: Write architecture evidence tests**

Create `RuntimeMigrationEvidenceTest` methods named `recordsWebFluxAndMyBatisDependencies`, `recordsReactorDagAndDisruptorUsage`, `recordsGroovyScriptHandlerUsage`, `recordsBitmapHashMapping`, `recordsTraceMysqlWritePath`, and `recordsCurrentPackageBoundaries`.

- [ ] **Step 2: Write baseline script tests**

Create `runtime-migration-baseline.test.mjs` tests named `printsMachineReadableJson`, `requiresCandidateKeys`, `includesProofCommandOutputFields`, `rejectsMissingMetrics`, and `exitsNonZeroWhenSourceEvidenceIsUnavailable`.

- [ ] **Step 3: Run proof tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest
node --test tools/perf/runtime-migration-baseline.test.mjs
```

Expected: FAIL because the evidence test and baseline script do not exist.

- [ ] **Step 4: Implement architecture evidence test**

Read source files and assert the current architecture facts that each ADR cites. Keep assertions factual: dependency present, class present, handler present, topic split absent or present, and trace sink state.

- [ ] **Step 5: Implement baseline script**

Emit JSON with `generatedAt`, `candidates`, `sourceEvidence`, `proofCommands`, `riskLevel`, `dependencyStatus`, and `decisionStatus`. Fail when a candidate lacks source evidence, proof command, or rollback note.

- [ ] **Step 6: Run proof tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest
node --test tools/perf/runtime-migration-baseline.test.mjs
```

Expected: PASS.

### Task 3: Candidate ADR Decisions

**Files:**
- Create: `docs/product-evolution/architecture-decisions/ADR-001-runtime-web-stack.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-002-dag-engine-execution-model.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-003-delivery-and-mq-topic-split.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-004-script-engine-sandbox.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-005-audience-bitmap-identity-mapping.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-006-trace-olap-storage.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-007-service-split-boundaries.md`

- [ ] **Step 1: Fill web stack ADR**

Record WebFlux plus blocking persistence evidence, expected benefit of MVC plus virtual threads, migration cost, compatibility constraints, rollback to current runtime, and proof command.

- [ ] **Step 2: Fill DAG engine ADR**

Record Reactor DAG complexity evidence, current retry/wait behavior, risk of rewriting the scheduler, accepted smaller proof scope, rollback, and child-spec gate.

- [ ] **Step 3: Fill delivery and MQ ADR**

Merge delivery queue and delivery outbox into the P0-003 path, record MQ topic split as a later candidate, and block MQ split rollout until outbox receipts and reconciliation are implemented.

- [ ] **Step 4: Fill script sandbox ADR**

Record Groovy handler evidence, sandbox and metaspace risk, candidate alternatives QLExpress and Aviator, compatibility test needs, rollback, and proof gate.

- [ ] **Step 5: Fill bitmap identity ADR**

Record current UID mapping evidence, collision risk, deterministic mapping requirement, migration and backfill cost, dual-read rollout, rollback, and validation command.

- [ ] **Step 6: Fill trace OLAP ADR**

Record MySQL trace pressure, link to P2-016 sink abstraction, compare Doris and ClickHouse criteria, rollback to MySQL sink, and evidence needed before production dual-write.

- [ ] **Step 7: Fill service split ADR**

Record monolith pressure, package boundaries, runtime lanes, operational cost, sequencing after P0/P1 gates, rollback via single artifact, and child-spec requirements.

### Task 4: Repair Old Plan Contradictions

**Files:**
- Create: `docs/product-evolution/architecture-decisions/README.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-003-delivery-and-mq-topic-split.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-005-audience-bitmap-identity-mapping.md`
- Create: `docs/product-evolution/architecture-decisions/ADR-007-service-split-boundaries.md`

- [ ] **Step 1: Capture severe plan gaps**

List the old optimization plans that were incomplete: roaring bitmap collision, monolith split, Groovy migration, canvas tenant isolation, delivery queue/outbox split, frontend state, circuit breaker Redis, cache invalidation, and type safety.

- [ ] **Step 2: Correct delivery queue/outbox sequencing**

Mark delivery queue and delivery outbox as merged into P0-003. State that MQ topic split remains an architecture candidate, not the first implementation slice.

- [ ] **Step 3: Correct type and architecture requirements**

Record that old plans with broken type references, missing Architecture header, missing TDD flow, description-only steps, or placeholder comments cannot be executed directly.

- [ ] **Step 4: Add child-spec gate language**

For each candidate, state the exact condition required before implementation starts: passing proof command, accepted rollback, dependency placement, and named child spec.

### Task 5: Final Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-018-runtime-architecture-migration-evidence.md`
- Modify: `docs/product-evolution/plans/p2-018-runtime-architecture-migration-evidence-plan.md`

- [ ] **Step 1: Run evidence verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest
node --test tools/perf/runtime-migration-baseline.test.mjs
node tools/perf/runtime-migration-baseline.mjs --format json
```

Expected: PASS, and the baseline command emits JSON with all seven ADR candidate keys.

- [ ] **Step 2: Run ADR completeness scan**

Run:

```bash
rg -n "status:|owner:|proof command|rollback|child spec" docs/product-evolution/architecture-decisions
```

Expected: every ADR contains status, owner, proof command, rollback, and child spec fields.

- [ ] **Step 3: Commit implementation slice**

Run:

```bash
git add docs/product-evolution/architecture-decisions backend/canvas-engine/src/test/java/org/chovy/canvas/architecture tools/perf docs/product-evolution/specs docs/product-evolution/plans
git commit -m "docs: add runtime migration evidence gates"
```

Expected: commit contains only ADRs, proof tests, proof scripts, spec, and plan files.
