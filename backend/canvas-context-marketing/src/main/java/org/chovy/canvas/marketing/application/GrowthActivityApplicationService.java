package org.chovy.canvas.marketing.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.marketing.api.GrowthActivityFacade;
import org.chovy.canvas.marketing.domain.GrowthActivityCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 编排GrowthActivity相关的应用层用例。
 */
@Service
public class GrowthActivityApplicationService implements GrowthActivityFacade {

    /**
     * 承载该应用服务的内存目录。
     */
    private final GrowthActivityCatalog catalog;

    /**
     * 创建GrowthActivityApplicationService实例。
     */
    public GrowthActivityApplicationService() {
        this(Clock.systemDefaultZone());
    }

    GrowthActivityApplicationService(Clock clock) {
        this.catalog = new GrowthActivityCatalog(clock);
    }

    /**
     * 执行upsertActivity业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertActivity(safeTenantId(tenantId), payload, actorOrDefault(actor));
    }

    /**
     * 查询activities列表。
     */
    @Override
    public List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, Integer limit) {
        return catalog.listActivities(safeTenantId(tenantId), activityType, status, normalizedLimit(limit));
    }

    /**
     * 返回activity字段值。
     */
    @Override
    public Map<String, Object> getActivity(Long tenantId, Long activityId) {
        return catalog.getActivity(safeTenantId(tenantId), activityId);
    }

    /**
     * 执行report业务操作。
     */
    @Override
    public Map<String, Object> report(Long tenantId, Long activityId) {
        return catalog.report(safeTenantId(tenantId), activityId);
    }

    /**
     * 执行readiness业务操作。
     */
    @Override
    public Map<String, Object> readiness(Long tenantId, Long activityId) {
        return catalog.readiness(safeTenantId(tenantId), activityId);
    }

    /**
     * 执行transitionActivity业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor) {
        return catalog.transitionActivity(safeTenantId(tenantId), activityId, transition, actorOrDefault(actor));
    }

    /**
     * 执行execute业务操作。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> execute(Long tenantId,
                                       Long activityId,
                                       String operation,
                                       Map<String, Object> payload,
                                       String actor) {
        return catalog.execute(safeTenantId(tenantId), activityId, operation, payload, actorOrDefault(actor));
    }

    /**
     * 查询列表。
     */
    @Override
    public List<Map<String, Object>> list(Long tenantId,
                                          Long activityId,
                                          String resource,
                                          Map<String, Object> criteria,
                                          Integer limit) {
        return catalog.list(safeTenantId(tenantId), activityId, resource, criteria, normalizedLimit(limit));
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
