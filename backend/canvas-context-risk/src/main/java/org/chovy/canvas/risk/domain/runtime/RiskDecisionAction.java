package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控决策动作。
 */
public enum RiskDecisionAction {
    /** 放行。 */
    /**
     * 表示 成员 枚举值。
     */
    ALLOW,
    /** 转人工复核。 */
    /**
     * 表示 成员 枚举值。
     */
    REVIEW,
    /** 要求二次验证。 */
    /**
     * 表示 成员 枚举值。
     */
    VERIFY,
    /** 阻断。 */
    /**
     * 表示 成员 枚举值。
     */
    BLOCK,
    /** 延迟处理。 */
    /**
     * 表示 成员 枚举值。
     */
    DELAY,
    /** 限流或限额。 */
    /**
     * 表示 成员 枚举值。
     */
    LIMIT,
    /** 仅影子建议，不改变实际路由。 */
    SHADOW_ONLY
}
