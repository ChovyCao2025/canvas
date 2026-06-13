package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控规则组类型。
 */
public enum RiskRuleGroupType {
    /** 名单门禁规则组。 */
    LIST_GATE,
    /** 评分规则组。 */
    SCORING,
    /** 决策表规则组。 */
    DECISION_TABLE,
    /** 模型门禁规则组。 */
    MODEL_GATE,
    /** 硬规则组。 */
    HARD_RULE
}
