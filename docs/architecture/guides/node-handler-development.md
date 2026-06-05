# NodeHandler Development Guide

## Scope

Use this guide when adding or changing a canvas node handler. The runtime entry point is `NodeHandler`, registration is through `@NodeHandlerType`, and graph structure comes from `DagParser`.

## Handler Contract

- Implement `NodeHandler#executeAsync(Map<String, Object> config, ExecutionContext ctx)`.
- Register the implementation as a Spring bean and annotate it with `@NodeHandlerType("<TYPE_KEY>")`.
- Return `NodeResult.ok`, `NodeResult.multiNext`, `NodeResult.timeout`, `NodeResult.terminal`, or `NodeResult.fail`; do not route by directly mutating scheduler state.
- Put outputs in the `NodeResult` output map. The engine owns context merge and trace writes.
- For side-effect nodes, override `requiresSideEffectIdempotency` and `sideEffectOperationKey` instead of hand-rolling dedup logic.

## Blocking-Call Rules

- Pure calculation handlers may return `Mono.just(...)`.
- Network handlers should compose reactive clients directly.
- Blocking SDKs, mapper reads, file IO, and script execution must be moved to the existing virtual-thread or bounded executor boundary used by the owning service.
- Never call `.block()` inside a request thread or handler execution path.

## Input And Output Contracts

- Inputs come from node `config`/`bizConfig` after `DagParser` merges edge fields.
- Stable next-hop fields must use `MapFieldKeys`, such as `nextNodeId`, `successNodeId`, `failNodeId`, `timeoutNodeId`, `hitNextNodeId`, and `missNextNodeId`.
- Outputs become execution context fields and must use stable names that the frontend variable picker can expose.
- Handler failures should return a structured `NodeResult.fail` reason unless the error is a programming or data corruption fault.

## Trace And Lifecycle Expectations

- The DAG engine writes trace rows and request/execution lifecycle state.
- Handlers should include enough reason codes in `NodeResult` for trace review.
- WAIT handlers are special: `WaitHandler` may return pending/timeout semantics, while wait/resume ownership stays with the wait services and scheduler.

## Mapper Access Restrictions

Mapper access in a handler must be treated as an exception. Prefer a domain service that owns tenant checks, retries, and transaction boundaries. Direct mapper access is only acceptable when the handler is itself the domain owner and has a focused test covering tenant, null, and failure behavior.

## Add-Handler Checklist

- Add or confirm `NodeType` and registry seed/migration entries.
- Implement `NodeHandler` plus `@NodeHandlerType`.
- Add focused handler tests beside existing handler tests, such as `StartHandlerTest` or `WaitHandlerTest`.
- Update `NodeRouteResolverTest` when the handler introduces new routing fields.
- Update frontend config panel and serialization tests when the handler has new editable config.
- Add observability or DLQ tests if the handler performs side effects.
- Update this guide and active ADR links when the handler changes runtime boundaries.
