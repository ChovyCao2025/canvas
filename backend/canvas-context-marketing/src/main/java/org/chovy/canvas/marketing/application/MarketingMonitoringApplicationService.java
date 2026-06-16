package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingMonitoringFacade;
import org.chovy.canvas.marketing.domain.MarketingMonitoringCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排MarketingMonitoring相关的应用层用例。
 */
@Service
public class MarketingMonitoringApplicationService implements MarketingMonitoringFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final MarketingMonitoringCatalog catalog;

    /**
     * 创建MarketingMonitoringApplicationService实例。
     */
    public MarketingMonitoringApplicationService() {
        this(Clock.systemDefaultZone());
    }

    MarketingMonitoringApplicationService(Clock clock) {
        this.catalog = new MarketingMonitoringCatalog(clock);
    }

    /**
     * 执行execute业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor) {
        return catalog.execute(safeTenantId(tenantId), operation, payload, actorOrDefault(actor));
    }

    /**
     * 查询列表。
     */
    @Override
    public List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, Integer limit) {
        return catalog.list(safeTenantId(tenantId), operation, criteria, normalizedLimit(limit));
    }

    /**
     * 执行safeTenantId业务操作。
     */
    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    /**
     * 执行actorOrDefault业务操作。
     */
    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    /**
     * 规范化dLimit输入值。
     */
    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
