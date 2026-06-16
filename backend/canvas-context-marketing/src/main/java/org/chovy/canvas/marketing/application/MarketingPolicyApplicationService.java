package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingPolicyFacade;
import org.chovy.canvas.marketing.domain.MarketingPolicyCatalog;
import org.springframework.stereotype.Service;

/**
 * 编排MarketingPolicy相关的应用层用例。
 */
@Service
public class MarketingPolicyApplicationService implements MarketingPolicyFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final MarketingPolicyCatalog catalog;

    /**
     * 创建MarketingPolicyApplicationService实例。
     */
    public MarketingPolicyApplicationService() {
        this(new MarketingPolicyCatalog());
    }

    MarketingPolicyApplicationService(MarketingPolicyCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行policyState业务操作。
     */
    @Override
    public PolicyState policyState(Long tenantId, String userId, String channel) {
        return catalog.policyState(tenantId, userId, channel);
    }

    /**
     * 执行upsertConsent业务操作。
     */
    @Override
    public ConsentView upsertConsent(Long tenantId, ConsentCommand command) {
        return catalog.upsertConsent(tenantId, command);
    }

    /**
     * 执行upsertSuppression业务操作。
     */
    @Override
    public SuppressionView upsertSuppression(Long tenantId, SuppressionCommand command) {
        return catalog.upsertSuppression(tenantId, command);
    }

    /**
     * 执行upsertChannel业务操作。
     */
    @Override
    public ChannelView upsertChannel(Long tenantId, ChannelCommand command) {
        return catalog.upsertChannel(tenantId, command);
    }
}
