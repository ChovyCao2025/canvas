package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("marketing_campaign_link")
public class MarketingCampaignLinkDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long campaignId;

    private String resourceType;

    private Long resourceId;

    private String resourceKey;

    private String resourceName;

    private String resourceRoute;

    private String dependencyRole;

    private String linkStatus;

    private Integer requiredForLaunch;

    private String metadataJson;

    private String createdBy;

    private String updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
