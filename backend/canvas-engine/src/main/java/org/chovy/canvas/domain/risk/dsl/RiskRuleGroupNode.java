package org.chovy.canvas.domain.risk.dsl;

import java.util.List;

/**
 * 风控规则分组节点，可包含条件和嵌套子组。
 *
 * @param logic 条件与子组之间的逻辑关系
 * @param conditions 当前分组下的条件列表
 * @param groups 当前分组下的子分组列表
 */
public record RiskRuleGroupNode(
        RiskRuleLogic logic,
        List<RiskRuleConditionNode> conditions,
        List<RiskRuleGroupNode> groups
) implements RiskRuleNode {

    public RiskRuleGroupNode {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
        groups = groups == null ? List.of() : List.copyOf(groups);
    }
}
