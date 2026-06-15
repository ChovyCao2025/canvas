package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.GrowthActivityFacade;
import org.chovy.canvas.marketing.domain.GrowthActivityCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrowthActivityApplicationService implements GrowthActivityFacade {

    private final GrowthActivityCatalog catalog;

    public GrowthActivityApplicationService() {
        this(Clock.systemDefaultZone());
    }

    GrowthActivityApplicationService(Clock clock) {
        this.catalog = new GrowthActivityCatalog(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertActivity(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, Integer limit) {
        return catalog.listActivities(safeTenantId(tenantId), activityType, status, normalizedLimit(limit));
    }

    @Override
    public Map<String, Object> getActivity(Long tenantId, Long activityId) {
        return catalog.getActivity(safeTenantId(tenantId), activityId);
    }

    @Override
    public Map<String, Object> report(Long tenantId, Long activityId) {
        return catalog.report(safeTenantId(tenantId), activityId);
    }

    @Override
    public Map<String, Object> readiness(Long tenantId, Long activityId) {
        return catalog.readiness(safeTenantId(tenantId), activityId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor) {
        return catalog.transitionActivity(safeTenantId(tenantId), activityId, transition, actorOrDefault(actor));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long tenantId,
                                       Long activityId,
                                       String operation,
                                       Map<String, Object> payload,
                                       String actor) {
        return catalog.execute(safeTenantId(tenantId), activityId, operation, payload, actorOrDefault(actor));
    }

    @Override
    public List<Map<String, Object>> list(Long tenantId,
                                          Long activityId,
                                          String resource,
                                          Map<String, Object> criteria,
                                          Integer limit) {
        return catalog.list(safeTenantId(tenantId), activityId, resource, criteria, normalizedLimit(limit));
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
