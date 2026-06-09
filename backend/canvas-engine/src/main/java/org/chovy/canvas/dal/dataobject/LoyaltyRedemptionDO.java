package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LoyaltyRedemptionDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("loyalty_redemption")
public class LoyaltyRedemptionDO {

    /** 会员忠诚度核销主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的用户 ID */
    private String userId;

    /** 关联的账户 ID */
    private Long accountId;

    /** 会员忠诚度核销核销业务键 */
    private String redemptionKey;

    /** 会员忠诚度核销奖励业务键 */
    private String rewardKey;

    /** 会员忠诚度核销积分成本 */
    private Integer pointsCost;

    /** 会员忠诚度核销当前状态 */
    private String status;

    /** 会员忠诚度核销失败原因 */
    private String failureReason;

    /** 会员忠诚度核销时间 */
    private LocalDateTime redeemedAt;

    /** 会员忠诚度核销创建时间 */
    private LocalDateTime createdAt;

    /** 会员忠诚度核销最后更新时间 */
    private LocalDateTime updatedAt;
}
