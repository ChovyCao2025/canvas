# P1-004 - 3000 Hardening Profile Contract And Evidence Spec

Priority: P1
Sequence: 004
Source: `docs/optimization/3000-concurrency-hardening-checklist.md`
Implementation plan: `../plans/p1-004-3000-hardening-profile-contract-and-evidence-plan.md`

## Implementation Status

Implemented and focused-verified on 2026-06-05. The hardening profile contract now validates protected lane borrow rules, required 3000 failure-mode profiles, profile-level gates/actions, and can write an evidence manifest for a run.

## Goal

Make 3000-concurrency hardening profiles machine-readable, complete, and able to produce an evidence manifest for each run.

## Current Baseline

- `tools/perf/3000-hardening-profiles.json` has target concurrency `3000`, lane budgets `600/1800/300/300`, protected lanes, borrow rules, required profile names, and per-profile gates/actions.
- `tools/perf/hardening-profile.mjs` validates lane totals, protected lane borrow rules, required profiles, and profile stop/rollback/degrade actions.
- The CLI renders a threshold runner command and can write `evidence-manifest.json` for a hardening run.

## In Scope

- Profile schema fields:
  - `protectedLanes`
  - `borrowRules`
  - `requiredProfiles`
  - profile-level `stopGates`, `rollbackActions`, and `degradeActions`
- Required profiles:
  - `default-mixed-3000`
  - `retry-surge-3000`
  - `heavy-surge-3000`
  - `redis-latency-spike-3000`
  - `mysql-saturation-3000`
  - `rocketmq-backlog-3000`
  - `downstream-partial-failure-3000`
  - `retry-backlog-explosion-3000`
- Evidence manifest builder and optional CLI output.

## Out Of Scope

- Stop-gate metric evaluation; split into P1-004B.
- Backend runtime metrics and lane resolver coverage; split into P1-004C.
- Operator runbook and baseline gate; split into P1-004D.
- Enabling 4000 target.

## Functional Requirements

1. Profile validation rejects lane totals that do not equal `targetConcurrency`.
2. Profile validation rejects HEAVY or RETRY borrowing from protected LIGHT or STANDARD lanes.
3. Profile validation rejects missing required 3000 failure-mode profiles.
4. Every profile has nonempty stop gates, rollback actions, and degradation actions.
5. Evidence manifest includes run id prefix, target concurrency, profile name, lane budgets, protected lanes, command, stop gates, rollback actions, degradation actions, and expected metric sample filenames.

## Technical Scope

- `tools/perf/3000-hardening-profiles.json`
- `tools/perf/hardening-profile.mjs`
- `tools/perf/hardening-profile.test.mjs`
- `tools/perf/README.md`

## Acceptance Criteria

- `node --test tools/perf/hardening-profile.test.mjs` passes.
- `node tools/perf/hardening-profile.mjs --profile-file tools/perf/3000-hardening-profiles.json --profile default-mixed-3000 --out-dir tmp/perf-3000-hardening --run-id-prefix perf_3000_hardening_doc_check --write-evidence true` writes an `evidence-manifest.json`.
- Required profile names and protected lane borrow rules are documented in `tools/perf/README.md`.
