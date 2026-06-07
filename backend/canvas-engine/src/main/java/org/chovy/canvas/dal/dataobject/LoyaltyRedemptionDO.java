package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("loyalty_redemption")
public class LoyaltyRedemptionDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String userId;

    private Long accountId;

    private String redemptionKey;

    private String rewardKey;

    private Integer pointsCost;

    private String status;

    private String failureReason;

    private LocalDateTime redeemedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
