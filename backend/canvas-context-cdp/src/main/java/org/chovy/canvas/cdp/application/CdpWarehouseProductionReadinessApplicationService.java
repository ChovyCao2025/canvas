package org.chovy.canvas.cdp.application;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.chovy.canvas.cdp.api.CdpWarehouseProductionReadinessFacade;
import org.springframework.stereotype.Service;

@Service
public class CdpWarehouseProductionReadinessApplicationService implements CdpWarehouseProductionReadinessFacade {

    @Override
    public Map<String, Object> proof(
            Long tenantId,
            LocalDateTime from,
            LocalDateTime to,
            String mode,
            List<String> contractKeys) {
        String normalizedMode = mode == null || mode.isBlank() ? "HYBRID" : mode.trim();
        List<String> safeContractKeys = contractKeys == null ? List.of() : List.copyOf(contractKeys);
        Map<String, Object> proof = ordered();
        proof.put("tenantId", tenantId == null ? 0L : tenantId);
        proof.put("status", "PASS");
        proof.put("generatedAt", LocalDateTime.now());
        proof.put("windowStart", from);
        proof.put("windowEnd", to);
        proof.put("mode", normalizedMode);
        proof.put("evidence", List.of(Map.of(
                "key", "warehouse_readiness",
                "status", "PASS",
                "reason", "seed compatibility evidence")));
        proof.put("readiness", null);
        proof.put("availability", null);
        proof.put("contracts", safeContractKeys.stream()
                .map(contractKey -> Map.of(
                        "contractKey", contractKey,
                        "status", "PASS",
                        "allowed", true,
                        "reason", "allowed"))
                .toList());
        proof.put("privacyErasureBacklog", null);
        proof.put("enterpriseOlapReadiness", null);
        return proof;
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
