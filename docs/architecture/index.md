# Marketing Canvas Architecture

This folder has four roles:

- [reviewed-packages/](reviewed-packages/README.md): verified source packages grouped by priority for traceability. This is not the active implementation backlog.
- [specs/](specs/README.md): navigation index for completed, archived priority-prefixed specs.
- [plans/](plans/README.md): navigation index for completed, archived priority-prefixed implementation plans.
- [work-products/](work-products/README.md): supporting artifacts produced by the P3 architecture packages.
- [archive/](archive/README.md): processed source documents and completed spec/plan packages kept for traceability.

Use `archive/completed/` as the completed spec/plan record, `reviewed-packages/` as the source-package traceability record, and `work-products/` for supporting decisions, inventories, matrices, and checklists. The old review and remediation documents have been archived after extracting and re-checking their main claims against the current repository.

## Status Semantics

- Done documentation packages: `archive/completed/specs/` and `archive/completed/plans/`.
- Supporting outputs from completed P3 packages: `work-products/`.
- Reviewed source packages retained for traceability: `reviewed-packages/`.
- Not yet accepted or stale findings: only `reviewed-packages/needs-review/`.

## Reviewed Source Packages

| Priority | Focus |
|---|---|
| [P0](reviewed-packages/p0/) | security, correctness, production availability, concurrency, and data isolation |
| [P1](reviewed-packages/p1/) | API contracts, DAG boundaries, frontend editor state, observability and ops |
| [P2](reviewed-packages/p2/) | testing foundation, capacity/cost, documentation, dependency abstraction, compliance, and frontend quality |
| [P3](reviewed-packages/p3/) | platform evolution decision packages and promotion gates |
| [needs-review](reviewed-packages/needs-review/) | stale, duplicate, or decision-dependent findings |

## Package Index

P0:
- [Security hardening](reviewed-packages/p0/security-hardening/spec.md)
- [Reactive threading and transactions](reviewed-packages/p0/reactive-threading-and-transactions/spec.md)
- [Canvas state and data consistency](reviewed-packages/p0/canvas-state-data-consistency/spec.md)
- [Execution concurrency safety](reviewed-packages/p0/execution-concurrency-safety/spec.md)
- [Production resilience and DR](reviewed-packages/p0/production-resilience-and-dr/spec.md)
- [Data security and tenant isolation](reviewed-packages/p0/data-security-and-tenant-isolation/spec.md)

P1:
- [DAG engine and handler boundaries](reviewed-packages/p1/dag-engine-and-handler-boundaries/spec.md)
- [API contract and validation](reviewed-packages/p1/api-contract-and-validation/spec.md)
- [Frontend canvas state](reviewed-packages/p1/frontend-canvas-state/spec.md)
- [Observability and ops](reviewed-packages/p1/observability-and-ops/spec.md)
- [Release deployment governance](reviewed-packages/p1/release-deployment-governance/spec.md)

P2:
- [Testing foundation](reviewed-packages/p2/testing-foundation/spec.md)
- [Cost capacity and retention](reviewed-packages/p2/cost-capacity-and-retention/spec.md)
- [Documentation ADR and runbooks](reviewed-packages/p2/documentation-adr-and-runbooks/spec.md)
- [Dependency abstraction and vendor lock-in](reviewed-packages/p2/dependency-abstraction-and-vendor-lock-in/spec.md)
- [Compliance data governance](reviewed-packages/p2/compliance-data-governance/spec.md)
- [Frontend accessibility and quality](reviewed-packages/p2/frontend-accessibility-and-quality/spec.md)

P3:
- [Platform evolution source package](reviewed-packages/p3/platform-evolution/spec.md)
- [Platform evolution promotion checklist](work-products/p3-01-platform-evolution/platform-evolution-promotion-checklist.md)
- [P3-00 Architecture boundary review](archive/completed/specs/P3-00-architecture-boundary-review-spec.md)
- [P3-01 Platform evolution overview](archive/completed/specs/P3-01-platform-evolution-spec.md)
- [P3-02 Service decomposition and domain boundaries](archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md)
- [P3-03 Data platform architecture](archive/completed/specs/P3-03-data-platform-architecture-spec.md)
- [P3-04 Multi datasource isolation](archive/completed/specs/P3-04-multi-datasource-isolation-spec.md)
- [P3-05 WebFlux to MVC migration](archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md)
- [P3-06 K8s deployment platform](archive/completed/specs/P3-06-k8s-deployment-platform-spec.md)
- [P3-07 Production platform components](archive/completed/specs/P3-07-production-platform-components-spec.md)
- [P3-08 WeCom SCRM module](archive/completed/specs/P3-08-wecom-scrm-module-spec.md)
- [P3-09 Identity, event, and tenant platform](archive/completed/specs/P3-09-identity-event-and-tenant-platform-spec.md)

## Verification Notes

See [reviewed-packages/verification-summary.md](reviewed-packages/verification-summary.md) for evidence, [reviewed-packages/coverage-matrix.md](reviewed-packages/coverage-matrix.md) for source-to-package traceability, and [archive/completed/specs/P3-00-architecture-boundary-code-verification.md](archive/completed/specs/P3-00-architecture-boundary-code-verification.md) for the code-level architecture boundary check. Key corrections:

- JWT secret startup validation exists; deployment config enforcement remains.
- Tests exist; the remaining issue is critical-path and integration coverage.
- Some Redis route consistency fixes exist; remaining work is covered by state/data consistency and production resilience packages.

## Archive Categories

- `archive/reference/`: stable architecture/reference documents.
- `archive/reviews/`: prior review reports and checklists.
- `archive/remediation/`: original issue remediation documents.
- `archive/evolution/`: long-term architecture evolution material.
- `archive/completed/`: completed priority-prefixed specs and plans.
