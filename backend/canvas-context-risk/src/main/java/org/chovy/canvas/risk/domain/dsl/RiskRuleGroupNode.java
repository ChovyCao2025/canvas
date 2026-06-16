package org.chovy.canvas.risk.domain.dsl;

import java.util.List;
import java.util.Objects;

/**
 * 风控规则分组节点，可包含条件和嵌套子组。
 *
 * @param logic 条件与子组之间的逻辑关系
 * @param conditions 当前分组下的条件列表
 * @param groups 当前分组下的子分组列表
 */
public final class RiskRuleGroupNode implements RiskRuleNode {

    /**
     * RiskRuleGroupNode 的 logic 字段。
     */
    private final RiskRuleLogic logic;


    /**
     * RiskRuleGroupNode 的 conditions 字段。
     */
    private final List<RiskRuleConditionNode> conditions;


    /**
     * RiskRuleGroupNode 的 groups 字段。
     */
    private final List<RiskRuleGroupNode> groups;


    /**
     * 创建 RiskRuleGroupNode。
     *
     * @param logic RiskRuleGroupNode 的 logic 字段
     * @param conditions RiskRuleGroupNode 的 conditions 字段
     * @param groups RiskRuleGroupNode 的 groups 字段
     */
    public RiskRuleGroupNode(RiskRuleLogic logic, List<RiskRuleConditionNode> conditions, List<RiskRuleGroupNode> groups) {
        conditions = conditions == null ? List.of() : List.copyOf(conditions);
                groups = groups == null ? List.of() : List.copyOf(groups);
        this.logic = logic;
        this.conditions = conditions;
        this.groups = groups;
    }

    /**
     * 返回 RiskRuleGroupNode 的 logic 字段。
     *
     * @return logic 字段值
     */
    public RiskRuleLogic logic() {
        return logic;
    }

    /**
     * 返回 RiskRuleGroupNode 的 conditions 字段。
     *
     * @return conditions 字段值
     */
    public List<RiskRuleConditionNode> conditions() {
        return conditions;
    }

    /**
     * 返回 RiskRuleGroupNode 的 groups 字段。
     *
     * @return groups 字段值
     */
    public List<RiskRuleGroupNode> groups() {
        return groups;
    }

    /**
     * 比较当前 RiskRuleGroupNode 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskRuleGroupNode other)) {
            return false;
        }
        return Objects.equals(logic, other.logic)
                && Objects.equals(conditions, other.conditions)
                && Objects.equals(groups, other.groups);
    }

    /**
     * 计算 RiskRuleGroupNode 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(logic, conditions, groups);
    }

    /**
     * 返回 RiskRuleGroupNode 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskRuleGroupNode[logic=" + logic + ", conditions=" + conditions + ", groups=" + groups + "]";
    }
}
