package org.chovy.canvas.risk.domain.dsl;

import java.util.Objects;

/**
 * 风控名单定义。
 *
 * @param key 名单业务键
 * @param subjectType 名单主体类型
 * @param valueType 名单值类型
 */
public final class RiskListDefinition {

    /**
     * RiskListDefinition 的 key 字段。
     */
    private final String key;


    /**
     * RiskListDefinition 的 subjectType 字段。
     */
    private final RiskSubjectType subjectType;


    /**
     * RiskListDefinition 的 valueType 字段。
     */
    private final RiskValueType valueType;


    /**
     * 创建 RiskListDefinition。
     *
     * @param key RiskListDefinition 的 key 字段
     * @param subjectType RiskListDefinition 的 subjectType 字段
     * @param valueType RiskListDefinition 的 valueType 字段
     */
    public RiskListDefinition(String key, RiskSubjectType subjectType, RiskValueType valueType) {
        this.key = key;
        this.subjectType = subjectType;
        this.valueType = valueType;
    }

    /**
     * 返回 RiskListDefinition 的 key 字段。
     *
     * @return key 字段值
     */
    public String key() {
        return key;
    }

    /**
     * 返回 RiskListDefinition 的 subjectType 字段。
     *
     * @return subjectType 字段值
     */
    public RiskSubjectType subjectType() {
        return subjectType;
    }

    /**
     * 返回 RiskListDefinition 的 valueType 字段。
     *
     * @return valueType 字段值
     */
    public RiskValueType valueType() {
        return valueType;
    }

    /**
     * 比较当前 RiskListDefinition 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskListDefinition other)) {
            return false;
        }
        return Objects.equals(key, other.key)
                && Objects.equals(subjectType, other.subjectType)
                && Objects.equals(valueType, other.valueType);
    }

    /**
     * 计算 RiskListDefinition 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, subjectType, valueType);
    }

    /**
     * 返回 RiskListDefinition 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskListDefinition[key=" + key + ", subjectType=" + subjectType + ", valueType=" + valueType + "]";
    }
}
