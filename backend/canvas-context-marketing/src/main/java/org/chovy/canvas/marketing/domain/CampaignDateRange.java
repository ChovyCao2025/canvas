package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 表示CampaignDateRange的数据结构。
 */
public final class CampaignDateRange {

    /**
     * 开始时间。
     */
    private final LocalDateTime startAt;

    /**
     * 结束时间。
     */
    private final LocalDateTime endAt;

    /**
     * 创建CampaignDateRange实例。
     */
    public CampaignDateRange(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
        throw new IllegalArgumentException("endAt must be after startAt");
        }

        this.startAt = startAt;
        this.endAt = endAt;
    }

    /**
     * 返回开始时间。
     */
    public LocalDateTime startAt() {
        return startAt;
    }

    /**
     * 返回结束时间。
     */
    public LocalDateTime endAt() {
        return endAt;
    }




    /**
     * 执行of业务操作。
     */
    public static CampaignDateRange of(LocalDateTime startAt, LocalDateTime endAt) {
        return new CampaignDateRange(startAt, endAt);
    }

    /**
     * 比较两个实例的组件值是否一致。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CampaignDateRange that = (CampaignDateRange) o;
        return                 Objects.equals(startAt, that.startAt) &&
                Objects.equals(endAt, that.endAt);
    }

    /**
     * 根据组件值计算哈希值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(startAt, endAt);
    }

    /**
     * 返回与记录类型一致的组件展示文本。
     */
    @Override
    public String toString() {
        return "CampaignDateRange[startAt=" + startAt + ", endAt=" + endAt + "]";
    }
}
