package org.chovy.canvas.risk.domain.dsl;

/**
 * RiskRuleNode 承载对应领域的业务规则、流程编排和结果转换。
 */
public sealed interface RiskRuleNode permits RiskRuleGroupNode, RiskRuleConditionNode {
}
