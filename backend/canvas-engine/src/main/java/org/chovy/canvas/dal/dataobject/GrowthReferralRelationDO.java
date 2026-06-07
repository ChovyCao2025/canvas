package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_referral_relation")
public class GrowthReferralRelationDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long activityId;

    private Long referralCodeId;

    private Long referrerParticipantId;

    private String inviteeUserId;

    private String status;

    private String riskEvidenceJson;

    private Long inviterRewardGrantId;

    private Long inviteeRewardGrantId;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
