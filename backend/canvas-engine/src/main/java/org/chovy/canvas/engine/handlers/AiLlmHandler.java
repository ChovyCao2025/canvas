package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.MapFieldKeys;
import org.chovy.canvas.common.enums.NodeType;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.chovy.canvas.engine.handler.NodeResult;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AiLlmHandler 参与 engine.handlers 场景的画布执行引擎处理。
 */
@Component
@NodeHandlerType(NodeType.AI_LLM)
public class AiLlmHandler implements NodeHandler {

    private static final String PROVIDER_ID = "providerId";
    private static final String MODEL_KEY = "modelKey";
    private static final String PROMPT_OVERRIDE = "promptOverride";
    private static final String SCHEMA_OVERRIDE = "schemaOverride";
    private static final String OUTPUT_PREFIX = "outputPrefix";
    private static final String TEMPERATURE = "temperature";
    private static final String MAX_TOKENS = "maxTokens";
    private static final String TIMEOUT_MS = "timeoutMs";
    private static final String AI_OUTPUT = "ai_output";
    private static final String AI_STATUS = "ai_status";
    private static final String AI_FALLBACK_USED = "ai_fallback_used";
    private static final String AI_PROVIDER_ID = "ai_provider_id";
    private static final String AI_TEMPLATE_ID = "ai_template_id";
    private static final String AI_MODEL_KEY = "ai_model_key";
    private static final String AI_LATENCY_MS = "ai_latency_ms";
    private static final String AI_MESSAGE = "ai_message";

    private final AiLlmGateway gateway;
    private final ObjectMapper objectMapper;

    /**
     * 创建 AiLlmHandler 实例并注入 engine.handlers 场景依赖。
     * @param gateway gateway 参数，用于 AiLlmHandler 流程中的校验、计算或对象转换。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public AiLlmHandler(AiLlmGateway gateway, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 AI 大模型节点：根据模板、模型和变量配置构造一次 LLM 请求，并把模型输出写入节点输出。
     *
     * <p>该方法不直接开启数据库事务；实际模板解析、调用供应商、审计或缓存行为由
     * {@link AiLlmGateway} 实现负责。调用成功时沿 {@code nextNodeId} 继续执行，
     * 输出包含标准 AI 状态字段以及结构化结果的展开字段；调用失败时若配置了失败节点则路由到
     * {@code failNodeId}，否则返回失败结果并终止当前节点。</p>
     *
     * @param config 节点配置，关键字段包括 {@code templateId}、{@code providerId}、{@code modelKey}、
     *               {@code variables}、{@code params}、{@code outputPrefix} 和失败/成功路由
     * @param ctx 执行上下文，用于注入租户、画布、执行 ID、用户与上下文变量
     * @return 异步节点结果；成功结果携带 AI 输出，失败结果携带错误消息或失败分支输出
     */
    @Override
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        Long templateId = longValue(config.get(MapFieldKeys.TEMPLATE_ID));
        if (templateId == null) {
            return Mono.just(NodeResult.fail("AI_LLM: templateId 未配置"));
        }
        String nodeId = string(config.get(MapFieldKeys.NODE_ID_INTERNAL), null);
        AiLlmGateway.AiLlmRequest request = new AiLlmGateway.AiLlmRequest(
                longValue(config.get(PROVIDER_ID)),
                templateId,
                string(config.get(MODEL_KEY), null),
                string(config.get(PROMPT_OVERRIDE), null),
                variables(config, ctx),
                schemaOverride(config),
                params(config),
                intValue(config.get(TIMEOUT_MS)),
                ctx == null ? null : ctx.getCanvasId(),
                ctx == null ? null : ctx.getExecutionId(),
                nodeId);

        Mono<AiLlmGateway.AiLlmResult> evaluation;
        try {
            evaluation = gateway.evaluate(ctx == null ? 0L : ctx.getTenantId(), request);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (Exception ex) {
            return Mono.just(failed(config, ex));
        }
        return evaluation
                .map(result -> {
                    Map<String, Object> output = output(config, result);
                    if (AiLlmGateway.STATUS_CONFIG_ERROR.equals(result.status())) {
                        return NodeResult.fail("AI_LLM: " + result.message(), output);
                    }
                    return NodeResult.ok(string(config.get(MapFieldKeys.NEXT_NODE_ID), null), output);
                })
                .onErrorResume(ex -> Mono.just(failed(config, ex)));
    }

    /**
     * 将 LLM 调用异常转换为节点结果，优先走配置的失败分支并保留错误状态输出。
     */
    private NodeResult failed(Map<String, Object> config, Throwable ex) {
        Map<String, Object> output = new LinkedHashMap<>();
        String prefix = string(config.get(OUTPUT_PREFIX), "");
        output.put(key(prefix, AI_STATUS), "FAILED");
        output.put(key(prefix, AI_MESSAGE), message(ex));
        return NodeResult.fail("AI_LLM: " + message(ex), output);
    }

