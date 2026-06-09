# P2-016B - Analytics Retention And Archive Policy Spec

Priority: P2
Sequence: 016B
Source: `docs/optimization/archive/production-readiness-checklist.md`, `docs/optimization/archive/production-design-gaps.md`
Implementation plan: `../plans/p2-016b-analytics-retention-and-archive-policy-plan.md`

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

## Goal

Add tenant-bounded retention and archive policies for analytics event and trace growth.

## Current Baseline

- `V132__analytics_event_trace_schema_and_sink.sql` provides archive status and legal-hold fields on `analytics_event` and `analytics_event_trace`.
- `V133__analytics_retention_policy.sql` already creates `analytics_retention_policy` and `analytics_retention_run`.
- Data objects, mappers, and mapper-level bounded archive/delete methods already exist for analytics event and trace rows.
- Before this slice there was no executable service that resolved tenant policy, enforced platform bounds, produced dry-run output, or recorded retention run audit rows.

## In Scope

- Existing migration `V133__analytics_retention_policy.sql`.
- Tenant policy resolution with tenant override and tenant `0` platform default fallback.
- Platform min/max retention-day bounds and max batch-size guardrails from `canvas.analytics.retention.*` configuration.
- Dry-run, archive, and delete actions in one bounded batch per invocation.
- Legal-hold skip behavior and retention run audit counters.

## Out Of Scope

- Analytics query APIs; split into P2-016C.
- Long-term warehouse choice; gated by P2-018 evidence.
- Message and webhook retention jobs; this slice only wires the analytics event/trace schema available after P2-016.

## Acceptance Criteria

- Retention tests prove tenant defaults, overrides, legal hold skip, bounded delete/archive, dry-run counts, audit output, and unbounded-job rejection.

## Verification Evidence

- 2026-06-09: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=RetentionPolicyServiceTest` passed with 5 tests, 0 failures, 0 errors.
