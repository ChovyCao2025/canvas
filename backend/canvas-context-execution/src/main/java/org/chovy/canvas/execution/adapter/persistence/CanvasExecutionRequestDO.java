package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasExecutionRequestDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_execution_request")
public class CanvasExecutionRequestDO {

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
     * 保存 triggerNodeType 对应的状态或配置。
     */
    public String triggerNodeType;

    /**
     * 保存 matchKey 对应的状态或配置。
     */
    public String matchKey;

    /**
     * 保存 payloadJson 对应的状态或配置。
     */
    public String payloadJson;

    /**
     * 保存 sourceMsgId 对应的状态或配置。
     */
    public String sourceMsgId;

    /**
     * 保存 status 对应的状态或配置。
     */
    public String status;

    /**
     * 保存 attemptCount 对应的状态或配置。
     */
    public Integer attemptCount;

    /**
     * 保存 nextRetryAt 对应的状态或配置。
     */
    public LocalDateTime nextRetryAt;

    /**
     * 保存 lastError 对应的状态或配置。
     */
    public String lastError;

    /**
     * 保存 resultJson 对应的状态或配置。
     */
    public String resultJson;

    /**
     * 保存 runToken 对应的状态或配置。
     */
    public String runToken;

    /**
     * 保存 replayCount 对应的状态或配置。
     */
    public Integer replayCount;

    /**
     * 保存 lastReplayAt 对应的状态或配置。
     */
    public LocalDateTime lastReplayAt;

    /**
     * 保存 lastReplayBy 对应的状态或配置。
     */
    public String lastReplayBy;

    /**
     * 保存 lastReplayReason 对应的状态或配置。
     */
    public String lastReplayReason;

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
