package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控名单类型。
 */
public enum RiskListType {
    /** 合规黑名单，优先级最高。 */
    COMPLIANCE_BLACK,
    /** 普通黑名单。 */
    BLACK,
    /** 白名单。 */
    WHITE,
    /** 灰名单，需要复核。 */
    GRAY,
    /** 观察名单，仅影子记录。 */
    OBSERVE
}
