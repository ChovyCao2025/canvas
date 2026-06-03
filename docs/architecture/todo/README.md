# Architecture Todo Index

This folder is the active architecture work queue. Items here were extracted from the previous architecture reviews and re-checked against the current repository.

For source-to-package traceability, see [coverage-matrix.md](coverage-matrix.md).

Flattened priority-prefixed specs are available in [../specs/](../specs/README.md), and matching implementation plans are available in [../plans/](../plans/README.md).

## Priority Folders

- `p0/`: security, correctness, production availability, and data consistency risks.
- `p1/`: high-impact architecture debt that should be planned soon.
- `p2/`: important foundation work with moderate urgency.
- `p3/`: long-term evolution and platform direction.
- `needs-review/`: stale, duplicate, or decision-dependent findings.

## Active Packages

| Priority | Package | Status |
|---|---|---|
| P0 | [security-hardening](p0/security-hardening/spec.md) | mixed confirmed / partially confirmed |
| P0 | [reactive-threading-and-transactions](p0/reactive-threading-and-transactions/spec.md) | confirmed |
| P0 | [canvas-state-data-consistency](p0/canvas-state-data-consistency/spec.md) | confirmed |
| P0 | [execution-concurrency-safety](p0/execution-concurrency-safety/spec.md) | confirmed |
| P0 | [production-resilience-and-dr](p0/production-resilience-and-dr/spec.md) | partially confirmed |
| P0 | [data-security-and-tenant-isolation](p0/data-security-and-tenant-isolation/spec.md) | confirmed |
| P1 | [dag-engine-and-handler-boundaries](p1/dag-engine-and-handler-boundaries/spec.md) | confirmed |
| P1 | [api-contract-and-validation](p1/api-contract-and-validation/spec.md) | confirmed |
| P1 | [frontend-canvas-state](p1/frontend-canvas-state/spec.md) | confirmed |
| P1 | [observability-and-ops](p1/observability-and-ops/spec.md) | confirmed |
| P1 | [release-deployment-governance](p1/release-deployment-governance/spec.md) | partially confirmed |
| P2 | [testing-foundation](p2/testing-foundation/spec.md) | partially confirmed |
| P2 | [cost-capacity-and-retention](p2/cost-capacity-and-retention/spec.md) | partially confirmed |
| P2 | [documentation-adr-and-runbooks](p2/documentation-adr-and-runbooks/spec.md) | partially confirmed |
| P2 | [dependency-abstraction-and-vendor-lock-in](p2/dependency-abstraction-and-vendor-lock-in/spec.md) | confirmed risk |
| P2 | [compliance-data-governance](p2/compliance-data-governance/spec.md) | partially confirmed |
| P2 | [frontend-accessibility-and-quality](p2/frontend-accessibility-and-quality/spec.md) | partially confirmed |
| P3 | [platform-evolution](p3/platform-evolution/spec.md) | planning material |

See [verification-summary.md](verification-summary.md) for the evidence summary and stale findings.
