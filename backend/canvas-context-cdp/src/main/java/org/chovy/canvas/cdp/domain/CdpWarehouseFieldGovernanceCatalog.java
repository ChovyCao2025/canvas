package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpWarehouseFieldGovernanceCatalog {

    private static final String BI_EVALUATE_ACTION = "BI_EVALUATE";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String POLICY_ALLOW = "ALLOW";
    private static final String POLICY_DENY = "DENY";
    private static final String POLICY_MASK = "MASK";
    private static final String DECISION_ALLOW = "ALLOW";
    private static final String DECISION_DENY = "DENY";
    private static final List<String> DEFAULT_USAGES = List.of("SELECT", "FILTER", "SORT", "GROUP");
    private static final Set<String> SUPPORTED_DATASETS = Set.of("canvas_daily_stats");

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Map<String, Map<String, Object>>> policiesByTenant = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> deniedAudits = new ArrayList<>();

    public List<Map<String, Object>> listPolicies(Long tenantId, String datasetKey, String lifecycleStatus) {
        String filteredDataset = textOrNull(datasetKey);
        String filteredStatus = upperOrNull(lifecycleStatus);
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Long scopedTenant : List.of(0L, tenantId)) {
            for (Map<String, Object> policy : policiesByTenant.getOrDefault(scopedTenant, Map.of()).values()) {
                if (filteredDataset != null && !filteredDataset.equals(policy.get("datasetKey"))) {
                    continue;
                }
                if (filteredStatus != null && !filteredStatus.equals(policy.get("lifecycleStatus"))) {
                    continue;
                }
                merged.put(policyKey(policy), copy(policy));
            }
        }
        return new ArrayList<>(merged.values());
    }

    public Map<String, Object> upsertPolicy(Long tenantId, Map<String, Object> payload) {
        Map<String, Object> policy = ordered();
        policy.put("id", ids.incrementAndGet());
        policy.put("tenantId", tenantId);
        policy.put("datasetKey", required(payload, "datasetKey"));
        policy.put("fieldKey", required(payload, "fieldKey"));
        policy.put("physicalName", required(payload, "physicalName"));
        policy.put("columnName", required(payload, "columnName"));
        policy.put("valueType", upperRequired(payload, "valueType"));
        policy.put("semanticType", upperOrNull(payload.get("semanticType")));
        policy.put("piiLevel", upperDefault(payload.get("piiLevel"), "NORMAL"));
        policy.put("accessPolicy", upperDefault(payload.get("accessPolicy"), POLICY_ALLOW));
        policy.put("minRole", upperDefault(payload.get("minRole"), "OPERATOR"));
        policy.put("allowedUsages", normalizedUsages(payload.get("allowedUsages")));
        policy.put("maskStrategy", upperOrNull(payload.get("maskStrategy")));
        policy.put("lifecycleStatus", upperDefault(payload.get("lifecycleStatus"), STATUS_ACTIVE));
        policy.put("ownerName", textOrNull(payload.get("ownerName")));
        policy.put("description", textOrNull(payload.get("description")));
        policiesByTenant.computeIfAbsent(tenantId, ignored -> new LinkedHashMap<>())
                .put(policyKey(policy), policy);
        return copy(policy);
    }

    public Map<String, Object> evaluateBiQuery(Long tenantId, String actor, String role, Map<String, Object> request) {
        String datasetKey = required(request, "datasetKey");
        if (!SUPPORTED_DATASETS.contains(datasetKey)) {
            throw new IllegalArgumentException("dataset is not supported: " + datasetKey);
        }
        Map<String, Map<String, Object>> activePolicies = activePolicyMap(tenantId, datasetKey);
        List<Map<String, Object>> decisions = collectUsages(request).stream()
                .map(usage -> decide(usage, activePolicies.get(String.valueOf(usage.get("fieldKey"))), role))
                .toList();
        List<Map<String, Object>> denied = decisions.stream()
                .filter(decision -> DECISION_DENY.equals(decision.get("decision")))
                .toList();
        if (!denied.isEmpty()) {
            denied.forEach(decision -> deniedAudits.add(audit(tenantId, actor, role, datasetKey, decision)));
        }

        Map<String, Object> evaluation = ordered();
        evaluation.put("tenantId", tenantId);
        evaluation.put("datasetKey", datasetKey);
        evaluation.put("actor", actor);
        evaluation.put("actorId", actor);
        evaluation.put("role", upperDefault(role, "OPERATOR"));
        evaluation.put("actorRole", upperDefault(role, "OPERATOR"));
        evaluation.put("actionKey", BI_EVALUATE_ACTION);
        evaluation.put("allowed", denied.isEmpty());
        evaluation.put("decisions", decisions);
        evaluation.put("reason", denied.isEmpty() ? "allowed" : denied.get(0).get("reason"));
        return evaluation;
    }

    private Map<String, Map<String, Object>> activePolicyMap(Long tenantId, String datasetKey) {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        for (Long scopedTenant : List.of(0L, tenantId)) {
            for (Map<String, Object> policy : policiesByTenant.getOrDefault(scopedTenant, Map.of()).values()) {
                if (datasetKey.equals(policy.get("datasetKey")) && STATUS_ACTIVE.equals(policy.get("lifecycleStatus"))) {
                    merged.put(String.valueOf(policy.get("fieldKey")), policy);
                }
            }
        }
        return merged;
    }

    private static List<Map<String, Object>> collectUsages(Map<String, Object> request) {
        List<Map<String, Object>> usages = new ArrayList<>();
        for (String dimension : stringList(request.get("dimensions"))) {
            usages.add(usage(dimension, "SELECT"));
            usages.add(usage(dimension, "GROUP"));
        }
        for (String metric : stringList(request.get("metrics"))) {
            usages.add(usage(metric, "SELECT"));
        }
        for (Map<String, Object> filter : mapList(request.get("filters"))) {
            usages.add(usage(fieldName(filter), "FILTER"));
        }
        Object sortSource = request.containsKey("sorts") ? request.get("sorts") : request.get("sort");
        for (Map<String, Object> sort : mapList(sortSource)) {
            usages.add(usage(fieldName(sort), "SORT"));
        }
        return usages.stream()
                .filter(usage -> usage.get("fieldKey") != null)
                .toList();
    }

    private static Map<String, Object> decide(Map<String, Object> usage, Map<String, Object> policy, String role) {
        if (policy == null) {
            return decision(usage, DECISION_ALLOW, "no active field policy", "UNKNOWN", POLICY_ALLOW, "OPERATOR");
        }
        String accessPolicy = upperDefault(policy.get("accessPolicy"), POLICY_ALLOW);
        String minRole = upperDefault(policy.get("minRole"), "OPERATOR");
        String piiLevel = upperDefault(policy.get("piiLevel"), "NORMAL");
        String usageName = String.valueOf(usage.get("usage"));
        if (POLICY_DENY.equals(accessPolicy)) {
            return decision(usage, DECISION_DENY, "field policy DENY blocks BI usage", piiLevel, accessPolicy, minRole);
        }
        if (!allowedUsages(policy.get("allowedUsages")).contains(usageName)) {
            return decision(usage, DECISION_DENY, "usage " + usageName + " is not allowed for field",
                    piiLevel, accessPolicy, minRole);
        }
        if (!Set.of(POLICY_ALLOW, POLICY_MASK, POLICY_DENY).contains(accessPolicy)) {
            return decision(usage, DECISION_DENY, "unknown access policy " + accessPolicy,
                    piiLevel, accessPolicy, minRole);
        }
        String actorRole = upperDefault(role, "OPERATOR");
        if (roleRank(actorRole) < roleRank(minRole)) {
            return decision(usage, DECISION_DENY, "role " + actorRole + " is below required " + minRole,
                    piiLevel, accessPolicy, minRole);
        }
        String reason = POLICY_MASK.equals(accessPolicy)
                ? "MASK policy allowed for role " + actorRole
                : "field policy allowed";
        return decision(usage, DECISION_ALLOW, reason, piiLevel, accessPolicy, minRole);
    }

    private static Map<String, Object> decision(Map<String, Object> usage, String decision, String reason,
                                                String piiLevel, String accessPolicy, String minRole) {
        Map<String, Object> result = ordered();
        result.put("fieldKey", usage.get("fieldKey"));
        result.put("usage", usage.get("usage"));
        result.put("decision", decision);
        result.put("reason", reason);
        result.put("piiLevel", piiLevel);
        result.put("accessPolicy", accessPolicy);
        result.put("minRole", minRole);
        return result;
    }

    private static Map<String, Object> audit(Long tenantId, String actor, String role, String datasetKey,
                                             Map<String, Object> decision) {
        Map<String, Object> audit = ordered();
        audit.put("tenantId", tenantId);
        audit.put("actor", actor);
        audit.put("role", role);
        audit.put("datasetKey", datasetKey);
        audit.put("fieldKey", decision.get("fieldKey"));
        audit.put("actionKey", BI_EVALUATE_ACTION);
        audit.put("reason", decision.get("reason"));
        return audit;
    }

    private static int roleRank(String role) {
        return switch (upperDefault(role, "OPERATOR")) {
            case "TENANT_ADMIN" -> 2;
            case "ADMIN", "SUPER_ADMIN" -> 3;
            default -> 1;
        };
    }

    private static List<String> allowedUsages(Object value) {
        String normalized = normalizedUsages(value);
        return normalized.isBlank() ? DEFAULT_USAGES : List.of(normalized.split(","));
    }

    private static String normalizedUsages(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return String.join(",", DEFAULT_USAGES);
        }
        LinkedHashSet<String> usages = new LinkedHashSet<>();
        for (String item : String.valueOf(value).split(",")) {
            if (!item.isBlank()) {
                usages.add(item.trim().toUpperCase(Locale.ROOT));
            }
        }
        return String.join(",", usages);
    }

    private static String fieldName(Map<String, Object> item) {
        for (String key : List.of("fieldKey", "field", "metricKey", "dimensionKey", "column")) {
            Object value = item.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static Map<String, Object> usage(String fieldKey, String usage) {
        Map<String, Object> result = ordered();
        result.put("fieldKey", fieldKey);
        result.put("usage", usage);
        return result;
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .map(String::trim)
                .toList();
    }

    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }
        List<Map<String, Object>> maps = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> converted = ordered();
                map.forEach((key, mapValue) -> converted.put(String.valueOf(key), mapValue));
                maps.add(converted);
            }
        }
        return maps;
    }

    private static String required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    private static String upperRequired(Map<String, Object> payload, String key) {
        return required(payload, key).toUpperCase(Locale.ROOT);
    }

    private static String upperDefault(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank()
                ? defaultValue
                : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private static String upperOrNull(Object value) {
        String text = textOrNull(value);
        return text == null ? null : text.toUpperCase(Locale.ROOT);
    }

    private static String textOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value).trim();
    }

    private static String policyKey(Map<String, Object> policy) {
        return policy.get("datasetKey") + ":" + policy.get("fieldKey");
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }
}
