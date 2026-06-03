# P2-016B - Analytics Retention And Archive Policy Spec

Priority: P2
Sequence: 016B
Source: `docs/optimization/production-readiness-checklist.md`, `docs/optimization/production-design-gaps.md`
Implementation plan: `../plans/p2-016b-analytics-retention-and-archive-policy-plan.md`

## Goal

Add tenant-bounded retention and archive policies for event, trace, message, and webhook growth.

## Current Baseline

- Event, trace, and message tables grow without tenant-level retention controls.
- P2-016 adds archive status and legal hold fields where needed.
- There is no retention dry-run, audit output, or bounded archive/delete job.

## In Scope

- Migration `V128__analytics_retention_policy.sql`.
- Tenant policy with platform min/max bounds.
- Dry-run, archive, and delete actions in bounded batches.
- Legal-hold skip behavior and audit counters.

## Out Of Scope

- Analytics query APIs; split into P2-016C.
- Long-term warehouse choice; gated by P2-018 evidence.

## Acceptance Criteria

- Retention tests prove tenant defaults, overrides, legal hold skip, bounded delete/archive, dry-run counts, audit output, and unbounded-job rejection.
