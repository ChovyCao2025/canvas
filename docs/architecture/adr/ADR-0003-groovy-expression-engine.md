# ADR-0003 Groovy Expression Engine

## Status

Accepted with guardrails

## Context

Groovy-backed nodes let operators transform execution context fields without shipping Java code. The same capability can create security, latency, and determinism risk if scripts perform unbounded work, access unsafe APIs, or hide side effects.

## Decision

Keep Groovy as a constrained expression engine for configured transformation nodes only. Groovy handlers must execute through the existing handler boundary, respect execution timeout and trace rules, and avoid external side effects. Any expansion beyond transformation logic requires a new ADR or child spec.

## Alternatives

- Remove Groovy support: rejected because existing operator workflows depend on configurable transformations.
- Allow arbitrary JVM scripting: rejected because it expands security and capacity risk beyond the current execution model.

## Consequences

- Groovy remains useful for low-code transformations.
- Security and capacity controls must remain part of handler review.
- Long-running or side-effecting scripts must move to typed handlers or integration services.

## Rollback Trigger

Disable or replace Groovy if script execution causes a production incident, exceeds the execution SLO budget, or fails future sandbox/security review.

## Owner

Canvas engine owner with security reviewer sign-off.

## Linked Specs

- `docs/architecture/specs/P0-01-security-hardening-spec.md`
- `docs/architecture/specs/P1-01-dag-engine-and-handler-boundaries-spec.md`
- `docs/architecture/specs/P2-02-cost-capacity-and-retention-spec.md`
