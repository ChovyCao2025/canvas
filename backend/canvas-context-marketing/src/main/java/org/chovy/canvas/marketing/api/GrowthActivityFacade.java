package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

/**
 * 定义GrowthActivityFacade的营销上下文访问契约。
 */
public interface GrowthActivityFacade {

    /**
     * 执行upsertActivity业务操作。
     */
    Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * 查询activities列表。
     */
    List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, Integer limit);

    /**
     * 返回activity字段值。
     */
    Map<String, Object> getActivity(Long tenantId, Long activityId);

    /**
     * 执行report业务操作。
     */
    Map<String, Object> report(Long tenantId, Long activityId);

    /**
     * 执行readiness业务操作。
     */
    Map<String, Object> readiness(Long tenantId, Long activityId);

    /**
     * 执行transitionActivity业务操作。
     */
    Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor);

    /**
     * 执行execute业务操作。
     */
    Map<String, Object> execute(Long tenantId,
                                Long activityId,
                                String operation,
                                Map<String, Object> payload,
                                String actor);

    /**
     * 查询列表。
     */
    List<Map<String, Object>> list(Long tenantId,
                                   Long activityId,
                                   String resource,
                                   Map<String, Object> criteria,
                                   Integer limit);
}
