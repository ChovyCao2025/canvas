package org.chovy.canvas.cdp.api;

import java.util.List;
import java.util.Map;

public interface CdpWarehouseAvailabilityFacade {

    Map<String, Object> availability(Long tenantId, String from, String to, String mode);

    Map<String, Object> recordAssetAvailability(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listAssetAvailability(
            Long tenantId,
            String assetType,
            String assetKey,
            String mode,
            Integer limit);

    Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor);

    List<Map<String, Object>> listContracts(Long tenantId, String consumerType, String status);

    Map<String, Object> evaluateContract(Long tenantId, String contractKey, String from, String to);

    Map<String, Object> scanWarehouseIncidents(Long tenantId, Map<String, Object> payload, String actor);

    Map<String, Object> scanConsumerIncidents(Long tenantId, Map<String, Object> payload, String actor);
}
