# P2-017D - Test Users And Single User Rerun Spec

Priority: P2
Sequence: 017D
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/archive/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p2-017d-test-users-and-single-user-rerun-plan.md`

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Add seed/test user management and single-user rerun modes with reason text and side-effect controls.

## Current Baseline

- Dry-run endpoints exist for canvas execution.
- Execution request replay exists for pending request recovery, not operator-controlled single-user rerun with explicit modes.

## In Scope

- Migration `V131__test_users_and_rerun_audit.sql`.
- `TestUserController` and `ExecutionRerunController`.
- Seed-user sets, selected user preview, dry-run mode, side-effect skip mode, admin replay mode, reason text, original execution reference, and audit rows.

## Out Of Scope

- Timeline UI and batch operations; split into P2-017E.

## Acceptance Criteria

- Backend tests prove reason requirement, mode defaults, original execution reference, audit write, and idempotency boundary.
