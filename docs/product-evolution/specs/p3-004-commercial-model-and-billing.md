# P3-004 - Commercial Model And Billing Spec

Priority: P3
Sequence: 004
Source: `todo/p3/strategic-opportunities-from-filtered-scope.md#commercial-model-and-billing`
Implementation plan: `../plans/p3-004-commercial-model-and-billing-plan.md`

## Implementation Status

Status: discovery package and validator are complete in the current workspace record. Commit and merge status was not verified in this docs-only audit.

## Goal

Define the evidence, governance, and child-spec gates for billable metrics, plan tiers, overage policy, payment, invoices, renewals, and upgrade recommendations before billing infrastructure is built.

## User And Business Value

This lets the business evaluate monetization options without adding premature billing tables, customer-facing invoices, or payment workflows before finance, legal, product, and engineering owners agree on the first billable slice.

## In Scope

- Commercial metric inventory for executions, contacts, messages, seats, storage, AI usage, and premium connectors.
- Billing governance policy for finance approval, legal review, invoice accuracy, entitlement boundaries, tenant isolation, and rollback.
- Evidence scoring for metric measurability, customer value, implementation dependency, support burden, and launch readiness.
- Decision log for billing capabilities that may graduate into child specs.

## Out Of Scope

- Implementing metering ledgers, entitlement enforcement, payment provider integration, invoice generation, renewal automation, or upgrade UI.
- Charging customers or changing production plan access.
- Flyway schema changes.

## Functional Requirements

1. The billing strategy package must record each commercial capability with owner, metric definition, source-of-truth evidence, finance gate, legal gate, support gate, proof command, rollback path, decision status, and child spec path.
2. No capability can be marked `Accepted For Child Spec` unless finance and legal gates are present and the source metric can be measured by existing or explicitly planned instrumentation.
3. The package must distinguish discovery metrics from billable commitments and must not imply active charging.
4. The validator must fail on missing owner, metric definition, source evidence, approval gate, rollback path, proof command, or child spec path for accepted capabilities.
5. Rollout notes must confirm that this slice creates documentation and validation tooling only.

## Technical Scope

### Documentation Touchpoints

- `docs/product-evolution/discovery/p3-004-commercial-billing/README.md`
- `docs/product-evolution/discovery/p3-004-commercial-billing/evidence.json`
- `docs/product-evolution/discovery/p3-004-commercial-billing/governance-policy.md`
- `docs/product-evolution/discovery/p3-004-commercial-billing/decision-log.md`

### Tooling Touchpoints

- `tools/strategy/commercial-billing-evidence.mjs`
- `tools/strategy/commercial-billing-evidence.test.mjs`

### Data And Configuration Touchpoints

- No Flyway migration is part of this slice. Billing data models, ledgers, entitlements, and invoice tables require a later child spec after finance/legal gates and metric evidence are accepted.

### Test Touchpoints

- `tools/strategy/commercial-billing-evidence.test.mjs`

## Dependencies

- Product usage analytics and channel/message instrumentation should inform measurable usage candidates.
- Finance and legal owners must approve billing gates before any child spec designs customer-impacting behavior.
- Security and tenant isolation constraints must be named before entitlement or invoice data models are proposed.

## Risks And Controls

- Premature charging risk: keep this slice discovery-only and require finance/legal gates for accepted child specs.
- Metric accuracy risk: require source-of-truth evidence and proof commands before selecting a billable metric.
- Customer trust risk: require rollback and support gates before any billing workflow is designed.
- Scope creep: separate metering, entitlement, invoice, payment, renewal, and upgrade implementation into child specs.

## Acceptance Criteria

- The evidence validator tests pass and reject accepted billing capabilities missing metric, owner, gates, proof command, rollback, or child spec path.
- The evidence package covers billable metrics, tiers, overage, payment, invoices, renewal, and upgrade recommendations.
- The decision log keeps unproven commercial ideas out of implementation.
- Rollout notes state no migration, no charging behavior, no entitlement change, no invoice generation, and no customer-facing UI.
- The plan includes scoped `git add` and commit commands limited to the discovery docs, validator, and this spec/plan pair.
