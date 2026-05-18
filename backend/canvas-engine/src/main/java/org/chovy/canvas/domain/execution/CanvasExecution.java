package org.chovy.canvas.domain.execution;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 画布执行记录（canvas_execution）。
 *
 * <p>每次画布触发生成一条执行记录，记录本次执行的触发来源、状态和结果。
 * 执行过程中的详细节点轨迹存储在 {@link CanvasExecutionTrace} 中。
 */
@Data
@TableName("canvas_execution")
public class CanvasExecution {

    /** 执行 ID，UUID 格式，也作为执行上下文（ExecutionContext）的唯一标识 */
    @TableId
    private String id;

    /** 所属画布 ID */
    private Long canvasId;

    /** 执行时使用的版本 ID（-1 表示 dry-run，使用草稿） */
    private Long versionId;

    /** 触发该执行的用户 ID */
    private String userId;

    /**
     * 触发类型，见 {@link org.chovy.canvas.domain.constant.TriggerType}。
     * 如 MQ、DIRECT_CALL、BEHAVIOR、DRY_RUN、DLQ_REPLAY 等。
     */
    private String triggerType;

    /**
     * 执行状态，见 {@link org.chovy.canvas.domain.constant.ExecutionStatus}。
     * RUNNING=0, PAUSED=1（等待人工审批等挂起场景）, SUCCESS=2, FAILED=3
     */
    private Integer status;

    /** 执行结果 JSON（成功时为输出数据，失败时包含 error 字段） */
    private String result;

    /**
     * 去重 Key，正在执行时持有，执行结束后清空。
     * Watchdog 通过此字段识别僵尸执行并释放去重锁。
     */
    private String lastDedupKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
