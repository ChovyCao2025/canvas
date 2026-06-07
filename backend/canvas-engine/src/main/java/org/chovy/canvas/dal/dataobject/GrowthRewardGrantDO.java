package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("growth_reward_grant")
public class GrowthRewardGrantDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long activityId;

    private Long poolId;

    private Long participantId;

    private Long referralRelationId;

    private Long taskProgressId;

    private String grantReason;

    private String status;

    private String idempotencyKey;

    private String providerRequestJson;

    private String providerResponseJson;

    private BigDecimal costAmount;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
