# P2-019 - AI LLM Node Productionization Spec

Priority: P2
Sequence: 019
Source: `docs/optimization/todo/2026-05-31-ai-capability-roadmap.md`, `docs/optimization/todo/competitor-analysis-report.md`, `docs/superpowers/specs/todo/2026-05-30-ai-llm-node-design.md`
Implementation plan: `../plans/p2-019-ai-llm-node-productionization-plan.md`

## Goal

Add a governed `AI_LLM` canvas node that calls configured LLM providers and writes validated structured output into execution context, while migrating any historical `AI_NEXT_BEST_ACTION` node rows if they exist.

## User And Business Value

Operators can use AI as an assistive data-producing step without pretending the platform has autonomous AI. This closes the misleading stub gap while preserving auditability, tenant isolation, cost controls, and deterministic fallback behavior.

## Evidence From Optimization

- Source docs describe `AI_NEXT_BEST_ACTION` as a fallback-only stub. The current source catalog no longer includes a production AI handler, so this spec creates the bounded replacement instead of preserving a public placeholder.
- AI capability docs repeatedly call out AI coverage below production level and identify AI_LLM as the smallest useful foundation.
- Competitor analysis shows AI copy, scoring, timing, and recommendations are important, but autonomous agents depend on governance and evaluation.

## In Scope

- Add `AI_LLM` as the production node type and migrate historical `AI_NEXT_BEST_ACTION` registry and canvas rows to it when those rows exist.
- Add tenant-scoped `ai_provider`, `ai_prompt_template`, and `ai_usage_audit` tables.
- Add provider CRUD with masked secrets, enabled state, default parameters, and tenant checks.
- Add prompt template CRUD with output schema, default fallback values, category, and enabled state.
- Add `AiLlmGateway`, `LlmClient`, and an OpenAI-compatible client for OpenAI, DeepSeek, Moonshot, Qwen-compatible gateways, and custom endpoints.
- Add `AiLlmHandler` that renders prompt variables from execution context, calls the gateway, validates JSON output, writes `ai_output`, and routes to `nextNodeId`.
- Add config-panel support for selecting provider/template and editing safe overrides.
- Record audit rows for calls, latency, fallback use, token estimates, provider, model, template, tenant, canvas, execution, and node.

## Out Of Scope

- Autonomous routing, AI agents, natural-language canvas generation, and automatic optimization.
- Training or hosting custom ML models.
- Image generation, chatbot UX, or a public AI marketplace.
- Replacing deterministic scoring, selector, or condition nodes with LLM decisions.

## Functional Requirements

1. AI output must be structured JSON and must be validated against the selected template schema before writing to `ai_output`.
2. LLM failure must not crash the DAG path when the template defines default values; the handler writes defaults and records `fallbackUsed=true`.
3. Provider API keys must never be returned in list/detail API responses or frontend state.
4. Provider API keys must be encrypted at rest with a required runtime secret or managed secret service.
5. Provider and template reads/writes must be tenant-scoped and role-gated.
6. Historical `AI_NEXT_BEST_ACTION` canvas rows, if present in a deployed database, must be rewritten to `AI_LLM` with an equivalent fallback template.
7. Audit rows must be written for success, fallback, timeout, invalid JSON, provider-disabled, and missing-template outcomes.
8. The node must expose only one success route in the first release; downstream routing remains in existing IF, SCORING, SELECTOR, or DELAY nodes.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClient.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiProviderService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiUsageAuditService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java`

### Frontend Touchpoints

- `frontend/src/components/canvas/constants.ts`
- `frontend/src/components/config-panel/index.tsx`
- `frontend/src/components/config-panel/AiLlmConfigPanel.tsx`
- `frontend/src/services/aiApi.ts`
- `frontend/src/pages/system-options/index.tsx`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V114__ai_llm_node_productionization.sql`
- `backend/canvas-engine/src/main/resources/application.yml`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClientTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiProviderControllerTest.java`
- `frontend/src/components/config-panel/aiLlmConfigPanel.test.tsx`
- `frontend/src/components/canvas/constants.test.ts`

## Dependencies

- P0-001 should keep current AI-like nodes hidden or beta-marked until this spec is implemented.
- P1-007 frontend schema validation makes AI node config safer but is not a hard blocker.
- P1-008 connector work remains separate; this spec only produces structured context data.

## Risks And Controls

- Cost runaway: enforce per-provider timeout, max tokens, retry count, and audit fields from the first release.
- Secret leakage: return masked provider secrets and add tests that raw keys never serialize.
- Prompt injection expectations: AI output is not a router or policy engine; existing deterministic nodes consume the output.
- Runtime latency: default timeout is 10 seconds and node-level override is capped by server config.
- Migration risk: rewrite old database rows before exposing `AI_LLM` and do not reintroduce `AI_NEXT_BEST_ACTION` as public catalog surface.

## Acceptance Criteria

- `AI_NEXT_BEST_ACTION` is no longer exposed as a production node type after migration; `AI_LLM` is exposed with provider/template selectors.
- Backend tests prove success, fallback, timeout, invalid JSON, missing template, disabled provider, tenant isolation, and secret masking.
- Frontend tests prove provider/template loading, schema override rendering, disabled-state copy, and saved config shape.
- Manual verification can create an AI_LLM node, run it with a mock OpenAI-compatible response, see `ai_output` in trace context, and route downstream through IF_CONDITION.
- No autonomous AI, NL canvas generation, or model-training scope is introduced by this spec.
