package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseSloPolicyFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseSloPolicyFacade {

    /**
     * status)。
     */
    List<Map<String, Object>> listPolicies(Long tenantId, String status);

    /**
     * policy Key)。
     */
    Map<String, Object> effectivePolicy(Long tenantId, String policyKey);

    /**
     * payload)。
     */
    Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload);
}
