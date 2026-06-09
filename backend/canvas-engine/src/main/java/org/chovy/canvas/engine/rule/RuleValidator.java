package org.chovy.canvas.engine.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * RuleValidator 参与 engine.rule 场景的画布执行引擎处理。
 */
public class RuleValidator {

    private static final String FIELD_PATTERN = "[A-Za-z_][A-Za-z0-9_.-]*";

    /**
     * validateOrThrow 校验或转换 engine.rule 场景的数据。
     * @param rule rule 参数，用于 validateOrThrow 流程中的校验、计算或对象转换。
     * @param options options 参数，用于 validateOrThrow 流程中的校验、计算或对象转换。
     */
    public void validateOrThrow(RuleGroup rule, RuleValidationOptions options) {
        List<String> errors = validate(rule, options);
        if (!errors.isEmpty()) {
            throw new RuleValidationException(String.join("; ", errors));
        }
    }

    /**
     * validate 校验或转换 engine.rule 场景的数据。
     * @param rule rule 参数，用于 validate 流程中的校验、计算或对象转换。
     * @param options options 参数，用于 validate 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    public List<String> validate(RuleGroup rule, RuleValidationOptions options) {
        RuleValidationOptions resolved = options == null ? RuleValidationOptions.strict("rule") : options;
        List<String> errors = new ArrayList<>();
        if (rule == null) {
            errors.add(resolved.scope() + ": 规则不能为空");
            return errors;
        }
        if (rule.isEmpty() && !rule.explicitMatchAll() && !resolved.allowEmpty()) {
            errors.add(resolved.scope() + ": 规则不能为空；如需全量匹配请显式设置 matchAll=true");
        }
        validateNode(rule, resolved, errors, 1, new int[]{0});
        return errors;
    }

    /**
     * 递归校验规则节点。
     *
     * @param node 当前规则节点
     * @param options 校验选项
     * @param errors 错误收集列表
     * @param depth 当前递归深度
     * @param conditionCount 条件数量计数器
     */
    private void validateNode(RuleNode node,
                              RuleValidationOptions options,
                              List<String> errors,
                              int depth,
                              int[] conditionCount) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (depth > options.maxDepth()) {
            errors.add(options.scope() + ": 规则嵌套深度超过 " + options.maxDepth());
            return;
        }
        if (node instanceof RuleGroup group) {
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            for (RuleNode child : group.children()) {
                validateNode(child, options, errors, depth + 1, conditionCount);
            }
            return;
        }
        if (node instanceof RuleCondition condition) {
            conditionCount[0]++;
            if (conditionCount[0] > options.maxConditions()) {
                errors.add(options.scope() + ": 条件数量超过 " + options.maxConditions());
                // 汇总前面计算出的状态和明细，返回给调用方。
                return;
            }
            validateCondition(condition, options, errors);
        }
    }

    /**
     * 校验单条规则条件。
     *
     * @param condition 规则条件
     * @param options 校验选项
     * @param errors 错误收集列表
     */
    private void validateCondition(RuleCondition condition,
                                   RuleValidationOptions options,
                                   List<String> errors) {
        String field = condition.field();
        if (field == null || field.isBlank()) {
            errors.add(options.scope() + ": 字段不能为空");
        // 根据前序判断结果进入后续条件分支。
        } else if (!field.matches(FIELD_PATTERN)) {
            errors.add(options.scope() + ": 非法字段 " + field);
        // 根据前序判断结果进入后续条件分支。
        } else if (options.hasFieldWhitelist() && !options.allowedFields().contains(field)) {
            errors.add(options.scope() + ": 未知字段 " + field);
        }

        if (condition.operator() == null) {
            errors.add(options.scope() + ": 操作符不能为空");
            return;
        }
        if (condition.operator().isOrderComparison() && !isResolvableReference(condition.value()) && !isNumber(condition.value())) {
            errors.add(options.scope() + ": 数值比较的期望值必须是数字或上下文引用");
        }
        if (condition.operator() == RuleOperator.IN && isBlankValue(condition.value())) {
            errors.add(options.scope() + ": IN 操作符必须配置非空列表");
        }
    }

    /**
     * 判断值是否可解析为数字。
     *
     * @param value 待判断值
     * @return true 表示可用于大小比较
     */
    private boolean isNumber(Object value) {
        if (value instanceof Number) return true;
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        // 捕获异常并转为业务兜底处理，避免异常扩散到主流程。
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断值是否为可在运行时解析的上下文引用。
     *
     * @param value 待判断值
     * @return true 表示形如 ${field} 的引用
     */
    private boolean isResolvableReference(Object value) {
        return value instanceof String text && text.startsWith("${") && text.endsWith("}");
    }

    /**
     * 判断规则期望值是否为空。
     *
     * @param value 待判断值
     * @return true 表示空值、空集合或空白字符串
     */
    private boolean isBlankValue(Object value) {
        if (value == null) return true;
        if (value instanceof java.util.Collection<?> collection) return collection.isEmpty();
        return String.valueOf(value).isBlank();
    }
}
