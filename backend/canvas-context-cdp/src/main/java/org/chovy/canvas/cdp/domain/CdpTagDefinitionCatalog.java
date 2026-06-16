package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 维护 CdpTagDefinition 的内存目录和查询视图。
 */
public class CdpTagDefinitionCatalog {

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong definitionIds = new AtomicLong();

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong valueIds = new AtomicLong();
    private final Map<Long, Map<Long, Map<String, Object>>> definitionsByTenant = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Map<String, Object>>> valuesByTenant = new ConcurrentHashMap<>();

    /**
     * 查询Definitions列表。
     */
    public Map<String, Object> listDefinitions(Long tenantId, Integer page, Integer size, String tagType,
            Integer enabled) {
        List<Map<String, Object>> matched = definitionsByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .filter(row -> matches(row.get("tagType"), tagType))
                .filter(row -> matchesEnabled(row.get("enabled"), enabled))
                .sorted(Comparator.comparing(row -> Long.class.cast(row.get("id"))))
                .map(CdpTagDefinitionCatalog::copy)
                .toList();
        return page(tenantId, page, size, matched);
    }

    /**
     * 创建Definition。
     */
    public Map<String, Object> createDefinition(Long tenantId, Map<String, Object> payload, String actor,
            LocalDateTime now) {
        String tagCode = required(payload, "tagCode");
        Map<String, Object> row = definitionBase(tenantId, definitionIds.incrementAndGet(), tagCode, payload, actor,
                now);
        row.put("createdBy", actor);
        row.put("createdAt", now);
        definitionsByTenant.computeIfAbsent(tenantId, ignored -> new ConcurrentHashMap<>()).put((Long) row.get("id"),
                row);
        return copy(row);
    }

    /**
     * 更新Definition。
     */
    public Map<String, Object> updateDefinition(Long tenantId, Long id, Map<String, Object> payload, String actor,
            LocalDateTime now) {
        Map<String, Object> existing = definitionsByTenant.getOrDefault(tenantId, Map.of()).get(id);
        if (existing == null) {
            throw new IllegalArgumentException("Tag definition not found");
        }
        Map<String, Object> row = new LinkedHashMap<>(existing);
        if (payload.containsKey("tagCode")) {
            row.put("tagCode", stringOrDefault(payload.get("tagCode"), String.valueOf(row.get("tagCode"))));
        }
        if (payload.containsKey("tagName")) {
            row.put("tagName", stringOrDefault(payload.get("tagName"), String.valueOf(row.get("tagName"))));
        }
        if (payload.containsKey("tagType")) {
            row.put("tagType", stringOrDefault(payload.get("tagType"), String.valueOf(row.get("tagType"))));
        }
        if (payload.containsKey("enabled")) {
            row.put("enabled", booleanOrDefault(payload.get("enabled"), Boolean.TRUE.equals(row.get("enabled"))));
        }
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        definitionsByTenant.computeIfAbsent(tenantId, ignored -> new ConcurrentHashMap<>()).put(id, row);
        return copy(row);
    }

    /**
     * 删除Definition。
     */
    public Map<String, Object> deleteDefinition(Long tenantId, Long id, String actor, LocalDateTime now) {
        definitionsByTenant.getOrDefault(tenantId, Map.of()).remove(id);
        Map<String, Object> row = ordered();
        row.put("tenantId", tenantId);
        row.put("id", id);
        row.put("deleted", true);
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        return row;
    }

