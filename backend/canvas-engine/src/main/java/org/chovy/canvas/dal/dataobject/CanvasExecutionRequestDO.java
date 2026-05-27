package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 画布执行请求 数据对象，对应数据库表 {@code canvas_execution_request}。
 *
 * <p>该类是 MyBatis-Plus 与数据库之间的持久化模型，字段命名和类型需要与迁移脚本、Mapper XML 保持一致。
 * <p>业务层应通过 Service/Mapper 读写该对象，避免在控制器中直接暴露数据库结构。
 */
@Data
@TableName("canvas_execution_request")
public class CanvasExecutionRequestDO {

    @TableId
    /** 执行请求主键 ID，贯穿排队、执行、重试和重放流程 */
    private String id;

    /** 目标画布 ID */
    private Long canvasId;

    /** 触发用户 ID */
    private String userId;

    /** 压测批次 ID，普通业务流量为空 */
    private String perfRunId;

    /** 触发类型，如 MQ、BEHAVIOR、DIRECT_CALL 或 DLQ_REPLAY */
    private String triggerType;

    /** 触发节点类型，如 MQ_TRIGGER、EVENT_TRIGGER、API_TRIGGER */
    private String triggerNodeType;

    /** 触发匹配键，MQ 场景通常为 topic/tag，行为事件场景通常为 eventCode */
    private String matchKey;

    /** 原始触发载荷 JSON，派发执行和失败重试时复用 */
    private String payloadJson;

    /** 外部消息 ID 或事件 ID，用于幂等追踪和排查 */
    private String sourceMsgId;

    /** 请求处理状态，见 {@link org.chovy.canvas.engine.request.CanvasExecutionRequestStatus} */
    private String status;

    /** 当前已尝试执行次数 */
    private Integer attemptCount;

    /** 下次允许重试时间，null 表示无需延迟重试 */
    private LocalDateTime nextRetryAt;

    /** 最近一次执行失败原因 */
    private String lastError;

    /** 执行完成后的结果 JSON */
    private String resultJson;

    /** 当前运行令牌，用于并发派发时标识本次占用 */
    private String runToken;

    /** 人工或系统重放次数 */
    private Integer replayCount;

    /** 最近一次重放时间 */
    private LocalDateTime lastReplayAt;

    /** 最近一次重放操作人 */
    private String lastReplayBy;

    /** 最近一次重放原因 */
    private String lastReplayReason;

    @TableField(fill = FieldFill.INSERT)
    /** 请求创建时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    /** 请求最后更新时间，由 MyBatis-Plus 自动填充 */
    private LocalDateTime updatedAt;
}
