package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

/**
 * 表示 WarehouseMaterializationRun 的业务数据或处理组件。
 */
public final class WarehouseMaterializationRun {

    /**
     * 状态。
     */
    private final String status;

    /**
     * 完成时间。
     */
    private final LocalDateTime finishedAt;

    /**
     * 开始时间。
     */
    private final LocalDateTime startedAt;

    /**
     * 使用记录字段创建 WarehouseMaterializationRun。
     */
    public WarehouseMaterializationRun(
            String status,
            LocalDateTime finishedAt,
            LocalDateTime startedAt) {
        this.status = status;
        this.finishedAt = finishedAt;
        this.startedAt = startedAt;
    }

    /**
     * 返回状态。
     */
    public String status() {
        return status;
    }

    /**
     * 返回完成时间。
     */
    public LocalDateTime finishedAt() {
        return finishedAt;
    }

    /**
     * 返回开始时间。
     */
    public LocalDateTime startedAt() {
        return startedAt;
    }

    /**
     * 按所有字段比较 WarehouseMaterializationRun。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseMaterializationRun that = (WarehouseMaterializationRun) o;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(finishedAt, that.finishedAt)
                && java.util.Objects.equals(startedAt, that.startedAt);
    }

    /**
     * 根据所有字段计算 WarehouseMaterializationRun 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, finishedAt, startedAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseMaterializationRun[" + "status=" + status + ", finishedAt=" + finishedAt + ", startedAt=" + startedAt + "]";
    }
}
