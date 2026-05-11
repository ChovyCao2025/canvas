package com.photon.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("canvas_execution_trace")
public class CanvasExecutionTrace {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionId;
    private String nodeId;
    private String nodeType;
    private String nodeName;

    /** 0执行中 1成功 2失败 3跳过 */
    private Integer status;

    private String inputData;
    private String outputData;
    private String errorMsg;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
