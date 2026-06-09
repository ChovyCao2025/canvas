package org.chovy.canvas.domain.risk.dsl;

import java.util.List;

/**
 * 风控规则校验结果。
 *
 * @param valid 是否通过校验
 * @param errors 校验错误列表
 */
public record RiskRuleValidationResult(
        boolean valid,
        List<RiskValidationError> errors
) {

    public RiskRuleValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
