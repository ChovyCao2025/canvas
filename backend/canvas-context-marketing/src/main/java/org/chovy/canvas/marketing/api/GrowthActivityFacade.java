package org.chovy.canvas.marketing.api;

import java.util.List;
import java.util.Map;

public interface GrowthActivityFacade {

    Map<String, Object> upsertActivity(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listActivities(Long tenantId, String activityType, String status, Integer limit);

    Map<String, Object> getActivity(Long tenantId, Long activityId);

    Map<String, Object> report(Long tenantId, Long activityId);

    Map<String, Object> readiness(Long tenantId, Long activityId);

    Map<String, Object> transitionActivity(Long tenantId, Long activityId, String transition, String actor);

    Map<String, Object> execute(Long tenantId,
                                Long activityId,
                                String operation,
                                Map<String, Object> payload,
                                String actor);

    List<Map<String, Object>> list(Long tenantId,
                                   Long activityId,
                                   String resource,
                                   Map<String, Object> criteria,
                                   Integer limit);
}
