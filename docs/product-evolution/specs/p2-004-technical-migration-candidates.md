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

- `backend/canvas-engine/src/main/java/org/chovy/canvas/architecture`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TechnicalMigrationCandidateController.java`

### Frontend Touchpoints

- `frontend/src/services/technicalMigrationApi.ts`
- `frontend/src/pages/technical-migration-candidates/technicalMigrationCandidates.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V267__technical_migration_candidate_metrics.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/architecture/TechnicalMigrationCandidateTest.java`
- `frontend/src/pages/technical-migration-candidates/technical-migration-candidates.test.ts`

## Implementation Status

Completed on 2026-06-05.

- Added `V267__technical_migration_candidate_metrics.sql` for tenant-scoped migration evidence. The plan originally named `V163`; the current workspace already has later migrations including `V265__bi_datasource_health_snapshot.sql`, so this implementation uses the next available migration version.
- Added `TechnicalMigrationCandidateService` and `JdbcTechnicalMigrationCandidateRepository` to register proof commands, baseline JSON, rollback commands, default `BLOCKED_PENDING_REVIEW` decisions, and tenant-scoped release-gate checks.
- Added authenticated `TechnicalMigrationCandidateController` at `/architecture/migration-candidates/evidence`, guarded by `TenantContextResolver.currentOrError()`. The server records `submittedBy` from the authenticated tenant context rather than trusting the frontend payload.
- Reused the existing frontend `technicalMigrationApi` and `technicalMigrationCandidates` helpers for payload creation, candidate labels, gate copy, and endpoint coverage.
- Rollout: run `V267__technical_migration_candidate_metrics.sql`, then allow authenticated architects/operators to register evidence. Runtime migration remains blocked until reviewed evidence reaches `APPROVED_FOR_CHILD_SPEC`. Rollback: hide the evidence entry point and stop calling the endpoint; the table is additive governance metadata and no runtime path depends on it.

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
