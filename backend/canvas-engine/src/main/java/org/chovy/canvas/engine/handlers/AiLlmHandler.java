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

@Component
@NodeHandlerType(NodeType.AI_LLM)
public class AiLlmHandler implements NodeHandler {

    private static final String PROVIDER_ID = "providerId";
    private static final String MODEL_KEY = "modelKey";
    private static final String PROMPT_OVERRIDE = "promptOverride";
    private static final String OUTPUT_PREFIX = "outputPrefix";
    private static final String TEMPERATURE = "temperature";
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

    public AiLlmHandler(AiLlmGateway gateway, ObjectMapper objectMapper) {
        this.gateway = gateway;
        this.objectMapper = objectMapper;
    }

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
                params(config),
                intValue(config.get(TIMEOUT_MS)),
                ctx == null ? null : ctx.getCanvasId(),
                ctx == null ? null : ctx.getExecutionId(),
                nodeId);

        Mono<AiLlmGateway.AiLlmResult> evaluation;
        try {
            evaluation = gateway.evaluate(ctx == null ? 0L : ctx.getTenantId(), request);
        } catch (Exception ex) {
            return Mono.just(failed(config, ex));
        }
        return evaluation
                .map(result -> NodeResult.ok(string(config.get(MapFieldKeys.NEXT_NODE_ID), null),
                        output(config, result)))
                .onErrorResume(ex -> Mono.just(failed(config, ex)));
    }

    private NodeResult failed(Map<String, Object> config, Throwable ex) {
        String failNodeId = string(config.get(MapFieldKeys.FAIL_NODE_ID), null);
        if (failNodeId == null) {
            return NodeResult.fail("AI_LLM: " + message(ex));
        }
        Map<String, Object> output = new LinkedHashMap<>();
        String prefix = string(config.get(OUTPUT_PREFIX), "");
        output.put(key(prefix, AI_STATUS), "FAILED");
        output.put(key(prefix, AI_MESSAGE), message(ex));
        return NodeResult.routed("fail", failNodeId, output);
    }

    private JsonNode variables(Map<String, Object> config, ExecutionContext ctx) {
        Map<String, Object> variables = new LinkedHashMap<>();
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
            map.forEach((key, value) -> {
                String name = string(key, null);
                if (name != null) {
                    variables.put(name, resolveConfiguredValue(value, ctx));
                }
            });
        }
        return objectMapper.valueToTree(variables);
    }

    private Object resolveConfiguredValue(Object value, ExecutionContext ctx) {
        if (value instanceof String text) {
            String normalized = text.startsWith("$${") ? text.substring(1) : text;
            if (normalized.startsWith("${") && normalized.endsWith("}") && ctx != null) {
                return ctx.getContextValue(normalized.substring(2, normalized.length() - 1));
            }
        }
        return value;
    }

    private Map<String, Object> params(Map<String, Object> config) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (config.get(MapFieldKeys.PARAMS) instanceof Map<?, ?> map) {
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
        return params;
    }

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

    private static String key(String prefix, String field) {
        return prefix == null || prefix.isBlank() ? field : prefix + "." + field;
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String string(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private static String message(Throwable throwable) {
        Throwable cause = throwable == null || throwable.getCause() == null ? throwable : throwable.getCause();
        if (cause == null) {
            return "";
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
