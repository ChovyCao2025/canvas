package org.chovy.canvas.risk.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class RiskGovernanceCatalog {

    private final Map<Long, TenantRiskGovernance> tenants = new LinkedHashMap<>();

    public List<Map<String, Object>> decisionTraces(Long tenantId, String sceneKey, int limit) {
        TenantRiskGovernance governance = governance(tenantId);
        if (governance.traces.isEmpty()) {
            governance.traces.add(trace(tenantId, value(sceneKey, "DEFAULT_SCENE"), "seed-trace-1"));
        }
        return governance.traces.stream()
                .filter(item -> matches(item, "sceneKey", sceneKey))
                .limit(limit)
                .map(RiskGovernanceCatalog::copy)
                .toList();
    }

    public Map<String, Object> createList(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "listKey");
        TenantRiskGovernance governance = governance(tenantId);
        String listKey = String.valueOf(payload.get("listKey"));
        Map<String, Object> list = record(payload);
        list.put("tenantId", tenantId);
        list.put("listKey", listKey);
        list.put("listType", upper(value(payload.get("listType"), "BLACK")));
        list.put("subjectType", upper(value(payload.get("subjectType"), "USER_ID")));
        list.put("status", upper(value(payload.get("status"), "ACTIVE")));
        list.put("updatedBy", actor);
        governance.lists.put(listKey, list);
        return copy(list);
    }

    public Map<String, Object> addListEntry(Long tenantId, String listKey, Map<String, Object> payload,
                                            String actor) {
        requiredString(listKey, "listKey");
        TenantRiskGovernance governance = governance(tenantId);
        governance.lists.computeIfAbsent(listKey, key -> seedList(tenantId, key));
        Map<String, Object> entry = record(payload);
        entry.put("tenantId", tenantId);
        entry.put("listKey", listKey);
        entry.put("entryId", (long) governance.entries.size() + 1);
        entry.put("subjectValue", value(payload.get("subjectValue"), "subject-" + entry.get("entryId")));
        entry.put("status", "ACTIVE");
        entry.put("updatedBy", actor);
        governance.entries.add(entry);
        return copy(entry);
    }

    public List<Map<String, Object>> listEntries(Long tenantId, String listKey) {
        requiredString(listKey, "listKey");
        return governance(tenantId).entries.stream()
                .filter(item -> matches(item, "listKey", listKey))
                .filter(item -> matches(item, "status", "ACTIVE"))
                .map(RiskGovernanceCatalog::copy)
                .toList();
    }

    public Map<String, Object> removeListEntry(Long tenantId, String listKey, Long entryId, String actor) {
        Map<String, Object> entry = find(governance(tenantId).entries, "entryId", entryId, "entry not found");
        if (!Objects.equals(entry.get("listKey"), listKey)) {
            throw new IllegalArgumentException("entry not found");
        }
        entry.put("status", "REMOVED");
        entry.put("updatedBy", actor);
        return Map.of("tenantId", tenantId, "listKey", listKey, "entryId", entryId, "removed", true);
    }

    public Map<String, Object> importListEntries(Long tenantId, String listKey, Map<String, Object> payload,
                                                 String actor) {
        requiredString(listKey, "listKey");
        int imported = values(payload).size();
        for (String value : values(payload)) {
            addListEntry(tenantId, listKey, Map.of("subjectValue", value), actor);
        }
        return Map.of("tenantId", tenantId, "listKey", listKey, "importedCount", imported, "updatedBy", actor);
    }

    public Map<String, Object> createStrategyDraft(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "strategyKey");
        TenantRiskGovernance governance = governance(tenantId);
        String strategyKey = String.valueOf(payload.get("strategyKey"));
        Map<String, Object> strategy = record(payload);
        strategy.put("tenantId", tenantId);
        strategy.put("strategyKey", strategyKey);
        strategy.put("sceneKey", value(payload.get("sceneKey"), "DEFAULT_SCENE"));
        strategy.put("name", value(payload.get("name"), strategyKey));
        strategy.put("status", "DRAFT");
        strategy.put("draftVersion", nextVersion(governance, strategyKey));
        strategy.put("activeVersion", 0);
        strategy.put("updatedBy", actor);
        governance.strategies.put(strategyKey, strategy);
        governance.versions.add(versionRow(tenantId, strategyKey, (Integer) strategy.get("draftVersion"), "DRAFT"));
        return copy(strategy);
    }

    public Map<String, Object> getStrategy(Long tenantId, String strategyKey) {
        return copy(strategy(tenantId, strategyKey));
    }

    public List<Map<String, Object>> listStrategyVersions(Long tenantId, String strategyKey) {
        return governance(tenantId).versions.stream()
                .filter(item -> matches(item, "strategyKey", strategyKey))
                .map(RiskGovernanceCatalog::copy)
                .toList();
    }

    public Map<String, Object> validateStrategyVersion(Long tenantId, String strategyKey, int version, String actor) {
        Map<String, Object> row = version(tenantId, strategyKey, version);
        row.put("validationStatus", "PASSED");
        row.put("updatedBy", actor);
        return copy(row);
    }

    public Map<String, Object> simulateStrategyVersion(Long tenantId, String strategyKey, int version, String actor) {
        Map<String, Object> row = version(tenantId, strategyKey, version);
        row.put("simulationStatus", "PASSED");
        row.put("updatedBy", actor);
        return copy(row);
    }

    public Map<String, Object> submitStrategyVersion(Long tenantId, String strategyKey, int version, String actor) {
        return transition(tenantId, strategyKey, version, "SUBMITTED", actor);
    }

    public Map<String, Object> approveStrategyVersion(Long tenantId, String strategyKey, int version, String actor) {
        return transition(tenantId, strategyKey, version, "APPROVED", actor);
    }

    public Map<String, Object> activateStrategyVersion(Long tenantId, String strategyKey, int version, String actor) {
        Map<String, Object> strategy = strategy(tenantId, strategyKey);
        strategy.put("activeVersion", version);
        strategy.put("status", "ACTIVE");
        strategy.put("updatedBy", actor);
        transition(tenantId, strategyKey, version, "ACTIVE", actor);
        return copy(strategy);
    }

    public Map<String, Object> rollbackStrategy(Long tenantId, String strategyKey, Map<String, Object> payload,
                                                String actor) {
        Map<String, Object> strategy = strategy(tenantId, strategyKey);
        int targetVersion = intValue(payload.get("targetVersion"), 1);
        strategy.put("activeVersion", targetVersion);
        strategy.put("status", "ROLLED_BACK");
        strategy.put("updatedBy", actor);
        return copy(strategy);
    }

    public Map<String, Object> pauseStrategy(Long tenantId, String strategyKey, String actor) {
        Map<String, Object> strategy = strategy(tenantId, strategyKey);
        strategy.put("status", "PAUSED");
        strategy.put("updatedBy", actor);
        return copy(strategy);
    }

    public Map<String, Object> diffStrategyVersions(Long tenantId, String strategyKey, int left, int right) {
        return Map.of("tenantId", tenantId, "strategyKey", strategyKey, "left", left, "right", right,
                "changeCount", left == right ? 0 : 1);
    }

    public Map<String, Object> startSimulation(Long tenantId, Map<String, Object> payload, String actor) {
        TenantRiskGovernance governance = governance(tenantId);
        Map<String, Object> simulation = record(payload);
        simulation.put("tenantId", tenantId);
        simulation.put("simulationId", (long) governance.simulations.size() + 1);
        simulation.put("sceneKey", value(payload.get("sceneKey"), "DEFAULT_SCENE"));
        simulation.put("status", "COMPLETED");
        simulation.put("updatedBy", actor);
        governance.simulations.add(simulation);
        governance.traces.add(trace(tenantId, String.valueOf(simulation.get("sceneKey")),
                "simulation-" + simulation.get("simulationId")));
        return copy(simulation);
    }

    public List<Map<String, Object>> listSimulations(Long tenantId, String sceneKey, int limit) {
        return governance(tenantId).simulations.stream()
                .filter(item -> matches(item, "sceneKey", sceneKey))
                .limit(limit)
                .map(RiskGovernanceCatalog::copy)
                .toList();
    }

    private TenantRiskGovernance governance(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new TenantRiskGovernance());
    }

    private Map<String, Object> strategy(Long tenantId, String strategyKey) {
        requiredString(strategyKey, "strategyKey");
        Map<String, Object> strategy = governance(tenantId).strategies.get(strategyKey);
        if (strategy == null) {
            throw new IllegalArgumentException("strategy not found");
        }
        return strategy;
    }

    private Map<String, Object> version(Long tenantId, String strategyKey, int version) {
        return find(governance(tenantId).versions, "version", version, "version not found", "strategyKey", strategyKey);
    }

    private Map<String, Object> transition(Long tenantId, String strategyKey, int version, String status,
                                           String actor) {
        Map<String, Object> row = version(tenantId, strategyKey, version);
        row.put("status", status);
        row.put("updatedBy", actor);
        return copy(row);
    }

    private static int nextVersion(TenantRiskGovernance governance, String strategyKey) {
        return (int) governance.versions.stream()
                .filter(item -> matches(item, "strategyKey", strategyKey))
                .count() + 1;
    }

    private static Map<String, Object> seedList(Long tenantId, String listKey) {
        Map<String, Object> list = new LinkedHashMap<>();
        list.put("tenantId", tenantId);
        list.put("listKey", listKey);
        list.put("listType", "BLACK");
        list.put("subjectType", "USER_ID");
        list.put("status", "ACTIVE");
        return list;
    }

    private static Map<String, Object> versionRow(Long tenantId, String strategyKey, int version, String status) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("tenantId", tenantId);
        row.put("strategyKey", strategyKey);
        row.put("version", version);
        row.put("status", status);
        return row;
    }

    private static Map<String, Object> trace(Long tenantId, String sceneKey, String traceKey) {
        return Map.of("tenantId", tenantId, "traceKey", traceKey, "sceneKey", sceneKey, "decision", "ALLOW");
    }

    private static Map<String, Object> record(Map<String, Object> payload) {
        return new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static boolean matches(Map<String, Object> item, String field, Object expected) {
        return expected == null || String.valueOf(expected).isBlank() || Objects.equals(item.get(field), expected);
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String key, Object value, String message) {
        return rows.stream()
                .filter(row -> Objects.equals(row.get(key), value))
                .findFirst()
                .map(RiskGovernanceCatalog::copy)
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String key, Object value, String message,
                                            String secondKey, Object secondValue) {
        return rows.stream()
                .filter(row -> Objects.equals(row.get(key), value))
                .filter(row -> Objects.equals(row.get(secondKey), secondValue))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
    }

    private static void required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    private static void requiredString(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    private static String value(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private static String upper(String value) {
        return value == null || value.isBlank() ? value : value.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> values(Map<String, Object> payload) {
        Object values = payload.get("values");
        if (values instanceof Iterable<?> iterable) {
            List<String> result = new ArrayList<>();
            for (Object value : iterable) {
                if (value != null && !String.valueOf(value).isBlank()) {
                    result.add(String.valueOf(value).trim());
                }
            }
            return result;
        }
        if (values == null || String.valueOf(values).isBlank()) {
            return List.of();
        }
        String[] parts = String.valueOf(values).split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part.trim());
            }
        }
        return result;
    }

    private static final class TenantRiskGovernance {
        private final Map<String, Map<String, Object>> lists = new LinkedHashMap<>();
        private final List<Map<String, Object>> entries = new ArrayList<>();
        private final Map<String, Map<String, Object>> strategies = new LinkedHashMap<>();
        private final List<Map<String, Object>> versions = new ArrayList<>();
        private final List<Map<String, Object>> simulations = new ArrayList<>();
        private final List<Map<String, Object>> traces = new ArrayList<>();
    }
}
