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
    /** 原始触发类型（MQ / DIRECT_CALL / BEHAVIOR）—— 重放时需要 */
    private String triggerType;
    /** 原始触发节点类型（MQ_TRIGGER / BEHAVIOR_IN_APP 等）—— 重放时定位触发器节点 */
    private String triggerNodeType;
    /** 原始 matchKey（MQ topicKey / 行为 eventCode）*/
    private String matchKey;
    private LocalDateTime failedAt;
}
