package org.chovy.canvas.domain.approval;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("canvas_manual_approval")
public class CanvasManualApproval {

    @TableId
    private String id;             // approvalId = executionId:nodeId

    private String  executionId;
    private Long    canvasId;
    private String  nodeId;
    private String  userId;
    private String  approvers;     // JSON 数组字符串
    private String  onTimeout;     // REJECT / APPROVE / KEEP_WAITING
    private LocalDateTime timeoutAt;
    private String  status;        // PENDING / APPROVED / REJECTED / TIMEOUT
    private String  resultBy;
    private LocalDateTime resultAt;
    private LocalDateTime createdAt;
}
