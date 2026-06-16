package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasExecutionDlqDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_execution_dlq")
public class CanvasExecutionDlqDO {

    /**
     * 保存 id 对应的状态或配置。
     */
    @TableId(type = IdType.AUTO)
    public Long id;

    /**
     * 保存 executionId 对应的状态或配置。
     */
    public String executionId;

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
     * 保存 failedNodeId 对应的状态或配置。
     */
    public String failedNodeId;

    /**
     * 保存 failedNodeType 对应的状态或配置。
     */
    public String failedNodeType;

    /**
     * 保存 errorMsg 对应的状态或配置。
     */
    public String errorMsg;

    /**
     * 保存 retryCount 对应的状态或配置。
     */
    public Integer retryCount;

    /**
     * 保存 triggerPayload 对应的状态或配置。
     */
    public String triggerPayload;

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
     * 保存 failedAt 对应的状态或配置。
     */
    public LocalDateTime failedAt;
}
