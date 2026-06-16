package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控策略编译错误码。
 */
public enum RiskStrategyCompileErrorCode {
    /** 动作未知。 */
    /**
     * 表示 成员 枚举值。
     */
    UNKNOWN_ACTION,
    /** 规则组策略未知。 */
    /**
     * 表示 成员 枚举值。
     */
    UNKNOWN_GROUP_POLICY,
    /** 规则数量超过限制。 */
    /**
     * 表示 成员 枚举值。
     */
    RULE_LIMIT_EXCEEDED,
    /** 规则组数量超过限制。 */
    /**
     * 表示 成员 枚举值。
     */
    GROUP_LIMIT_EXCEEDED,
    /** 依赖特征数量超过限制。 */
    /**
     * 表示 成员 枚举值。
     */
    FEATURE_LIMIT_EXCEEDED,
    /** 安全表达式数量超过限制。 */
    /**
     * 表示 成员 枚举值。
     */
    SAFE_EXPRESSION_LIMIT_EXCEEDED,
    /** 已编译表达式大小超过限制。 */
    /**
     * 表示 成员 枚举值。
     */
    COMPILED_EXPRESSION_BUDGET_EXCEEDED,
    /** DSL 无效。 */
    INVALID_DSL
}
