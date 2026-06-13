package org.chovy.canvas.execution.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
@NodeHandlerType("IF_CONDITION")
public class IfConditionNodeHandler implements NodeHandler {

    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        List<Map<String, Object>> rules = NodeHandlerSupport.listOfMaps(context.node().config().get("rules"));
        boolean passed = rules.isEmpty() || rules.stream().allMatch(rule -> evaluate(rule, context));
        String target = passed
                ? firstConfigured(context.node().config(), "successNodeId", "nextNodeId")
                : NodeHandlerSupport.string(context.node().config().get("failNodeId"), null);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("passed", passed);
        output.put("matchedRules", passed ? rules.size() : 0);

        if (target == null) {
            return NodeExecutionResult.success(output);
        }
        return NodeExecutionResult.routed(output, Map.of(passed ? "success" : "fail", target));
    }

    private boolean evaluate(Map<String, Object> rule, NodeExecutionContext context) {
        String field = NodeHandlerSupport.string(rule.getOrDefault("field", rule.get("key")), null);
        Object actual = NodeHandlerSupport.resolve(context, field);
        Object expected = rule.getOrDefault("value", rule.get("expected"));
        String operator = NodeHandlerSupport.upper(rule.getOrDefault("operator", rule.get("op")), "EQ");

        return switch (operator) {
            case "EQ", "=", "==" -> equalsValue(actual, expected);
            case "NE", "!=", "<>" -> !equalsValue(actual, expected);
            case "GT", ">" -> compare(actual, expected) > 0;
            case "GTE", ">=" -> compare(actual, expected) >= 0;
            case "LT", "<" -> compare(actual, expected) < 0;
            case "LTE", "<=" -> compare(actual, expected) <= 0;
            case "IN", "CONTAINS" -> NodeHandlerSupport.collectionContains(expected, actual);
            case "EXISTS" -> actual != null;
            default -> false;
        };
    }

    private boolean equalsValue(Object actual, Object expected) {
        Number actualNumber = NodeHandlerSupport.number(actual);
        Number expectedNumber = NodeHandlerSupport.number(expected);
        if (actualNumber != null && expectedNumber != null) {
            return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue()) == 0;
        }
        return Objects.equals(actual, expected)
                || (actual != null && expected != null && String.valueOf(actual).equals(String.valueOf(expected)));
    }

    private int compare(Object actual, Object expected) {
        Number actualNumber = NodeHandlerSupport.number(actual);
        Number expectedNumber = NodeHandlerSupport.number(expected);
        if (actualNumber == null || expectedNumber == null) {
            return -1;
        }
        return Double.compare(actualNumber.doubleValue(), expectedNumber.doubleValue());
    }

    private String firstConfigured(Map<String, Object> config, String first, String second) {
        String value = NodeHandlerSupport.string(config.get(first), null);
        return value == null ? NodeHandlerSupport.string(config.get(second), null) : value;
    }
}
