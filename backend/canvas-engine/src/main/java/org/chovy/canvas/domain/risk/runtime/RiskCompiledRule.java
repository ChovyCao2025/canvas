package org.chovy.canvas.domain.risk.runtime;

import org.chovy.canvas.domain.risk.dsl.RiskRuleGroupNode;

/**
 * 已编译的单条风控规则。
 *
 * @param groupKey 所属规则组键
 * @param ruleKey 规则键
 * @param rule 解析后的规则 DSL 节点
 * @param action 规则命中后的建议动作
 * @param scoreDelta 规则命中后的分数增量
 * @param reasonCode 命中原因码
 * @param shadowRule 是否为影子规则
 */
public record RiskCompiledRule(
        String groupKey,
        String ruleKey,
        RiskRuleGroupNode rule,
        RiskDecisionAction action,
        int scoreDelta,
        String reasonCode,
        boolean shadowRule
) {

    /**
     * 返回影子规则副本。
     */
    public RiskCompiledRule shadow() {
        return new RiskCompiledRule(groupKey, ruleKey, rule, action, scoreDelta, reasonCode, true);
    }
}
