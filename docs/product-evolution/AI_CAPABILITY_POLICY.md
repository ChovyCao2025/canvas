# AI Capability Policy

Status: Internal policy for product-evolution execution order.

## Visible Now

- Non-AI journey orchestration nodes listed in `NodeType`.
- `AI_LLM` only because P2-019 provides the governed provider, template, audit, handler, and frontend configuration path.
- Documentation references to future AI work when they point to an approved spec.

## Hidden Until Governed

- `AI_NEXT_BEST_ACTION`.
- Any node that calls an LLM provider, prompt template, embedding store, autonomous decision service, or generated offer selector without the P2-019 runtime, audit, and policy controls.

## Rules

1. Do not expose unfinished AI nodes in `/meta/node-types`.
2. Do not add AI node constants to `NodeType` unless the backing runtime behavior, audit trail, tenant controls, and frontend configuration exist.
3. Do not present AI copy in operator UI unless the backing runtime behavior exists.
4. Use P2-019 for governed `AI_LLM` provider, template, audit, and output-schema work.
5. Use P3 AI strategy specs for autonomous marketing operations, marketplaces, generated offer selectors, or public ecosystem claims.
