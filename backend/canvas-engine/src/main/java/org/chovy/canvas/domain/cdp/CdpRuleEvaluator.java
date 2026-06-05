package org.chovy.canvas.domain.cdp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class CdpRuleEvaluator {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;

    public record Evaluation(boolean matched, Object value) {
    }

    public Evaluation evaluate(String expressionJson, Map<String, Object> properties) {
        Map<String, Object> expression = readExpression(expressionJson);
        String field = requireText((String) expression.get("field"), "field");
        String op = requireText((String) expression.get("op"), "op");
        Object actual = readPath(properties, field);
        Object expected = expression.get("value");
        boolean matched = compare(actual, op, expected);
        Object value = matched ? expression.getOrDefault("then", Boolean.TRUE) : null;
        return new Evaluation(matched, value);
    }

    public void validate(String expressionJson) {
        Map<String, Object> expression = readExpression(expressionJson);
        requireText((String) expression.get("field"), "field");
        String op = requireText((String) expression.get("op"), "op");
        if (!isSupportedOperator(op)) {
            throw new IllegalArgumentException("unsupported rule operator: " + op);
        }
    }

    public Map<String, Object> readProperties(String propertiesJson) {
        if (propertiesJson == null || propertiesJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(propertiesJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("profile properties JSON is invalid", e);
        }
    }

    public String writeProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("profile properties JSON is invalid", e);
        }
    }

    private Map<String, Object> readExpression(String expressionJson) {
        if (expressionJson == null || expressionJson.isBlank()) {
            throw new IllegalArgumentException("expressionJson cannot be blank");
        }
        try {
            return objectMapper.readValue(expressionJson, MAP_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("expressionJson is invalid", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object readPath(Map<String, Object> properties, String path) {
        Object current = properties;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = ((Map<String, Object>) currentMap).get(part);
        }
        return current;
    }

    private boolean compare(Object actual, String op, Object expected) {
        if (!isSupportedOperator(op)) {
            throw new IllegalArgumentException("unsupported rule operator: " + op);
        }
        return switch (op) {
            case ">=" -> asDouble(actual) >= asDouble(expected);
            case ">" -> asDouble(actual) > asDouble(expected);
            case "<=" -> asDouble(actual) <= asDouble(expected);
            case "<" -> asDouble(actual) < asDouble(expected);
            case "==" -> Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "!=" -> !Objects.equals(String.valueOf(actual), String.valueOf(expected));
            case "contains" -> actual != null && expected != null && String.valueOf(actual).contains(String.valueOf(expected));
            default -> false;
        };
    }

    private boolean isSupportedOperator(String op) {
        return ">=".equals(op) || ">".equals(op) || "<=".equals(op) || "<".equals(op)
                || "==".equals(op) || "!=".equals(op) || "contains".equals(op);
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("rule value is not numeric: " + value);
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value.trim();
    }
}
