package org.chovy.canvas.cdp.api;

import java.time.LocalDateTime;

/**
 * 表示 AudienceSnapshotView 的业务数据或处理组件。
 */
public final class AudienceSnapshotView {

    /**
     * 唯一标识。
     */
    private final Long id;

    /**
     * 人群标识。
     */
    private final Long audienceId;

    /**
     * 画布标识。
     */
    private final Long canvasId;

    /**
     * 画布版本标识。
     */
    private final Long canvasVersionId;

    /**
     * 节点标识。
     */
    private final String nodeId;

    /**
     * snapshot Mode。
     */
    private final String snapshotMode;

    /**
     * user Count。
     */
    private final long userCount;

    /**
     * 创建人。
     */
    private final String createdBy;

    /**
     * 创建时间。
     */
    private final LocalDateTime createdAt;

    /**
     * 使用记录字段创建 AudienceSnapshotView。
     */
    public AudienceSnapshotView(
            Long id,
            Long audienceId,
            Long canvasId,
            Long canvasVersionId,
            String nodeId,
            String snapshotMode,
            long userCount,
            String createdBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.audienceId = audienceId;
        this.canvasId = canvasId;
        this.canvasVersionId = canvasVersionId;
        this.nodeId = nodeId;
        this.snapshotMode = snapshotMode;
        this.userCount = userCount;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /**
     * 返回唯一标识。
     */
    public Long id() {
        return id;
    }

    /**
     * 返回人群标识。
     */
    public Long audienceId() {
        return audienceId;
    }

    /**
     * 返回画布标识。
     */
    public Long canvasId() {
        return canvasId;
    }

    /**
     * 返回画布版本标识。
     */
    public Long canvasVersionId() {
        return canvasVersionId;
    }

    /**
     * 返回节点标识。
     */
    public String nodeId() {
        return nodeId;
    }

    /**
     * 返回snapshot Mode。
     */
    public String snapshotMode() {
        return snapshotMode;
    }

    /**
     * 返回user Count。
     */
    public long userCount() {
        return userCount;
    }

    /**
     * 返回创建人。
     */
    public String createdBy() {
        return createdBy;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime createdAt() {
        return createdAt;
    }

    /**
     * 按所有字段比较 AudienceSnapshotView。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudienceSnapshotView that = (AudienceSnapshotView) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(audienceId, that.audienceId)
                && java.util.Objects.equals(canvasId, that.canvasId)
                && java.util.Objects.equals(canvasVersionId, that.canvasVersionId)
                && java.util.Objects.equals(nodeId, that.nodeId)
                && java.util.Objects.equals(snapshotMode, that.snapshotMode)
                && java.util.Objects.equals(userCount, that.userCount)
                && java.util.Objects.equals(createdBy, that.createdBy)
                && java.util.Objects.equals(createdAt, that.createdAt);
    }

    /**
     * 根据所有字段计算 AudienceSnapshotView 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, audienceId, canvasId, canvasVersionId, nodeId, snapshotMode, userCount, createdBy, createdAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "AudienceSnapshotView[" + "id=" + id + ", audienceId=" + audienceId + ", canvasId=" + canvasId + ", canvasVersionId=" + canvasVersionId + ", nodeId=" + nodeId + ", snapshotMode=" + snapshotMode + ", userCount=" + userCount + ", createdBy=" + createdBy + ", createdAt=" + createdAt + "]";
    }
}
