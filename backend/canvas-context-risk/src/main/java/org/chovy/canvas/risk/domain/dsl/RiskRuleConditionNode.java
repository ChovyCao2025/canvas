package org.chovy.canvas.risk.domain.dsl;

/**
 * 风控规则条件节点。
 *
 * @param left 左操作数
 * @param op 条件操作符
 * @param right 右操作数，单目操作符可为空
 */
public record RiskRuleConditionNode(
        RiskRuleOperand left,
        RiskRuleOperator op,
        RiskRuleOperand right
) implements RiskRuleNode {
}
