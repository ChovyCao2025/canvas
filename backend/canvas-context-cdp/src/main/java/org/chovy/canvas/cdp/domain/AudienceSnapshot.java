package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表示 AudienceSnapshot 的业务数据或处理组件。
 */
public final class AudienceSnapshot {

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
    private final AudienceSnapshotMode snapshotMode;

    /**
     * user Ids。
     */
    private final List<String> userIds;

    /**
     * 创建人。
     */
    private final String createdBy;

    /**
     * 创建时间。
     */
    private final LocalDateTime createdAt;

    /**
     * 使用记录字段创建 AudienceSnapshot。
     */
    public AudienceSnapshot(
            Long id,
            Long audienceId,
            Long canvasId,
            Long canvasVersionId,
            String nodeId,
            AudienceSnapshotMode snapshotMode,
            List<String> userIds,
            String createdBy,
            LocalDateTime createdAt) {
        this.id = id;
        this.audienceId = audienceId;
        this.canvasId = canvasId;
        this.canvasVersionId = canvasVersionId;
        this.nodeId = nodeId;
        this.snapshotMode = snapshotMode;
        this.userIds = userIds;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

/**
 * 返回替换Id后的副本。
 */
public AudienceSnapshot withId(Long newId) {
        return new AudienceSnapshot(newId, audienceId, canvasId, canvasVersionId, nodeId, snapshotMode, userIds,
                createdBy, createdAt);
    }

    /**
     * 执行 userCount 对应的 CDP 业务操作。
     */
    public long userCount() {
        return userIds == null ? 0L : userIds.size();
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
    public AudienceSnapshotMode snapshotMode() {
        return snapshotMode;
    }

    /**
     * 返回user Ids。
     */
    public List<String> userIds() {
        return userIds;
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
     * 按所有字段比较 AudienceSnapshot。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AudienceSnapshot that = (AudienceSnapshot) o;
        return java.util.Objects.equals(id, that.id)
                && java.util.Objects.equals(audienceId, that.audienceId)
                && java.util.Objects.equals(canvasId, that.canvasId)
                && java.util.Objects.equals(canvasVersionId, that.canvasVersionId)
                && java.util.Objects.equals(nodeId, that.nodeId)
                && java.util.Objects.equals(snapshotMode, that.snapshotMode)
                && java.util.Objects.equals(userIds, that.userIds)
                && java.util.Objects.equals(createdBy, that.createdBy)
                && java.util.Objects.equals(createdAt, that.createdAt);
    }

    /**
     * 根据所有字段计算 AudienceSnapshot 的哈希值。
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, audienceId, canvasId, canvasVersionId, nodeId, snapshotMode, userIds, createdBy, createdAt);
    }

    /**
     * 返回与记录结构一致的调试字符串。
     */
    @Override
    public String toString() {
        return "AudienceSnapshot[" + "id=" + id + ", audienceId=" + audienceId + ", canvasId=" + canvasId + ", canvasVersionId=" + canvasVersionId + ", nodeId=" + nodeId + ", snapshotMode=" + snapshotMode + ", userIds=" + userIds + ", createdBy=" + createdBy + ", createdAt=" + createdAt + "]";
    }
}
