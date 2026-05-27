package org.chovy.canvas.engine.handlers;

import org.chovy.canvas.engine.context.ExecutionContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntPredicate;

/**
 * 条件规则求值工具。
 *
 * <p>统一处理节点配置中的字段比较、包含判断、数值比较和多条件组合，供条件分支、接口响应校验等节点复用。
 * <p>工具类保持无状态，输入仅来自规则列表和上下文 Map，便于单元测试覆盖边界判断。
 */
final class ConditionEvaluator {
    /**
     * 构造 ConditionEvaluator 实例。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     */
    private ConditionEvaluator() {
    }

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return evaluate(rule, ctx::getContextValue);
    }

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param values values 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    static boolean evaluate(Map<String, Object> rule, Map<String, Object> values) {
        return evaluate(rule, values::get);
    }

    /**
     * 执行 all Match 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rules rules 方法执行所需的业务参数
     * @param ctx 执行上下文，提供当前画布、用户和节点运行态数据
     * @return 判断结果，true 表示校验通过或条件成立
     */
    static boolean allMatch(List<Map<String, Object>> rules, ExecutionContext ctx) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, ctx));
    }

    /**
     * 执行 all Match 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rules rules 方法执行所需的业务参数
     * @param values values 待写入、比较或转换的业务值
     * @return 判断结果，true 表示校验通过或条件成立
     */
    static boolean allMatch(List<Map<String, Object>> rules, Map<String, Object> values) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, values));
    }

    /**
     * 执行 evaluate 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rule rule 方法执行所需的业务参数
     * @param resolver resolver 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean evaluate(Map<String, Object> rule, Function<String, Object> resolver) {
        if (rule == null) return false;
        String field = string(rule.get("field"));
        String operator = string(rule.get("operator"));
        if (field.isBlank() || operator.isBlank()) return false;

        String actual = stringify(resolver.apply(field));
        String expected = resolveExpected(rule.get("value"), resolver);

        // operator 只接受白名单值，未知操作符直接判 false，避免配置错误误放行。
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

    /**
     * 构建、解析或转换 resolve Expected 相关的业务数据。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param rawValue rawValue 待写入、比较或转换的业务值
     * @param resolver resolver 方法执行所需的业务参数
     * @return 转换或查询得到的字符串结果
     */
    private static String resolveExpected(Object rawValue, Function<String, Object> resolver) {
        String raw = stringify(rawValue);
        if (raw.startsWith("${") && raw.endsWith("}")) {
            // 期望值支持 ${field} 引用另一个上下文字段，实现字段间比较。
            String fieldKey = raw.substring(2, raw.length() - 1);
            return stringify(resolver.apply(fieldKey));
        }
        return raw;
    }

    /**
     * 执行 contains Check 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param actual actual 方法执行所需的业务参数
     * @param expected expected 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean containsCheck(String actual, String expected) {
        if (expected.contains(",")) {
            // 逗号分隔期望值表示枚举集合，要求 actual 精确等于其中一个候选。
            for (String candidate : expected.split(",")) {
                if (actual.equals(candidate.trim())) return true;
            }
            return false;
        }
        return actual.contains(expected);
    }

    /**
     * 执行 compare Numbers 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param actual actual 方法执行所需的业务参数
     * @param expected expected 方法执行所需的业务参数
     * @param matcher matcher 方法执行所需的业务参数
     * @return 判断结果，true 表示校验通过或条件成立
     */
    private static boolean compareNumbers(String actual, String expected, IntPredicate matcher) {
        try {
            return matcher.test(new BigDecimal(actual).compareTo(new BigDecimal(expected)));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 执行 stringify 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private static String stringify(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    /**
     * 执行 string 对应的业务逻辑。
     *
     * <p>方法会结合入参、当前对象状态和依赖组件完成处理，调用方需关注返回值以及可能产生的状态变更。
     *
     * @param value value 待写入、比较或转换的业务值
     * @return 转换或查询得到的字符串结果
     */
    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
