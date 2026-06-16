package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasExecutionDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_execution")
public class CanvasExecutionDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId
    public String id;

    /**
     * 保存 tenantId 对应的状态或配置。
     */
    @TableField("tenant_id")
    public Long tenantId;

    /**
     * 保存 canvasId 对应的状态或配置。
     */
    public Long canvasId;

    /**
     * 保存 versionId 对应的状态或配置。
     */
    public Long versionId;

    /**
     * 保存 userId 对应的状态或配置。
     */
    public String userId;

    /**
     * 保存 perfRunId 对应的状态或配置。
     */
    public String perfRunId;

    /**
     * 保存 triggerType 对应的状态或配置。
     */
    public String triggerType;

    /**
     * 保存 status 对应的状态或配置。
     */
    public Integer status;

    /**
     * 保存 result 对应的状态或配置。
     */
    public String result;

    /**
     * 保存 contextSnapshotJson 对应的状态或配置。
     */
    public String contextSnapshotJson;

    /**
     * 保存 lastDedupKey 对应的状态或配置。
     */
    public String lastDedupKey;

    /**
     * 保存 createdAt 对应的状态或配置。
     */
    @TableField(fill = FieldFill.INSERT)
    public LocalDateTime createdAt;

    /**
     * 保存 updatedAt 对应的状态或配置。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    public LocalDateTime updatedAt;
}
