package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * GrowthRewardPoolDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("growth_reward_pool")
public class GrowthRewardPoolDO {

    /** 增长奖励池主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的活动 ID */
    private Long activityId;

    /** 增长奖励池池业务键 */
    private String poolKey;

    /** 增长奖励池奖励类型 */
    private String rewardType;

    /** 增长奖励池发放渠道 */
    private String grantChannel;

    /** 增长奖励池券类型业务键 */
    private String couponTypeKey;

    /** 增长奖励池会员忠诚度奖励业务键 */
    private String loyaltyRewardKey;

    /** 增长奖励池积分类型 */
    private String pointsType;

    /** 增长奖励池外部契约业务键 */
    private String externalContractKey;

    /** 增长奖励池库存模式 */
    private String inventoryMode;

    /** 增长奖励池总计库存 */
    private Long totalInventory;

    /** 增长奖励池预留库存 */
    private Long reservedInventory;

    /** 增长奖励池已发放库存 */
    private Long grantedInventory;

    /** 增长奖励池每用户限制 */
    private Integer perUserLimit;

    /** 增长奖励池每裂变关系限制 */
    private Integer perReferralLimit;

    /** 增长奖励池预算金额 */
    private BigDecimal budgetAmount;

    /** 增长奖励池预留金额 */
    private BigDecimal reservedAmount;

    /** 增长奖励池已发放金额 */
    private BigDecimal grantedAmount;

    /** 增长奖励池成本币种 */
    private String costCurrency;

    /** 增长奖励池当前状态 */
    private String status;

    /** 增长奖励池扩展元数据 JSON */
    private String metadataJson;

    /** 增长奖励池创建人 */
    private String createdBy;

    /** 增长奖励池最后更新人 */
    private String updatedBy;

    /** 增长奖励池创建时间 */
    private LocalDateTime createdAt;

    /** 增长奖励池最后更新时间 */
    private LocalDateTime updatedAt;
}
