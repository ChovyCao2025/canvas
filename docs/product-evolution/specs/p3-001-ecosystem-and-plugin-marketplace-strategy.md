# P3-001 - Ecosystem And Plugin Marketplace Strategy Spec

Priority: P3
Sequence: 001
Source: `todo/p3/ecosystem-and-plugin-marketplace-strategy.md`
Implementation plan: `../plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md`

## Implementation Status

Status: discovery package and validator are complete in the current workspace record. Commit and merge status was not verified in this docs-only audit.

## Goal

Convert the public plugin marketplace idea into an evidence-backed governance package that can decide whether a later child spec should build submission, review, publishing, and partner-support workflows.

## User And Business Value

This protects the platform from committing to public plugin distribution before internal plugin foundations, security review, partner support, and commercial ownership are proven.

## In Scope

- Marketplace candidate evidence for plugin submission, review, publishing, pricing, support, and takedown.
- Governance policy for SDK compatibility, security review, tenant safety, support ownership, and launch gates.
- Decision log that separates `Accepted For Child Spec`, `Needs Evidence`, `Deferred`, and `Rejected` marketplace capabilities.
- Validator script and tests that prevent promotion without evidence, owner, rollback, and proof command.

## Out Of Scope

- Runtime marketplace implementation, plugin upload APIs, public storefront UI, billing integration, or partner portal build-out.
- Legal, commercial, or security commitments without named owners and linked evidence.
- Flyway schema changes or application data persistence.

## Functional Requirements

1. The strategy package must list concrete marketplace capabilities and assign each one a decision status, owner, evidence link, proof command, launch gate, and rollback path.
2. No capability can be marked `Accepted For Child Spec` unless internal plugin foundations are listed as a dependency and the evidence file includes a passing proof command.
3. Security and support gates must cover package signing, tenant isolation, permission review, vulnerability response, support escalation, and plugin takedown.
4. The validator must fail the package when required evidence fields are missing or when a capability is promoted without a child spec path.
5. The rollout notes must state that this slice creates Git-tracked discovery artifacts only and does not expose runtime endpoints or UI.

## Technical Scope

### Documentation Touchpoints

- `docs/product-evolution/discovery/p3-001-plugin-marketplace/README.md`
- `docs/product-evolution/discovery/p3-001-plugin-marketplace/evidence.json`
- `docs/product-evolution/discovery/p3-001-plugin-marketplace/governance-policy.md`
- `docs/product-evolution/discovery/p3-001-plugin-marketplace/decision-log.md`

### Tooling Touchpoints

- `tools/strategy/plugin-marketplace-evidence.mjs`
- `tools/strategy/plugin-marketplace-evidence.test.mjs`

### Data And Configuration Touchpoints

- No Flyway migration is part of this slice. The work writes versioned Markdown and JSON evidence only; runtime marketplace tables would be designed in a later child spec after this package reaches `Accepted For Child Spec`.

### Test Touchpoints

- `tools/strategy/plugin-marketplace-evidence.test.mjs`

## Dependencies

- P2 plugin and integration foundations must be stable before any marketplace implementation child spec starts.
- Security, support, and commercial owners must be named in the discovery evidence before launch gates can pass.
- Public marketplace claims must be checked against current partner, legal, and security constraints during execution.

## Risks And Controls

- Scope creep: keep this slice to evidence, governance, and decision gates; open a child spec for the first runtime workflow after acceptance.
- Unsupported plugin risk: require support escalation and takedown rules before publishing is accepted.
- Tenant safety regression: require security review and permission-boundary evidence before any upload or install API is designed.
- Premature revenue commitment: keep commercial policy as a gate until pricing and partner terms have explicit owners.

## Acceptance Criteria

- The evidence validator tests pass and fail on missing owner, proof command, launch gate, rollback, or child spec path.
- The evidence package contains capability rows for submission, review, publishing, SDK compatibility, security review, commercial terms, support, and takedown.
- The decision log leaves unproven capabilities in `Needs Evidence`, `Deferred`, or `Rejected` rather than implying implementation approval.
- Rollout notes explicitly say no migration, no runtime route, and no UI are shipped by this slice.
- The plan includes scoped `git add` and commit commands limited to the discovery docs, validator, and this spec/plan pair.
