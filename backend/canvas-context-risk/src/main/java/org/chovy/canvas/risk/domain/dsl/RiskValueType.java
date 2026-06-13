package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则值类型。
 */
public enum RiskValueType {
    /** 字符串。 */
    STRING,
    /** 整数。 */
    INTEGER,
    /** 小数。 */
    DECIMAL,
    /** 布尔值。 */
    BOOLEAN,
    /** 日期时间。 */
    DATETIME,
    /** 字符串集合。 */
    STRING_SET,
    /** 数值集合。 */
    NUMBER_SET
}
