# Runtime Migration Architecture Decisions

This directory gates high-blast-radius runtime migrations before implementation starts.

## Required ADR Fields

Every ADR must include:

- `status: Draft | Proof Required | Accepted For Child Spec | Deferred | Rejected | Merged Into Existing Spec`
- `owner: <role or team>`
- `source evidence`
- `current-code evidence`
- `decision`
- `expected benefit`
- `cost`
- `rollback`
- `proof command`
- `accepted evidence`
- `child spec`
- `dependency notes`

No ADR may move to `Accepted For Child Spec` without a passing proof command, an accepted rollback plan, dependency placement, and a named child spec.

## Dependency Graph

1. P0/P1 production safety gates must stay ahead of broad runtime rewrites.
2. Delivery outbox, receipts, and reconciliation must land before MQ topic split rollout.
3. P2-016 trace schema and sink abstraction must land before OLAP trace storage migration.
4. CDP identity evidence and deterministic ID mapping must land before bitmap remapping or backfill.
5. 3000/4000 concurrency hardening evidence must land before service split.
6. Script-engine migration must prove semantic compatibility before replacing Groovy.
7. Web stack migration must prove blocking persistence isolation before replacing WebFlux.

## Candidate Status

| ADR | Candidate | Status | Owner | Proof Command | Child Spec |
| --- | --- | --- | --- | --- | --- |
| ADR-001 | Runtime Web Stack | Proof Required | Platform Runtime | `mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest` | Required before MVC/virtual-thread migration |
| ADR-002 | DAG Execution Model | Deferred | Execution Engine | `mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest` | Required before imperative DAG rewrite |
| ADR-003 | Delivery And MQ Topic Split | Merged Into Existing Spec | Delivery Platform | `node tools/perf/runtime-migration-baseline.mjs --format json` | P0-003 plus later MQ split child spec |
| ADR-004 | Script Engine Sandbox | Proof Required | Execution Engine | `mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest` | Required before QLExpress/Aviator migration |
| ADR-005 | Audience Bitmap Mapping | Proof Required | CDP Data | `mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest` | Required before bitmap remapping |
| ADR-006 | Trace OLAP Storage | Deferred | Data Platform | `mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest` | P2-016 child rollout |
| ADR-007 | Service Split Boundaries | Deferred | Platform Architecture | `node tools/perf/runtime-migration-baseline.mjs --format json` | Required after hardening gates |

## Corrected Old Plan Gaps

The following optimization plans are not executable directly without this evidence gate: RoaringBitmap collision remediation, monolith split, Groovy migration, canvas tenant isolation, delivery queue/outbox split, frontend state migration, circuit-breaker Redis, cache invalidation, and frontend type safety.

Common defects captured by this gate: broken type references, missing architecture section, missing TDD flow, description-only steps, placeholder comments, and unclear rollback.

Delivery queue and delivery outbox sequencing is corrected here: outbox, receipts, idempotency, and reconciliation are implementation work under P0-003; MQ topic split remains a later architecture candidate.
