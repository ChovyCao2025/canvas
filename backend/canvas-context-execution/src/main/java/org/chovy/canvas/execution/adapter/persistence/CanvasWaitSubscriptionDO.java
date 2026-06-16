package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 定义 CanvasWaitSubscriptionDO 的执行上下文数据结构或业务契约。
 */
@TableName("canvas_wait_subscription")
public class CanvasWaitSubscriptionDO {

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
     * 保存 versionId 对应的状态或配置。
     */
    public Long versionId;

    /**
     * 保存 userId 对应的状态或配置。
     */
    public String userId;

    /**
     * 保存 nodeId 对应的状态或配置。
     */
    public String nodeId;

    /**
     * 保存 waitType 对应的状态或配置。
     */
    public String waitType;

    /**
     * 保存 eventCode 对应的状态或配置。
     */
    public String eventCode;

    /**
     * 保存 eventFilters 对应的状态或配置。
     */
    public String eventFilters;

    /**
     * 保存 resumePayload 对应的状态或配置。
     */
    public String resumePayload;

    /**
     * 保存 expiresAt 对应的状态或配置。
     */
    public LocalDateTime expiresAt;

    /**
     * 保存 status 对应的状态或配置。
     */
    public String status;

    /**
     * 保存 createdAt 对应的状态或配置。
     */
    public LocalDateTime createdAt;

    /**
     * 保存 updatedAt 对应的状态或配置。
     */
    public LocalDateTime updatedAt;
}