    /**
     * 汇总发送给提示词模板的变量，先注入运行上下文，再用节点配置中的显式变量覆盖或补充。
     */
    private JsonNode variables(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> variables = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (ctx != null) {
            variables.putAll(ctx.getTriggerPayload());
            variables.putAll(ctx.getFlatContext());
            variables.put("userId", ctx.getUserId());
            variables.put("canvasId", ctx.getCanvasId());
            variables.put("executionId", ctx.getExecutionId());
            variables.put("triggerType", ctx.getTriggerType());
            variables.put("triggerPayload", ctx.getTriggerPayload());
            variables.put("ctx", ctx.getFlatContext());
            variables.put("nodeOutputs", ctx.getNodeOutputs());
        }
        Object configured = config.get(MapFieldKeys.VARIABLES);
        if (configured instanceof Map<?, ?> map) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            map.forEach((key, value) -> {
                String name = string(key, null);
                if (name != null) {
                    variables.put(name, resolveConfiguredValue(value, ctx));
                }
            });
        }
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        return objectMapper.valueToTree(variables);
    }

    /**
     * 解析配置变量中的上下文表达式，支持 {@code ${key}} 和转义后的 {@code $${key}}。
     */
    private Object resolveConfiguredValue(Object value, ExecutionContext ctx) {
        if (value instanceof String text) {
            String normalized = text.startsWith("$${") ? text.substring(1) : text;
            if (normalized.startsWith("${") && normalized.endsWith("}") && ctx != null) {
                return ctx.getContextValue(normalized.substring(2, normalized.length() - 1));
            }
        }
        return value;
    }

    /**
     * 提取透传给模型网关的参数，并把顶层温度配置合并到参数集合中。
     */
    private Map<String, Object> params(Map<String, Object> config) {
        Map<String, Object> params = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (config.get(MapFieldKeys.PARAMS) instanceof Map<?, ?> map) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            map.forEach((key, value) -> {
                String name = string(key, null);
                if (name != null && value != null) {
                    params.put(name, value);
                }
            });
        }
        Object temperature = config.get(TEMPERATURE);
        if (temperature != null) {
            params.put(TEMPERATURE, temperature);
        }
        Object maxTokens = config.get(MAX_TOKENS);
        if (maxTokens != null) {
            params.put(MAX_TOKENS, maxTokens);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return params;
    }

    /**
     * 解析 AI_LLM 输出 schema 覆盖配置。
     */
    private JsonNode schemaOverride(Map<String, Object> config) {
        Object value = config.get(SCHEMA_OVERRIDE);
        if (value == null) {
            return null;
        }
        if (value instanceof JsonNode node) {
            return node.isNull() ? null : node;
        }
        if (value instanceof Map<?, ?> map) {
            return objectMapper.valueToTree(map);
        }
        String text = string(value, null);
        if (text == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(text);
            if (!node.isObject()) {
                throw new IllegalArgumentException("schemaOverride must be a JSON object");
            }
            return node;
        } catch (Exception ex) {
            throw new IllegalArgumentException("schemaOverride must be valid JSON", ex);
        }
    }

    /**
     * 生成写回执行上下文的 AI 节点输出，包含标准元数据和对象型模型结果的字段展开。
     */
    private Map<String, Object> output(Map<String, Object> config, AiLlmGateway.AiLlmResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        String prefix = string(config.get(OUTPUT_PREFIX), "");
        Object aiOutput = objectMapper.convertValue(result.output(), Object.class);
        output.put(key(prefix, AI_OUTPUT), aiOutput);
        output.put(key(prefix, AI_STATUS), result.status());
        output.put(key(prefix, AI_FALLBACK_USED), result.fallbackUsed());
        output.put(key(prefix, AI_PROVIDER_ID), result.providerId());
        output.put(key(prefix, AI_TEMPLATE_ID), result.templateId());
        output.put(key(prefix, AI_MODEL_KEY), result.modelKey());
        output.put(key(prefix, AI_LATENCY_MS), result.latencyMs());
        output.put(key(prefix, AI_MESSAGE), result.message());
        if (result.output() != null && result.output().isObject()) {
            result.output().fields().forEachRemaining(entry ->
                    output.put(key(prefix, entry.getKey()), objectMapper.convertValue(entry.getValue(), Object.class)));
        }
        return output;
    }

    /**
     * 生成 AI 输出字段 key。
     *
     * @param prefix 输出前缀
     * @param field 字段名
     * @return 带前缀的字段 key
     */
    private static String key(String prefix, String field) {
        return prefix == null || prefix.isBlank() ? field : prefix + "." + field;
    }

    /**
     * 将对象转换为 Long。
     *
     * @param value 原始值
     * @return Long 值，无法解析时返回 null
     */
    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将对象转换为 Integer。
     *
     * @param value 原始值
     * @return Integer 值，无法解析时返回 null
     */
    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * 将对象转换为非空字符串。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 去除首尾空白后的字符串或默认值
     */
    private static String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    /**
     * 提取异常消息。
     *
     * @param throwable 异常
     * @return 异常消息或异常类名
     */
    private static String message(Throwable throwable) {
        Throwable cause = throwable == null || throwable.getCause() == null ? throwable : throwable.getCause();
        if (cause == null) {
            return "";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
