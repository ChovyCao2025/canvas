package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控策略编译错误码。
 */
public enum RiskStrategyCompileErrorCode {
    /** 动作未知。 */
    UNKNOWN_ACTION,
    /** 规则组策略未知。 */
    UNKNOWN_GROUP_POLICY,
    /** 规则数量超过限制。 */
    RULE_LIMIT_EXCEEDED,
    /** 规则组数量超过限制。 */
    GROUP_LIMIT_EXCEEDED,
    /** 依赖特征数量超过限制。 */
    FEATURE_LIMIT_EXCEEDED,
    /** 安全表达式数量超过限制。 */
    SAFE_EXPRESSION_LIMIT_EXCEEDED,
    /** 已编译表达式大小超过限制。 */
    COMPILED_EXPRESSION_BUDGET_EXCEEDED,
    /** DSL 无效。 */
    INVALID_DSL
}
