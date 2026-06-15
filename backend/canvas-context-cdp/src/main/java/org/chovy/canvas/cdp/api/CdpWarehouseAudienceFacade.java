package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseAudienceFacade {

    Map<String, Object> materialize(Long tenantId, Long audienceId, String actor);

    Map<String, Object> materializeGated(Long tenantId, Long audienceId, Map<String, Object> payload, String actor);

    Map<String, Object> materializeContractGated(Long tenantId, Long audienceId, Map<String, Object> payload,
            String actor);

    Map<String, Object> rollback(Long tenantId, Long audienceId, Map<String, Object> payload, String actor);

    Map<String, Object> refreshDue(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> refreshDueGated(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> recentRuns(Long tenantId, Long audienceId, String status, Integer limit);
}
