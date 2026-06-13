package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_execution_trace")
public class CanvasExecutionTraceDO {

    @TableId(type = IdType.AUTO)
    public Long id;

    @TableField("tenant_id")
    public Long tenantId;

    public String executionId;
    public String nodeId;
    public String nodeType;
    public String nodeName;
    public Integer status;
    public String inputData;
    public String outputData;
    public String errorMsg;
    public LocalDateTime startedAt;
    public LocalDateTime finishedAt;
    public Long durationMs;
}
