package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护MarketingIntegration相关的内存业务目录。
 */
public class MarketingIntegrationCatalog {

    /**
     * 执行upsertContract业务操作。
     */
    public Map<String, Object> upsertContract(Long tenantId, Map<String, Object> payload, String actor) {
        String providerKey = requiredString(payload, "providerKey");
        return contract(tenantId, 3001L, providerKey, stringValue(payload, "providerFamily", "SOCIAL"),
                stringValue(payload, "status", "ACTIVE"), actor);
    }

    /**
     * 查询contracts列表。
     */
    public List<Map<String, Object>> listContracts(Long tenantId, String status, String providerFamily, int limit) {
        return List.of(contract(tenantId, 3001L, "meta", blankToDefault(providerFamily, "SOCIAL"),
                blankToDefault(status, "ACTIVE"), "operator-1"));
    }

    /**
     * 查询contractAuditEvents列表。
     */
    public List<Map<String, Object>> listContractAuditEvents(Long tenantId, Long contractId, int limit) {
        return List.of(values(
                "auditEventId", 3002L,
                "tenantId", tenantId,
                "contractId", contractId,
                "eventType", "CONTRACT_UPDATED",
                "actor", "operator-1",
                "createdAt", now(),
                "limit", limit));
    }

    /**
     * 执行archiveContract业务操作。
     */
    public Map<String, Object> archiveContract(Long tenantId, Long contractId, String actor) {
        return contract(tenantId, contractId, "meta", "SOCIAL", "ARCHIVED", actor);
    }

    /**
     * 执行recordProbeRun业务操作。
     */
    public Map<String, Object> recordProbeRun(Long tenantId, Long contractId, Map<String, Object> payload,
                                              String actor) {
        return values(
                "probeRunId", 3101L,
                "tenantId", tenantId,
                "contractId", contractId,
                "probeKey", stringValue(payload, "probeKey", "auth"),
                "status", "PASSED",
                "providerFamily", stringValue(payload, "providerFamily", "SOCIAL"),
                "recordedBy", actor,
                "recordedAt", now());
    }

    /**
     * 查询probeRuns列表。
     */
    public List<Map<String, Object>> listProbeRuns(Long tenantId, String status, String providerFamily, int limit) {
        return List.of(values(
                "probeRunId", 3101L,
                "tenantId", tenantId,
                "contractId", 3001L,
                "status", blankToDefault(status, "PASSED"),
                "providerFamily", blankToDefault(providerFamily, "SOCIAL"),
                "limit", limit));
    }

    /**
     * 执行scanProbeRuns业务操作。
     */
    public Map<String, Object> scanProbeRuns(Long tenantId, int limit, String actor) {
        return values(
                "tenantId", tenantId,
                "scannedCount", Math.min(1, limit),
                "createdRunCount", Math.min(1, limit),
                "triggeredBy", actor,
                "scannedAt", now());
    }

    /**
     * 查询contractSloEvaluations列表。
     */
    public List<Map<String, Object>> listContractSloEvaluations(Long tenantId, int limit) {
        return List.of(values(
                "evaluationId", 3201L,
                "tenantId", tenantId,
                "contractId", 3001L,
                "status", "PASS",
                "availability", 0.999,
                "evaluatedAt", now(),
                "limit", limit));
    }

    /**
     * 执行recordProbe业务操作。
     */
    public Map<String, Object> recordProbe(Long tenantId, Long contractId, Map<String, Object> payload, String actor) {
        return probe(tenantId, contractId, stringValue(payload, "probeKey", "auth"), "ACTIVE", actor);
    }

    /**
     * 查询contractProbes列表。
     */
    public List<Map<String, Object>> listContractProbes(Long tenantId, Long contractId, int limit) {
        Map<String, Object> probe = probe(tenantId, contractId, "auth", "ACTIVE", "operator-1");
        probe.put("limit", limit);
        return List.of(probe);
    }

    /**
     * 查询recentProbes列表。
     */
    public List<Map<String, Object>> listRecentProbes(Long tenantId, String status, int limit) {
        Map<String, Object> probe = probe(tenantId, 3001L, "auth", blankToDefault(status, "ACTIVE"), "operator-1");
        probe.put("limit", limit);
        return List.of(probe);
    }

    /**
     * 执行contract业务操作。
     */
    private static Map<String, Object> contract(Long tenantId, Long contractId, String providerKey,
                                                String providerFamily, String status, String actor) {
        return values(
                "contractId", contractId,
                "tenantId", tenantId,
                "providerKey", providerKey,
                "providerFamily", providerFamily,
                "status", status,
                "updatedBy", actor,
                "updatedAt", now());
    }

    /**
     * 执行probe业务操作。
     */
    private static Map<String, Object> probe(Long tenantId, Long contractId, String probeKey, String status,
                                             String actor) {
        return values(
                "probeId", 3301L,
                "tenantId", tenantId,
                "contractId", contractId,
                "probeKey", probeKey,
                "status", status,
                "updatedBy", actor,
                "updatedAt", now());
    }

    /**
     * 执行values业务操作。
     */
    private static Map<String, Object> values(Object... keysAndValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return values;
    }

    /**
     * 校验并返回dString必填值。
     */
    private static String requiredString(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return value;
    }

    /**
     * 执行stringValue业务操作。
     */
    private static String stringValue(Map<String, Object> payload, String key, String defaultValue) {
        if (payload == null) {
            return defaultValue;
        }
        Object value = payload.get(key);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text.trim();
    }

    /**
     * 执行blankToDefault业务操作。
     */
    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /**
     * 执行now业务操作。
     */
    private static LocalDateTime now() {
        return LocalDateTime.now();
    }
}
