package org.chovy.canvas.cdp.domain;

/**
 * 表示 CdpWarehouseReadinessSection 的业务数据或处理组件。
 */
public final class CdpWarehouseReadinessSection {

    /**
     * 键。
     */
    private final String key;

    /**
     * 状态。
     */
    private final String status;

    /**
     * 原因。
     */
    private final String reason;

    /**
     * 使用记录字段创建 CdpWarehouseReadinessSection。
     */
    public CdpWarehouseReadinessSection(
            String key,
            String status,
            String reason) {
        this.key = key;
        this.status = status;
        this.reason = reason;
    }

    /**
     * 返回键。
     */
    public String key() {
        return key;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回原因。
     */
    public String reason() {
        return reason;
    }

    /**
     * 按所有字段比较 CdpWarehouseReadinessSection。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CdpWarehouseReadinessSection that = (CdpWarehouseReadinessSection) o;
        return java.util.Objects.equals(key, that.key)
                && java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(reason, that.reason);
    }

    /**
     * 根据所有字段计算 CdpWarehouseReadinessSection 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, status, reason);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "CdpWarehouseReadinessSection[" + "key=" + key + ", status=" + status + ", reason=" + reason + "]";
    }
}
