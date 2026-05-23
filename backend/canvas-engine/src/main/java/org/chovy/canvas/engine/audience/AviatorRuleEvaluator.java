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

/**
 * 基于 Aviator 的人群规则求值器。
 *
 * <p>将规则树转成 Aviator 表达式执行，适合在线快速判断场景。
 */
@Slf4j
@Component("AVIATOR")
@RequiredArgsConstructor
public class AviatorRuleEvaluator implements RuleEvaluator {

    /** 规则 JSON 解析器。 */
    private final ObjectMapper objectMapper;

    @Override
    public boolean evaluate(String ruleJson, Map<String, Object> context) {
        try {
            Map<String, Object> rule = objectMapper.readValue(ruleJson, new TypeReference<>() {
            });
            // 运行态上下文会在 IN 操作符场景附加临时 list 变量
            Map<String, Object> runtimeContext = new HashMap<>(context);
            String expression = toExpression(rule, runtimeContext);
            // Aviator 求值结果约定应为 Boolean
            Object result = AviatorEvaluator.execute(expression, runtimeContext);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            // 引擎异常统一按“未命中”处理，避免影响整体流程稳定性
            log.error("[AUDIENCE][AVIATOR] evaluate failed: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private String toExpression(Map<String, Object> rule, Map<String, Object> context) {
        // 每个分组通过 logic 决定条件拼接关系（AND/OR）
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
                // Aviator include(list, value) 语义：value 是否在 list 中
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
