package org.chovy.canvas.domain.bi.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.chovy.canvas.domain.bi.query.BiDatasetSpec;
import org.chovy.canvas.domain.bi.query.BiFieldSpec;
import org.chovy.canvas.domain.bi.query.BiFilter;
import org.chovy.canvas.domain.bi.query.BiMetricSpec;
import org.chovy.canvas.domain.bi.query.BiSort;
import org.chovy.canvas.engine.llm.AiLlmGateway;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmBiAskDataPlanner implements BiAskDataPlanner {

    public static final long DEFAULT_TEMPLATE_ID = 3L;

    private final AiLlmGateway llmGateway;
    private final ObjectMapper objectMapper;

    public LlmBiAskDataPlanner(AiLlmGateway llmGateway, ObjectMapper objectMapper) {
        this.llmGateway = llmGateway;
        this.objectMapper = objectMapper;
    }

    @Override
    public BiAskDataPlanningResult plan(BiAskDataPlanningContext context) {
        BiAskDataRequest request = context.request();
        AiLlmGateway.AiLlmResult result = llmGateway.evaluate(context.tenantId(), new AiLlmGateway.AiLlmRequest(
                request == null ? null : request.providerId(),
                request == null || request.templateId() == null ? DEFAULT_TEMPLATE_ID : request.templateId(),
                request == null ? null : request.modelKey(),
                null,
                variables(context),
                request == null ? Map.of() : request.params(),
                request == null ? null : request.timeoutMs(),
                null,
                null,
                "bi-ask-data-agent")).block();
        if (result == null) {
            throw new IllegalStateException("AI ask-data planner returned no result");
        }
        return new BiAskDataPlanningResult(result.status(), result.fallbackUsed(), toPlan(result.output()));
    }

    private JsonNode variables(BiAskDataPlanningContext context) {
        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("question", context.question());
        if (context.requestedDatasetKey() == null) {
            variables.putNull("requestedDatasetKey");
        } else {
            variables.put("requestedDatasetKey", context.requestedDatasetKey());
        }
        variables.set("datasets", objectMapper.valueToTree(context.datasets().stream()
                .map(this::datasetCatalog)
                .toList()));
        return variables;
    }

    private Map<String, Object> datasetCatalog(BiDatasetSpec dataset) {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("datasetKey", dataset.datasetKey());
        catalog.put("fields", dataset.fields().values().stream()
                .sorted(Comparator.comparing(BiFieldSpec::fieldKey))
                .map(field -> Map.of(
                        "fieldKey", field.fieldKey(),
                        "role", field.role().name(),
                        "valueType", field.valueType()))
                .toList());
        catalog.put("metrics", dataset.metrics().values().stream()
                .sorted(Comparator.comparing(BiMetricSpec::metricKey))
                .map(metric -> Map.of(
                        "metricKey", metric.metricKey(),
                        "valueType", metric.valueType(),
                        "allowedDimensions", metric.allowedDimensions()))
                .toList());
        return catalog;
    }

    private BiAskDataPlan toPlan(JsonNode output) {
        JsonNode source = output == null || output.isNull() ? objectMapper.createObjectNode() : output;
        return new BiAskDataPlan(
                text(source, "datasetKey"),
                list(source.path("dimensions"), new TypeReference<>() {
                }),
                list(source.path("metrics"), new TypeReference<>() {
                }),
                list(source.path("filters"), new TypeReference<>() {
                }),
                list(source.path("sorts"), new TypeReference<>() {
                }),
                source.path("limit").asInt(0),
                text(source, "explanation"));
    }

    private <T> List<T> list(JsonNode node, TypeReference<List<T>> type) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(node, type);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }
}
