package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade;
import org.chovy.canvas.marketing.domain.MarketingPolicyCatalog;
import org.springframework.stereotype.Service;

@Service
public class MarketingPolicyApplicationService implements MarketingPolicyFacade {

    private final MarketingPolicyCatalog catalog;

    public MarketingPolicyApplicationService() {
        this(new MarketingPolicyCatalog());
    }

    MarketingPolicyApplicationService(MarketingPolicyCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PolicyState policyState(Long tenantId, String userId, String channel) {
        return catalog.policyState(tenantId, userId, channel);
    }

    @Override
    public ConsentView upsertConsent(Long tenantId, ConsentCommand command) {
        return catalog.upsertConsent(tenantId, command);
    }

    @Override
    public SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command) {
        return catalog.upsertSuppression(tenantId, command);
    }

    @Override
    public ChannelView upsertChannel(Long tenantId, ChannelCommand command) {
        return catalog.upsertChannel(tenantId, command);
    }
}
