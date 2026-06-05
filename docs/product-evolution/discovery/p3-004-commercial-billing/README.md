# P3-004 Commercial Billing Discovery

This package evaluates commercial model and billing capabilities. It ships no migration, no charging behavior, no entitlement enforcement, no invoice generation, no payment provider integration, no renewal automation, and no customer-facing upgrade UI.

## Source

- Spec: `docs/product-evolution/specs/p3-004-commercial-model-and-billing.md`
- Plan: `docs/product-evolution/plans/p3-004-commercial-model-and-billing-plan.md`
- Related evidence sources: product usage analytics, execution traces, and message delivery analytics

## Promotion Rule

A billing capability can move to `Accepted For Child Spec` only with owner, metric definition, source-of-truth evidence, finance gate, legal gate, support gate, proof command, rollback path, and child spec path.

## Verification

Run:

```bash
node --test tools/strategy/commercial-billing-evidence.test.mjs
node tools/strategy/commercial-billing-evidence.mjs
```

Expected: both commands pass and the validator prints all commercial capability keys.

## Rollout Stance

This slice ships no migration, no charging behavior, no entitlement change, no invoice generation, and no customer-facing UI.
