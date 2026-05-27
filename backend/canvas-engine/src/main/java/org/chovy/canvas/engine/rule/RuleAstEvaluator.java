package org.chovy.canvas.engine.rule;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

public final class RuleAstEvaluator {
    private RuleAstEvaluator() {
    }

    public static boolean matches(RuleNode node, Function<String, Object> resolver) {
        if (node instanceof RuleGroup group) {
            if (group.children().isEmpty()) {
                return true;
            }
            if (group.logic() == RuleLogic.OR) {
                return group.children().stream().anyMatch(child -> matches(child, resolver));
            }
            return group.children().stream().allMatch(child -> matches(child, resolver));
        }
        if (node instanceof RuleCondition condition) {
            return matchesCondition(condition, resolver);
        }
        return false;
    }

    private static boolean matchesCondition(RuleCondition condition, Function<String, Object> resolver) {
        Object actualValue = resolver.apply(condition.field());
        Object expectedValue = resolveExpected(condition.value(), resolver);
        String actual = stringify(actualValue);
        String expected = stringify(expectedValue);

        return switch (condition.operator()) {
            case EQ -> Objects.equals(actual, expected);
            case NEQ -> !Objects.equals(actual, expected);
            case CONTAINS -> contains(actual, expected);
            case IN -> in(actualValue, expectedValue);
            case GT -> compareNumbers(actual, expected, result -> result > 0);
            case GTE -> compareNumbers(actual, expected, result -> result >= 0);
            case LT -> compareNumbers(actual, expected, result -> result < 0);
            case LTE -> compareNumbers(actual, expected, result -> result <= 0);
            case EXISTS -> actualValue != null;
            case IS_EMPTY -> isEmpty(actualValue);
        };
    }

    private static Object resolveExpected(Object value, Function<String, Object> resolver) {
        if (value instanceof String text && text.startsWith("${") && text.endsWith("}")) {
            return resolver.apply(text.substring(2, text.length() - 1));
        }
        return value;
    }

    private static boolean contains(String actual, String expected) {
        if (expected.contains(",")) {
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) {
                    return true;
                }
            }
            return false;
        }
        return actual.contains(expected);
    }

    private static boolean in(Object actualValue, Object expectedValue) {
        String actual = stringify(actualValue);
        if (expectedValue instanceof Collection<?> collection) {
            return collection.stream().map(RuleAstEvaluator::stringify).anyMatch(actual::equals);
        }
        String expected = stringify(expectedValue);
        if (expected.contains(",")) {
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) {
                    return true;
                }
            }
        }
        return actual.equals(expected);
    }

    private static boolean compareNumbers(String actual, String expected, IntPredicate comparison) {
        try {
            int result = new BigDecimal(actual).compareTo(new BigDecimal(expected));
            return comparison.test(result);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof CharSequence text) return text.isEmpty();
        if (value instanceof Collection<?> collection) return collection.isEmpty();
        if (value instanceof java.util.Map<?, ?> map) return map.isEmpty();
        return false;
    }

    private static String stringify(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
