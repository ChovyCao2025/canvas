package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.handler.NodeHandler;
import org.chovy.canvas.engine.handler.NodeHandlerType;
import org.springframework.stereotype.Component;
import org.chovy.canvas.engine.handler.NodeResult;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * IF 判断节点：所有 rules 均满足 → successNodeId，否则 → failNodeId
 *
 * 规则语义：
 * - 多条规则之间是 AND 关系（allMatch）；
 * - 每条规则支持上下文字段与常量比较。
 */
@Component
@NodeHandlerType("IF_CONDITION")
public class IfConditionHandler implements NodeHandler {

    @Override
    @SuppressWarnings("unchecked")
    public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
        List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
        String successNodeId = (String) config.get("successNodeId");
        String failNodeId    = (String) config.get("failNodeId");

        // rules 为空时按“无约束”为 true，直接走 success 分支
        boolean allMatch = rules == null || rules.stream().allMatch(r -> evaluate(r, ctx));
        return Mono.just(NodeResult.ifResult(allMatch, successNodeId, failNodeId));
    }

    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        String field    = (String) rule.get("field");
        String operator = (String) rule.get("operator");
        // 支持上下文字段引用：value 以 "${" 开头时，从 ctx 读取实际值
        String rawValue = String.valueOf(rule.get("value"));
        String expected = resolveValue(rawValue, ctx);
        Object raw      = ctx.getContextValue(field);
        String actual   = raw == null ? "null" : String.valueOf(raw);

        return switch (operator) {
            case "EQ"       -> actual.equals(expected);
            case "NEQ"      -> !actual.equals(expected);
            case "CONTAINS" -> containsCheck(actual, expected);
            case "GT"       -> numericCompare(actual, expected) > 0;
            case "LT"       -> numericCompare(actual, expected) < 0;
            case "GTE"      -> numericCompare(actual, expected) >= 0;
            case "LTE"      -> numericCompare(actual, expected) <= 0;
            // 未知操作符视为不匹配，避免误放行
            default         -> false;
        };
    }

    /**
     * 解析 value：若以 ${...} 包裹，从 ctx 读取上下文字段值。
     * 例：${orderId} → ctx.getContextValue("orderId")
     */
    private static String resolveValue(String raw, ExecutionContext ctx) {
        if (raw != null && raw.startsWith("${") && raw.endsWith("}")) {
            String fieldKey = raw.substring(2, raw.length() - 1);
            Object v = ctx.getContextValue(fieldKey);
            return v == null ? "null" : String.valueOf(v);
        }
        return raw;
    }

    /** CONTAINS：expected 含逗号则为 IN 语义，否则为子串匹配 */
    private static boolean containsCheck(String actual, String expected) {
        if (expected.contains(",")) {
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) return true;
            }
            return false;
        }
        return actual.contains(expected);
    }

    /**
     * 数值比较：优先 BigDecimal 解析，失败则字典序（防止 "10" < "9" 的字典序错误）
     */
    static int numericCompare(String a, String b) {
        try {
            return new BigDecimal(a).compareTo(new BigDecimal(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
