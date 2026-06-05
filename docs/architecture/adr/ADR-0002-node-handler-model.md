# ADR-0002 NodeHandler Model

## Status

Accepted

## Context

Canvas execution relies on a registry of node handlers. Each implementation maps one node type to the `NodeHandler` interface and is discovered through `@NodeHandlerType`. The DAG runtime is shared by direct execution, event triggers, MQ triggers, scheduled triggers, wait/resume, and replay paths.

## Decision

Keep a single `NodeHandler` extension model. New node behavior must be implemented as a Spring bean plus `@NodeHandlerType("<TYPE_KEY>")`, return `Mono<NodeResult>`, and let the engine own routing, trace writes, side-effect idempotency, and terminal state transitions.

## Alternatives

- Inline node logic inside the DAG engine: rejected because it would couple every node change to scheduler control flow.
- Register handlers through manual maps only: rejected because annotation discovery makes handler ownership and tests easier to audit.

## Consequences

- Node behavior stays isolated and testable.
- Handler code must not bypass the engine by mutating execution state, trace rows, or request lifecycle directly.
- Mapper access must stay in services or explicitly documented handler dependencies.

## Rollback Trigger

Revisit this ADR if handler registration becomes ambiguous, if one handler needs cross-node orchestration, or if a future service split moves node execution out of the current backend.

## Owner

Canvas engine owner.

## Linked Specs

- `docs/architecture/specs/P1-01-dag-engine-and-handler-boundaries-spec.md`
- `docs/architecture/specs/P1-02-api-contract-and-validation-spec.md`
- `docs/architecture/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
