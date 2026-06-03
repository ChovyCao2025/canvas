# AI LLM Node Productionization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a governed `AI_LLM` node that calls configured LLM providers and writes validated structured output into execution context, while migrating historical `AI_NEXT_BEST_ACTION` rows if they exist.

**Architecture:** Add tenant-scoped provider/template/audit storage, a gateway that handles provider routing and fallback, and a single canvas handler that treats AI as a structured data producer. Keep downstream decisions in deterministic nodes and expose only admin-safe provider/template configuration to the frontend.

**Tech Stack:** Java 21, Spring Boot WebFlux style controllers, WebClient, MyBatis, Flyway, Jackson, React 18, TypeScript, Ant Design, Vitest, JUnit 5, Mockito, WireMock or WebClient mock exchange functions.

---

## Spec Reference

- `docs/product-evolution/specs/p2-019-ai-llm-node-productionization.md`
- Source design: `docs/superpowers/specs/todo/2026-05-30-ai-llm-node-design.md`
- Source plan lineage: `docs/superpowers/plans/todo/2026-05-30-ai-llm-node.md`

## File Structure

**Backend**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmProviderType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiProviderService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiUsageAuditService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUsageAuditDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUsageAuditMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java`

**Frontend**
- Modify: `frontend/src/components/canvas/constants.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Create: `frontend/src/components/config-panel/AiLlmConfigPanel.tsx`
- Create: `frontend/src/services/aiApi.ts`

**Data And Config**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V114__ai_llm_node_productionization.sql`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClientTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiProviderControllerTest.java`
- Create: `frontend/src/components/config-panel/aiLlmConfigPanel.test.tsx`
- Modify: `frontend/src/components/canvas/constants.test.ts`

