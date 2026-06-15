package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.MarketingMonitoringFacade;
import org.chovy.canvas.marketing.domain.MarketingMonitoringCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingMonitoringApplicationService implements MarketingMonitoringFacade {

    private final MarketingMonitoringCatalog catalog;

    public MarketingMonitoringApplicationService() {
        this(Clock.systemDefaultZone());
    }

    MarketingMonitoringApplicationService(Clock clock) {
        this.catalog = new MarketingMonitoringCatalog(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long tenantId, String operation, Map<String, Object> payload, String actor) {
        return catalog.execute(safeTenantId(tenantId), operation, payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> list(Long tenantId, String operation, Map<String, Object> criteria, Integer limit) {
        return catalog.list(safeTenantId(tenantId), operation, criteria, normalizedLimit(limit));
    }

    private static Long safeTenantId(Long tenantId) {
        return tenantId == null || tenantId < 0 ? 0L : tenantId;
    }

    private static String actorOrDefault(String actor) {
        return actor == null || actor.isBlank() ? "system" : actor.trim();
    }

    private static int normalizedLimit(Integer limit) {
        if (limit == null) {
            return 50;
        }
        return Math.max(1, Math.min(limit, 100));
    }
}
