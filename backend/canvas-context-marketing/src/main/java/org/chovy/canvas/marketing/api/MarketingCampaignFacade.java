package org.chovy.canvas.marketing.api;

import java.util.List;

/**
 * 定义MarketingCampaignFacade的营销上下文访问契约。
 */
public interface MarketingCampaignFacade {

    /**
     * 执行upsertCampaign业务操作。
     */
    MarketingCampaignView upsertCampaign(Long tenantId, MarketingCampaignCommand command, String actor);

    /**
     * 查询campaigns列表。
     */
    List<MarketingCampaignView> listCampaigns(Long tenantId, String status, Integer limit);

    /**
     * 执行linkResource业务操作。
     */
    MarketingCampaignLinkView linkResource(Long tenantId, MarketingCampaignLinkCommand command, String actor);

    /**
     * 查询links列表。
     */
    List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId);

    /**
     * 执行readiness业务操作。
     */
    MarketingCampaignReadinessView readiness(Long tenantId, Long campaignId);

    /**
     * 执行unlinkResource业务操作。
     */
    void unlinkResource(Long tenantId, Long linkId);
}
