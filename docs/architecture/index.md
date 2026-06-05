# Marketing Canvas Architecture

This folder now has two roles:

- [todo/](todo/README.md): active, verified architecture work packages grouped by priority.
- [specs/](specs/README.md): flattened priority-prefixed specs for each active package.
- [plans/](plans/README.md): flattened priority-prefixed implementation plans for each active package.
- [archive/](archive/README.md): processed source documents kept for traceability.

Use `todo/` as the traceability queue, `specs/` for package requirements, and `plans/` for execution handoff. The old review and remediation documents have been archived after extracting and re-checking their main claims against the current repository.

## Active Priority Queue

| Priority | Focus |
|---|---|
| [P0](todo/p0/) | security, correctness, production availability, concurrency, and data isolation |
| [P1](todo/p1/) | API contracts, DAG boundaries, frontend editor state, observability and ops |
| [P2](todo/p2/) | testing foundation, capacity/cost, documentation, dependency abstraction, compliance, and frontend quality |
| [P3](todo/p3/) | platform evolution decision packages and promotion gates |
| [needs-review](todo/needs-review/) | stale, duplicate, or decision-dependent findings |

## Current Packages

P0:
- [Security hardening](todo/p0/security-hardening/spec.md)
- [Reactive threading and transactions](todo/p0/reactive-threading-and-transactions/spec.md)
- [Canvas state and data consistency](todo/p0/canvas-state-data-consistency/spec.md)
- [Execution concurrency safety](todo/p0/execution-concurrency-safety/spec.md)
- [Production resilience and DR](todo/p0/production-resilience-and-dr/spec.md)
- [Data security and tenant isolation](todo/p0/data-security-and-tenant-isolation/spec.md)

P1:
- [DAG engine and handler boundaries](todo/p1/dag-engine-and-handler-boundaries/spec.md)
- [API contract and validation](todo/p1/api-contract-and-validation/spec.md)
- [Frontend canvas state](todo/p1/frontend-canvas-state/spec.md)
- [Observability and ops](todo/p1/observability-and-ops/spec.md)
- [Release deployment governance](todo/p1/release-deployment-governance/spec.md)

P2:
- [Testing foundation](todo/p2/testing-foundation/spec.md)
- [Cost capacity and retention](todo/p2/cost-capacity-and-retention/spec.md)
- [Documentation ADR and runbooks](todo/p2/documentation-adr-and-runbooks/spec.md)
- [Dependency abstraction and vendor lock-in](todo/p2/dependency-abstraction-and-vendor-lock-in/spec.md)
- [Compliance data governance](todo/p2/compliance-data-governance/spec.md)
- [Frontend accessibility and quality](todo/p2/frontend-accessibility-and-quality/spec.md)

P3:
- [Platform evolution source queue](todo/p3/platform-evolution/spec.md)
- [Platform evolution promotion checklist](platform-evolution-promotion-checklist.md)
- [P3-00 Architecture boundary review](archive/specs/P3-00-architecture-boundary-review-spec.md)
- [P3-01 Platform evolution overview](archive/specs/P3-01-platform-evolution-spec.md)
- [P3-02 Service decomposition and domain boundaries](archive/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md)
- [P3-03 Data platform architecture](archive/specs/P3-03-data-platform-architecture-spec.md)
- [P3-04 Multi datasource isolation](archive/specs/P3-04-multi-datasource-isolation-spec.md)
- [P3-05 WebFlux to MVC migration](archive/specs/P3-05-webflux-to-mvc-migration-spec.md)
- [P3-06 K8s deployment platform](archive/specs/P3-06-k8s-deployment-platform-spec.md)
- [P3-07 Production platform components](archive/specs/P3-07-production-platform-components-spec.md)
- [P3-08 WeCom SCRM module](archive/specs/P3-08-wecom-scrm-module-spec.md)
- [P3-09 Identity, event, and tenant platform](archive/specs/P3-09-identity-event-and-tenant-platform-spec.md)

## Verification Notes

See [todo/verification-summary.md](todo/verification-summary.md) for evidence, [todo/coverage-matrix.md](todo/coverage-matrix.md) for source-to-package traceability, and [specs/P3-00-architecture-boundary-code-verification.md](archive/specs/P3-00-architecture-boundary-code-verification.md) for the code-level architecture boundary check. Key corrections:

- JWT secret startup validation exists; deployment config enforcement remains.
- Tests exist; the remaining issue is critical-path and integration coverage.
- Some Redis route consistency fixes exist; remaining work is covered by state/data consistency and production resilience packages.

## Archive Categories

- `archive/reference/`: stable architecture/reference documents.
- `archive/reviews/`: prior review reports and checklists.
- `archive/remediation/`: original issue remediation documents.
- `archive/evolution/`: long-term architecture evolution material.
