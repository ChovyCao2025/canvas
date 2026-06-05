package org.chovy.canvas.domain.analytics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class BehaviorAudienceRuleCompiler {

    private static final String SOURCE_CDP_EVENT_METRIC = "CDP_EVENT_METRIC";
    private static final int MAX_FILTERS = 10;
    private static final int MAX_WINDOW_DAYS = 366;
    private static final Set<String> SUPPORTED_METRICS = Set.of(
            "COUNT", "SUM_PROPERTY", "MAX_PROPERTY", "LAST_SEEN_DAYS_AGO");
    private static final Set<String> SUPPORTED_OPERATORS = Set.of("=", "!=", ">", ">=", "<", "<=");
    private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

    private final ObjectMapper objectMapper;

    public CompiledBehaviorAudienceQuery compile(Long tenantId, String ruleJson, int limit) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        JsonNode root = parse(ruleJson);
        String source = text(root, "source");
        if (!SOURCE_CDP_EVENT_METRIC.equals(source)) {
            throw new IllegalArgumentException("unsupported source: " + source);
        }

        String eventCode = text(root, "eventCode");
        if (eventCode == null || eventCode.isBlank()) {
            throw new IllegalArgumentException("eventCode is required");
        }

        int windowDays = root.path("windowDays").asInt(0);
        if (windowDays <= 0 || windowDays > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException("windowDays must be between 1 and " + MAX_WINDOW_DAYS);
        }

        String metric = text(root, "metric");
        if (!SUPPORTED_METRICS.contains(metric)) {
            throw new IllegalArgumentException("unsupported metric: " + metric);
        }

        String operator = text(root, "operator");
        if (!SUPPORTED_OPERATORS.contains(operator)) {
            throw new IllegalArgumentException("unsupported operator: " + operator);
        }

        JsonNode value = root.get("value");
        if (value == null || value.isNull() || value.isContainerNode()) {
            throw new IllegalArgumentException("value must be scalar");
        }

        List<Filter> filters = compileFilters(root.path("filters"));
        return new CompiledBehaviorAudienceQuery(
                tenantId,
                eventCode.trim(),
                windowDays,
                metric,
                operator,
                value.asText(),
                filters,
                limit);
    }

    private JsonNode parse(String ruleJson) {
        if (ruleJson == null || ruleJson.isBlank()) {
            throw new IllegalArgumentException("ruleJson is required");
        }
        try {
            return objectMapper.readTree(ruleJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("ruleJson is invalid", ex);
        }
    }

    private List<Filter> compileFilters(JsonNode filtersNode) {
        if (filtersNode.isMissingNode() || filtersNode.isNull()) {
            return List.of();
        }
        if (!filtersNode.isArray()) {
            throw new IllegalArgumentException("filters must be an array");
        }
        if (filtersNode.size() > MAX_FILTERS) {
            throw new IllegalArgumentException("filters must contain at most " + MAX_FILTERS + " entries");
        }

        List<Filter> filters = new ArrayList<>();
        for (JsonNode node : filtersNode) {
            String field = text(node, "field");
            if (field == null || !SAFE_PATH.matcher(field).matches()) {
                throw new IllegalArgumentException("filter field is unsafe");
            }
            String operator = text(node, "operator");
            if (!SUPPORTED_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("unsupported filter operator: " + operator);
            }
            JsonNode value = node.get("value");
            if (value == null || value.isNull() || value.isContainerNode()) {
                throw new IllegalArgumentException("filter value must be scalar");
            }
            filters.add(new Filter(field, operator, value.asText()));
        }
        return List.copyOf(filters);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    public record CompiledBehaviorAudienceQuery(
            Long tenantId,
            String eventCode,
            int windowDays,
            String metric,
            String operator,
            String threshold,
            List<Filter> filters,
            int limit) {
    }

    public record Filter(String field, String operator, String value) {
    }
}
