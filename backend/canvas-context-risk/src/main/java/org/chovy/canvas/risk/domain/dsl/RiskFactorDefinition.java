package org.chovy.canvas.risk.domain.dsl;

import java.util.Objects;

/**
 * 风控特征定义。
 *
 * @param key 特征业务键
 * @param valueType 特征值类型
 * @param availability 特征在线可用性
 * @param subjectType 特征关联的主体类型
 */
public final class RiskFactorDefinition {

    /**
     * RiskFactorDefinition 的 key 字段。
     */
    private final String key;


    /**
     * RiskFactorDefinition 的 valueType 字段。
     */
    private final RiskValueType valueType;


    /**
     * RiskFactorDefinition 的 availability 字段。
     */
    private final RiskFeatureAvailability availability;


    /**
     * RiskFactorDefinition 的 subjectType 字段。
     */
    private final RiskSubjectType subjectType;


    /**
     * 创建 RiskFactorDefinition。
     *
     * @param key RiskFactorDefinition 的 key 字段
     * @param valueType RiskFactorDefinition 的 valueType 字段
     * @param availability RiskFactorDefinition 的 availability 字段
     * @param subjectType RiskFactorDefinition 的 subjectType 字段
     */
    public RiskFactorDefinition(String key, RiskValueType valueType, RiskFeatureAvailability availability, RiskSubjectType subjectType) {
        this.key = key;
        this.valueType = valueType;
        this.availability = availability;
        this.subjectType = subjectType;
    }

    /**
     * 返回 RiskFactorDefinition 的 key 字段。
     *
     * @return key 字段值
     */
    public String key() {
        return key;
    }

    /**
     * 返回 RiskFactorDefinition 的 valueType 字段。
     *
     * @return valueType 字段值
     */
    public RiskValueType valueType() {
        return valueType;
    }

    /**
     * 返回 RiskFactorDefinition 的 availability 字段。
     *
     * @return availability 字段值
     */
    public RiskFeatureAvailability availability() {
        return availability;
    }

    /**
     * 返回 RiskFactorDefinition 的 subjectType 字段。
     *
     * @return subjectType 字段值
     */
    public RiskSubjectType subjectType() {
        return subjectType;
    }

    /**
     * 比较当前 RiskFactorDefinition 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskFactorDefinition other)) {
            return false;
        }
        return Objects.equals(key, other.key)
                && Objects.equals(valueType, other.valueType)
                && Objects.equals(availability, other.availability)
                && Objects.equals(subjectType, other.subjectType);
    }

    /**
     * 计算 RiskFactorDefinition 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, valueType, availability, subjectType);
    }

    /**
     * 返回 RiskFactorDefinition 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskFactorDefinition[key=" + key + ", valueType=" + valueType + ", availability=" + availability + ", subjectType=" + subjectType + "]";
    }
}
