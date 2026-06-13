package org.chovy.canvas.marketing.api;

import java.util.List;

public interface MarketingCampaignFacade {

    MarketingCampaignView upsertCampaign(Long tenantId, MarketingCampaignCommand command, String actor);

    List<MarketingCampaignView> listCampaigns(Long tenantId, String status, Integer limit);

    MarketingCampaignLinkView linkResource(Long tenantId, MarketingCampaignLinkCommand command, String actor);

    List<MarketingCampaignLinkView> listLinks(Long tenantId, Long campaignId);

    MarketingCampaignReadinessView readiness(Long tenantId, Long campaignId);

    void unlinkResource(Long tenantId, Long linkId);
}