### Task 1: Data Model And Red Tests

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V114__ai_llm_node_productionization.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AiProviderControllerTest.java`

- [ ] **Step 1: Add additive Flyway migration**

Create the migration with these tables and migration statements:

```sql
CREATE TABLE ai_provider (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    provider_type VARCHAR(30) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    encrypted_api_key VARCHAR(1000) NOT NULL,
    default_model VARCHAR(100) NULL,
    default_params JSON NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_provider_tenant_enabled (tenant_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_prompt_template (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    prompt_template TEXT NOT NULL,
    output_schema JSON NOT NULL,
    default_values JSON NOT NULL,
    recommended_provider_id BIGINT NULL,
    recommended_params JSON NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_template_tenant_category (tenant_id, category, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE ai_usage_audit (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    canvas_id BIGINT NULL,
    execution_id BIGINT NULL,
    node_id VARCHAR(100) NULL,
    provider_id BIGINT NULL,
    template_id BIGINT NULL,
    model VARCHAR(100) NULL,
    status VARCHAR(30) NOT NULL,
    fallback_used TINYINT NOT NULL DEFAULT 0,
    latency_ms BIGINT NULL,
    prompt_tokens INT NULL,
    completion_tokens INT NULL,
    error_code VARCHAR(80) NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ai_audit_execution (tenant_id, execution_id, node_id),
    KEY idx_ai_audit_provider_time (tenant_id, provider_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE canvas_node SET node_type = 'AI_LLM' WHERE node_type = 'AI_NEXT_BEST_ACTION';
DELETE FROM node_type_registry WHERE type_key = 'AI_NEXT_BEST_ACTION';

INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class, config_schema, output_schema, outlet_schema, summary_template, runtime_policy_schema, risk_level, is_trigger, is_terminal, description, enabled)
VALUES
  ('AI_LLM','AI 智能节点','决策增强','org.chovy.canvas.engine.handlers.AiLlmHandler',
   '[{"key":"templateId","label":"场景模板","type":"select","required":true,"dataSource":"/meta/ai-templates"},{"key":"providerId","label":"模型服务","type":"select","required":true,"dataSource":"/meta/ai-providers"},{"key":"model","label":"模型名称","type":"text","required":false},{"key":"temperature","label":"温度","type":"number","required":false,"defaultValue":0.3},{"key":"maxTokens","label":"最大Token","type":"number","required":false,"defaultValue":800},{"key":"timeoutSeconds","label":"超时秒数","type":"number","required":false,"defaultValue":10},{"key":"promptOverride","label":"提示词覆盖","type":"textarea","required":false},{"key":"schemaOverride","label":"输出Schema覆盖","type":"textarea","required":false},{"key":"nextNodeId","label":"下一节点","type":"nodeSelect","required":false}]',
   '[{"fieldKey":"ai_output","fieldName":"AI结构化输出","dataType":"JSON"},{"fieldKey":"aiFallbackUsed","fieldName":"是否使用回退","dataType":"BOOLEAN"},{"fieldKey":"aiTemplateId","fieldName":"模板ID","dataType":"NUMBER"}]',
   '[{"id":"success","label":"成功","color":"#52c41a","targetField":"nextNodeId"}]',
   'AI 智能节点（{{templateId}}）','[]','HIGH',0,0,'调用 LLM 生成结构化 JSON，写入上下文 ai_output。',1)
ON DUPLICATE KEY UPDATE
  type_name = VALUES(type_name),
  category = VALUES(category),
  handler_class = VALUES(handler_class),
  config_schema = VALUES(config_schema),
  output_schema = VALUES(output_schema),
  outlet_schema = VALUES(outlet_schema),
  summary_template = VALUES(summary_template),
  runtime_policy_schema = VALUES(runtime_policy_schema),
  risk_level = VALUES(risk_level),
  is_trigger = VALUES(is_trigger),
  is_terminal = VALUES(is_terminal),
  description = VALUES(description),
  enabled = VALUES(enabled);

INSERT INTO ai_prompt_template
  (tenant_id, name, category, prompt_template, output_schema, default_values, enabled)
VALUES
  (NULL, '文案生成', 'text_generate',
   '根据用户画像 ${userProfile}、渠道 ${channelType}、商品或活动 ${productInfo} 生成触达文案，返回 JSON。',
   '{"type":"object","properties":{"text":{"type":"string"},"subject":{"type":"string"},"tone":{"type":"string"}},"required":["text","tone"]}',
   '{"text":"为你准备了新的专属权益。","tone":"warm"}', 1),
  (NULL, '智能评分', 'scoring',
   '根据行为数据 ${behaviorData} 输出用户评分、分档和原因，返回 JSON。',
   '{"type":"object","properties":{"score":{"type":"number"},"band":{"type":"string"},"reason":{"type":"string"}},"required":["score","band","reason"]}',
   '{"score":50,"band":"medium","reason":"AI不可用，使用中性评分"}', 1),
  (NULL, '智能时机', 'timing',
   '根据活跃数据 ${activityData} 和时区 ${timezone} 推荐最佳触达时间，返回 JSON。',
   '{"type":"object","properties":{"bestTime":{"type":"string"},"confidence":{"type":"number"}},"required":["bestTime","confidence"]}',
   '{"bestTime":"","confidence":0.5}', 1),
  (NULL, '旧AI节点回退', 'legacy_fallback',
   '将旧 AI_NEXT_BEST_ACTION 节点回退配置转换为结构化输出，返回 JSON。',
   '{"type":"object","properties":{"nextBestAction":{"type":"string"},"aiFallbackUsed":{"type":"boolean"}},"required":["nextBestAction","aiFallbackUsed"]}',
   '{"nextBestAction":"continue","aiFallbackUsed":true}', 1);
```

- [ ] **Step 2: Write gateway tests**

In `AiLlmGatewayTest`, cover these test methods:

```java
@Test void callReturnsValidatedJsonWhenProviderSucceeds()
@Test void callReturnsTemplateDefaultsWhenProviderTimesOut()
@Test void callReturnsTemplateDefaultsWhenProviderReturnsInvalidJson()
@Test void callRejectsDisabledProviderBeforeHttpCall()
@Test void callWritesAuditForSuccessAndFallback()
```

- [ ] **Step 3: Write handler tests**

In `AiLlmHandlerTest`, cover prompt variable rendering, `ai_output` context write, `nextNodeId` routing, missing template failure, and fallback defaults.

```java
@Test void handlerWritesAiOutputAndRoutesToNextNode()
@Test void handlerRendersPromptFromExecutionContext()
@Test void handlerUsesFallbackDefaultsWithoutFailingPath()
@Test void handlerFailsClosedWhenTemplateMissing()
@Test void fallbackDefaultsProduceAiOutputWithoutFailingPath()
```

- [ ] **Step 4: Write provider controller tests**

In `AiProviderControllerTest`, cover create, list, detail, update, disable, tenant isolation, and secret masking. Assert response JSON never contains `encryptedApiKey` or the raw API key string.

- [ ] **Step 5: Run backend red tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AiLlmGatewayTest,AiLlmHandlerTest,AiProviderControllerTest
```

Expected: FAIL because the AI domain services, gateway, handler, and controllers do not exist.

### Task 2: Provider, Template, Audit, And Meta APIs

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiUsageAuditDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiUsageAuditMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiProviderService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiPromptTemplateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/AiUsageAuditService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java`

- [ ] **Step 1: Add MyBatis entities and mappers**

Use `@TableName` names matching the migration. `AiProviderDO` must expose `encryptedApiKey` only inside backend domain code. `AiUsageAuditDO.status` values are `SUCCESS`, `FALLBACK`, `TIMEOUT`, `INVALID_JSON`, `PROVIDER_DISABLED`, and `CONFIG_ERROR`.

- [ ] **Step 2: Implement provider service**

`AiProviderService` must provide:

```java
AiProviderDTO create(Long tenantId, AiProviderCreateReq req);
List<AiProviderDTO> list(Long tenantId);
AiProviderSecretView getEnabledForCall(Long tenantId, Long providerId);
AiProviderDTO update(Long tenantId, Long providerId, AiProviderUpdateReq req);
void disable(Long tenantId, Long providerId);
```

Store provider secrets encrypted at rest. If no shared secret service exists, create a small AES-GCM helper backed by a required `CANVAS_AI_SECRET_KEY` environment/config value, and isolate `maskSecret`, `encryptSecret`, and `decryptSecret` in `AiProviderService` so a later KMS/SM4 implementation is one-file scoped.

- [ ] **Step 3: Implement template service**

`AiPromptTemplateService` must list enabled templates for `/meta/ai-templates`, load template details for execution, and reject disabled or cross-tenant templates. Built-in templates use `tenant_id IS NULL`; tenant templates use the current tenant id.

- [ ] **Step 4: Implement controllers and meta endpoints**

Add:

```text
GET    /ai/providers
POST   /ai/providers
PUT    /ai/providers/{id}
POST   /ai/providers/{id}/disable
GET    /ai/prompt-templates
POST   /ai/prompt-templates
PUT    /ai/prompt-templates/{id}
POST   /ai/prompt-templates/{id}/disable
GET    /meta/ai-providers
GET    /meta/ai-templates
```

All provider responses return `secretMasked=true` and no raw secret fields.

- [ ] **Step 5: Run provider tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AiProviderControllerTest
```

Expected: PASS for tenant scoping, CRUD, disable, and secret masking.

### Task 3: LLM Gateway And Handler

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmProviderType.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiCompatibleLlmClientTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java`

- [ ] **Step 1: Add node type constant safely**

Add `public static final String AI_LLM = "AI_LLM";`. Do not re-add `AI_NEXT_BEST_ACTION` to the public source catalog; database migration rewrites old registry and canvas rows when they exist.

- [ ] **Step 2: Implement LLM client contract**

Create:

```java
public interface LlmClient {
    boolean supports(LlmProviderType type);
    Mono<Map<String, Object>> chat(AiProviderSecretView provider, AiLlmGateway.Request request);
}
```

`OpenAiCompatibleLlmClient` supports `OPENAI_COMPATIBLE` and `CUSTOM_OPENAI_COMPATIBLE`, calls `/chat/completions`, sends `response_format` when configured, and parses `choices[0].message.content` as JSON.

- [ ] **Step 3: Implement gateway**

`AiLlmGateway.call` accepts tenant id, provider id, template id, prompt, schema JSON, defaults JSON, model, params, and audit context. It must cap timeout to `canvas.ai.max-timeout-seconds`, retry at most once by default, return defaults on timeout/provider/JSON failures, and write one audit row.

- [ ] **Step 4: Implement handler**

`AiLlmHandler` loads template, renders `${key}` variables from `ExecutionContext`, calls the gateway, and returns:

```java
NodeResult.ok(nextNodeId, Map.of(
    "ai_output", gatewayResult.output(),
    "aiFallbackUsed", gatewayResult.fallbackUsed(),
    "aiTemplateId", templateId
));
```

If no template exists and no inline `schemaOverride` plus defaults exist, return `NodeResult.fail("AI_LLM: template missing or disabled")`.

- [ ] **Step 5: Run gateway and handler tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AiLlmGatewayTest,OpenAiCompatibleLlmClientTest,AiLlmHandlerTest
```

Expected: PASS for structured JSON, fallback, audit, disabled provider, invalid JSON, and handler routing.

### Task 4: Frontend Config Panel

**Files:**
- Modify: `frontend/src/components/canvas/constants.ts`
- Modify: `frontend/src/components/canvas/constants.test.ts`
- Modify: `frontend/src/components/config-panel/index.tsx`
- Create: `frontend/src/components/config-panel/AiLlmConfigPanel.tsx`
- Create: `frontend/src/components/config-panel/aiLlmConfigPanel.test.tsx`
- Create: `frontend/src/services/aiApi.ts`

- [ ] **Step 1: Write frontend tests**

`aiLlmConfigPanel.test.tsx` covers provider loading, template loading, disabled provider warning, prompt override edit, schema override edit, and saved config shape:

```ts
expect(onChange).toHaveBeenCalledWith(expect.objectContaining({
  templateId: 10,
  providerId: 20,
  model: 'deepseek-chat',
  nextNodeId: 'if_1'
}))
```

- [ ] **Step 2: Add typed API wrapper**

`aiApi.ts` exports `listAiProvidersForMeta`, `listAiTemplatesForMeta`, `listAiProviders`, `saveAiProvider`, `disableAiProvider`, `listAiPromptTemplates`, `saveAiPromptTemplate`, and `disableAiPromptTemplate`.

- [ ] **Step 3: Register display constants**

Update node display names so `AI_LLM` renders as `AI 智能节点`. Keep any test fixture that mentions `AI_NEXT_BEST_ACTION` explicitly marked as legacy.

- [ ] **Step 4: Add config panel**

`AiLlmConfigPanel` renders provider select, template select, model input, temperature/max token/timeout numeric inputs, prompt override textarea, schema override textarea, and next-node selector using existing config-panel control chrome.

- [ ] **Step 5: Run frontend focused tests**

Run:

```bash
cd frontend && npm test -- aiLlmConfigPanel.test.tsx constants.test.ts
```

Expected: PASS for the AI panel and node constants.

### Task 5: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-019-ai-llm-node-productionization.md`
- Modify: `docs/product-evolution/plans/p2-019-ai-llm-node-productionization-plan.md`

- [ ] **Step 1: Run focused backend verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=AiLlmGatewayTest,OpenAiCompatibleLlmClientTest,AiLlmHandlerTest,AiProviderControllerTest
```

Expected: PASS.

- [ ] **Step 2: Run focused frontend verification**

Run:

```bash
cd frontend && npm test -- aiLlmConfigPanel.test.tsx constants.test.ts
```

Expected: PASS.

- [ ] **Step 3: Run affected regression**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=IfConditionHandlerTest,SelectorHandlerTest,ExecutionControllerTest
cd frontend && npm test -- --run
```

Expected: PASS or record unrelated failures with exact test names and reproduction commands.

- [ ] **Step 4: Commit implementation slice**

Run:

```bash
git add backend/canvas-engine/src frontend/src docs/product-evolution/specs docs/product-evolution/plans
git commit -m "feat: productionize AI LLM canvas node"
```

Expected: commit contains only AI_LLM provider, template, audit, handler, frontend config, spec, and plan changes.
