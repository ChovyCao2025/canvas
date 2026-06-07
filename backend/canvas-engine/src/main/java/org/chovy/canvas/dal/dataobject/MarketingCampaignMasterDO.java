package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("marketing_campaign_master")
public class MarketingCampaignMasterDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String campaignKey;

    private String campaignName;

    private String objective;

    private String status;

    private String primaryChannel;

    private String ownerTeam;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private BigDecimal budgetAmount;

    private String currency;

    private String briefJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
