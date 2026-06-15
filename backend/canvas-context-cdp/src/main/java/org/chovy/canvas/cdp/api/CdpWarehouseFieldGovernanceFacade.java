package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseFieldGovernanceFacade {

    List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus);

    Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload);

    Map<String, Object> evaluateBiQuery(Long tenantId, String actor, String role, Map<String, Object> request);
}
