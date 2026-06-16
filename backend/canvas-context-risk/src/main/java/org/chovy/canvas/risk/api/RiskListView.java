package org.chovy.canvas.risk.api;

import java.util.Objects;

/**
 * 定义 RiskListView 的风控模块职责和数据契约。
 */
public final class RiskListView {

    /**
     * RiskListView 的 tenantId 字段。
     */
    private final Long tenantId;


    /**
     * RiskListView 的 listKey 字段。
     */
    private final String listKey;


    /**
     * RiskListView 的 listType 字段。
     */
    private final String listType;


    /**
     * RiskListView 的 subjectType 字段。
     */
    private final String subjectType;


    /**
     * RiskListView 的 status 字段。
     */
    private final String status;


    /**
     * RiskListView 的 requiresApproval 字段。
     */
    private final boolean requiresApproval;


    /**
     * RiskListView 的 owner 字段。
     */
    private final String owner;


    /**
     * 创建 RiskListView。
     *
     * @param tenantId RiskListView 的 tenantId 字段
     * @param listKey RiskListView 的 listKey 字段
     * @param listType RiskListView 的 listType 字段
     * @param subjectType RiskListView 的 subjectType 字段
     * @param status RiskListView 的 status 字段
     * @param requiresApproval RiskListView 的 requiresApproval 字段
     * @param owner RiskListView 的 owner 字段
     */
    public RiskListView(Long tenantId, String listKey, String listType, String subjectType, String status, boolean requiresApproval, String owner) {
        this.tenantId = tenantId;
        this.listKey = listKey;
        this.listType = listType;
        this.subjectType = subjectType;
        this.status = status;
        this.requiresApproval = requiresApproval;
        this.owner = owner;
    }

    /**
     * 返回 RiskListView 的 tenantId 字段。
     *
     * @return tenantId 字段值
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回 RiskListView 的 listKey 字段。
     *
     * @return listKey 字段值
     */
    public String listKey() {
        return listKey;
    }

    /**
     * 返回 RiskListView 的 listType 字段。
     *
     * @return listType 字段值
     */
    public String listType() {
        return listType;
    }

    /**
     * 返回 RiskListView 的 subjectType 字段。
     *
     * @return subjectType 字段值
     */
    public String subjectType() {
        return subjectType;
    }

    /**
     * 返回 RiskListView 的 status 字段。
     *
     * @return status 字段值
     */
    public String status() {
        return status;
    }

    /**
     * 返回 RiskListView 的 requiresApproval 字段。
     *
     * @return requiresApproval 字段值
     */
    public boolean requiresApproval() {
        return requiresApproval;
    }

    /**
     * 返回 RiskListView 的 owner 字段。
     *
     * @return owner 字段值
     */
    public String owner() {
        return owner;
    }

    /**
     * 比较当前 RiskListView 与其他对象是否相等。
     *
     * @param o 待比较对象
     * @return 相等时返回 true
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RiskListView other)) {
            return false;
        }
        return Objects.equals(tenantId, other.tenantId)
                && Objects.equals(listKey, other.listKey)
                && Objects.equals(listType, other.listType)
                && Objects.equals(subjectType, other.subjectType)
                && Objects.equals(status, other.status)
                && requiresApproval == other.requiresApproval
                && Objects.equals(owner, other.owner);
    }

    /**
     * 计算 RiskListView 的哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(tenantId, listKey, listType, subjectType, status, requiresApproval, owner);
    }

    /**
     * 返回 RiskListView 的调试字符串。
     *
     * @return 调试字符串
     */
    @Override
    public String toString() {
        return "RiskListView[tenantId=" + tenantId + ", listKey=" + listKey + ", listType=" + listType + ", subjectType=" + subjectType + ", status=" + status + ", requiresApproval=" + requiresApproval + ", owner=" + owner + "]";
    }
}
