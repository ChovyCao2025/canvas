package org.chovy.canvas.domain.risk.dsl;

/**
 * 风控规则分组逻辑。
 */
public enum RiskRuleLogic {
    /** 所有条件或子组均需匹配。 */
    AND,
    /** 任一条件或子组匹配即可。 */
    OR
}
