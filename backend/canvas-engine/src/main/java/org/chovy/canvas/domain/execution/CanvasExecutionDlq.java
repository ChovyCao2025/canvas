package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("canvas_execution_dlq")
public class CanvasExecutionDlq {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private Long   canvasId;
    private String userId;
    private String failedNodeId;
    private String failedNodeType;
    private String errorMsg;
    private Integer retryCount;
    private String triggerPayload;
    private LocalDateTime failedAt;
}
