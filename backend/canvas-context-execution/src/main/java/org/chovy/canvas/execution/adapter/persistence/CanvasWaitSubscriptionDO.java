package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_wait_subscription")
public class CanvasWaitSubscriptionDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public String executionId;
    public Long canvasId;
    public Long versionId;
    public String userId;
    public String nodeId;
    public String waitType;
    public String eventCode;
    public String eventFilters;
    public String resumePayload;
    public LocalDateTime expiresAt;
    public String status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
