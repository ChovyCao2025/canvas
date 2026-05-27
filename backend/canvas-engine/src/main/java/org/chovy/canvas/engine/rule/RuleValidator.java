package org.chovy.canvas.engine.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RuleValidator {

    private static final String FIELD_PATTERN = "[A-Za-z_][A-Za-z0-9_.-]*";

    public void validateOrThrow(RuleGroup rule, RuleValidationOptions options) {
        List<String> errors = validate(rule, options);
        if (!errors.isEmpty()) {
            throw new RuleValidationException(String.join("; ", errors));
        }
    }

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

    private void validateNode(RuleNode node,
                              RuleValidationOptions options,
                              List<String> errors,
                              int depth,
                              int[] conditionCount) {
        if (depth > options.maxDepth()) {
            errors.add(options.scope() + ": 规则嵌套深度超过 " + options.maxDepth());
            return;
        }
        if (node instanceof RuleGroup group) {
            for (RuleNode child : group.children()) {
                validateNode(child, options, errors, depth + 1, conditionCount);
            }
            return;
        }
        if (node instanceof RuleCondition condition) {
            conditionCount[0]++;
            if (conditionCount[0] > options.maxConditions()) {
                errors.add(options.scope() + ": 条件数量超过 " + options.maxConditions());
                return;
            }
            validateCondition(condition, options, errors);
        }
    }

    private void validateCondition(RuleCondition condition,
                                   RuleValidationOptions options,
                                   List<String> errors) {
        String field = condition.field();
        if (field == null || field.isBlank()) {
            errors.add(options.scope() + ": 字段不能为空");
        } else if (!field.matches(FIELD_PATTERN)) {
            errors.add(options.scope() + ": 非法字段 " + field);
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

    private boolean isNumber(Object value) {
        if (value instanceof Number) return true;
        try {
            new BigDecimal(String.valueOf(value));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean isResolvableReference(Object value) {
        return value instanceof String text && text.startsWith("${") && text.endsWith("}");
    }

    private boolean isBlankValue(Object value) {
        if (value == null) return true;
        if (value instanceof java.util.Collection<?> collection) return collection.isEmpty();
        return String.valueOf(value).isBlank();
    }
}
