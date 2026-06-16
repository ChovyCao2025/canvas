package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控名单类型。
 */
public enum RiskListType {
    /** 合规黑名单，优先级最高。 */
    /**
     * 表示 成员 枚举值。
     */
    COMPLIANCE_BLACK,
    /** 普通黑名单。 */
    /**
     * 表示 成员 枚举值。
     */
    BLACK,
    /** 白名单。 */
    /**
     * 表示 成员 枚举值。
     */
    WHITE,
    /** 灰名单，需要复核。 */
    /**
     * 表示 成员 枚举值。
     */
    GRAY,
    /** 观察名单，仅影子记录。 */
    OBSERVE
}
