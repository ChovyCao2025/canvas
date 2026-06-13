package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则操作数来源类型。
 */
public enum RiskOperandType {
    /** 来自在线或请求覆盖的特征。 */
    FEATURE,
    /** 规则中声明的可信字面量。 */
    LITERAL,
    /** 受治理的风控名单引用。 */
    LIST,
    /** 决策上下文字段。 */
    CONTEXT,
    /** 事件事实字段。 */
    EVENT,
    /** 主体标识字段。 */
    SUBJECT
}
