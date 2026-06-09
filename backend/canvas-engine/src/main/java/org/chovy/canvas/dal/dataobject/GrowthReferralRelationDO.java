package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthReferralRelationDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_referral_relation")
public class GrowthReferralRelationDO {

    /** 增长裂变关系关系主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的裂变关系编码 ID */
    private Long referralCodeId;

    /** 关联的推荐人参与人 ID */
    private Long referrerParticipantId;

    /** 关联的受邀人用户 ID */
    private String inviteeUserId;

    /** 增长裂变关系关系当前状态 */
    private String status;

    /** 增长裂变关系关系风险证据明细 JSON */
    private String riskEvidenceJson;

    /** 关联的邀请人奖励发放 ID */
    private Long inviterRewardGrantId;

    /** 关联的受邀人奖励发放 ID */
    private Long inviteeRewardGrantId;

    /** 增长裂变关系关系创建人 */
    private String createdBy;

    /** 增长裂变关系关系最后更新人 */
    private String updatedBy;

    /** 增长裂变关系关系创建时间 */
    private LocalDateTime createdAt;

    /** 增长裂变关系关系最后更新时间 */
    private LocalDateTime updatedAt;
}
