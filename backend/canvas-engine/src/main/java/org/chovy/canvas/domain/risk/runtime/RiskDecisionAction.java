package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控决策动作。
 */
public enum RiskDecisionAction {
    /** 放行。 */
    ALLOW,
    /** 转人工复核。 */
    REVIEW,
    /** 要求二次验证。 */
    VERIFY,
    /** 阻断。 */
    BLOCK,
    /** 延迟处理。 */
    DELAY,
    /** 限流或限额。 */
    LIMIT,
    /** 仅影子建议，不改变实际路由。 */
    SHADOW_ONLY
}
