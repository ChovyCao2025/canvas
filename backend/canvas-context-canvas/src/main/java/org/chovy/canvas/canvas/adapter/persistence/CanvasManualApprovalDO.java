package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_manual_approval")
public class CanvasManualApprovalDO {
    @TableId
    private String id;
    private String executionId;
    @TableField("tenant_id")
    private Long tenantId;
    private Long canvasId;
    private String nodeId;
    private String userId;
    private String approvers;
    private String onTimeout;
    private LocalDateTime timeoutAt;
    private String status;
    private String resultBy;
    private LocalDateTime resultAt;
    private LocalDateTime createdAt;
}
