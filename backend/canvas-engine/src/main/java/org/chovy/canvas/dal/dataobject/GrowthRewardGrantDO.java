package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GrowthRewardGrantDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_reward_grant")
public class GrowthRewardGrantDO {

    /** 增长奖励发放主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的池 ID */
    private Long poolId;

    /** 关联的参与人 ID */
    private Long participantId;

    /** 关联的裂变关系关系 ID */
    private Long referralRelationId;

    /** 关联的任务进度 ID */
    private Long taskProgressId;

    /** 增长奖励发放发放原因 */
    private String grantReason;

    /** 增长奖励发放当前状态 */
    private String status;

    /** 增长奖励发放幂等键 */
    private String idempotencyKey;

    /** 增长奖励发放服务商请求报文 JSON */
    private String providerRequestJson;

    /** 增长奖励发放服务商响应报文 JSON */
    private String providerResponseJson;

    /** 增长奖励发放成本金额 */
    private BigDecimal costAmount;

    /** 增长奖励发放创建人 */
    private String createdBy;

    /** 增长奖励发放最后更新人 */
    private String updatedBy;

    /** 增长奖励发放创建时间 */
    private LocalDateTime createdAt;

    /** 增长奖励发放最后更新时间 */
    private LocalDateTime updatedAt;
}
