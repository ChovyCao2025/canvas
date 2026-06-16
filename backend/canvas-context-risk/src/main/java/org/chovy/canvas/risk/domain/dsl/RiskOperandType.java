package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则操作数来源类型。
 */
public enum RiskOperandType {
    /** 来自在线或请求覆盖的特征。 */
    /**
     * 表示 成员 枚举值。
     */
    FEATURE,
    /** 规则中声明的可信字面量。 */
    /**
     * 表示 成员 枚举值。
     */
    LITERAL,
    /** 受治理的风控名单引用。 */
    /**
     * 表示 成员 枚举值。
     */
    LIST,
    /** 决策上下文字段。 */
    /**
     * 表示 成员 枚举值。
     */
    CONTEXT,
    /** 事件事实字段。 */
    /**
     * 表示 成员 枚举值。
     */
    EVENT,
    /** 主体标识字段。 */
    SUBJECT
}
