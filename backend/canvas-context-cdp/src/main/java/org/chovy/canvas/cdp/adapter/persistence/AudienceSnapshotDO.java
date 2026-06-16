package org.chovy.canvas.cdp.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 承载 AudienceSnapshot 表的持久化字段。
 */
@TableName("audience_snapshot")
public class AudienceSnapshotDO {

    /**
     * 唯一标识。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 人群标识。
     */
    private Long audienceId;

    /**
     * 画布标识。
     */
    private Long canvasId;

    /**
     * 画布版本标识。
     */
    private Long canvasVersionId;

    /**
     * 节点标识。
     */
    private String nodeId;

    /**
     * snapshot Mode。
     */
    private String snapshotMode;

    /**
     * user Count。
     */
    private Long userCount;

    /**
     * user Ids Json。
     */
    private String userIdsJson;

    /**
     * 创建人。
     */
    private String createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 返回唯一标识。
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置唯一标识。
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回人群标识。
     */
    public Long getAudienceId() {
        return audienceId;
    }

    /**
     * 设置人群标识。
     */
    public void setAudienceId(Long audienceId) {
        this.audienceId = audienceId;
    }

    /**
     * 返回画布标识。
     */
    public Long getCanvasId() {
        return canvasId;
    }

    /**
     * 设置画布标识。
     */
    public void setCanvasId(Long canvasId) {
        this.canvasId = canvasId;
    }

    /**
     * 返回画布版本标识。
     */
    public Long getCanvasVersionId() {
        return canvasVersionId;
    }

    /**
     * 设置画布版本标识。
     */
    public void setCanvasVersionId(Long canvasVersionId) {
        this.canvasVersionId = canvasVersionId;
    }

    /**
     * 返回节点标识。
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 设置节点标识。
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * 返回snapshot Mode。
     */
    public String getSnapshotMode() {
        return snapshotMode;
    }

    /**
     * 设置snapshot Mode。
     */
    public void setSnapshotMode(String snapshotMode) {
        this.snapshotMode = snapshotMode;
    }

    /**
     * 返回user Count。
     */
    public Long getUserCount() {
        return userCount;
    }

    /**
     * 设置user Count。
     */
    public void setUserCount(Long userCount) {
        this.userCount = userCount;
    }

    /**
     * 返回user Ids Json。
     */
    public String getUserIdsJson() {
        return userIdsJson;
    }

    /**
     * 设置user Ids Json。
     */
    public void setUserIdsJson(String userIdsJson) {
        this.userIdsJson = userIdsJson;
    }

    /**
     * 返回创建人。
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * 设置创建人。
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 返回创建时间。
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 设置创建时间。
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
