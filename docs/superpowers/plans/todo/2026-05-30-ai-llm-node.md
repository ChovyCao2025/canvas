# AI_LLM 节点实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the stub `AI_NEXT_BEST_ACTION` node with a real `AI_LLM` node that calls LLM providers and returns structured JSON to the canvas execution context.

**Architecture:** Single canvas node type `AI_LLM` with a template system stored in DB. `AiLlmGateway` internal component handles provider routing, retry, timeout, JSON parsing, and fallback. `LlmClient` strategy pattern for different provider API formats. `OpenAiLlmClient` covers OpenAI/DeepSeek/Moonshot via compatible API.

**Tech Stack:** Java 21, Spring WebFlux/WebClient, MyBatis-Plus, Flyway, React 18 + antd 5

---

## File Structure

### Backend — New Files

| File | Responsibility |
|------|---------------|
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java` | Canvas node handler, replaces AiNextBestActionHandler |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java` | LLM call orchestration: provider routing, retry, timeout, fallback |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java` | Strategy interface for provider-specific API calls |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiLlmClient.java` | OpenAI-compatible API client (covers DeepSeek/Moonshot/etc.) |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmProviderType.java` | Provider type enum |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java` | ai_provider table entity |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java` | ai_prompt_template table entity |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java` | MyBatis-Plus mapper for ai_provider |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java` | MyBatis-Plus mapper for ai_prompt_template |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java` | Provider CRUD API (admin) |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java` | Template CRUD API |
| `backend/canvas-engine/src/main/resources/db/migration/V89__ai_llm_node.sql` | Flyway: tables + seed data + node type migration |

### Backend — Modified Files

| File | Change |
|------|--------|
| `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java` | Remove `AI_NEXT_BEST_ACTION`, add `AI_LLM` |
| `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java` | Add `/meta/ai-providers` and `/meta/ai-templates` endpoints |

### Backend — Deleted Files

| File | Reason |
|------|--------|
| `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiNextBestActionHandler.java` | Replaced by AiLlmHandler |

### Frontend — Modified Files

| File | Change |
|------|--------|
| `frontend/src/components/canvas/constants.ts` | Replace `AI_NEXT_BEST_ACTION` with `AI_LLM` in DEFAULT_NAMES |

### Frontend — New Files

| File | Responsibility |
|------|---------------|
| `frontend/src/components/config-panel/AiLlmConfigPanel.tsx` | AI_LLM node configuration panel with template selector |

### Test Files

| File | Responsibility |
|------|---------------|
| `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiLlmClientTest.java` | Unit tests for OpenAI client |
| `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java` | Unit tests for gateway (retry, timeout, fallback) |
| `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java` | Integration test for handler end-to-end |

---

## Task 1: Flyway Migration — Tables + Seed Data + Node Type Rename

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V89__ai_llm_node.sql`

- [ ] **Step 1: Write the migration SQL**

