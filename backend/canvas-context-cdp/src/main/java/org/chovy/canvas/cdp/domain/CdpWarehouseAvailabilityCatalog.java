package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CdpWarehouseAvailabilityCatalog {

    private final Map<Long, List<Map<String, Object>>> assetsByTenant = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Map<String, Object>>> contractsByTenant = new ConcurrentHashMap<>();

    public Map<String, Object> recordAsset(Long tenantId, Map<String, Object> payload, String actor) {
        String assetKey = required(payload, "assetKey");
        Map<String, Object> asset = ordered();
        asset.put("tenantId", tenantId);
        asset.put("assetType", stringOrDefault(payload.get("assetType"), "table"));
        asset.put("assetKey", assetKey);
        asset.put("mode", stringOrDefault(payload.get("mode"), "HYBRID"));
        asset.put("status", stringOrDefault(payload.get("status"), "AVAILABLE"));
        asset.put("freshnessLagMinutes", integerOrDefault(payload.get("freshnessLagMinutes"), 0));
        asset.put("updatedBy", actor);
        asset.put("updatedAt", LocalDateTime.parse("2026-06-14T00:00:00"));

        List<Map<String, Object>> assets = assetsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>());
        assets.removeIf(existing -> assetKey.equals(existing.get("assetKey")));
        assets.add(asset);
        return copy(asset);
    }

    public List<Map<String, Object>> listAssets(
            Long tenantId,
            String assetType,
            String assetKey,
            String mode,
            Integer limit) {
        int boundedLimit = limit == null || limit < 1 ? 50 : Math.min(limit, 200);
        return assetsByTenant.getOrDefault(tenantId, List.of()).stream()
                .filter(asset -> matches(asset.get("assetType"), assetType))
                .filter(asset -> matches(asset.get("assetKey"), assetKey))
                .filter(asset -> matches(asset.get("mode"), mode))
                .limit(boundedLimit)
                .map(CdpWarehouseAvailabilityCatalog::copy)
                .toList();
    }

    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        String contractKey = required(payload, "contractKey");
        Map<String, Object> contract = ordered();
        contract.put("tenantId", tenantId);
        contract.put("contractKey", contractKey);
        contract.put("consumerType", stringOrDefault(payload.get("consumerType"), "generic"));
        contract.put("assetKey", stringOrDefault(payload.get("assetKey"), contractKey));
        contract.put("maxFreshnessLagMinutes", integerOrDefault(payload.get("maxFreshnessLagMinutes"), 60));
        contract.put("status", stringOrDefault(payload.get("status"), "ACTIVE"));
        contract.put("updatedBy", actor);
        contract.put("updatedAt", LocalDateTime.parse("2026-06-14T00:00:00"));

        contractsByTenant.computeIfAbsent(tenantId, ignored -> new ConcurrentHashMap<>()).put(contractKey, contract);
        return copy(contract);
    }

    public List<Map<String, Object>> listContracts(Long tenantId, String consumerType, String status) {
        return contractsByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .filter(contract -> matches(contract.get("consumerType"), consumerType))
                .filter(contract -> matches(contract.get("status"), status))
                .map(CdpWarehouseAvailabilityCatalog::copy)
                .toList();
    }

    public Map<String, Object> contract(Long tenantId, String contractKey) {
        if (contractKey == null || contractKey.isBlank()) {
            throw new IllegalArgumentException("contractKey is required");
        }
        Map<String, Object> contract = contractsByTenant.getOrDefault(tenantId, Map.of()).get(contractKey);
        if (contract == null) {
            throw new IllegalArgumentException("contractKey not found");
        }
        return copy(contract);
    }

    public List<Map<String, Object>> assets(Long tenantId) {
        return listAssets(tenantId, null, null, null, 200);
    }

    private static boolean matches(Object actual, String expected) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(String.valueOf(actual));
    }

    private static String required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static String stringOrDefault(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value).trim();
    }

    private static Integer integerOrDefault(Object value, Integer defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }
}
