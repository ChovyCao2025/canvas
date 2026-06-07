package org.chovy.canvas.domain.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmMarketingMonitorInferenceGenerator implements MarketingMonitorInferenceGenerator {

    public static final long DEFAULT_TEMPLATE_ID = 9L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmMarketingMonitorInferenceGenerator(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    @Override
    public MarketingMonitorInferenceGenerationResult generate(MarketingMonitorInferenceGenerationContext context) {
        MarketingMonitorInferenceCommand command = context == null ? null : context.command();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                command == null ? null : command.providerId(),
                command == null || command.templateId() == null ? DEFAULT_TEMPLATE_ID : command.templateId(),
                command == null ? null : command.modelKey(),
                null,
                objectMapper.valueToTree(context.promptContext()),
                command == null ? null : command.params(),
                command == null ? null : command.timeoutMs(),
                null,
                null,
                "marketing-monitor-inference")).block();
        if (result == null) {
            return null;
        }
        JsonNode output = result.output();
        return new MarketingMonitorInferenceGenerationResult(
                result.providerId(),
                result.templateId(),
                result.modelKey(),
                value(command == null ? null : command.modelVersion(), "llm_v1"),
                value(result.status(), "UNKNOWN"),
                result.fallbackUsed(),
                text(output, "sentimentLabel", "NEUTRAL"),
                decimal(output, "sentimentScore", BigDecimal.ZERO),
                decimal(output, "confidence", BigDecimal.valueOf(result.fallbackUsed() ? 0.35 : 0.70)),
                entities(output),
                strings(output, "topics"),
                strings(output, "riskFlags"),
                evidence(output),
                result.latencyMs());
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return fallback;
        }
        return value.asText().trim();
    }

    private BigDecimal decimal(JsonNode node, String field, BigDecimal fallback) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isNumber()) {
            return fallback;
        }
        return value.decimalValue();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> entities(JsonNode node) {
        JsonNode value = node == null ? null : node.get("entities");
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        value.forEach(item -> {
            if (item != null && item.isObject()) {
                result.add(objectMapper.convertValue(item, Map.class));
            }
        });
        return result;
    }

    private List<String> strings(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        value.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                result.add(item.asText().trim());
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evidence(JsonNode node) {
        JsonNode value = node == null ? null : node.get("evidence");
        if (value == null || !value.isObject()) {
            return Map.of();
        }
        return objectMapper.convertValue(value, LinkedHashMap.class);
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
