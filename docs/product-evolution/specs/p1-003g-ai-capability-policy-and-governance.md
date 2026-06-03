# P1-003G - AI Capability Policy And Governance Spec

Priority: P1
Sequence: 003G
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003g-ai-capability-policy-and-governance-plan.md`

## Goal

Document internal AI visibility rules and keep unfinished AI nodes hidden until the production `AI_LLM` work in P2-019 lands.

## Current Baseline

- `NodeTypeGovernanceTest` already protects public node type constants.
- `AI_NEXT_BEST_ACTION` exists as unfinished or fallback-like capability in the codebase.
- P2-019 owns provider, template, audit, and output-schema work for production `AI_LLM`.

## In Scope

- `docs/product-evolution/AI_CAPABILITY_POLICY.md`.
- Governance test assertions that unfinished AI node constants remain absent from public `NodeType` constants.
- Cross-reference to P2-019 and P3 AI strategy specs.

## Out Of Scope

- AI node execution.
- LLM providers, prompt templates, embedding stores, autonomous decision services, generated offer selectors, or model evaluation.
- Public marketplace or community AI positioning.

## Functional Requirements

1. Policy states which capabilities are visible now and which are hidden until P2-019.
2. Governance test prevents `AI_LLM` and `AI_NEXT_BEST_ACTION` from being added to public node constants before the governed implementation exists.
3. Policy says operator UI copy must not advertise AI behavior without backing runtime behavior.

## Technical Scope

- `docs/product-evolution/AI_CAPABILITY_POLICY.md`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

## Acceptance Criteria

- Policy file exists and references P2-019 as the production AI owner.
- Governance test remains green and blocks unfinished public AI node constants.
