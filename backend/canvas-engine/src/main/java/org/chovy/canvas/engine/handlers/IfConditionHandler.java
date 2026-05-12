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

        boolean allMatch = rules == null || rules.stream().allMatch(r -> evaluate(r, ctx));
        return Mono.just(NodeResult.ifResult(allMatch, successNodeId, failNodeId));
    }

    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        String field    = (String) rule.get("field");
        String operator = (String) rule.get("operator");
        String expected = String.valueOf(rule.get("value"));
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
            default         -> false;
        };
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