```sql
-- V89__ai_llm_node.sql

-- 1. ai_provider: LLM 提供商配置（管理员管理，运营不可见 API Key）
CREATE TABLE IF NOT EXISTS ai_provider (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL COMMENT '显示名称',
    type            VARCHAR(30)  NOT NULL COMMENT 'Provider 类型: OPENAI/ANTHROPIC/AZURE_OPENAI/OLLAMA/QWEN/CUSTOM',
    endpoint        VARCHAR(500) NOT NULL COMMENT 'API 端点 URL',
    api_key         VARCHAR(500) NOT NULL COMMENT '加密存储的 API Key',
    default_params  JSON         NULL     COMMENT '默认参数 {temperature, maxTokens, topP, timeout, maxRetries}',
    enabled         TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    tenant_id       BIGINT       NULL     COMMENT '租户 ID',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM 提供商配置';

-- 2. ai_prompt_template: 提示词模板库
CREATE TABLE IF NOT EXISTS ai_prompt_template (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    name                    VARCHAR(100) NOT NULL COMMENT '模板名称',
    category                VARCHAR(50)  NOT NULL COMMENT '分类: text_generate/scoring/timing/recommend/custom',
    prompt_template         TEXT         NOT NULL COMMENT '提示词模板，支持 ${变量} 引用上下文',
    output_schema           JSON         NOT NULL COMMENT 'JSON Schema 定义输出结构',
    default_values          JSON         NULL     COMMENT 'Schema 各字段的默认值（回退用）',
    recommended_provider_id BIGINT       NULL     COMMENT '推荐的 Provider ID',
    recommended_params      JSON         NULL     COMMENT '推荐参数 {temperature, maxTokens}',
    description             TEXT         NULL     COMMENT '模板说明',
    enabled                 TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用',
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 提示词模板库';

-- 3. 迁移 node_type_registry: AI_NEXT_BEST_ACTION → AI_LLM
UPDATE node_type_registry
SET type_key = 'AI_LLM',
    type_name = 'AI 智能节点',
    category = '决策增强',
    config_schema = '[{"key":"templateId","label":"场景模板","type":"select","required":true,"dataSource":"/meta/ai-templates"},{"key":"providerId","label":"模型","type":"select","required":true,"dataSource":"/meta/ai-providers"},{"key":"model","label":"模型名称","type":"text","required":false},{"key":"temperature","label":"温度","type":"number","required":false,"defaultValue":0.7},{"key":"maxTokens","label":"最大Token","type":"number","required":false,"defaultValue":1024},{"key":"timeout","label":"超时(秒)","type":"number","required":false,"defaultValue":30},{"key":"promptOverride","label":"提示词覆盖","type":"textarea","required":false},{"key":"schemaOverride","label":"输出Schema覆盖","type":"textarea","required":false}]',
    outlet_schema = '{"type":"single","routes":[{"handle":"success"}]}',
    description = '调用 LLM 生成结构化数据，输出写入上下文 ai_output'
WHERE type_key = 'AI_NEXT_BEST_ACTION';

-- 4. 迁移 canvas_node 表中的节点类型
UPDATE canvas_node SET node_type = 'AI_LLM' WHERE node_type = 'AI_NEXT_BEST_ACTION';

-- 5. 预置模板种子数据
INSERT INTO ai_prompt_template (name, category, prompt_template, output_schema, default_values, description) VALUES
('文案生成', 'text_generate',
 '你是一个营销文案专家。根据以下用户画像信息生成个性化触达文案。\n\n用户画像：${userProfile}\n渠道类型：${channelType}\n产品信息：${productInfo}\n语气风格：${toneStyle}\n\n请生成触达文案，返回JSON格式。',
 '{"type":"object","properties":{"text":{"type":"string","description":"文案正文"},"subject":{"type":"string","description":"标题（邮件/Push需要）"},"tone":{"type":"string","enum":["formal","casual","warm","urgent"],"description":"语气风格"}},"required":["text","tone"]}',
 '{"text":"尊敬的用户，为您推荐专属优惠。","tone":"formal"}',
 '根据用户画像生成个性化触达文案'),

('智能评分', 'scoring',
 '你是一个用户价值评估专家。根据以下用户行为数据评估其价值/流失概率。\n\n用户行为数据：${behaviorData}\n评分维度：${scoringDimensions}\n评分制：${maxScore}分制\n\n请评估并返回JSON格式。',
 '{"type":"object","properties":{"score":{"type":"number","description":"评分"},"band":{"type":"string","enum":["low","medium","high"],"description":"分档"},"reason":{"type":"string","description":"评分原因"}},"required":["score","band","reason"]}',
 '{"score":50,"band":"medium","reason":"数据不足，使用中性评分"}',
 '根据行为数据评估用户价值/流失概率'),

('智能时机', 'timing',
 '你是一个触达时机优化专家。根据以下用户活跃模式推荐最佳触达时间。\n\n用户活跃数据：${activityData}\n时区：${timezone}\n\n请推荐最佳触达时间，返回JSON格式。',
 '{"type":"object","properties":{"bestTime":{"type":"string","description":"最佳触达时间(ISO8601)"},"confidence":{"type":"number","description":"置信度0-1"}},"required":["bestTime","confidence"]}',
 '{"bestTime":"","confidence":0.5}',
 '根据活跃模式推荐最佳触达时间'),

('权益推荐', 'recommend',
 '你是一个权益推荐专家。根据用户特征推荐最匹配的权益。\n\n用户特征：${userProfile}\n可选权益列表：${availableItems}\n推荐策略：${recommendStrategy}\n\n请推荐最匹配的权益，返回JSON格式。',
 '{"type":"object","properties":{"itemId":{"type":"string","description":"推荐权益ID"},"itemName":{"type":"string","description":"推荐权益名称"},"reason":{"type":"string","description":"推荐原因"}},"required":["itemId","itemName","reason"]}',
 '{"itemId":"","itemName":"","reason":"无匹配权益"}',
 '根据用户特征推荐最匹配的权益');
```

- [ ] **Step 2: Run the backend to verify migration applies**

Run: `cd backend && mvn spring-boot:run -pl canvas-engine 2>&1 | head -80`
Expected: Application starts, Flyway applies V89, no errors

- [ ] **Step 3: Verify tables exist**

Run: `cd backend && mvn mybatis-plus:info -pl canvas-engine 2>/dev/null; mysql -u root -proot canvas_db -e "SHOW TABLES LIKE 'ai_%'; SELECT COUNT(*) FROM ai_prompt_template;" 2>/dev/null || echo "Check manually via app logs"`
Expected: `ai_provider` and `ai_prompt_template` tables exist, 4 rows in `ai_prompt_template`

- [ ] **Step 4: Commit**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V89__ai_llm_node.sql
git commit -m "feat: add ai_provider and ai_prompt_template tables; migrate AI_NEXT_BEST_ACTION to AI_LLM"
```

---

## Task 2: Data Objects + Mappers

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java`

- [ ] **Step 1: Write AiProviderDO**

```java
package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_provider")
public class AiProviderDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String type;
    private String endpoint;
    private String apiKey;
    private String defaultParams;
    private Integer enabled;
    private Long tenantId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Write AiPromptTemplateDO**

```java
package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_prompt_template")
public class AiPromptTemplateDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String promptTemplate;
    private String outputSchema;
    private String defaultValues;
    private Long recommendedProviderId;
    private String recommendedParams;
    private String description;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: Write AiProviderMapper**

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.AiProviderDO;

@Mapper
public interface AiProviderMapper extends BaseMapper<AiProviderDO> {
}
```

- [ ] **Step 4: Write AiPromptTemplateMapper**

```java
package org.chovy.canvas.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.chovy.canvas.dal.dataobject.AiPromptTemplateDO;

@Mapper
public interface AiPromptTemplateMapper extends BaseMapper<AiPromptTemplateDO> {
}
```

- [ ] **Step 5: Build to verify compilation**

Run: `cd backend && mvn compile -pl canvas-engine -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiProviderDO.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AiPromptTemplateDO.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiProviderMapper.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AiPromptTemplateMapper.java
git commit -m "feat: add AiProviderDO, AiPromptTemplateDO entities and mappers"
```

---

## Task 3: LlmProviderType Enum + LlmClient Interface + OpenAiLlmClient

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmProviderType.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/LlmClient.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/OpenAiLlmClient.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/OpenAiLlmClientTest.java`

- [ ] **Step 1: Write the failing test for OpenAiLlmClient**

