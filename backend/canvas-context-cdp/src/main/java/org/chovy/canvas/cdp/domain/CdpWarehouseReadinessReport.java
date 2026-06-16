package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表示 CdpWarehouseReadinessReport 的业务数据或处理组件。
 */
public final class CdpWarehouseReadinessReport {

    /**
     * 租户标识。
     */
    private final Long tenantId;

    /**
     * 状态。
     */
    private final String status;

    /**
     * generated At。
     */
    private final LocalDateTime generatedAt;

    /**
     * sections。
     */
    private final List<CdpWarehouseReadinessSection> sections;

    /**
     * 使用记录字段创建 CdpWarehouseReadinessReport。
     */
    public CdpWarehouseReadinessReport(
            Long tenantId,
            String status,
            LocalDateTime generatedAt,
            List<CdpWarehouseReadinessSection> sections) {
        this.tenantId = tenantId;
        this.status = status;
        this.generatedAt = generatedAt;
        this.sections = sections;
    }

    /**
     * 返回租户标识。
     */
    public Long tenantId() {
        return tenantId;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回generated At。
     */
    public LocalDateTime generatedAt() {
        return generatedAt;
    }

    /**
     * 返回sections。
     */
    public List<CdpWarehouseReadinessSection> sections() {
        return sections;
    }

    /**
     * 按所有字段比较 CdpWarehouseReadinessReport。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpWarehouseReadinessReport that = (CdpWarehouseReadinessReport) o;
        return java.util.Objects.equals(tenantId, that.tenantId)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(generatedAt, that.generatedAt)
                && java.util.Objects.equals(sections, that.sections);
    }

    /**
     * 根据所有字段计算 CdpWarehouseReadinessReport 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(tenantId, status, generatedAt, sections);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpWarehouseReadinessReport[" + "tenantId=" + tenantId + ", status=" + status + ", generatedAt=" + generatedAt + ", sections=" + sections + "]";
    }
}
