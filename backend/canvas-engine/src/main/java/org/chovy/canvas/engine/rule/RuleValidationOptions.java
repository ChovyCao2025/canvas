package org.chovy.canvas.engine.rule;

import java.util.Set;

/**
 * RuleValidationOptions 承载 engine.rule 场景中的不可变数据快照。
 * @param scope scope 字段。
 * @param allowEmpty allowEmpty 字段。
 * @param maxDepth maxDepth 字段。
 * @param maxConditions maxConditions 字段。
 * @param allowedFields allowedFields 字段。
 */
public record RuleValidationOptions(
        String scope,
        boolean allowEmpty,
        int maxDepth,
        int maxConditions,
        Set<String> allowedFields
) {
    /**
     * strict 处理 engine.rule 场景的业务逻辑。
     * @param scope scope 参数，用于 strict 流程中的校验、计算或对象转换。
     * @return 返回 strict 流程生成的业务结果。
     */
    public static RuleValidationOptions strict(String scope) {
        return new RuleValidationOptions(scope, false, 8, 100, Set.of());
    }

    /**
     * withAllowEmpty 处理 engine.rule 场景的业务逻辑。
     * @return 返回 withAllowEmpty 流程生成的业务结果。
     */
    public RuleValidationOptions withAllowEmpty() {
        return new RuleValidationOptions(scope, true, maxDepth, maxConditions, allowedFields);
    }

    /**
     * hasFieldWhitelist 校验或转换 engine.rule 场景的数据。
     * @return 返回布尔判断结果。
     */
    public boolean hasFieldWhitelist() {
        return allowedFields != null && !allowedFields.isEmpty();
    }
}
