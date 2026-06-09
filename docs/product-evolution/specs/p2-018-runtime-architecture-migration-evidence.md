# P2-018 - Runtime Architecture Migration Evidence Spec

Priority: P2
Sequence: 018
Source: `docs/optimization/archive/production-design-gaps.md`, `docs/optimization/todo/plan-review-findings.md`, `docs/optimization/todo/specs`, `docs/optimization/todo/plans`
Implementation plan: `../plans/p2-018-runtime-architecture-migration-evidence-plan.md`

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Convert high-blast-radius architecture migration ideas into evidence-backed decisions, proofs, rollback notes, and corrected implementation order before runtime rewrites begin.

## User And Business Value

The team avoids speculative rewrites while still preserving the optimization work that matters for scale, safety, and maintainability.

## Evidence From Optimization

- Production design gaps identify WebFlux/MyBatis mismatch, Reactor DAG abstraction, monolith pressure, Disruptor overuse, Groovy sandbox risk, single MQ topic, RoaringBitmap collision, trace-to-OLAP, and React Flow workflow limits.
- Plan review findings identify incomplete or incorrect plans for RoaringBitmap, monolith split, Groovy to QLExpress, tenant isolation, delivery queue/outbox, frontend state, circuit breaker Redis, cache invalidation, and type safety.
- Existing `p2-004-technical-migration-candidates` preserves the ideas but is too broad to execute without discovery gates.

## In Scope

- Create ADRs and proof tasks for WebFlux to MVC plus virtual threads, imperative DAG engine, Disruptor replacement, Groovy to QLExpress/Aviator, MQ topic split, deterministic bitmap mapping, trace to Doris/ClickHouse, and service split.
- Repair plan-level contradictions from optimization review before any code migration is authorized.
- Add a migration dependency graph and sequencing gates.
- Define benchmark, rollback, compatibility, and feature-flag requirements for each migration.
- Mark candidates as accept, defer, reject, or merge into another spec based on measured evidence.

## Out Of Scope

- Executing the full migrations in this spec.
- Replacing current production behavior without a child implementation spec.
- Migrating React Flow to X6 without product evidence.

## Functional Requirements

1. Every migration candidate must have current-code evidence, expected benefit, cost, rollback strategy, and verification command.
2. Mutually dependent candidates must be ordered or merged before implementation starts.
3. Incorrect type references and missing architecture/TDD sections from old plans must be corrected in the new handoff docs.
4. A candidate cannot move to implementation until its proof passes and owner accepts the blast radius.
5. Rejected candidates must preserve rationale so the same debate does not restart.

## Technical Scope

### Docs Touchpoints

- `docs/product-evolution/architecture-decisions/ADR-001-runtime-web-stack.md`
- `docs/product-evolution/architecture-decisions/ADR-002-dag-engine-execution-model.md`
- `docs/product-evolution/architecture-decisions/ADR-003-delivery-and-mq-topic-split.md`
- `docs/product-evolution/architecture-decisions/ADR-004-script-engine-sandbox.md`
- `docs/product-evolution/architecture-decisions/ADR-005-audience-bitmap-identity-mapping.md`
- `docs/product-evolution/architecture-decisions/ADR-006-trace-olap-storage.md`
- `docs/product-evolution/architecture-decisions/ADR-007-service-split-boundaries.md`
- `docs/product-evolution/architecture-decisions/README.md`

### Proof Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/RuntimeMigrationEvidenceTest.java`
- `tools/perf/runtime-migration-baseline.mjs`
- `tools/perf/runtime-migration-baseline.test.mjs`

### Source Review Touchpoints

- `backend/canvas-engine/pom.xml`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/GroovyHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceBitmapStore.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/TraceWriteBuffer.java`

## Dependencies

- P0/P1 production stabilization remains ahead of broad rewrites.
- P0-003 handles delivery outbox as an implementation spec; this spec only captures broader MQ split and architecture decisions.

## Risks And Controls

- Rewrite risk: require proof, baseline, and rollback before implementation.
- Documentation drift risk: ADR status must be updated when a child spec starts.
- Overlap risk: dependency graph must merge delivery queue/outbox and separate implementation-ready work from discovery-only work.

## Acceptance Criteria

- ADR index exists with status for each migration candidate.
- Each candidate has a proof command, expected result, rollback note, and decision owner.
- Old optimization plan contradictions are captured and corrected in the new evidence docs.
- Runtime migration baseline script produces a machine-readable report.
- No candidate is marked implementation-ready without proof evidence and dependency placement.
