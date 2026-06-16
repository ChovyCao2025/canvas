package org.chovy.canvas.cdp.domain;

/**
 * 表示 WarehouseRealtimeStatus 的业务数据或处理组件。
 */
public final class WarehouseRealtimeStatus {

    /**
     * 总数。
     */
    private final int total;

    /**
     * 通过数。
     */
    private final long passed;

    /**
     * 告警数。
     */
    private final long warned;

    /**
     * 失败数。
     */
    private final long failed;

    /**
     * 使用记录字段创建 WarehouseRealtimeStatus。
     */
    public WarehouseRealtimeStatus(
            int total,
            long passed,
            long warned,
            long failed) {
        this.total = total;
        this.passed = passed;
        this.warned = warned;
        this.failed = failed;
    }

    /**
     * 返回总数。
     */
    public int total() {
        return total;
    }

    /**
     * 返回通过数。
     */
    public long passed() {
        return passed;
    }

    /**
     * 返回告警数。
     */
    public long warned() {
        return warned;
    }

    /**
     * 返回失败数。
     */
    public long failed() {
        return failed;
    }

    /**
     * 按所有字段比较 WarehouseRealtimeStatus。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseRealtimeStatus that = (WarehouseRealtimeStatus) o;
        return java.util.Objects.equals(total, that.total)
                && java.util.Objects.equals(passed, that.passed)
                && java.util.Objects.equals(warned, that.warned)
                && java.util.Objects.equals(failed, that.failed);
    }

    /**
     * 根据所有字段计算 WarehouseRealtimeStatus 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(total, passed, warned, failed);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseRealtimeStatus[" + "total=" + total + ", passed=" + passed + ", warned=" + warned + ", failed=" + failed + "]";
    }
}