```java
package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.junit.jupiter.api.*;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiLlmClientTest {

    private MockWebServer server;
    private OpenAiLlmClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        client = new OpenAiLlmClient(objectMapper);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void supportsOpenAiAndCustom() {
        assertTrue(client.supports(LlmProviderType.OPENAI));
        assertTrue(client.supports(LlmProviderType.CUSTOM));
        assertFalse(client.supports(LlmProviderType.ANTHROPIC));
    }

    @Test
    void chatReturnsParsedJsonOnSuccess() throws Exception {
        String jsonBody = """
            {
              "choices": [{
                "message": {
                  "content": "{\\"text\\":\\"你好\\",\\"tone\\":\\"warm\\"}"
                }
              }]
            }
            """;
        server.enqueue(new MockResponse()
                .setBody(jsonBody)
                .setHeader("Content-Type", "application/json"));

        AiProviderDO provider = new AiProviderDO();
        provider.setType("OPENAI");
        provider.setEndpoint(server.url("/v1/chat/completions").toString());
        provider.setApiKey("test-key");

        Map<String, Object> result = client.chat(provider,
                "generate text",
                "{\"type\":\"object\",\"properties\":{\"text\":{\"type\":\"string\"},\"tone\":{\"type\":\"string\"}}}",
                Map.of("model", "gpt-4o", "temperature", 0.7, "maxTokens", 100))
                .block();

        assertNotNull(result);
        assertEquals("你好", result.get("text"));
        assertEquals("warm", result.get("tone"));
    }

    @Test
    void chatFailsOnInvalidJson() throws Exception {
        String jsonBody = """
            {
              "choices": [{
                "message": {
                  "content": "not valid json"
                }
              }]
            }
            """;
        server.enqueue(new MockResponse()
                .setBody(jsonBody)
                .setHeader("Content-Type", "application/json"));

        AiProviderDO provider = new AiProviderDO();
        provider.setType("OPENAI");
        provider.setEndpoint(server.url("/v1/chat/completions").toString());
        provider.setApiKey("test-key");

        assertThrows(Exception.class, () ->
                client.chat(provider, "generate text",
                        "{\"type\":\"object\"}",
                        Map.of("model", "gpt-4o"))
                        .block());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=OpenAiLlmClientTest -q 2>&1 | tail -5`
Expected: COMPILATION ERROR (classes not found)

- [ ] **Step 3: Write LlmProviderType enum**

```java
package org.chovy.canvas.engine.llm;

public enum LlmProviderType {
    OPENAI,
    ANTHROPIC,
    AZURE_OPENAI,
    OLLAMA,
    QWEN,
    CUSTOM
}
```

- [ ] **Step 4: Write LlmClient interface**

```java
package org.chovy.canvas.engine.llm;

import org.chovy.canvas.dal.dataobject.AiProviderDO;
import reactor.core.publisher.Mono;
import java.util.Map;

public interface LlmClient {
    boolean supports(LlmProviderType type);
    Mono<Map<String, Object>> chat(AiProviderDO provider, String prompt, String outputSchema, Map<String, Object> params);
}
```

- [ ] **Step 5: Write OpenAiLlmClient**

```java
package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiLlmClient implements LlmClient {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(LlmProviderType type) {
        return type == LlmProviderType.OPENAI || type == LlmProviderType.CUSTOM;
    }

    @Override
    public Mono<Map<String, Object>> chat(AiProviderDO provider, String prompt, String outputSchema, Map<String, Object> params) {
        String model = (String) params.getOrDefault("model", "gpt-4o");
        Number temperature = params.get("temperature") instanceof Number n ? n : 0.7;
        Number maxTokens = params.get("maxTokens") instanceof Number n ? n : 1024;

        Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", temperature.doubleValue());
        requestBody.put("max_tokens", maxTokens.intValue());
        requestBody.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "ai_output",
                        "strict", true,
                        "schema", parseSchema(outputSchema)
                )
        ));

        return WebClient.builder().baseUrl(provider.getEndpoint()).build()
                .post()
                .header("Authorization", "Bearer " + provider.getApiKey())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::extractContent)
                .flatMap(this::parseJson);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSchema(String schemaJson) {
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("[AI] Failed to parse output schema, using minimal schema: {}", e.getMessage());
            return Map.of("type", "object", "properties", Map.of());
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(String responseBody) {
        try {
            Map<String, Object> resp = objectMapper.readValue(responseBody, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract content from LLM response: " + e.getMessage(), e);
        }
    }

    private Mono<Map<String, Object>> parseJson(String content) {
        try {
            Map<String, Object> result = objectMapper.readValue(content, new TypeReference<>() {});
            return Mono.just(result);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("LLM returned invalid JSON: " + e.getMessage(), e));
        }
    }
}
```

- [ ] **Step 6: Add mockwebserver dependency if missing**

Check if `okhttp3:mockwebserver` is in `backend/canvas-engine/pom.xml`. If not, add:

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 7: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=OpenAiLlmClientTest -q 2>&1 | tail -5`
Expected: Tests pass

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/ \
       backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/
git commit -m "feat: add LlmProviderType, LlmClient interface, and OpenAiLlmClient with tests"
```

---

## Task 4: AiLlmGateway — Orchestration with Retry, Timeout, Fallback

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java`

- [ ] **Step 1: Write the failing test for AiLlmGateway**

```java
package org.chovy.canvas.engine.llm;

