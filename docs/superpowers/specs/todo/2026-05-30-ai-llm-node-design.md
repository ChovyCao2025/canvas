# AI_LLM 节点设计

> 替换现有 `AI_NEXT_BEST_ACTION` 占位节点，实现通用 LLM 调用能力。

## 1. 核心定位

**AI 不决定路由，只产出结构化数据写入上下文。** 下游用现有节点（IfCondition / Scoring / Selector 等）消费 AI 输出做路由。

画布上只有 **1 个 AI 节点类型 `AI_LLM`**，场景差异通过**预置模板**区分，不是新增节点类型。

## 2. 运行时流程

```
1. 加载模板（templateId → 数据库取提示词 + Schema）
2. 渲染提示词（${变量} 替换为 ExecutionContext 中的值）
3. 调用 LLM（AiLlmGateway 统一处理重试/超时/JSON 解析/回退）
4. 解析 JSON 输出，校验 Schema
5. 写入上下文 ai_output.xxx
6. 返回 NodeResult.ok → 走 nextNodeId
```

## 3. 模板系统

模板是数据库记录，不是代码。新增场景 = 新增一条模板记录。

### ai_prompt_template 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(100) | 模板名称 |
| category | varchar(50) | 分类：text_generate / scoring / timing / recommend / custom |
| prompt_template | text | 提示词模板，支持 `${变量}` 引用上下文 |
| output_schema | json | JSON Schema 定义输出结构 |
| default_values | json | Schema 各字段的默认值（回退用） |
| recommended_provider_id | bigint | 推荐的 Provider |
| recommended_params | json | 推荐参数（temperature / maxTokens 等） |
| description | text | 模板说明 |
| enabled | boolean | 是否启用 |
| created_at | timestamp | 创建时间 |
| updated_at | timestamp | 更新时间 |

### 预置模板

| 模板 | category | 提示词概要 | 输出 Schema |
|------|----------|-----------|------------|
| 文案生成 | text_generate | 根据用户画像生成个性化触达文案 | `{ text: string, subject?: string, tone: string }` |
| 智能评分 | scoring | 根据行为数据评估用户价值/流失概率 | `{ score: number, band: string, reason: string }` |
| 智能时机 | timing | 根据活跃模式推荐最佳触达时间 | `{ bestTime: string, confidence: number }` |
| 权益推荐 | recommend | 根据用户特征推荐最匹配的权益 | `{ itemId: string, itemName: string, reason: string }` |
| 自定义 | custom | 用户自己写提示词 + Schema | 用户定义 |

## 4. Provider 配置中心

管理员统一配置 LLM Provider，运营不可见 API Key。

### ai_provider 表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| name | varchar(100) | 显示名称 |
| type | varchar(30) | Provider 类型枚举 |
| endpoint | varchar(500) | API 端点 URL |
| api_key | varchar(500) | 加密存储的 API Key |
| default_params | json | 默认参数 `{ temperature, maxTokens, topP, timeout, maxRetries }` |
| enabled | boolean | 是否启用 |
| tenant_id | bigint | 租户 ID（多租户隔离） |
| created_at | timestamp | 创建时间 |

### LlmProviderType 枚举

```java
public enum LlmProviderType {
    OPENAI,       // 兼容 OpenAI API 格式（OpenAI / DeepSeek / Moonshot / ...）
    ANTHROPIC,    // Anthropic Claude API
    AZURE_OPENAI, // Azure OpenAI 部署
    OLLAMA,       // 本地 Ollama
    QWEN,         // 阿里通义千问（DashScope）
    CUSTOM        // 自定义 OpenAI 兼容端点
}
```

大部分国内模型（DeepSeek / Moonshot / 零一万物）兼容 OpenAI API 格式，只需改 endpoint 和 api_key，用 `OPENAI` 类型即可。真正需要独立类型的是 Anthropic（不同 API 格式）和 Ollama（本地部署）。

## 5. 后端架构

### AiLlmHandler（画布节点）

替换现有 `AiNextBestActionHandler`。

```java
@Component
@NodeHandlerType(NodeType.AI_LLM)
public class AiLlmHandler implements NodeHandler {

    @Autowired AiLlmGateway gateway;
    @Autowired AiPromptTemplateRepository templates;

    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        // 1. 加载模板
        String templateId = string(config, "templateId", null);
        AiPromptTemplate tpl = templateId != null ? templates.findById(templateId) : null;

        // 2. 渲染提示词
        String prompt = renderPrompt(tpl, config, ctx);

        // 3. 合并参数（节点配置覆盖模板默认值覆盖 Provider 默认值）
        Map<String, Object> mergedParams = mergeParams(tpl, config);

        // 4. 调用 LLM
        JsonSchema schema = tpl != null ? tpl.getOutputSchema() : parseSchema(config);
        return gateway.call(prompt, schema, mergedParams)
            .map(json -> NodeResult.ok(nextNodeId(config), Map.of("ai_output", json)));
    }
}
```

### AiLlmGateway（内部组件，不是画布节点）

统一处理 LLM 调用的通用逻辑：Provider 路由、重试、超时、JSON 解析、回退。

