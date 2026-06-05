# P3-002 AI Commerce Bets Discovery

This package ranks long-term AI and ecosystem bets. It ships no model integration, customer-facing automation, billing behavior, globalization behavior, privacy workflow, or partner feature.

## Source

- Spec: `docs/product-evolution/specs/p3-002-long-term-ai-commerce-and-ecosystem-bets.md`
- Plan: `docs/product-evolution/plans/p3-002-long-term-ai-commerce-and-ecosystem-bets-plan.md`
- Related governance: `docs/product-evolution/discovery/p3-001-plugin-marketplace`

## Promotion Rule

A bet can move to `Accepted For Child Spec` only with customer evidence, dependency readiness, model-risk review, approval boundary, proof command, rollback path, owner, and child spec path. AI bets must keep customer-facing, spend-affecting, privacy-affecting, and partner-facing actions behind human approval.

## Verification

Run:

```bash
node --test tools/strategy/ai-commerce-bets-evidence.test.mjs
node tools/strategy/ai-commerce-bets-evidence.mjs
```

Expected: both commands pass and the validator prints all bet keys.

## Rollout Stance

This slice ships no migration, no runtime AI behavior, no customer-facing UI, and no billing or partner data changes.
