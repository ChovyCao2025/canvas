package org.chovy.canvas.engine.rule;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * RuleAstEvaluator 参与 engine.rule 场景的画布执行引擎处理。
 */
public final class RuleAstEvaluator {
    /**
     * 工具类不允许实例化。
     */
    private RuleAstEvaluator() {
    }

    /**
     * matches 处理 engine.rule 场景的业务逻辑。
     * @param node node 参数，用于 matches 流程中的校验、计算或对象转换。
     * @param resolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回布尔判断结果。
     */
    public static boolean matches(RuleNode node, Function<String, Object> resolver) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (node instanceof RuleGroup group) {
            if (group.children().isEmpty()) {
                return true;
            }
            if (group.logic() == RuleLogic.OR) {
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                return group.children().stream().anyMatch(child -> matches(child, resolver));
            }
            return group.children().stream().allMatch(child -> matches(child, resolver));
        }
        if (node instanceof RuleCondition condition) {
            return matchesCondition(condition, resolver);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * 判断单条规则条件是否命中。
     *
     * @param condition 规则条件
     * @param resolver 字段值解析器
     * @return true 表示条件命中
     */
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

    /**
     * 解析规则期望值中的上下文引用。
     *
     * @param value 原始期望值
     * @param resolver 字段值解析器
     * @return 解析后的期望值
     */
    private static Object resolveExpected(Object value, Function<String, Object> resolver) {
        if (value instanceof String text && text.startsWith("${") && text.endsWith("}")) {
            return resolver.apply(text.substring(2, text.length() - 1));
        }
        return value;
    }

    /**
     * 判断字符串是否包含期望值。
     *
     * @param actual 实际字符串
     * @param expected 期望字符串，可包含逗号分隔候选值
     * @return true 表示包含或命中候选值
     */
    private static boolean contains(String actual, String expected) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (expected.contains(",")) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) {
                    return true;
                }
            }
            return false;
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return actual.contains(expected);
    }

    /**
     * 判断实际值是否属于期望集合。
     *
     * @param actualValue 实际值
     * @param expectedValue 期望集合或逗号分隔字符串
     * @return true 表示实际值在期望范围内
     */
    private static boolean in(Object actualValue, Object expectedValue) {
        String actual = stringify(actualValue);
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (expectedValue instanceof Collection<?> collection) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return actual.equals(expected);
    }

    /**
     * 按数字大小比较实际值和期望值。
     *
     * @param actual 实际值字符串
     * @param expected 期望值字符串
     * @param comparison 比较结果谓词
     * @return true 表示数字比较成立
     */
    private static boolean compareNumbers(String actual, String expected, IntPredicate comparison) {
        try {
            int result = new BigDecimal(actual).compareTo(new BigDecimal(expected));
            return comparison.test(result);
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断值是否为空。
     *
     * @param value 待判断值
     * @return true 表示 null、空字符串、空集合或空 Map
     */
    private static boolean isEmpty(Object value) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (value == null) return true;
        if (value instanceof CharSequence text) return text.isEmpty();
        if (value instanceof Collection<?> collection) return collection.isEmpty();
        if (value instanceof java.util.Map<?, ?> map) return map.isEmpty();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return false;
    }

    /**
     * 将值转换为规则比较用字符串。
     *
     * @param value 原始值
     * @return 字符串值，null 转为字面量 null
     */
    private static String stringify(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
