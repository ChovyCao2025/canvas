package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控运行时依赖失败策略。
 */
public enum RiskFailPolicy {
    /** 失败放行。 */
    /**
     * 表示 成员 枚举值。
     */
    FAIL_OPEN,
    /** 失败转人工复核。 */
    /**
     * 表示 成员 枚举值。
     */
    FAIL_REVIEW,
    /** 失败阻断。 */
    FAIL_CLOSED
}
