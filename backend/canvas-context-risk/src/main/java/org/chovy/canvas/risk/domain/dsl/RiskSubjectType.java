package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控主体标识类型。
 */
public enum RiskSubjectType {
    /** 用户编号。 */
    USER_ID,
    /** 设备编号。 */
    DEVICE_ID,
    /** IP 地址。 */
    IP,
    /** 邮箱地址。 */
    EMAIL,
    /** 手机号。 */
    PHONE,
    /** 卡号。 */
    CARD,
    /** 通用主体标识。 */
    GENERIC
}
