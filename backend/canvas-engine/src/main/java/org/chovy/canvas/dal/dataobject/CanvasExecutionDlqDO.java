package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布执行死信队列（canvas_execution_dlq）。
 *
 * <p>节点执行失败且超过最大重试次数后，将失败信息写入 DLQ，供运营人员排查和手动重放。
 * 手动重放时，从 DLQ 中读取原始触发上下文，完整还原本次触发并重新执行。
 */
@Data
@Builder
@TableName("canvas_execution_dlq")
public class CanvasExecutionDlqDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始执行记录 ID */
    private String executionId;

    /** 所属画布 ID */
    private Long canvasId;

    /** 触发用户 ID */
    private String userId;

    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;

    /** 首次失败的节点 ID */
    private String failedNodeId;

    /** 首次失败的节点类型（见 NodeType 常量） */
    private String failedNodeType;

    /** 失败原因 */
    private String errorMsg;

    /** 已重试次数 */
    private Integer retryCount;

    /** 原始触发 payload JSON（重放时透传给执行引擎） */
    private String triggerPayload;

    /**
     * 原始触发类型（见 {@link org.chovy.canvas.common.enums.TriggerType}）。
     * 重放时作为 triggerType 参数传入执行引擎，保证执行记录来源的准确性。
     */
    private String triggerType;

    /**
     * 原始触发节点类型（见 NodeType 常量，如 MQ_TRIGGER / EVENT_TRIGGER 等）。
     * 重放时用于定位 DAG 入口触发器节点。
     */
    private String triggerNodeType;

    /**
     * 原始匹配 Key。
     * MQ 触发时为 topicKey，行为触发时为 eventCode，用于重放时路由到正确的触发器节点。
     */
    private String matchKey;

    /** 首次失败时间 */
    private LocalDateTime failedAt;
}
