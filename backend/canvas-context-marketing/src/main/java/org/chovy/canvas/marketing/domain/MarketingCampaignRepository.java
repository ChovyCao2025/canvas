package org.chovy.canvas.marketing.domain;

import java.util.List;

/**
 * 定义MarketingCampaignRepository的营销上下文访问契约。
 */
public interface MarketingCampaignRepository {

    /**
     * 查找byTenantAndKey业务对象。
     */
    MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey);

    /**
     * 查找byId业务对象。
     */
    MarketingCampaign findById(Long tenantId, Long campaignId);

    /**
     * 执行save业务操作。
     */
    MarketingCampaign save(MarketingCampaign campaign);

    /**
     * 查询列表。
     */
    List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit);

    /**
     * 查找link业务对象。
     */
    MarketingCampaignLink findLink(Long tenantId, Long campaignId, String resourceType, CampaignKey resourceKey);

    /**
     * 执行saveLink业务操作。
     */
    MarketingCampaignLink saveLink(MarketingCampaignLink link);

    /**
     * 查询links列表。
     */
    List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId);

    /**
     * 查找linkById业务对象。
     */
    MarketingCampaignLink findLinkById(Long tenantId, Long linkId);

    /**
     * 删除或停用link业务对象。
     */
    void deleteLink(Long tenantId, Long linkId);
}
