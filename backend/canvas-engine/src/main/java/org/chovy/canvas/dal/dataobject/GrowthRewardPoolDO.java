package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("growth_reward_pool")
public class GrowthRewardPoolDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long activityId;

    private String poolKey;

    private String rewardType;

    private String grantChannel;

    private String couponTypeKey;

    private String loyaltyRewardKey;

    private String pointsType;

    private String externalContractKey;

    private String inventoryMode;

    private Long totalInventory;

    private Long reservedInventory;

    private Long grantedInventory;

    private Integer perUserLimit;

    private Integer perReferralLimit;

    private BigDecimal budgetAmount;

    private BigDecimal reservedAmount;

    private BigDecimal grantedAmount;

    private String costCurrency;

    private String status;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
