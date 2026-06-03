# P1-004D - 3000 Concurrency Runbook And Baseline Gate Spec

Priority: P1
Sequence: 004D
Source: `docs/optimization/3000-concurrency-hardening-checklist.md`
Implementation plan: `../plans/p1-004d-3000-concurrency-runbook-and-baseline-gate-plan.md`

## Goal

Document the executable 3000 hardening release gate with entry requirements, baseline tests, profile execution, stop gates, rollback, degradation actions, and the 4000 block.

## Current Baseline

- P1-004 adds profile/evidence tooling.
- P1-004B adds stop-gate evaluation.
- P1-004C adds backend runtime metrics and lane routing tests.
- There is no dedicated runbook under `docs/product-evolution/runbooks/`.

## In Scope

- `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`.
- Link from `tools/perf/README.md`.
- Baseline command for the named backend tests.
- Profile execution command that writes evidence manifest.
- Explicit stop, rollback, degrade, and 4000-block sections.

## Out Of Scope

- Running live production hardening profiles.
- Changing code-level metrics or perf tools; split into P1-004 through P1-004C.
- Enabling 4000.

## Functional Requirements

1. Runbook states entry requirements before any 3000 promotion.
2. Runbook includes exact baseline command and pass condition.
3. Runbook includes exact profile/evidence command.
4. Runbook names the stop gates from P1-004B.
5. Runbook blocks 4000 readiness until all required 3000 profiles pass and artifacts are retained.

## Technical Scope

- `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`
- `tools/perf/README.md`

## Acceptance Criteria

- Runbook file exists and contains `Entry Requirements`, `Baseline Command`, `Profile Execution`, `Stop Gates`, `Rollback Actions`, `Degrade Actions`, and `4000 Block`.
- `tools/perf/README.md` links to the runbook.
