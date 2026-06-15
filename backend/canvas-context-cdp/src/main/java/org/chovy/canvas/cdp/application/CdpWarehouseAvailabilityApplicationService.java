package org.chovy.canvas.cdp.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseAvailabilityFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseAvailabilityCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseAvailabilityApplicationService implements CdpWarehouseAvailabilityFacade {

    private final CdpWarehouseAvailabilityCatalog catalog;

    public CdpWarehouseAvailabilityApplicationService() {
        this(new CdpWarehouseAvailabilityCatalog());
    }

    CdpWarehouseAvailabilityApplicationService(CdpWarehouseAvailabilityCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> availability(Long tenantId, String from, String to, String mode) {
        Long scopedTenantId = tenantIdOrDefault(tenantId);
        List<Map<String, Object>> assets = catalog.assets(scopedTenantId);
        long unavailableAssets = assets.stream()
                .filter(asset -> !"AVAILABLE".equalsIgnoreCase(String.valueOf(asset.get("status"))))
                .count();
        Map<String, Object> result = ordered();
        result.put("tenantId", scopedTenantId);
        result.put("mode", defaultString(mode, "HYBRID"));
        result.put("from", from);
        result.put("to", to);
        result.put("assetCount", assets.size());
        result.put("unavailableAssetCount", unavailableAssets);
        result.put("decision", unavailableAssets == 0 ? "AVAILABLE" : "DEGRADED");
        return result;
    }

    @Override
    public Map<String, Object> recordAssetAvailability(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.recordAsset(tenantIdOrDefault(tenantId), safePayload(payload), actor);
    }

    @Override
    public List<Map<String, Object>> listAssetAvailability(
            Long tenantId,
            String assetType,
            String assetKey,
            String mode,
            Integer limit) {
        return catalog.listAssets(tenantIdOrDefault(tenantId), assetType, assetKey, mode, limit);
    }

    @Override
    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        return catalog.upsertContract(tenantIdOrDefault(tenantId), safePayload(payload), actor);
    }

    @Override
    public List<Map<String, Object>> listContracts(Long tenantId, String consumerType, String status) {
        return catalog.listContracts(tenantIdOrDefault(tenantId), consumerType, status);
    }

    @Override
    public Map<String, Object> evaluateContract(Long tenantId, String contractKey, String from, String to) {
        Long scopedTenantId = tenantIdOrDefault(tenantId);
        Map<String, Object> contract = catalog.contract(scopedTenantId, contractKey);
        Map<String, Object> result = ordered();
        result.put("tenantId", scopedTenantId);
        result.put("contractKey", contract.get("contractKey"));
        result.put("consumerType", contract.get("consumerType"));
        result.put("assetKey", contract.get("assetKey"));
        result.put("from", from);
        result.put("to", to);
        result.put("decision", "ACTIVE".equalsIgnoreCase(String.valueOf(contract.get("status")))
                ? "AVAILABLE"
                : "DEGRADED");
        result.put("breachCount", 0);
        return result;
    }

    @Override
    public Map<String, Object> scanWarehouseIncidents(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantIdOrDefault(tenantId));
        result.put("mode", defaultString(safePayload(payload).get("mode"), "HYBRID"));
        result.put("operator", actor);
        result.put("incidentCount", 0);
        result.put("openedIncidentKeys", List.of());
        return result;
    }

    @Override
    public Map<String, Object> scanConsumerIncidents(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> body = safePayload(payload);
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantIdOrDefault(tenantId));
        result.put("contractKey", body.get("contractKey"));
        result.put("consumerType", defaultString(body.get("consumerType"), "generic"));
        result.put("operator", actor);
        result.put("incidentCount", 0);
        result.put("openedIncidentKeys", List.of());
        return result;
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    private static String defaultString(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value).trim();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
