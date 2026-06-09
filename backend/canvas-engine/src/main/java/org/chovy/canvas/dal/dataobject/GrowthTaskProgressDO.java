package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GrowthTaskProgressDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_task_progress")
public class GrowthTaskProgressDO {

    /** 增长任务进度主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的参与人 ID */
    private Long participantId;

    /** 关联的任务 ID */
    private Long taskId;

    /** 增长任务进度进度值 */
    private BigDecimal progressValue;

    /** 增长任务进度目标值 */
    private BigDecimal targetValue;

    /** 增长任务进度当前状态 */
    private String status;

    /** 增长任务进度最近事件业务键 */
    private String lastEventKey;

    /** 增长任务进度证据明细 JSON */
    private String evidenceJson;

    /** 关联的奖励发放 ID */
    private Long rewardGrantId;

    /** 增长任务进度最后更新人 */
    private String updatedBy;

    /** 增长任务进度完成时间 */
    private LocalDateTime completedAt;

    /** 增长任务进度最后更新时间 */
    private LocalDateTime updatedAt;
}