```java
@Component
public class AiLlmGateway {

    @Autowired List<LlmClient> clients;
    @Autowired AiProviderRepository providers;

    public Mono<Map<String, Object>> call(String prompt, JsonSchema schema, Map<String, Object> config) {
        AiProvider provider = providers.findById(config.get("providerId"));

        LlmClient client = clients.stream()
            .filter(c -> c.supports(provider.getType()))
            .findFirst()
            .orElseThrow();

        return client.chat(provider, prompt, schema, config)
            .timeout(Duration.ofSeconds(provider.getTimeout()))
            .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
            .onErrorResume(e -> fallback(schema, config));
    }

    private Mono<Map<String, Object>> fallback(JsonSchema schema, Map<String, Object> config) {
        // 返回 Schema 中定义的默认值，不阻塞画布执行
        return Mono.just(schema.getDefaultValues());
    }
}
```

### LlmClient 接口（策略模式）

```java
public interface LlmClient {
    boolean supports(LlmProviderType type);
    Mono<Map<String, Object>> chat(AiProvider provider, String prompt, JsonSchema schema, Map<String, Object> params);
}
```

新增 Provider 只需加一个 `LlmClient` 实现类。

### OpenAiLlmClient（覆盖大部分场景）

OpenAI 兼容格式，同时适用于 DeepSeek / Moonshot / 零一万物等。

```java
@Component
public class OpenAiLlmClient implements LlmClient {
    @Override
    public boolean supports(LlmProviderType type) { return type == OPENAI || type == CUSTOM; }

    @Override
    public Mono<Map<String, Object>> chat(AiProvider provider, String prompt, JsonSchema schema, Map<String, Object> params) {
        // WebClient 调用 OpenAI Chat Completions API
        // response_format: { type: "json_schema", json_schema: schema }
        // 解析 choices[0].message.content 为 JSON
    }
}
```

## 6. 回退策略

LLM 调用失败时，不阻塞画布执行，返回 Schema 中定义的默认值。

| 场景 | 回退行为 |
|------|---------|
| 文案生成失败 | 返回模板中预写的兜底文案 |
| 评分失败 | 返回中性分数（如 50 分，band: "medium"） |
| 时机预测失败 | 返回当前时间 +30 分钟（保守延迟） |
| 权益推荐失败 | 返回配置中的默认权益 ID |
| 自定义失败 | 返回 Schema 中所有字段的默认值 |

**设计原则**：AI 是增强不是关键路径。画布不应因 AI 服务不可用而中断。

## 7. 节点配置 JSON 结构

AI_LLM 节点在画布 JSON 中的 config 示例：

```json
{
  "templateId": 1,
  "providerId": 1,
  "model": "gpt-4o",
  "temperature": 0.7,
  "maxTokens": 1024,
  "timeout": 30,
  "promptOverride": null,
  "schemaOverride": null,
  "nextNodeId": "condition_001"
}
```

- `templateId`：选模板后自动填充提示词和 Schema
- `promptOverride` / `schemaOverride`：运营修改模板后，差异部分存这里
- `providerId` + `model`：从 Provider 列表选择

## 8. 前端交互

### AI_LLM 节点配置面板

```
┌─────────────────────────────────────┐
│  AI 智能节点                         │
├─────────────────────────────────────┤
│  场景模板：[文案生成 ▼]               │
│                                     │
│  ── 模型配置 ──                      │
│  模型：[OpenAI GPT-4o ▼]            │
│  温度：[0.7]  最大Token：[1024]       │
│  超时：[30s]                         │
│                                     │
│  ── 提示词 ──                        │
│  [根据用户画像 ${user.profile}       │
│   生成个性化短信文案...               │
│   ]                                  │
│                                     │
│  ── 输出格式 ──                      │
│  {                                   │
│    "text": "string",                 │
│    "tone": "string"                  │
│  }                                   │
│                                     │
│  ── 回退配置 ──                      │
│  调用失败时使用默认值                  │
│  text: [尊敬的用户您好...]            │
│  tone: [formal]                      │
└─────────────────────────────────────┘
```

选模板后：提示词、Schema、推荐模型、回退默认值自动填充。运营可微调，也可完全自定义。

## 9. NodeType 变更

```java
// 删除
public static final String AI_NEXT_BEST_ACTION = "AI_NEXT_BEST_ACTION";

// 新增
public static final String AI_LLM = "AI_LLM";
```

Flyway 迁移需：
- 更新 `canvas_node` 表中 `node_type = 'AI_NEXT_BEST_ACTION'` 的记录为 `AI_LLM`
- 创建 `ai_provider` 表
- 创建 `ai_prompt_template` 表
- 插入 4 条预置模板种子数据

## 10. 与现有节点的协作

AI_LLM 输出写入上下文 `ai_output`，下游节点直接引用：

```
AI_LLM（智能评分）
  → ai_output = { score: 85, band: "high", reason: "高频购买用户" }
      │
      ▼
IfCondition（ai_output.band == "high"）
  → true  → SendPush（高价值用户专属 Push）
  → false → SendSms（普通用户短信）
```

```
AI_LLM（文案生成）
  → ai_output = { text: "张先生，您收藏的商品降价了...", subject: "降价提醒" }
      │
      ▼
SendSms（content = ${ai_output.text}）
```

不需要新增任何路由节点，完全复用现有 IfCondition / Scoring / Selector 体系。
