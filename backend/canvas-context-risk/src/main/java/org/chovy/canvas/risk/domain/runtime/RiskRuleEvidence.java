package org.chovy.canvas.risk.domain.runtime;

import java.util.Objects;

/**
 * 风控规则条件评估证据。
 *
 * @param path 条件路径
 * @param operator 操作符
 * @param leftValue 左值
 * @param rightValue 右值
 * @param matched 条件是否匹配
 */
public final class RiskRuleEvidence {

    /**
     * RiskRuleEvidence 的 path 字段。
     */
    private final String path;


    /**
     * RiskRuleEvidence 的 operator 字段。
     */
    private final String operator;


    /**
     * RiskRuleEvidence 的 leftValue 字段。
     */
    private final Object leftValue;


    /**
     * RiskRuleEvidence 的 rightValue 字段。
     */
    private final Object rightValue;


    /**
     * RiskRuleEvidence 的 matched 字段。
     */
    private final boolean matched;


    /**
     * 创建 RiskRuleEvidence。
     *
     * @param path RiskRuleEvidence 的 path 字段
     * @param operator RiskRuleEvidence 的 operator 字段
     * @param leftValue RiskRuleEvidence 的 leftValue 字段
     * @param rightValue RiskRuleEvidence 的 rightValue 字段
     * @param matched RiskRuleEvidence 的 matched 字段
     */
    public RiskRuleEvidence(String path, String operator, Object leftValue, Object rightValue, boolean matched) {
        this.path = path;
        this.operator = operator;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.matched = matched;
    }

    /**
     * 返回 RiskRuleEvidence 的 path 字段。
     *
     * @return path 字段值
     */
    public String path() {
        return path;
    }

    /**
     * 返回 RiskRuleEvidence 的 operator 字段。
     *
     * @return operator 字段值
     */
    public String operator() {
        return operator;
    }

    /**
     * 返回 RiskRuleEvidence 的 leftValue 字段。
     *
     * @return leftValue 字段值
     */
    public Object leftValue() {
        return leftValue;
    }

    /**
     * 返回 RiskRuleEvidence 的 rightValue 字段。
     *
     * @return rightValue 字段值
     */
    public Object rightValue() {
        return rightValue;
    }

    /**
     * 返回 RiskRuleEvidence 的 matched 字段。
     *
     * @return matched 字段值
     */
    public boolean matched() {
        return matched;
    }

    /**
     * 比较当前 RiskRuleEvidence 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskRuleEvidence other)) {
            return false;
        }
        return Objects.equals(path, other.path)
                && Objects.equals(operator, other.operator)
                && Objects.equals(leftValue, other.leftValue)
                && Objects.equals(rightValue, other.rightValue)
                && matched == other.matched;
    }

    /**
     * 计算 RiskRuleEvidence 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(path, operator, leftValue, rightValue, matched);
    }

    /**
     * 返回 RiskRuleEvidence 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskRuleEvidence[path=" + path + ", operator=" + operator + ", leftValue=" + leftValue + ", rightValue=" + rightValue + ", matched=" + matched + "]";
    }
}
