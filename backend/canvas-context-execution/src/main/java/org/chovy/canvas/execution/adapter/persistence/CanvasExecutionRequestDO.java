package org.chovy.canvas.execution.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("canvas_execution_request")
public class CanvasExecutionRequestDO {

    @TableId
    public String id;

    @TableField("tenant_id")
    public Long tenantId;

    public Long canvasId;
    public String userId;
    public String perfRunId;
    public String triggerType;
    public String triggerNodeType;
    public String matchKey;
    public String payloadJson;
    public String sourceMsgId;
    public String status;
    public Integer attemptCount;
    public LocalDateTime nextRetryAt;
    public String lastError;
    public String resultJson;
    public String runToken;
    public Integer replayCount;
    public LocalDateTime lastReplayAt;
    public String lastReplayBy;
    public String lastReplayReason;

    @TableField(fill = FieldFill.INSERT)
    public LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    public LocalDateTime updatedAt;
}
