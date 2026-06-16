package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_execution_dlq")
public class CanvasExecutionDlqDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    public String executionId;
    public Long canvasId;
    public String userId;
    public String perfRunId;
    public String failedNodeId;
    public String failedNodeType;
    public String errorMsg;
    public Integer retryCount;
    public String triggerPayload;
    public String triggerType;
    public String triggerNodeType;
    public String matchKey;
    public LocalDateTime failedAt;
}
