package org.chovy.canvas.canvas.adapter.persistence;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 封装CanvasManualApprovalDO相关的业务逻辑。
 */
@TableName("canvas_manual_approval")
public class CanvasManualApprovalDO {

    /**
     * 保存标识。
     */
    @TableId
    private String id;

    /**
     * 保存execution标识。
     */
    private String executionId;

    /**
     * 保存租户标识。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 保存画布标识。
     */
    private Long canvasId;

    /**
     * 保存节点标识。
     */
    private String nodeId;

    /**
     * 保存用户标识。
     */
    private String userId;

    /**
     * 保存approvers。
     */
    private String approvers;

    /**
     * 保存onTimeout。
     */
    private String onTimeout;

    /**
     * 保存timeout时间。
     */
    private LocalDateTime timeoutAt;

    /**
     * 保存状态。
     */
    private String status;

    /**
     * 保存resultBy。
     */
    private String resultBy;

    /**
     * 保存结果时间。
     */
    private LocalDateTime resultAt;

    /**
     * 保存创建时间。
     */
    private LocalDateTime createdAt;
}
