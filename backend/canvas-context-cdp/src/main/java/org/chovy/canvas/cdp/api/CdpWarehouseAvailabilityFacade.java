package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

/**
 * 定义 CdpWarehouseAvailabilityFacade 对外暴露的 CDP 业务能力。
 */
public interface CdpWarehouseAvailabilityFacade {

    /**
     * mode)。
     */
    Map<String, Object> availability(Long tenantId, String from, String to, String mode);

    /**
     * actor)。
     */
    Map<String, Object> recordAssetAvailability(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listAssetAvailability(
            Long tenantId,
            String assetType,
            String assetKey,
            String mode,
            /**
             * limit)。
             */
            Integer limit);

    /**
     * actor)。
     */
    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * status)。
     */
    List<Map<String, Object>> listContracts(Long tenantId, String consumerType, String status);

    /**
     * to)。
     */
    Map<String, Object> evaluateContract(Long tenantId, String contractKey, String from, String to);

    /**
     * actor)。
     */
    Map<String, Object> scanWarehouseIncidents(Long tenantId, Map<String, Object> payload, String actor);

    /**
     * actor)。
     */
    Map<String, Object> scanConsumerIncidents(Long tenantId, Map<String, Object> payload, String actor);
}
