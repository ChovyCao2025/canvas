# Spec: Documentation, ADRs, And Runbooks

## Verification Status

Partially confirmed. Documentation exists, but decision records and runbooks are incomplete.

## Problems Covered

- Key architecture decisions lack ADRs.
- Handler development guidance, Redis key map, DAG execution flow, failure triage, OpenAPI annotations/spec, and operational runbooks are incomplete.
- Archived documents are broad review material; active implementation docs need clearer ownership and current-state traceability.

## Source Coverage

- `archive/reviews/architecture-supplement-review-2026-05.md`: knowledge management gap.
- `archive/reviews/architect-checklist-report.md`: implementation guidance and technical documentation gaps.
- `archive/reference/*`: existing docs retained as source/reference.
- `archive/evolution/production-practice-review.md`: proposed production conventions.

## Acceptance Criteria

- ADR directory exists with key decisions for WebFlux/MVC, Disruptor, NodeHandler model, Groovy, Redis/MQ choices, and data isolation.
- Handler development guide exists.
- Redis key catalog exists.
- DAG execution flow and failure triage runbooks exist.
- OpenAPI docs are generated from annotated API contracts.
