# Spec: Documentation, ADRs, And Runbooks

Source package: `docs/architecture/active/reviewed-packages/p2/documentation-adr-and-runbooks/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed for repository-controlled artifacts. ADRs, handler guidance, Redis key catalog, DAG execution flow, failure triage, operational runbooks, OpenAPI annotations, frontend OpenAPI metadata parsing, and archive-source policy are implemented and verified in-repo.

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

- [x] ADR directory exists with key decisions for WebFlux/MVC, Disruptor, NodeHandler model, Groovy, Redis/MQ choices, and data isolation.
- [x] Handler development guide exists.
- [x] Redis key catalog exists.
- [x] DAG execution flow and failure triage runbooks exist.
- [x] OpenAPI docs are generated from annotated API contracts.

## Implementation Evidence

- `docs/architecture/decisions/adr/`
- `docs/architecture/evidence/guides/node-handler-development.md`
- `docs/architecture/decisions/reference/redis-key-catalog.md`
- `docs/architecture/evidence/runbooks/dag-execution-flow.md`
- `docs/architecture/evidence/runbooks/failure-triage.md`
- `docs/architecture/evidence/runbooks/dlq-replay.md`
- `docs/architecture/evidence/runbooks/route-rebuild.md`
- `docs/architecture/evidence/runbooks/cache-invalidation.md`
- `docs/architecture/evidence/runbooks/deploy-rollback.md`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/OpenApiSecurityConfig.java`
- `frontend/src/pages/api-docs/openApiDocs.ts`
- `docs/architecture/evidence/P2-03-documentation-adr-and-runbooks.md`
