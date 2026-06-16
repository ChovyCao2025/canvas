package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingPreferenceFacade;
import org.chovy.canvas.marketing.domain.MarketingPreferenceCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排MarketingPreference相关的应用层用例。
 */
@Service
public class MarketingPreferenceApplicationService implements MarketingPreferenceFacade {

    /**
     * 保存DEFAULT_TENANT_ID字段值。
     */
    private static final Long DEFAULT_TENANT_ID = 0L;

    /**
     * 承载该应用服务的内存目录。
     */
    private final MarketingPreferenceCatalog catalog;

    /**
     * 创建MarketingPreferenceApplicationService实例。
     */
    public MarketingPreferenceApplicationService() {
        this(new MarketingPreferenceCatalog());
    }

    /**
     * 创建MarketingPreferenceApplicationService实例。
     */
    public MarketingPreferenceApplicationService(MarketingPreferenceCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * 执行report业务操作。
     */
    @Override
    public PreferenceReport report(Long tenantId, String userId) {
        return catalog.report(safeTenantId(tenantId), userId);
    }

    /**
     * 更新consent业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsentRow updateConsent(Long tenantId, String userId, ConsentUpdateCommand command) {
        return catalog.updateConsent(safeTenantId(tenantId), userId, command);
    }

    /**
     * 更新channel业务对象。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChannelRow updateChannel(Long tenantId, String userId, ChannelUpdateCommand command) {
        return catalog.updateChannel(safeTenantId(tenantId), userId, command);
    }

    /**
     * 执行addSuppression业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SuppressionRow addSuppression(Long tenantId, String userId, SuppressionCreateCommand command) {
        return catalog.addSuppression(safeTenantId(tenantId), userId, command);
    }

    /**
     * 执行deactivateSuppression业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateSuppression(Long tenantId, Long suppressionId) {
        catalog.deactivateSuppression(safeTenantId(tenantId), suppressionId);
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }
}