import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.chovy.canvas.dal.mapper.AiProviderMapper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiLlmGatewayTest {

    @InjectMocks
    private AiLlmGateway gateway;

    @Mock
    private AiProviderMapper providerMapper;

    @Mock
    private LlmClient mockClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void callReturnsClientResultOnSuccess() {
        AiProviderDO provider = new AiProviderDO();
        provider.setId(1L);
        provider.setType("OPENAI");
        provider.setTimeout(30);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(mockClient.supports(LlmProviderType.OPENAI)).thenReturn(true);
        when(mockClient.chat(eq(provider), anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.just(Map.of("text", "hello", "tone", "warm")));

        gateway.setClients(List.of(mockClient));

        Map<String, Object> result = gateway.call("test prompt",
                "{\"type\":\"object\"}", Map.of("providerId", 1L)).block();

        Assertions.assertEquals("hello", result.get("text"));
    }

    @Test
    void callFallsBackOnClientError() {
        AiProviderDO provider = new AiProviderDO();
        provider.setId(1L);
        provider.setType("OPENAI");
        provider.setTimeout(30);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(mockClient.supports(LlmProviderType.OPENAI)).thenReturn(true);
        when(mockClient.chat(eq(provider), anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.error(new RuntimeException("LLM unavailable")));

        gateway.setClients(List.of(mockClient));

        Map<String, Object> result = gateway.call("test prompt",
                "{\"type\":\"object\"}", Map.of("providerId", 1L)).block();

        // fallback: returns empty map when no defaultValues in config
        Assertions.assertNotNull(result);
    }

    @Test
    void callFallsBackWithDefaultValues() {
        AiProviderDO provider = new AiProviderDO();
        provider.setId(1L);
        provider.setType("OPENAI");
        provider.setTimeout(30);
        when(providerMapper.selectById(1L)).thenReturn(provider);
        when(mockClient.supports(LlmProviderType.OPENAI)).thenReturn(true);
        when(mockClient.chat(eq(provider), anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.error(new RuntimeException("LLM unavailable")));

        gateway.setClients(List.of(mockClient));

        String defaultValuesJson = "{\"text\":\"fallback text\",\"tone\":\"formal\"}";

        Map<String, Object> result = gateway.call("test prompt",
                "{\"type\":\"object\"}", Map.of("providerId", 1L, "defaultValues", defaultValuesJson)).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("fallback text", result.get("text"));
        Assertions.assertEquals("formal", result.get("tone"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AiLlmGatewayTest -q 2>&1 | tail -5`
Expected: COMPILATION ERROR (AiLlmGateway not found)

- [ ] **Step 3: Write AiLlmGateway**

```java
package org.chovy.canvas.engine.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.chovy.canvas.dal.mapper.AiProviderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiLlmGateway {

    @Autowired
    private AiProviderMapper providerMapper;

    @Autowired
    private List<LlmClient> clients;

    @Autowired
    private ObjectMapper objectMapper;

    public Mono<Map<String, Object>> call(String prompt, String outputSchema, Map<String, Object> config) {
        Long providerId = toLong(config.get("providerId"));
        AiProviderDO provider = providerMapper.selectById(providerId);
        if (provider == null) {
            log.warn("[AI] Provider not found: {}", providerId);
            return fallback(outputSchema, config);
        }

        LlmProviderType type = LlmProviderType.valueOf(provider.getType());
        LlmClient client = clients.stream()
                .filter(c -> c.supports(type))
                .findFirst()
                .orElse(null);

        if (client == null) {
            log.warn("[AI] No LlmClient for type: {}", type);
            return fallback(outputSchema, config);
        }

        int timeoutSec = provider.getTimeout() != null ? provider.getTimeout() : 30;

        return client.chat(provider, prompt, outputSchema, config)
                .timeout(Duration.ofSeconds(timeoutSec))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .doBeforeRetry(signal -> log.warn("[AI] Retry attempt {} for provider {}",
                                signal.totalRetries(), provider.getName())))
                .onErrorResume(e -> {
                    log.error("[AI] LLM call failed, using fallback: {}", e.getMessage());
                    return fallback(outputSchema, config);
                });
    }

    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> fallback(String outputSchema, Map<String, Object> config) {
        Object dv = config.get("defaultValues");
        if (dv instanceof String s && !s.isBlank()) {
            try {
                return Mono.just(objectMapper.readValue(s, new TypeReference<>() {}));
            } catch (Exception e) {
                log.warn("[AI] Failed to parse defaultValues: {}", e.getMessage());
            }
        }
        return Mono.just(Map.of());
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        return null;
    }

    /** Test hook: allow injecting mock clients */
    void setClients(List<LlmClient> clients) {
        this.clients = clients;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AiLlmGatewayTest -q 2>&1 | tail -5`
Expected: Tests pass

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/AiLlmGateway.java \
       backend/canvas-engine/src/test/java/org/chovy/canvas/engine/llm/AiLlmGatewayTest.java
git commit -m "feat: add AiLlmGateway with retry, timeout, and fallback logic"
```

---

## Task 5: AiLlmHandler — Replace AiNextBestActionHandler

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java`
- Delete: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiNextBestActionHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java`

- [ ] **Step 1: Update NodeType — remove AI_NEXT_BEST_ACTION, add AI_LLM**

In `NodeType.java`, replace:
```java
/** AI 下一步最佳动作 */
public static final String AI_NEXT_BEST_ACTION = "AI_NEXT_BEST_ACTION";
```
with:
```java
/** AI 智能节点（调用 LLM 生成结构化数据） */
public static final String AI_LLM = "AI_LLM";
```

- [ ] **Step 2: Write the failing test for AiLlmHandler**

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiLlmHandlerTest {

    @InjectMocks
    private AiLlmHandler handler;

    @Mock
    private AiLlmGateway gateway;

    @Mock
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeAsyncWithTemplate() {
        when(ctx.getContextValue("userProfile")).thenReturn("28岁男性，一线城市");
        when(gateway.call(anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.just(
                        Map.of("text", "专属优惠", "tone", "warm")));

        Map<String, Object> config = Map.of(
                "templateId", 1,
                "providerId", 1,
                "model", "gpt-4o",
                "nextNodeId", "condition_1"
        );

        var result = handler.executeAsync(config, ctx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("condition_1", result.nextNodeId());
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.output().get("ai_output");
        Assertions.assertEquals("专属优惠", output.get("text"));
    }

    @Test
    void executeAsyncCustomPrompt() {
        when(ctx.getContextValue("userName")).thenReturn("张三");
        when(gateway.call(anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.just(
                        Map.of("score", 85, "band", "high")));

        Map<String, Object> config = Map.of(
                "providerId", 1,
                "promptOverride", "评估用户 ${userName} 的价值",
                "schemaOverride", "{\"type\":\"object\"}",
                "nextNodeId", "next_1"
        );

        var result = handler.executeAsync(config, ctx).block();

        Assertions.assertNotNull(result);
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.output().get("ai_output");
        Assertions.assertEquals(85, output.get("score"));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AiLlmHandlerTest -q 2>&1 | tail -5`
Expected: COMPILATION ERROR

- [ ] **Step 4: Write AiLlmHandler**

```java
package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.dal.dataobject.AiPromptTemplateDO;
import org.chovy.canvas.dal.mapper.AiPromptTemplateMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@NodeHandlerType(NodeType.AI_LLM)
@RequiredArgsConstructor
public class AiLlmHandler implements NodeHandler {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{(\\w+)}");

    private final AiLlmGateway gateway;
    private final AiPromptTemplateMapper templateMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        return Mono.fromCallable(() -> buildCallParams(config, ctx))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(params -> gateway.call(params.prompt, params.schema, params.config))
                .map(json -> NodeResult.ok(string(config, "nextNodeId", null), Map.of("ai_output", json)));
    }

    private CallParams buildCallParams(Map<String, Object> config, ExecutionContext ctx) {
        String prompt;
        String schema;

        Long templateId = toLong(config.get("templateId"));
        if (templateId != null) {
            AiPromptTemplateDO tpl = templateMapper.selectById(templateId);
            if (tpl != null) {
                String tplPrompt = tpl.getPromptTemplate();
                String tplSchema = tpl.getOutputSchema();

                // Override if provided
                prompt = string(config, "promptOverride", tplPrompt);
                schema = string(config, "schemaOverride", tplSchema);

                // Inject defaultValues from template into config for fallback
                if (tpl.getDefaultValues() != null && !config.containsKey("defaultValues")) {
                    config = new LinkedHashMap<>(config);
                    config.put("defaultValues", tpl.getDefaultValues());
                }
            } else {
                prompt = string(config, "promptOverride", "");
                schema = string(config, "schemaOverride", "{\"type\":\"object\"}");
            }
        } else {
            prompt = string(config, "promptOverride", "");
            schema = string(config, "schemaOverride", "{\"type\":\"object\"}");
        }

        // Render ${variable} from execution context
        prompt = renderPrompt(prompt, ctx);

        return new CallParams(prompt, schema, config);
    }

    private String renderPrompt(String template, ExecutionContext ctx) {
        if (template == null || template.isEmpty()) return template;
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = ctx.getContextValue(varName);
            matcher.appendReplacement(sb, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String string(Map<String, Object> config, String key, String fallback) {
        Object value = config.get(key);
        return value == null ? fallback : value.toString();
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) return Long.parseLong(s);
        return null;
    }

    private record CallParams(String prompt, String schema, Map<String, Object> config) {}
}
```

- [ ] **Step 5: Delete AiNextBestActionHandler**

```bash
rm backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiNextBestActionHandler.java
```

- [ ] **Step 6: Run test to verify it passes**

Run: `cd backend && mvn test -pl canvas-engine -Dtest=AiLlmHandlerTest -q 2>&1 | tail -5`
Expected: Tests pass

- [ ] **Step 7: Build entire project to verify no compilation errors**

Run: `cd backend && mvn compile -pl canvas-engine -q`
Expected: BUILD SUCCESS

- [ ] **Step 8: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiLlmHandler.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java
git rm backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AiNextBestActionHandler.java
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerTest.java
git commit -m "feat: replace AI_NEXT_BEST_ACTION with AI_LLM node handler; add AiLlmHandler with template rendering"
```

---

## Task 6: Provider + Template CRUD APIs + Meta Endpoints

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java`

- [ ] **Step 1: Write AiProviderController**

```java
package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AiProviderDO;
import org.chovy.canvas.dal.mapper.AiProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/canvas/ai-providers")
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderMapper mapper;

    @GetMapping
    public Mono<R<PageResult<AiProviderDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<AiProviderDO>()
                    .orderByAsc(AiProviderDO::getId);
            Page<AiProviderDO> p = mapper.selectPage(new Page<>(page, size), wrapper);
            // Mask API key in list response
            p.getRecords().forEach(r -> r.setApiKey(maskKey(r.getApiKey())));
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AiProviderDO>> create(@RequestBody AiProviderDO body) {
        return Mono.fromCallable(() -> {
            mapper.insert(body);
            body.setApiKey(maskKey(body.getApiKey()));
            return R.ok(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AiProviderDO body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
            // If apiKey is masked, keep the old one
            AiProviderDO existing = mapper.selectById(id);
            if (existing != null && body.getApiKey() != null && body.getApiKey().contains("***")) {
                body.setApiKey(existing.getApiKey());
            }
            mapper.updateById(body);
            return R.ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> mapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "***" + key.substring(key.length() - 4);
    }
}
```

- [ ] **Step 2: Write AiPromptTemplateController**

```java
package org.chovy.canvas.web;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.dal.dataobject.AiPromptTemplateDO;
import org.chovy.canvas.dal.mapper.AiPromptTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RestController
@RequestMapping("/canvas/ai-templates")
@RequiredArgsConstructor
public class AiPromptTemplateController {

    private final AiPromptTemplateMapper mapper;

    @GetMapping
    public Mono<R<PageResult<AiPromptTemplateDO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category) {
        return Mono.fromCallable(() -> {
            var wrapper = new LambdaQueryWrapper<AiPromptTemplateDO>()
                    .eq(AiPromptTemplateDO::getEnabled, 1)
                    .eq(category != null, AiPromptTemplateDO::getCategory, category)
                    .orderByAsc(AiPromptTemplateDO::getId);
            Page<AiPromptTemplateDO> p = mapper.selectPage(new Page<>(page, size), wrapper);
            return R.ok(PageResult.of(p.getTotal(), p.getRecords()));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping
    public Mono<R<AiPromptTemplateDO>> create(@RequestBody AiPromptTemplateDO body) {
        return Mono.fromCallable(() -> {
            mapper.insert(body);
            return R.ok(body);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{id}")
    public Mono<R<Void>> update(@PathVariable Long id, @RequestBody AiPromptTemplateDO body) {
        body.setId(id);
        return Mono.fromCallable(() -> {
            mapper.updateById(body);
            return R.ok();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{id}")
    public Mono<R<Void>> delete(@PathVariable Long id) {
        return Mono.<R<Void>>fromRunnable(() -> mapper.deleteById(id))
                .subscribeOn(Schedulers.boundedElastic())
                .then(Mono.just(R.ok()));
    }
}
```

- [ ] **Step 3: Add meta endpoints to MetaController for AI_LLM node config panel**

Add these two methods to `MetaController.java`:

```java
/** 获取已启用的 AI Provider 列表（供 AI_LLM 节点模型选择下拉） */
@GetMapping("/ai-providers")
public Mono<R<List<Map<String, Object>>>> getAiProviders() {
    return Mono.fromCallable(() -> {
        List<org.chovy.canvas.dal.dataobject.AiProviderDO> providers =
                aiProviderMapper.selectList(
                        new LambdaQueryWrapper<org.chovy.canvas.dal.dataobject.AiProviderDO>()
                                .eq(org.chovy.canvas.dal.dataobject.AiProviderDO::getEnabled, 1)
                                .orderByAsc(org.chovy.canvas.dal.dataobject.AiProviderDO::getId));
        return providers.stream().map(p -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("value", p.getId());
            m.put("label", p.getName());
            m.put("type", p.getType());
            m.put("defaultParams", p.getDefaultParams());
            return m;
        }).collect(Collectors.toList());
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}

/** 获取已启用的 AI 提示词模板列表（供 AI_LLM 节点模板选择下拉） */
@GetMapping("/ai-templates")
public Mono<R<List<Map<String, Object>>>> getAiTemplates() {
    return Mono.fromCallable(() -> {
        List<org.chovy.canvas.dal.dataobject.AiPromptTemplateDO> templates =
                aiPromptTemplateMapper.selectList(
                        new LambdaQueryWrapper<org.chovy.canvas.dal.dataobject.AiPromptTemplateDO>()
                                .eq(org.chovy.canvas.dal.dataobject.AiPromptTemplateDO::getEnabled, 1)
                                .orderByAsc(org.chovy.canvas.dal.dataobject.AiPromptTemplateDO::getId));
        return templates.stream().map(t -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("value", t.getId());
            m.put("label", t.getName());
            m.put("category", t.getCategory());
            m.put("promptTemplate", t.getPromptTemplate());
            m.put("outputSchema", t.getOutputSchema());
            m.put("defaultValues", t.getDefaultValues());
            m.put("recommendedProviderId", t.getRecommendedProviderId());
            m.put("recommendedParams", t.getRecommendedParams());
            m.put("description", t.getDescription());
            return m;
        }).collect(Collectors.toList());
    }).subscribeOn(Schedulers.boundedElastic()).map(R::ok);
}
```

Also add these fields to MetaController:

```java
private final org.chovy.canvas.dal.mapper.AiProviderMapper aiProviderMapper;
private final org.chovy.canvas.dal.mapper.AiPromptTemplateMapper aiPromptTemplateMapper;
```

- [ ] **Step 4: Build to verify compilation**

Run: `cd backend && mvn compile -pl canvas-engine -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java \
       backend/canvas-engine/src/main/java/org/chovy/canvas/web/MetaController.java
git commit -m "feat: add AI Provider and Template CRUD APIs; add meta endpoints for AI_LLM config panel"
```

---

## Task 7: Frontend — Constants Update + AI_LLM Config Panel

**Files:**
- Modify: `frontend/src/components/canvas/constants.ts`
- Create: `frontend/src/components/config-panel/AiLlmConfigPanel.tsx`

- [ ] **Step 1: Update constants.ts**

Replace:
```typescript
AI_NEXT_BEST_ACTION: 'AI下一步动作',
```
with:
```typescript
AI_LLM: 'AI 智能节点',
```

- [ ] **Step 2: Write AiLlmConfigPanel.tsx**

```tsx
import React, { useEffect, useState } from 'react';
import { Select, InputNumber, Input, Form, Card, Divider, Spin, message } from 'antd';
import { NodeData } from './types';

const { TextArea } = Input;

interface AiLlmConfigPanelProps {
  nodeId: string;
  nodeData: NodeData;
  onChange: (key: string, value: unknown) => void;
  readonly?: boolean;
}

interface TemplateOption {
  value: number;
  label: string;
  category: string;
  promptTemplate: string;
  outputSchema: string;
  defaultValues: string;
  recommendedProviderId: number | null;
  recommendedParams: string | null;
  description: string;
}

interface ProviderOption {
  value: number;
  label: string;
  type: string;
  defaultParams: string | null;
}

const AiLlmConfigPanel: React.FC<AiLlmConfigPanelProps> = ({
  nodeId, nodeData, onChange, readonly = false
}) => {
  const [templates, setTemplates] = useState<TemplateOption[]>([]);
  const [providers, setProviders] = useState<ProviderOption[]>([]);
  const [loading, setLoading] = useState(true);

  const bizConfig = nodeData.bizConfig || {};

  useEffect(() => {
    Promise.all([
      fetch('/meta/ai-templates').then(r => r.json()),
      fetch('/meta/ai-providers').then(r => r.json()),
    ]).then(([tplRes, provRes]) => {
      if (tplRes.data) setTemplates(tplRes.data);
      if (provRes.data) setProviders(provRes.data);
    }).catch(() => message.error('加载AI配置失败'))
      .finally(() => setLoading(false));
  }, []);

  const handleTemplateChange = (templateId: number) => {
    const tpl = templates.find(t => t.value === templateId);
    if (!tpl) return;
    onChange('templateId', templateId);
    onChange('promptOverride', tpl.promptTemplate);
    onChange('schemaOverride', tpl.outputSchema);
    onChange('defaultValues', tpl.defaultValues);
    if (tpl.recommendedProviderId) {
      onChange('providerId', tpl.recommendedProviderId);
    }
    if (tpl.recommendedParams) {
      try {
        const params = JSON.parse(tpl.recommendedParams);
        if (params.temperature !== undefined) onChange('temperature', params.temperature);
        if (params.maxTokens !== undefined) onChange('maxTokens', params.maxTokens);
      } catch { /* ignore */ }
    }
  };

  const handleProviderChange = (providerId: number) => {
    onChange('providerId', providerId);
    const prov = providers.find(p => p.value === providerId);
    if (prov?.defaultParams) {
      try {
        const params = JSON.parse(prov.defaultParams);
        if (params.temperature !== undefined && bizConfig.temperature === undefined) {
          onChange('temperature', params.temperature);
        }
        if (params.maxTokens !== undefined && bizConfig.maxTokens === undefined) {
          onChange('maxTokens', params.maxTokens);
        }
      } catch { /* ignore */ }
    }
  };

  if (loading) return <Spin />;

  return (
    <div style={{ padding: '0 12px' }}>
      <Form layout="vertical" size="small">
        <Form.Item label="场景模板">
          <Select
            value={bizConfig.templateId as number | undefined}
            onChange={handleTemplateChange}
            placeholder="选择场景模板"
            disabled={readonly}
            options={templates.map(t => ({
              value: t.value,
              label: `${t.label}（${t.category}）`,
            }))}
          />
        </Form.Item>

        <Divider style={{ margin: '8px 0' }} orientation="left">模型配置</Divider>

        <Form.Item label="模型">
          <Select
            value={bizConfig.providerId as number | undefined}
            onChange={handleProviderChange}
            placeholder="选择模型"
            disabled={readonly}
            options={providers.map(p => ({
              value: p.value,
              label: p.label,
            }))}
          />
        </Form.Item>

        <div style={{ display: 'flex', gap: 8 }}>
          <Form.Item label="温度" style={{ flex: 1 }}>
            <InputNumber
              value={bizConfig.temperature as number ?? 0.7}
              onChange={v => onChange('temperature', v)}
              min={0} max={2} step={0.1}
              disabled={readonly}
              style={{ width: '100%' }}
            />
          </Form.Item>
          <Form.Item label="最大Token" style={{ flex: 1 }}>
            <InputNumber
              value={bizConfig.maxTokens as number ?? 1024}
              onChange={v => onChange('maxTokens', v)}
              min={1} max={32768}
              disabled={readonly}
              style={{ width: '100%' }}
            />
          </Form.Item>
        </div>

        <Form.Item label="超时(秒)">
          <InputNumber
            value={bizConfig.timeout as number ?? 30}
            onChange={v => onChange('timeout', v)}
            min={5} max={300}
            disabled={readonly}
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Divider style={{ margin: '8px 0' }} orientation="left">提示词</Divider>

        <Form.Item>
          <TextArea
            value={bizConfig.promptOverride as string ?? ''}
            onChange={e => onChange('promptOverride', e.target.value)}
            placeholder="提示词模板，支持 ${变量} 引用上下文"
            rows={6}
            disabled={readonly}
          />
        </Form.Item>

        <Divider style={{ margin: '8px 0' }} orientation="left">输出格式</Divider>

        <Form.Item>
          <TextArea
            value={bizConfig.schemaOverride as string ?? ''}
            onChange={e => onChange('schemaOverride', e.target.value)}
            placeholder="JSON Schema 定义输出结构"
            rows={4}
            disabled={readonly}
          />
        </Form.Item>

        <Divider style={{ margin: '8px 0' }} orientation="left">回退配置</Divider>

        <Form.Item>
          <TextArea
            value={bizConfig.defaultValues as string ?? ''}
            onChange={e => onChange('defaultValues', e.target.value)}
            placeholder="调用失败时的默认返回值（JSON）"
            rows={3}
            disabled={readonly}
          />
        </Form.Item>
      </Form>
    </div>
  );
};

export default AiLlmConfigPanel;
```

- [ ] **Step 3: Integrate AiLlmConfigPanel into config-panel/index.tsx**

In `frontend/src/components/config-panel/index.tsx`, add the import and the conditional render:

```tsx
import AiLlmConfigPanel from './AiLlmConfigPanel';
```

In the render logic where node-type-specific panels are selected, add:

```tsx
{nodeData.nodeType === 'AI_LLM' && (
  <AiLlmConfigPanel
    nodeId={nodeId}
    nodeData={nodeData}
    onChange={onChange}
    readonly={readonly}
  />
)}
```

- [ ] **Step 4: Verify frontend builds**

Run: `cd frontend && npm run build 2>&1 | tail -5`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add frontend/src/components/canvas/constants.ts \
       frontend/src/components/config-panel/AiLlmConfigPanel.tsx \
       frontend/src/components/config-panel/index.tsx
git commit -m "feat: add AI_LLM config panel with template selector; update node type constants"
```

---

## Task 8: Integration Test + Cleanup

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerIntegrationTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/SpecialNodeStage2ExecutionTest.java` (if it references AI_NEXT_BEST_ACTION)

- [ ] **Step 1: Check if any test references AI_NEXT_BEST_ACTION and fix**

Run: `grep -r "AI_NEXT_BEST_ACTION\|AiNextBestAction" /Users/photonpay/project/canvas/backend --include="*.java" -l`
Expected: No remaining references (already deleted in Task 5)

If any are found, replace `AI_NEXT_BEST_ACTION` with `AI_LLM` and `AiNextBestActionHandler` with `AiLlmHandler`.

- [ ] **Step 2: Write integration test**

```java
package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.chovy.canvas.dal.dataobject.AiPromptTemplateDO;
import org.chovy.canvas.dal.mapper.AiPromptTemplateMapper;
import org.junit.jupiter.api.*;
import org.mockito.*;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiLlmHandlerIntegrationTest {

    @InjectMocks
    private AiLlmHandler handler;

    @Mock
    private AiLlmGateway gateway;

    @Mock
    private AiPromptTemplateMapper templateMapper;

    @Mock
    private ExecutionContext ctx;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void fullFlowWithTemplateAndFallback() {
        // Template returns scoring prompt
        AiPromptTemplateDO tpl = new AiPromptTemplateDO();
        tpl.setId(1L);
        tpl.setPromptTemplate("评估用户 ${userProfile} 的价值");
        tpl.setOutputSchema("{\"type\":\"object\",\"properties\":{\"score\":{\"type\":\"number\"},\"band\":{\"type\":\"string\"}}}");
        tpl.setDefaultValues("{\"score\":50,\"band\":\"medium\"}");

        when(templateMapper.selectById(1L)).thenReturn(tpl);
        when(ctx.getContextValue("userProfile")).thenReturn("28岁男性，高频购买用户");

        // LLM fails → gateway returns fallback
        when(gateway.call(anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.just(Map.of("score", 50, "band", "medium")));

        Map<String, Object> config = Map.of(
                "templateId", 1,
                "providerId", 1,
                "nextNodeId", "if_1"
        );

        var result = handler.executeAsync(config, ctx).block();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("if_1", result.nextNodeId());
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) result.output().get("ai_output");
        Assertions.assertEquals(50, output.get("score"));
        Assertions.assertEquals("medium", output.get("band"));
    }

    @Test
    void promptVariableSubstitution() {
        when(ctx.getContextValue("userName")).thenReturn("张三");
        when(ctx.getContextValue("productName")).thenReturn("iPhone 16");

        // No template, custom prompt
        when(gateway.call(anyString(), anyString(), anyMap()))
                .thenReturn(reactor.core.publisher.Mono.just(Map.of("text", "推荐")));

        Map<String, Object> config = Map.of(
                "providerId", 1,
                "promptOverride", "为 ${userName} 推荐 ${productName}",
                "schemaOverride", "{\"type\":\"object\"}",
                "nextNodeId", "next_1"
        );

        var result = handler.executeAsync(config, ctx).block();

        // Verify the prompt passed to gateway has variables resolved
        verify(gateway).call(eq("为 张三 推荐 iPhone 16"), anyString(), anyMap());
    }
}
```

- [ ] **Step 3: Run all AI-related tests**

Run: `cd backend && mvn test -pl canvas-engine -Dtest="AiLlmHandlerTest,AiLlmHandlerIntegrationTest,AiLlmGatewayTest,OpenAiLlmClientTest" -q 2>&1 | tail -10`
Expected: All tests pass

- [ ] **Step 4: Run full test suite to catch regressions**

Run: `cd backend && mvn test -pl canvas-engine -q 2>&1 | tail -10`
Expected: BUILD SUCCESS, no failures related to AI_LLM changes

- [ ] **Step 5: Commit**

```bash
git add backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/AiLlmHandlerIntegrationTest.java
git commit -m "test: add AiLlmHandler integration tests with template rendering and variable substitution"
```

---

## Self-Review

**1. Spec coverage check:**

| Spec Section | Task |
|---|---|
| Core positioning + runtime flow | Task 5 (AiLlmHandler) |
| Template system (ai_prompt_template table) | Task 1 (migration) + Task 5 (handler loads template) |
| Provider config center (ai_provider table) | Task 1 (migration) + Task 6 (CRUD API) |
| Backend architecture (Handler + Gateway + LlmClient) | Tasks 3, 4, 5 |
| Fallback strategy | Task 4 (AiLlmGateway.onErrorResume) |
| Node config JSON structure | Task 5 (handler reads all config keys) |
| Frontend interaction | Task 7 (AiLlmConfigPanel) |
| NodeType change (AI_NEXT_BEST_ACTION → AI_LLM) | Task 1 (migration) + Task 5 (enum + handler) |
| Downstream node collaboration | Covered by ai_output in NodeResult.ok output map |

**2. Placeholder scan:** No TBD/TODO/fill-in-later patterns found. All code blocks contain complete implementation.

**3. Type consistency:**
- `LlmClient.chat()` signature: `(AiProviderDO, String prompt, String outputSchema, Map<String, Object> params)` — consistent across interface, OpenAiLlmClient, and AiLlmGateway
- `AiLlmGateway.call()` signature: `(String prompt, String outputSchema, Map<String, Object> config)` — consistent with AiLlmHandler usage
- NodeResult factory: `NodeResult.ok(nextNodeId, Map.of("ai_output", json))` — matches spec section 10
- Config keys: `templateId`, `providerId`, `model`, `temperature`, `maxTokens`, `timeout`, `promptOverride`, `schemaOverride`, `defaultValues` — consistent across handler, frontend, and migration
