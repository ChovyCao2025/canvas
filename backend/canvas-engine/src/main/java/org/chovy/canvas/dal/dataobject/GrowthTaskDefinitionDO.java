package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GrowthTaskDefinitionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_task_definition")
public class GrowthTaskDefinitionDO {

    /** 增长任务定义主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 增长任务定义任务业务键 */
    private String taskKey;

    /** 增长任务定义任务类型 */
    private String taskType;

    /** 增长任务定义完成策略 */
    private String completionPolicy;

    /** 增长任务定义重置策略 */
    private String resetPolicy;

    /** 关联的奖励池 ID */
    private Long rewardPoolId;

    /** 增长任务定义目标值 */
    private BigDecimal targetValue;

    /** 增长任务定义当前状态 */
    private String status;

    /** 增长任务定义规则配置 JSON */
    private String ruleJson;

    /** 增长任务定义创建人 */
    private String createdBy;

    /** 增长任务定义最后更新人 */
    private String updatedBy;

    /** 增长任务定义创建时间 */
    private LocalDateTime createdAt;

    /** 增长任务定义最后更新时间 */
    private LocalDateTime updatedAt;
}
