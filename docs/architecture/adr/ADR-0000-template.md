# ADR-0000 Template

## Status

Proposed | Accepted | Superseded | Deprecated

## Context

Describe the forces, constraints, production incidents, specs, and source evidence that make this decision necessary.

## Decision

State the selected architecture decision in direct operational language.

## Alternatives

- Alternative 1: why it was not selected.
- Alternative 2: why it was not selected.

## Data Ownership

Name the owned tables, read models, retention owner, PII class, and migration path.

## API Contracts

Name synchronous APIs, DTOs, compatibility window, versioning rule, and client owners.

## Event Contracts

Name emitted and consumed events, schema owner, ordering, replay, and idempotency keys.

## Rollout Plan

Name deployment order, feature flags, dual-read or dual-write behavior, and smoke checks.

## Rollback Plan

Name rollback trigger, rollback command or sequence, reconciliation, and data repair owner.

## Observability

Name metrics, logs, traces, dashboards, alerts, SLOs, and runbook links.

## Tenant Propagation

Name tenant context source, propagation mechanism, cross-tenant denial behavior, and tests.

## Idempotency

Name idempotency keys, duplicate behavior, retry class, and reconciliation path.

## Exit Criteria

Name the measurable conditions required before this decision can move from proposed to accepted or from modular-monolith cleanup to physical extraction.

## Consequences

- Positive consequence.
- Negative consequence or tradeoff.
- Follow-up work required.

## Rollback Trigger

Name the measurable signal or date that forces reconsideration or rollback.

## Owner

Name the owning team or role.

## Linked Specs

- `docs/architecture/archive/specs/<spec>.md`
