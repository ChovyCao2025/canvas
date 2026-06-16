package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseAudienceFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseAudienceFacade {

    /**
     * actor)。
     */
    Map<String, Object> materialize(Long tenantId, Long audienceId, String actor);

    /**
     * actor)。
     */
    Map<String, Object> materializeGated(Long tenantId, Long audienceId, Map<String, Object> payload, String actor);

    Map<String, Object> materializeContractGated(Long tenantId, Long audienceId, Map<String, Object> payload,
            /**
             * actor)。
             */
            String actor);

    /**
     * actor)。
     */
    Map<String, Object> rollback(Long tenantId, Long audienceId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> refreshDue(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> refreshDueGated(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * limit)。
     */
    List<Map<String, Object>> recentRuns(Long tenantId, Long audienceId, String status, Integer limit);
}
