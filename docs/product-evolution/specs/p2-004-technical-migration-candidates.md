# P2-004 - Technical Migration Candidates Spec

Priority: P2
Sequence: 004
Source: `todo/p2/technical-migration-candidates.md`
Implementation plan: `../plans/p2-004-technical-migration-candidates-plan.md`

## Goal

Evaluate architecture migration candidates with evidence, rollback, and regression tests before changing runtime foundations.

## User And Business Value

The team can make technology moves based on measured need instead of whitepaper momentum.

## In Scope

- Validate PowerJob dynamic scheduling migration.
- Validate virtual-thread executor replacement for Disruptor task distribution.
- Validate RocketMQ topic split and Outbox delivery.
- Validate deterministic audience mapping plus Redis BITMAP.
- Validate Spring MVC plus command-style DAG migration as a combined candidate.
- Validate Aviator plus QLExpress script execution replacement.
- Validate Doris and Flink CDC after analytics requirements stabilize.

## Out Of Scope

- Direct migration without a separate proof and rollback plan.
- React Flow replacement unless product needs prove it insufficient.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/perf`

### Frontend Touchpoints

- `frontend/src/pages/canvas-editor/index.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V103__technical_migration_candidate_metrics.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/perf/PerfRunContextTest.java`

## Dependencies

- Requires current-code evidence, performance baseline, and rollback command for each candidate.
- Data infrastructure migration waits for analytics data contracts.

## Risks And Controls

- Scope creep: keep the first implementation to the workflow in this spec and move broader ideas to a follow-up spec.
- Tenant or permission regression: add backend tests for tenant-scoped data and role checks before exposing UI.
- UI complexity: use one page or one panel first, then expand only after the workflow is verified.
- Data migration risk: make every migration additive and reversible by disabling the new route or feature flag.

## Acceptance Criteria

- The source item has a visible implemented workflow or a documented discovery exit if this is a P3 strategy item.
- All changed backend endpoints reject unauthorized access and preserve tenant scoping.
- All changed frontend routes handle loading, empty, error, and permission states.
- Tests named in the plan pass in the local commands for backend and frontend slices.
- The implementation includes rollout notes covering feature flag, migration, and rollback behavior.
