# Spec: WeCom SCRM Module

Source package: `docs/architecture/todo/p3/platform-evolution/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`

## Verification Status

Implemented as a WeCom integration slice and boundary package. Implementation is deferred until product, compliance, security, operations, Integration ownership, and sandbox secret handling are confirmed. Evidence and plans are in `docs/architecture/evidence/p3-08-wecom-scrm.md`, `docs/architecture/wecom-scrm-implementation-slice.md`, `docs/architecture/wecom-scrm-integration-boundary.md`, and `docs/architecture/wecom-scrm-test-plan.md`.

## Source Documents

- `docs/architecture/archive/evolution/wecom-scrm-module-design.md`

## Scope

Define a WeCom SCRM module that integrates customer identity, contact/group operations, callbacks, journey nodes, frontend configuration, and delivery/reach tracking without coupling WeCom-specific logic into the generic DAG engine. Per `P3-00-architecture-boundary-review-spec.md`, WeCom starts as an Integration bounded context unless the service extraction triggers are satisfied.

## Acceptance Criteria

- WeCom integration is isolated behind domain services and external-client adapters.
- Callback security, idempotency, retry, replay, and dead-letter behavior are defined.
- Data model covers customer/contact/group/session identity and consent/compliance fields.
- DAG nodes expose stable generic contracts rather than raw WeCom client details.
- Frontend configuration and API contracts are typed and testable.
