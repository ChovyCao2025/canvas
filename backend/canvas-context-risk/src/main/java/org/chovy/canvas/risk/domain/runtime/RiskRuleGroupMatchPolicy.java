package org.chovy.canvas.risk.domain.runtime;

/**
 * 风控规则组命中策略。
 */
public enum RiskRuleGroupMatchPolicy {
    /** 任一规则命中即可。 */
    ANY_MATCHED,
    /** 所有规则均需命中。 */
    ALL_MATCHED,
    /** 只采用首个命中规则。 */
    FIRST_MATCH,
    /** 按权重或分值汇总。 */
    WEIGHTED_SCORE
}
