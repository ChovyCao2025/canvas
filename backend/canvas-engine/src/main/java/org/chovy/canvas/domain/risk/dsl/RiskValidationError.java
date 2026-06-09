package org.chovy.canvas.domain.risk.dsl;

/**
 * 风控规则校验错误。
 *
 * @param path 错误所在 DSL 路径
 * @param code 错误码
 * @param message 错误说明
 */
public record RiskValidationError(
        String path,
        RiskValidationErrorCode code,
        String message
) {
}
