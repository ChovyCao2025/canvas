package org.chovy.canvas.cdp.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseE2eCertificationFacade;
import org.chovy.canvas.cdp.domain.CdpWarehouseE2eCertificationCatalog;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseE2eCertificationApplicationService implements CdpWarehouseE2eCertificationFacade {

    private final CdpWarehouseE2eCertificationCatalog catalog;

    public CdpWarehouseE2eCertificationApplicationService() {
        this(new CdpWarehouseE2eCertificationCatalog());
    }

    CdpWarehouseE2eCertificationApplicationService(CdpWarehouseE2eCertificationCatalog catalog) {
        this.catalog = catalog;
    }

    @Override
    public Map<String, Object> certify(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof) {
        return catalog.certification(tenantIdOrDefault(tenantId), from, to, normalizeMode(mode),
                safeContractKeys(contractKeys), requirePhysical, requireRealtime, requireDataPathProof);
    }

    @Override
    public Map<String, Object> gate(Long tenantId, String mode, List<String> contractKeys, boolean requirePhysical,
            boolean requireRealtime, boolean requireDataPathProof, Long maxAgeMinutes) {
        Long scopedTenantId = tenantIdOrDefault(tenantId);
        String normalizedMode = normalizeMode(mode);
        List<String> safeContractKeys = safeContractKeys(contractKeys);
        long safeMaxAge = maxAgeMinutes == null || maxAgeMinutes < 1 ? 60L : maxAgeMinutes;
        Map<String, Object> matched = catalog.latestMatchingRun(scopedTenantId, normalizedMode, requirePhysical,
                requireRealtime, requireDataPathProof);

        Map<String, Object> decision = ordered();
        decision.put("tenantId", scopedTenantId);
        decision.put("status", matched == null ? "FAIL" : "PASS");
        decision.put("reason", matched == null ? "no matching certification run" : "fresh PASS certification evidence");
        decision.put("matchedRunId", matched == null ? null : matched.get("id"));
        decision.put("matchedRunStatus", matched == null ? null : matched.get("status"));
        decision.put("matchedFinishedAt", matched == null ? null : matched.get("finishedAt"));
        decision.put("expiresAt", matched == null ? null : "2026-06-15T02:30:00");
        decision.put("mode", normalizedMode);
        decision.put("requirePhysical", requirePhysical);
        decision.put("requireRealtime", requireRealtime);
        decision.put("requireDataPathProof", requireDataPathProof);
        decision.put("maxAgeMinutes", safeMaxAge);
        decision.put("contractKeys", safeContractKeys);
        return decision;
    }

    @Override
    public Map<String, Object> run(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof, String requestedBy) {
        return catalog.createRun(tenantIdOrDefault(tenantId), from, to, normalizeMode(mode),
                safeContractKeys(contractKeys), requirePhysical, requireRealtime, requireDataPathProof,
                defaultString(requestedBy, "system"));
    }

    @Override
    public List<Map<String, Object>> recent(Long tenantId, Integer limit) {
        return catalog.recent(tenantIdOrDefault(tenantId), limit);
    }

    @Override
    public Map<String, Object> get(Long tenantId, Long id) {
        return catalog.get(tenantIdOrDefault(tenantId), id);
    }

    private static Long tenantIdOrDefault(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static String normalizeMode(String mode) {
        return defaultString(mode, "HYBRID").toUpperCase(Locale.ROOT);
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static List<String> safeContractKeys(List<String> contractKeys) {
        if (contractKeys == null) {
            return List.of();
        }
        return contractKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
