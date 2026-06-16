package org.chovy.canvas.cdp.domain;

/**
 * 表示 WarehouseBiDatasource 的业务数据或处理组件。
 */
public final class WarehouseBiDatasource {

    /**
     * 是否可用。
     */
    private final boolean available;

    /**
     * 使用记录字段创建 WarehouseBiDatasource。
     */
    public WarehouseBiDatasource(
            boolean available) {
        this.available = available;
    }

    /**
     * 返回是否可用。
     */
    public boolean available() {
        return available;
    }

    /**
     * 按所有字段比较 WarehouseBiDatasource。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseBiDatasource that = (WarehouseBiDatasource) o;
        return java.util.Objects.equals(available, that.available);
    }

    /**
     * 根据所有字段计算 WarehouseBiDatasource 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(available);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseBiDatasource[" + "available=" + available + "]";
    }
}
