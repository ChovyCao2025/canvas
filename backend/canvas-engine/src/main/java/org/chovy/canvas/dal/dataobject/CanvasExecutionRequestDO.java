package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("canvas_execution_request")
public class CanvasExecutionRequestDO {

    @TableId
    private String id;

    private Long canvasId;
    private String userId;
    private String perfRunId;
    private String triggerType;
    private String triggerNodeType;
    private String matchKey;
    private String payloadJson;
    private String sourceMsgId;
    private String status;
    private Integer attemptCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private String resultJson;
    private String runToken;
    private Integer replayCount;
    private LocalDateTime lastReplayAt;
    private String lastReplayBy;
    private String lastReplayReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
