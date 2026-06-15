package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AbExperimentCatalog {

    private final Clock clock;
    private final Map<Long, Map<String, Object>> experiments = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> groups = new LinkedHashMap<>();
    private long nextExperimentId = 1L;
    private long nextGroupId = 1L;

    public AbExperimentCatalog(Clock clock) {
        this.clock = clock;
    }

    public synchronized Map<String, Object> list(Long tenantId, Map<String, Object> query) {
        Map<String, Object> safe = safePayload(query);
        Integer enabled = nullableInt(safe.get("enabled"));
        return list(tenantId, nullableInt(safe.get("page")), nullableInt(safe.get("size")),
                enabled == null ? null : enabled != 0);
    }

    public synchronized Map<String, Object> list(Long tenantId, Integer pageValue, Integer sizeValue,
                                                 Boolean enabledValue) {
        Integer enabled = enabledValue == null ? null : enabledValue ? 1 : 0;
        int page = Math.max(1, pageValue == null ? 1 : pageValue);
        int size = Math.max(1, Math.min(sizeValue == null ? 20 : sizeValue, 100));
        List<Map<String, Object>> rows = experiments.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> enabled == null || enabled.equals(row.get("enabled")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id"), Comparator.reverseOrder()))
                .map(AbExperimentCatalog::copy)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", (long) rows.size());
        result.put("records", rows.stream().skip((long) (page - 1) * size).limit(size).toList());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public synchronized Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        Long id = resolveId(safe, "id", nextExperimentId);
        Map<String, Object> row = experiments.getOrDefault(id, baseRow(id, tenantId, actor));
        row.put("tenantId", tenantId);
        row.put("experimentKey", text(safe.get("experimentKey"), "experiment-" + id));
        row.put("name", text(safe.get("name"), "AB Experiment " + id));
        row.put("description", text(safe.get("description"), ""));
        row.put("trafficPercent", intValue(safe.get("trafficPercent"), 100));
        row.put("enabled", intValue(safe.get("enabled"), 1));
        touch(row, actor);
        experiments.put(id, row);
        nextExperimentId = Math.max(nextExperimentId, id + 1);
        return copy(row);
    }

    public synchronized Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> row = requireExperiment(tenantId, id);
        Map<String, Object> safe = safePayload(payload);
        if (safe.containsKey("experimentKey")) {
            row.put("experimentKey", text(safe.get("experimentKey"), (String) row.get("experimentKey")));
        }
        if (safe.containsKey("name")) {
            row.put("name", text(safe.get("name"), (String) row.get("name")));
        }
        if (safe.containsKey("description")) {
            row.put("description", text(safe.get("description"), ""));
        }
        if (safe.containsKey("trafficPercent")) {
            row.put("trafficPercent", intValue(safe.get("trafficPercent"), 100));
        }
        if (safe.containsKey("enabled")) {
            row.put("enabled", intValue(safe.get("enabled"), 1));
        }
        touch(row, actor);
        return copy(row);
    }

    public synchronized Map<String, Object> delete(Long tenantId, Long id) {
        Map<String, Object> row = requireExperiment(tenantId, id);
        row.put("enabled", 0);
        row.put("updatedAt", now());
        return copy(row);
    }

    public synchronized List<Map<String, Object>> listGroups(Long tenantId, Long experimentId,
                                                             boolean includeDisabled) {
        requireExperiment(tenantId, experimentId);
        return groups.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> experimentId.equals(row.get("experimentId")))
                .filter(row -> includeDisabled || Integer.valueOf(1).equals(row.get("enabled")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id")))
                .map(AbExperimentCatalog::copy)
                .toList();
    }

    public synchronized Map<String, Object> createGroup(Long tenantId, Long experimentId, Map<String, Object> payload,
                                                        String actor) {
        requireExperiment(tenantId, experimentId);
        Map<String, Object> safe = safePayload(payload);
        Long id = resolveId(safe, "id", nextGroupId);
        Map<String, Object> row = groups.getOrDefault(id, baseRow(id, tenantId, actor));
        row.put("tenantId", tenantId);
        row.put("experimentId", experimentId);
        row.put("groupKey", text(safe.get("groupKey"), "group-" + id));
        row.put("name", text(safe.get("name"), "Experiment Group " + id));
        row.put("weight", intValue(safe.get("weight"), 50));
        row.put("enabled", intValue(safe.get("enabled"), 1));
        touch(row, actor);
        groups.put(id, row);
        nextGroupId = Math.max(nextGroupId, id + 1);
        return copy(row);
    }

    public synchronized Map<String, Object> updateGroup(Long tenantId, Long experimentId, Long groupId,
                                                        Map<String, Object> payload, String actor) {
        requireExperiment(tenantId, experimentId);
        Map<String, Object> row = requireGroup(tenantId, experimentId, groupId);
        Map<String, Object> safe = safePayload(payload);
        if (safe.containsKey("groupKey")) {
            row.put("groupKey", text(safe.get("groupKey"), (String) row.get("groupKey")));
        }
        if (safe.containsKey("name")) {
            row.put("name", text(safe.get("name"), (String) row.get("name")));
        }
        if (safe.containsKey("weight")) {
            row.put("weight", intValue(safe.get("weight"), 50));
        }
        if (safe.containsKey("enabled")) {
            row.put("enabled", intValue(safe.get("enabled"), 1));
        }
        touch(row, actor);
        return copy(row);
    }

    public synchronized Map<String, Object> deleteGroup(Long tenantId, Long experimentId, Long groupId) {
        requireExperiment(tenantId, experimentId);
        Map<String, Object> row = requireGroup(tenantId, experimentId, groupId);
        row.put("enabled", 0);
        row.put("updatedAt", now());
        return copy(row);
    }

    public synchronized Map<String, Object> evaluateGovernance(Long tenantId, Long experimentId,
                                                               String controlVariantKey, String actor) {
        Map<String, Object> experiment = requireExperiment(tenantId, experimentId);
        String controlKey = text(controlVariantKey, "A");
        List<Map<String, Object>> enabledGroups = listGroups(tenantId, experimentId, false);
        boolean controlExists = enabledGroups.stream()
                .anyMatch(row -> controlKey.equals(row.get("groupKey")));
        if (!controlExists) {
            throw new IllegalArgumentException("Control variant not found: " + controlKey);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("experimentId", experimentId);
        result.put("experimentKey", experiment.get("experimentKey"));
        result.put("controlVariantKey", controlKey);
        result.put("variantCount", enabledGroups.size());
        result.put("totalWeight", enabledGroups.stream().mapToInt(row -> (Integer) row.get("weight")).sum());
        result.put("eligible", Integer.valueOf(1).equals(experiment.get("enabled")) && !enabledGroups.isEmpty());
        result.put("evaluatedBy", actor);
        result.put("evaluatedAt", now());
        return result;
    }

    private Map<String, Object> baseRow(Long id, Long tenantId, String actor) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("tenantId", tenantId);
        row.put("createdBy", actor);
        row.put("createdAt", now());
        row.put("updatedBy", actor);
        row.put("updatedAt", now());
        return row;
    }

    private void touch(Map<String, Object> row, String actor) {
        row.putIfAbsent("createdBy", actor);
        row.putIfAbsent("createdAt", now());
        row.put("updatedBy", actor);
        row.put("updatedAt", now());
    }

    private Map<String, Object> requireExperiment(Long tenantId, Long id) {
        Map<String, Object> row = experiments.get(id);
        if (row == null || !tenantId.equals(row.get("tenantId"))) {
            throw new IllegalArgumentException("AB experiment not found: " + id);
        }
        return row;
    }

    private Map<String, Object> requireGroup(Long tenantId, Long experimentId, Long groupId) {
        Map<String, Object> row = groups.get(groupId);
        if (row == null || !tenantId.equals(row.get("tenantId")) || !experimentId.equals(row.get("experimentId"))) {
            throw new IllegalArgumentException("AB experiment group not found: " + groupId);
        }
        return row;
    }

    private Instant now() {
        return Instant.now(clock);
    }

    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> copy(Map<String, Object> row) {
        return new LinkedHashMap<>(row);
    }

    private static Long resolveId(Map<String, Object> payload, String key, Long fallback) {
        Long id = nullableLong(payload.get(key));
        return id == null || id < 1 ? fallback : id;
    }

    private static String text(Object value, String fallback) {
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return value.toString().trim();
    }

    private static Integer nullableInt(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return intValue(value, 0);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return fallback;
        }
        return Integer.parseInt(value.toString());
    }

    private static Long nullableLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.parseLong(value.toString());
    }
}
