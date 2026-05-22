package org.chovy.canvas.engine.audience;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("AVIATOR")
@RequiredArgsConstructor
public class AviatorRuleEvaluator implements RuleEvaluator {

    private final ObjectMapper objectMapper;

    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {
            });
            Map<String, Object> runtimeContext = new HashMap<>(context);
            String expression = toExpression(rule, runtimeContext);
            Object result = AviatorEvaluator.execute(expression, runtimeContext);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("[AUDIENCE][AVIATOR] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String toExpression(Map<String, Object> rule, Map<String, Object> context) {
        String logic = String.valueOf(rule.getOrDefault("logic", "AND"));
        String joinOp = "OR".equalsIgnoreCase(logic) ? " || " : " && ";
        List<String> parts = new ArrayList<>();

        List<Map<String, Object>> conditions = (List<Map<String, Object>>) rule.get("conditions");
        if (conditions != null) {
            for (Map<String, Object> condition : conditions) {
                parts.add(toConditionExpr(condition, context));
            }
        }

        List<Map<String, Object>> groups = (List<Map<String, Object>>) rule.get("groups");
        if (groups != null) {
            for (Map<String, Object> group : groups) {
                parts.add("(" + toExpression(group, context) + ")");
            }
        }

        return parts.isEmpty() ? "true" : String.join(joinOp, parts);
    }

    @SuppressWarnings("unchecked")
    private String toConditionExpr(Map<String, Object> condition, Map<String, Object> context) {
        String field = String.valueOf(condition.get("field"));
        String op = String.valueOf(condition.get("op"));
        Object value = condition.get("value");

        return switch (op) {
            case "=" -> field + " == " + quoteIfString(value);
            case "!=" -> field + " != " + quoteIfString(value);
            case ">" -> field + " > " + value;
            case ">=" -> field + " >= " + value;
            case "<" -> field + " < " + value;
            case "<=" -> field + " <= " + value;
            case "IN" -> {
                String listKey = field + "_list";
                context.put(listKey, value instanceof List<?> list ? list : List.of());
                yield "include(" + listKey + ", " + field + ")";
            }
            default -> "true";
        };
    }

    private String quoteIfString(Object value) {
        if (value instanceof String text) {
            return '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return String.valueOf(value);
    }
}
