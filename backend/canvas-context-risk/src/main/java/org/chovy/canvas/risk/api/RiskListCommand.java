package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskListCommand 的风控模块职责和数据契约。
 */
public final class RiskListCommand {

    /**
     * RiskListCommand 的 listKey 字段。
     */
    private final String listKey;


    /**
     * RiskListCommand 的 listType 字段。
     */
    private final String listType;


    /**
     * RiskListCommand 的 subjectType 字段。
     */
    private final String subjectType;


    /**
     * RiskListCommand 的 requiresApproval 字段。
     */
    private final boolean requiresApproval;


    /**
     * 创建 RiskListCommand。
     *
     * @param listKey RiskListCommand 的 listKey 字段
     * @param listType RiskListCommand 的 listType 字段
     * @param subjectType RiskListCommand 的 subjectType 字段
     * @param requiresApproval RiskListCommand 的 requiresApproval 字段
     */
    public RiskListCommand(String listKey, String listType, String subjectType, boolean requiresApproval) {
        this.listKey = listKey;
        this.listType = listType;
        this.subjectType = subjectType;
        this.requiresApproval = requiresApproval;
    }

    /**
     * 返回 RiskListCommand 的 listKey 字段。
     *
     * @return listKey 字段值
     */
    public String listKey() {
        return listKey;
    }

    /**
     * 返回 RiskListCommand 的 listType 字段。
     *
     * @return listType 字段值
     */
    public String listType() {
        return listType;
    }

    /**
     * 返回 RiskListCommand 的 subjectType 字段。
     *
     * @return subjectType 字段值
     */
    public String subjectType() {
        return subjectType;
    }

    /**
     * 返回 RiskListCommand 的 requiresApproval 字段。
     *
     * @return requiresApproval 字段值
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /**
     * 比较当前 RiskListCommand 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskListCommand other)) {
            return false;
        }
        return Objects.equals(listKey, other.listKey)
                && Objects.equals(listType, other.listType)
                && Objects.equals(subjectType, other.subjectType)
                && requiresApproval == other.requiresApproval;
    }

    /**
     * 计算 RiskListCommand 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(listKey, listType, subjectType, requiresApproval);
    }

    /**
     * 返回 RiskListCommand 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskListCommand[listKey=" + listKey + ", listType=" + listType + ", subjectType=" + subjectType + ", requiresApproval=" + requiresApproval + "]";
    }
}
