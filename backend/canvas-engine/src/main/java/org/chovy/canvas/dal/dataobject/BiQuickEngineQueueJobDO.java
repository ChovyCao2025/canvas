package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BiQuickEngineQueueJobDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("bi_quick_engine_queue_job")
public class BiQuickEngineQueueJobDO {

    /** BI快速引擎队列任务主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** BI快速引擎队列任务池业务键 */
    private String poolKey;

    /** BI快速引擎队列任务SQL哈希 */
    private String sqlHash;

    /** BI快速引擎队列任务数据集业务键 */
    private String datasetKey;

    /** BI快速引擎队列任务请求人 */
    private String requestedBy;

    /** BI快速引擎队列任务当前状态 */
    private String status;

    /** BI快速引擎队列任务尝试数量 */
    private Integer attemptCount;

    /** BI快速引擎队列任务入队时间 */
    private LocalDateTime queuedAt;

    /** BI快速引擎队列任务过期时间 */
    private LocalDateTime expiresAt;

    /** BI快速引擎队列任务认领人 */
    private String claimedBy;

    /** BI快速引擎队列任务认领时间 */
    private LocalDateTime claimedAt;

    /** BI快速引擎队列任务完成时间 */
    private LocalDateTime completedAt;

    /** BI快速引擎队列任务阻塞原因 */
    private String blockedReason;

    /** BI快速引擎队列任务创建时间 */
    private LocalDateTime createdAt;

    /** BI快速引擎队列任务最后更新时间 */
    private LocalDateTime updatedAt;
}
