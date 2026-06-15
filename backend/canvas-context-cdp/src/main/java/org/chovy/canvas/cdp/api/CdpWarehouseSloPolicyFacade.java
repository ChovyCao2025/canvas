package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseSloPolicyFacade {

    List<Map<String, Object>> listPolicies(Long tenantId, String status);

    Map<String, Object> effectivePolicy(Long tenantId, String policyKey);

    Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload);
}
