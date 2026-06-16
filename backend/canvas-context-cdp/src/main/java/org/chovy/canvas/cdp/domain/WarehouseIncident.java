package org.chovy.canvas.cdp.domain;

/**
 * 表示 WarehouseIncident 的业务数据或处理组件。
 */
public final class WarehouseIncident {

    /**
     * 严重级别。
     */
    private final String severity;

    /**
     * 状态。
     */
    private final String status;

    /**
     * 使用记录字段创建 WarehouseIncident。
     */
    public WarehouseIncident(
            String severity,
            String status) {
        this.severity = severity;
        this.status = status;
    }

    /**
     * 返回严重级别。
     */
    public String severity() {
        return severity;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 按所有字段比较 WarehouseIncident。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseIncident that = (WarehouseIncident) o;
        return java.util.Objects.equals(severity, that.severity)
                && java.util.Objects.equals(status, that.status);
    }

    /**
     * 根据所有字段计算 WarehouseIncident 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(severity, status);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseIncident[" + "severity=" + severity + ", status=" + status + "]";
    }
}
