package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;

/**
 * 表示 WarehouseWatermark 的业务数据或处理组件。
 */
public final class WarehouseWatermark {

    /**
     * watermark Time。
     */
    private final LocalDateTime watermarkTime;

    /**
     * 更新时间。
     */
    private final LocalDateTime updatedAt;

    /**
     * 使用记录字段创建 WarehouseWatermark。
     */
    public WarehouseWatermark(
            LocalDateTime watermarkTime,
            LocalDateTime updatedAt) {
        this.watermarkTime = watermarkTime;
        this.updatedAt = updatedAt;
    }

    /**
     * 返回watermark Time。
     */
    public LocalDateTime watermarkTime() {
        return watermarkTime;
    }

    /**
     * 返回更新时间。
     */
    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    /**
     * 按所有字段比较 WarehouseWatermark。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseWatermark that = (WarehouseWatermark) o;
        return java.util.Objects.equals(watermarkTime, that.watermarkTime)
                && java.util.Objects.equals(updatedAt, that.updatedAt);
    }

    /**
     * 根据所有字段计算 WarehouseWatermark 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(watermarkTime, updatedAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "WarehouseWatermark[" + "watermarkTime=" + watermarkTime + ", updatedAt=" + updatedAt + "]";
    }
}
