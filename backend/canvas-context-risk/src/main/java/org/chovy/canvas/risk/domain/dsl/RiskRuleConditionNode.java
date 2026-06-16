package org.chovy.canvas.risk.domain.dsl;

import java.util.Objects;

/**
 * 风控规则条件节点。
 *
 * @param left 左操作数
 * @param op 条件操作符
 * @param right 右操作数，单目操作符可为空
 */
public final class RiskRuleConditionNode implements RiskRuleNode {

    /**
     * RiskRuleConditionNode 的 left 字段。
     */
    private final RiskRuleOperand left;


    /**
     * RiskRuleConditionNode 的 op 字段。
     */
    private final RiskRuleOperator op;


    /**
     * RiskRuleConditionNode 的 right 字段。
     */
    private final RiskRuleOperand right;


    /**
     * 创建 RiskRuleConditionNode。
     *
     * @param left RiskRuleConditionNode 的 left 字段
     * @param op RiskRuleConditionNode 的 op 字段
     * @param right RiskRuleConditionNode 的 right 字段
     */
    public RiskRuleConditionNode(RiskRuleOperand left, RiskRuleOperator op, RiskRuleOperand right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    /**
     * 返回 RiskRuleConditionNode 的 left 字段。
     *
     * @return left 字段值
     */
    public RiskRuleOperand left() {
        return left;
    }

    /**
     * 返回 RiskRuleConditionNode 的 op 字段。
     *
     * @return op 字段值
     */
    public RiskRuleOperator op() {
        return op;
    }

    /**
     * 返回 RiskRuleConditionNode 的 right 字段。
     *
     * @return right 字段值
     */
    public RiskRuleOperand right() {
        return right;
    }

    /**
     * 比较当前 RiskRuleConditionNode 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskRuleConditionNode other)) {
            return false;
        }
        return Objects.equals(left, other.left)
                && Objects.equals(op, other.op)
                && Objects.equals(right, other.right);
    }

    /**
     * 计算 RiskRuleConditionNode 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(left, op, right);
    }

    /**
     * 返回 RiskRuleConditionNode 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskRuleConditionNode[left=" + left + ", op=" + op + ", right=" + right + "]";
    }
}
