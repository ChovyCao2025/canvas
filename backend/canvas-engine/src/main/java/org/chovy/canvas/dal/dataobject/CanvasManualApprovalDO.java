package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 人工审批记录（canvas_manual_approval）。
 *
 * <p>当画布执行到 MANUAL_APPROVAL 节点时创建该记录，执行流程挂起等待人工操作。
 * 审批完成（通过/拒绝/超时）后，Watchdog 或 API 回写结果并恢复执行。
 */
@Data
@Builder
@TableName("canvas_manual_approval")
public class CanvasManualApprovalDO {

    /**
     * 审批 ID，格式为 "{executionId}:{nodeId}"，保证同一执行同一节点唯一。
     * 幂等插入依赖此主键，防止 Watchdog 重复创建审批记录。
     */
    @TableId
    private String id;

    /** 所属执行记录 ID */
    private String executionId;

    /** 所属画布 ID */
    private Long canvasId;

    /** 所属 MANUAL_APPROVAL 节点 ID */
    private String nodeId;

    /** 触发该执行的用户 ID（被审批对象） */
    private String userId;

    /**
     * 审批人列表，JSON 数组格式（如 ["user1", "user2"]）。
     * 任意一人操作即可完成审批（OR 语义）。
     */
    private String approvers;

    /**
     * 超时策略，见 {@link org.chovy.canvas.common.enums.ApprovalOnTimeoutAction}。
     * APPROVE = 超时自动通过, REJECT = 超时自动拒绝, KEEP_WAITING = 续期不处理
     */
    private String onTimeout;

    /** 审批截止时间，超过此时间由 Watchdog 按 onTimeout 策略处理 */
    private LocalDateTime timeoutAt;

    /**
     * 审批状态，见 {@link org.chovy.canvas.common.enums.ApprovalStatus}。
     * PENDING = 待审批, APPROVED = 已通过, REJECTED = 已拒绝, TIMEOUT = 已超时
     */
    private String status;

    /** 操作人（审批人 userId 或 "watchdog"），null 表示尚未处理 */
    private String resultBy;

    /** 审批操作时间，null 表示尚未处理 */
    private LocalDateTime resultAt;

    /** 记录创建时间。 */
    private LocalDateTime createdAt;
}
