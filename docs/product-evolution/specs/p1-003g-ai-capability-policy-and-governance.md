# P1-003G - AI Capability Policy And Governance Spec

Priority: P1
Sequence: 003G
Source: `todo/p1/mautic-inspired-quick-adoptions.md`
Implementation plan: `../plans/p1-003g-ai-capability-policy-and-governance-plan.md`

## Implementation Status

Implemented and focused-verified on 2026-06-05. The original "hide `AI_LLM` until P2-019" assumption is superseded by the current codebase, where P2-019 runtime artifacts exist; governance now permits `AI_LLM` only with backing runtime, audit, tenant, and UI controls, and continues to block `AI_NEXT_BEST_ACTION`.

## Goal

Document internal AI visibility rules and keep unfinished AI nodes hidden unless the governed runtime work exists.

## Current Baseline

- `NodeTypeGovernanceTest` already protects public node type constants.
- `AI_LLM` exists in the current codebase with provider, template, audit, handler, and frontend configuration artifacts from P2-019 work.
- `AI_NEXT_BEST_ACTION` remains an unfinished or fallback-like legacy capability and must not be exposed as a public node constant.

## In Scope

- `docs/product-evolution/AI_CAPABILITY_POLICY.md`.
- Governance test assertions that unfinished AI node constants remain absent from public `NodeType` constants.
- Governance test assertions that public `AI_LLM` visibility is tied to concrete runtime and policy artifacts.
- Cross-reference to P2-019 and P3 AI strategy specs.

## Out Of Scope

- AI node execution.
- LLM providers, prompt templates, embedding stores, autonomous decision services, generated offer selectors, or model evaluation.
- Public marketplace or community AI positioning.

## Functional Requirements

1. Policy states which capabilities are visible now and which remain hidden until governed.
2. Governance test prevents `AI_NEXT_BEST_ACTION` from being added to public node constants and requires `AI_LLM` visibility to have backing runtime and policy artifacts.
3. Policy says operator UI copy must not advertise AI behavior without backing runtime behavior.

## Technical Scope

- `docs/product-evolution/AI_CAPABILITY_POLICY.md`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

## Acceptance Criteria

- Policy file exists and references P2-019 as the production AI owner.
- Governance test remains green and blocks unfinished public AI node constants.
- If `AI_LLM` is public, governance test proves P2-019 runtime/policy artifacts exist.
