package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GrowthReferralCodeDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_referral_code")
public class GrowthReferralCodeDO {

    /** 增长裂变关系编码主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 关联的参与人 ID */
    private Long participantId;

    /** 增长裂变关系编码 */
    private String code;

    /** 增长裂变关系编码当前状态 */
    private String status;

    /** 增长裂变关系编码创建人 */
    private String createdBy;

    /** 增长裂变关系编码创建时间 */
    private LocalDateTime createdAt;
}