    /**
     * 查询Values列表。
     */
    public Map<String, Object> listValues(Long tenantId, String tagCode, Integer enabled) {
        List<Map<String, Object>> matched = valuesByTenant.getOrDefault(tenantId, Map.of()).values().stream()
                .filter(row -> tagCode.equals(row.get("tagCode")))
                .filter(row -> matchesEnabled(row.get("enabled"), enabled))
                .sorted(Comparator.comparing(row -> Long.class.cast(row.get("id"))))
                .map(CdpTagDefinitionCatalog::copy)
                .toList();
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantId);
        result.put("tagCode", tagCode);
        result.put("total", (long) matched.size());
        result.put("records", matched);
        return result;
    }

    /**
     * 创建Value。
     */
    public Map<String, Object> createValue(Long tenantId, String tagCode, Map<String, Object> payload, String actor,
            LocalDateTime now) {
        String valueCode = required(payload, "valueCode");
        Map<String, Object> row = valueBase(tenantId, valueIds.incrementAndGet(), tagCode, valueCode, payload, actor,
                now);
        row.put("createdBy", actor);
        row.put("createdAt", now);
        valuesByTenant.computeIfAbsent(tenantId, ignored -> new ConcurrentHashMap<>()).put((Long) row.get("id"), row);
        return copy(row);
    }

    /**
     * 更新Value。
     */
    public Map<String, Object> updateValue(Long tenantId, Long id, Map<String, Object> payload, String actor,
            LocalDateTime now) {
        Map<String, Object> existing = valuesByTenant.getOrDefault(tenantId, Map.of()).get(id);
        if (existing == null) {
            existing = valueBase(tenantId, id, stringOrDefault(payload.get("tagCode"), "vip_level"),
                    stringOrDefault(payload.get("valueCode"), "gold"), payload, actor, now);
        }
        Map<String, Object> row = new LinkedHashMap<>(existing);
        if (payload.containsKey("valueCode")) {
            row.put("valueCode", stringOrDefault(payload.get("valueCode"), String.valueOf(row.get("valueCode"))));
        }
        if (payload.containsKey("valueName")) {
            row.put("valueName", stringOrDefault(payload.get("valueName"), String.valueOf(row.get("valueName"))));
        }
        if (payload.containsKey("enabled")) {
            row.put("enabled", booleanOrDefault(payload.get("enabled"), Boolean.TRUE.equals(row.get("enabled"))));
        }
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        valuesByTenant.computeIfAbsent(tenantId, ignored -> new ConcurrentHashMap<>()).put(id, row);
        return copy(row);
    }

    /**
     * 删除Value。
     */
    public Map<String, Object> deleteValue(Long tenantId, Long id, String actor, LocalDateTime now) {
        valuesByTenant.getOrDefault(tenantId, Map.of()).remove(id);
        Map<String, Object> row = ordered();
        row.put("tenantId", tenantId);
        row.put("id", id);
        row.put("deleted", true);
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        return row;
    }

    /**
     * 执行 definitionBase 对应的 CDP 业务操作。
     */
    private static Map<String, Object> definitionBase(Long tenantId, Long id, String tagCode,
            Map<String, Object> payload, String actor, LocalDateTime now) {
        Map<String, Object> row = ordered();
        row.put("tenantId", tenantId);
        row.put("id", id);
        row.put("tagCode", tagCode);
        row.put("tagName", stringOrDefault(payload.get("tagName"), tagCode));
        row.put("tagType", stringOrDefault(payload.get("tagType"), "PROFILE"));
        row.put("enabled", booleanOrDefault(payload.get("enabled"), true));
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        return row;
    }

    /**
     * 执行 valueBase 对应的 CDP 业务操作。
     */
    private static Map<String, Object> valueBase(Long tenantId, Long id, String tagCode, String valueCode,
            Map<String, Object> payload, String actor, LocalDateTime now) {
        Map<String, Object> row = ordered();
        row.put("tenantId", tenantId);
        row.put("id", id);
        row.put("tagCode", tagCode);
        row.put("valueCode", valueCode);
        row.put("valueName", stringOrDefault(payload.get("valueName"), valueCode));
        row.put("enabled", booleanOrDefault(payload.get("enabled"), true));
        row.put("updatedBy", actor);
        row.put("updatedAt", now);
        return row;
    }

    /**
     * 执行 page 对应的 CDP 业务操作。
     */
    private static Map<String, Object> page(Long tenantId, Integer page, Integer size, List<Map<String, Object>> rows) {
        int from = Math.min((page - 1) * size, rows.size());
        int to = Math.min(from + size, rows.size());
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantId);
        result.put("page", page);
        result.put("size", size);
        result.put("total", (long) rows.size());
        result.put("records", new ArrayList<>(rows.subList(from, to)));
        return result;
    }

    /**
     * 执行 matches 对应的 CDP 业务操作。
     */
    private static boolean matches(Object actual, String expected) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(String.valueOf(actual));
    }

    /**
     * 执行 matchesEnabled 对应的 CDP 业务操作。
     */
    private static boolean matchesEnabled(Object actual, Integer expected) {
        return expected == null || (expected == 1) == Boolean.TRUE.equals(actual);
    }

    /**
     * 读取并校验必填的d。
     */
    private static String required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
        return String.valueOf(value).trim();
    }

    /**
     * 执行 stringOrDefault 对应的 CDP 业务操作。
     */
    private static String stringOrDefault(Object value, String defaultValue) {
        return value == null || String.valueOf(value).isBlank() ? defaultValue : String.valueOf(value).trim();
    }

    /**
     * 执行 booleanOrDefault 对应的 CDP 业务操作。
     */
    private static boolean booleanOrDefault(Object value, boolean defaultValue) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 执行 ordered 对应的 CDP 业务操作。
     */
    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    /**
     * 执行 copy 对应的 CDP 业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }
}
