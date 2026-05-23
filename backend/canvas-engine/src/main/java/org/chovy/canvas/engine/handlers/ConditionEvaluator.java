package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

final class ConditionEvaluator {
    private ConditionEvaluator() {
    }

    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return evaluate(rule, ctx::getContextValue);
    }

    static boolean evaluate(Map<String, Object> rule, Map<String, Object> values) {
        return evaluate(rule, values::get);
    }

    static boolean allMatch(List<Map<String, Object>> rules, ExecutionContext ctx) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, ctx));
    }

    static boolean allMatch(List<Map<String, Object>> rules, Map<String, Object> values) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, values));
    }

    private static boolean evaluate(Map<String, Object> rule, Function<String, Object> resolver) {
        if (rule == null) return false;
        String field = string(rule.get("field"));
        String operator = string(rule.get("operator"));
        if (field.isBlank() || operator.isBlank()) return false;

        String actual = stringify(resolver.apply(field));
        String expected = resolveExpected(rule.get("value"), resolver);

        return switch (operator) {
            case "EQ" -> Objects.equals(actual, expected);
            case "NEQ" -> !Objects.equals(actual, expected);
            case "CONTAINS" -> containsCheck(actual, expected);
            case "GT" -> compareNumbers(actual, expected, comparison -> comparison > 0);
            case "LT" -> compareNumbers(actual, expected, comparison -> comparison < 0);
            case "GTE" -> compareNumbers(actual, expected, comparison -> comparison >= 0);
            case "LTE" -> compareNumbers(actual, expected, comparison -> comparison <= 0);
            default -> false;
        };
    }

    private static String resolveExpected(Object rawValue, Function<String, Object> resolver) {
        String raw = stringify(rawValue);
        if (raw.startsWith("${") && raw.endsWith("}")) {
            String fieldKey = raw.substring(2, raw.length() - 1);
            return stringify(resolver.apply(fieldKey));
        }
        return raw;
    }

    private static boolean containsCheck(String actual, String expected) {
        if (expected.contains(",")) {
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) return true;
            }
            return false;
        }
        return actual.contains(expected);
    }

    private static boolean compareNumbers(String actual, String expected, IntPredicate matcher) {
        try {
            return matcher.test(new BigDecimal(actual).compareTo(new BigDecimal(expected)));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String stringify(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
