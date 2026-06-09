package org.chovy.canvas.domain.risk.runtime;

/**
 * 风控规则命中明细。
 *
 * @param groupKey 规则组键
 * @param ruleKey 规则键
 * @param action 命中动作
 * @param scoreDelta 分数增量
 * @param reasonCode 原因码
 * @param shadow 是否影子命中
 */
public record RiskDecisionRuleHit(
        String groupKey,
        String ruleKey,
        RiskDecisionAction action,
        int scoreDelta,
        String reasonCode,
        boolean shadow
) {
}
