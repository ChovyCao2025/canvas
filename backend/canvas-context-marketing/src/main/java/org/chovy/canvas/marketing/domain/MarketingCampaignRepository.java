package org.chovy.canvas.marketing.domain;

import java.util.List;

public interface MarketingCampaignRepository {

    MarketingCampaign findByTenantAndKey(Long tenantId, CampaignKey campaignKey);

    MarketingCampaign findById(Long tenantId, Long campaignId);

    MarketingCampaign save(MarketingCampaign campaign);

    List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit);

    MarketingCampaignLink findLink(Long tenantId, Long campaignId, String resourceType, CampaignKey resourceKey);

    MarketingCampaignLink saveLink(MarketingCampaignLink link);

    List<MarketingCampaignLink> listLinks(Long tenantId, Long campaignId);

    MarketingCampaignLink findLinkById(Long tenantId, Long linkId);

    void deleteLink(Long tenantId, Long linkId);
}
