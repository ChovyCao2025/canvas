# DDD Modular Rewrite

This directory contains the dedicated design material for the Marketing Canvas
DDD modular rewrite. It is intentionally separate from `docs/superpowers/`.

- [Specification](./2026-06-08-ddd-modular-rewrite-spec.md)
- [Implementation and Collaboration Plan](./2026-06-08-ddd-modular-rewrite-plan.md)
- [Conventions and Examples](./2026-06-08-ddd-rewrite-conventions-and-examples.md)

References:

- [Rich Domain Model Reference](./references/rich-domain-model-reference.md)
- [Class Placement Reference](./references/class-placement-reference.md)

Guardrails:

- [Guardrails Index](./guardrails/README.md)
- [LLM Drift and Failure Modes](./guardrails/llm-drift-and-failure-modes.md)
- [Automated Guardrail Checks](./guardrails/automated-guardrail-checks.md)
- [Review and Escalation Gates](./guardrails/review-and-escalation-gates.md)
- [Worker Enforcement Protocol](./guardrails/worker-enforcement-protocol.md)

Implementation support:

- [Inventory Guide](./inventory/README.md)
- [Context Ownership Seed Inventory](./inventory/context-ownership-seed.md)
- [Task Pack Index](./task-packs/README.md)
- [Marketing Pilot Child Spec](./child-specs/marketing-pilot-spec.md)
- [Canvas/Execution Contract Child Spec](./child-specs/canvas-execution-contract-spec.md)
- [Compatibility Contract Test Plan](./contract-tests/compatibility-test-plan.md)

Program coordination:

- [DDD and Open Source Growth Coordination](../program-coordination/README.md)

The rewrite is planned as a DDD-style modular monolith. The first delivery goal
is structural correctness and behavioral compatibility, not a simultaneous
product redesign.
