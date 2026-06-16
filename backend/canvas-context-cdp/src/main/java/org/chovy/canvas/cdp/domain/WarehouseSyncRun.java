package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

/**
 * 表示 WarehouseSyncRun 的业务数据或处理组件。
 */
public final class WarehouseSyncRun {

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
     * window End。
     */
    private final LocalDateTime windowEnd;

    /**
     * window Start。
     */
    private final LocalDateTime windowStart;

    /**
     * 使用记录字段创建 WarehouseSyncRun。
     */
    public WarehouseSyncRun(
            String status,
            LocalDateTime finishedAt,
            LocalDateTime startedAt,
            LocalDateTime windowEnd,
            LocalDateTime windowStart) {
        this.status = status;
        this.finishedAt = finishedAt;
        this.startedAt = startedAt;
        this.windowEnd = windowEnd;
        this.windowStart = windowStart;
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
     * 返回window End。
     */
    public LocalDateTime windowEnd() {
        return windowEnd;
    }

    /**
     * 返回window Start。
     */
    public LocalDateTime windowStart() {
        return windowStart;
    }

    /**
     * 按所有字段比较 WarehouseSyncRun。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseSyncRun that = (WarehouseSyncRun) o;
        return java.util.Objects.equals(status, that.status)
                && java.util.Objects.equals(finishedAt, that.finishedAt)
                && java.util.Objects.equals(startedAt, that.startedAt)
                && java.util.Objects.equals(windowEnd, that.windowEnd)
                && java.util.Objects.equals(windowStart, that.windowStart);
    }

    /**
     * 根据所有字段计算 WarehouseSyncRun 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(status, finishedAt, startedAt, windowEnd, windowStart);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseSyncRun[" + "status=" + status + ", finishedAt=" + finishedAt + ", startedAt=" + startedAt + ", windowEnd=" + windowEnd + ", windowStart=" + windowStart + "]";
    }
}
