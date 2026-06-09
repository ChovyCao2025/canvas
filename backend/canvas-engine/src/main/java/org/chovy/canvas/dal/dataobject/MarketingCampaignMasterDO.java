package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MarketingCampaignMasterDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_campaign_master")
public class MarketingCampaignMasterDO {

    /** 营销营销活动主记录主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 营销营销活动主记录营销活动KEY业务键 */
    private String campaignKey;

    /** 营销营销活动主记录营销活动NAME */
    private String campaignName;

    /** 营销营销活动主记录OBJECTIVE */
    private String objective;

    /** 营销营销活动主记录当前状态 */
    private String status;

    /** 营销营销活动主记录PRIMARYCHANNEL */
    private String primaryChannel;

    /** 营销营销活动主记录OWNERTEAM */
    private String ownerTeam;

    /** 营销营销活动主记录STARTAT时间 */
    private LocalDateTime startAt;

    /** 营销营销活动主记录ENDAT时间 */
    private LocalDateTime endAt;

    /** 营销营销活动主记录BUDGETAMOUNT */
    private BigDecimal budgetAmount;

    /** 营销营销活动主记录CURRENCY */
    private String currency;

    /** 营销营销活动主记录BRIEFJSON明细 JSON */
    private String briefJson;

    /** 营销营销活动主记录创建人 */
    private String createdBy;

    /** 营销营销活动主记录最后更新人 */
    private String updatedBy;

    /** 营销营销活动主记录创建时间 */
    private LocalDateTime createdAt;

    /** 营销营销活动主记录最后更新时间 */
    private LocalDateTime updatedAt;
}
