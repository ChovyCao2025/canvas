# P3-001 Plugin Marketplace Discovery

This package is a discovery and governance slice for public plugin marketplace strategy. It does not create application tables, routes, UI, payment behavior, plugin upload behavior, or publishing behavior.

## Source

- Spec: `docs/product-evolution/specs/p3-001-ecosystem-and-plugin-marketplace-strategy.md`
- Plan: `docs/product-evolution/plans/p3-001-ecosystem-and-plugin-marketplace-strategy-plan.md`
- Dependency: P2-002 plugin and integration foundations

## Promotion Rule

A capability can move to `Accepted For Child Spec` only when `evidence.json` includes an owner, evidence, proof command, launch gate, rollback path, dependencies, and a child spec path. Accepted capabilities must also keep P2-002 plugin foundations as an explicit dependency.

## Verification

Run:

```bash
node --test tools/strategy/plugin-marketplace-evidence.test.mjs
node tools/strategy/plugin-marketplace-evidence.mjs
```

Expected: both commands pass and the validator prints candidate keys for all marketplace capabilities.

## Rollout Stance

This slice ships no migration, no runtime route, and no UI. Rollback is limited to reverting this discovery package or moving candidate statuses below `Accepted For Child Spec`.
