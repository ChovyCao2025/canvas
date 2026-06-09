package org.chovy.canvas.engine.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.engine.context.ExecutionContext;
import org.chovy.canvas.engine.rule.RuleAstEvaluator;
import org.chovy.canvas.engine.rule.RuleParser;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 条件规则求值工具。
 *
 * <p>统一处理节点配置中的字段比较、包含判断、数值比较和多条件组合，供条件分支、接口响应校验等节点复用。
 * <p>工具类保持无状态，输入仅来自规则列表和上下文 Map，便于单元测试覆盖边界判断。
 */
final class ConditionEvaluator {
    private static final RuleParser RULE_PARSER = new RuleParser(new ObjectMapper());

    /**
     * 初始化 ConditionEvaluator 实例。
     */
    private ConditionEvaluator() {
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param rule rule 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param ctx ctx 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 的布尔判断结果。
     */
    static boolean evaluate(Map<String, Object> rule, ExecutionContext ctx) {
        return evaluate(rule, ctx::getContextValue);
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param rule rule 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 的布尔判断结果。
     */
    static boolean evaluate(Map<String, Object> rule, Map<String, Object> values) {
        return evaluate(rule, values::get);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ListMapString list map string 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @param rules rules 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @param ctx ctx 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @return 返回 all match 的布尔判断结果。
     */
    static boolean allMatch(List<Map<String, Object>> rules, ExecutionContext ctx) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, ctx));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param ListMapString list map string 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @param rules rules 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @param MapString map string 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @param values values 参数，用于 allMatch 流程中的校验、计算或对象转换。
     * @return 返回 all match 的布尔判断结果。
     */
    static boolean allMatch(List<Map<String, Object>> rules, Map<String, Object> values) {
        return rules == null || rules.stream().allMatch(rule -> evaluate(rule, values));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param MapString map string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param rule rule 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param FunctionString function string 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param resolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @return 返回 evaluate 的布尔判断结果。
     */
    private static boolean evaluate(Map<String, Object> rule, Function<String, Object> resolver) {
        if (rule == null) return false;
        try {
            return RuleAstEvaluator.matches(RULE_PARSER.parseCanvasCondition(rule), resolver);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
