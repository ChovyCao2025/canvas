package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;
import org.chovy.canvas.marketing.domain.MarketingPreferenceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingPreferenceApplicationService implements MarketingPreferenceFacade {

    private static final Long DEFAULT_TENANT_ID = 0L;

    private final MarketingPreferenceCatalog catalog;

    public MarketingPreferenceApplicationService() {
        this(new MarketingPreferenceCatalog());
    }

    public MarketingPreferenceApplicationService(MarketingPreferenceCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public PreferenceReport report(Long tenantId, String userId) {
        return catalog.report(safeTenantId(tenantId), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsentRow updateConsent(Long tenantId, String userId, ConsentUpdateCommand command) {
        return catalog.updateConsent(safeTenantId(tenantId), userId, command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChannelRow updateChannel(Long tenantId, String userId, ChannelUpdateCommand command) {
        return catalog.updateChannel(safeTenantId(tenantId), userId, command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command) {
        return catalog.addSuppression(safeTenantId(tenantId), userId, command);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateSuppression(Long tenantId, Long suppressionId) {
        catalog.deactivateSuppression(safeTenantId(tenantId), suppressionId);
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
