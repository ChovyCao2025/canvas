package org.chovy.canvas.dal.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * MarketingCampaignLinkDO 映射 dal.dataobject 场景的持久化数据行。
 */
@Data
@TableName("marketing_campaign_link")
public class MarketingCampaignLinkDO {

    /** 营销营销活动链接主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属租户 ID */
    private Long tenantId;

    /** 关联的营销活动 ID */
    private Long campaignId;

    /** 营销营销活动链接资源类型 */
    private String resourceType;

    /** 营销营销活动链接资源 ID */
    private Long resourceId;

    /** 营销营销活动链接资源业务键 */
    private String resourceKey;

    /** 营销营销活动链接资源名称 */
    private String resourceName;

    /** 营销营销活动链接资源路由 */
    private String resourceRoute;

    /** 营销营销活动链接依赖角色 */
    private String dependencyRole;

    /** 营销营销活动链接状态 */
    private String linkStatus;

    /** 营销营销活动LINKREQUIREDFORLAUNCH */
    private Integer requiredForLaunch;

    /** 营销营销活动链接扩展元数据 JSON */
    private String metadataJson;

    /** 营销营销活动链接创建人 */
    private String createdBy;

    /** 营销营销活动链接最后更新人 */
    private String updatedBy;

    /** 营销营销活动链接创建时间 */
    private LocalDateTime createdAt;

    /** 营销营销活动链接最后更新时间 */
    private LocalDateTime updatedAt;
}
