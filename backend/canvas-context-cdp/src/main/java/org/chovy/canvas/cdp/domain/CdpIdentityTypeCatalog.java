package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * 维护 CdpIdentityType 的内存目录和查询视图。
 */
public class CdpIdentityTypeCatalog {

    /**
     * 执行 compile 对应的 CDP 业务操作。
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]{1,63}");

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Map<String, Object>> rows = new ConcurrentHashMap<>();

    /**
     * 执行 newKeySet 对应的 CDP 业务操作。
     */
    private final Set<Long> usedIdentityTypeIds = ConcurrentHashMap.newKeySet();

    /**
     * 查询list列表。
     */
    public Map<String, Object> list(Integer enabled, Integer allowImport) {
        List<Map<String, Object>> matched = rows.values().stream()
                .filter(row -> matchesInteger(row.get("enabled"), enabled))
                .filter(row -> matchesInteger(row.get("allowImport"), allowImport))
                .sorted(Comparator.comparing(row -> Long.class.cast(row.get("id"))))
                .map(CdpIdentityTypeCatalog::copy)
                .toList();
        Map<String, Object> result = ordered();
        result.put("total", (long) matched.size());
        result.put("list", matched);
        return result;
    }

    /**
     * 创建create。
     */
    public Map<String, Object> create(Map<String, Object> payload) {
        Map<String, Object> row = rowBase(ids.incrementAndGet(), payload);
        LocalDateTime now = LocalDateTime.now();
        row.put("createdAt", now);
        row.put("updatedAt", now);
        rows.put((Long) row.get("id"), row);
        return copy(row);
    }

    /**
     * 更新update。
     */
    public Map<String, Object> update(Long id, Map<String, Object> payload) {
        Map<String, Object> existing = rows.get(requireId(id));
        if (existing == null) {
            throw new IllegalArgumentException("identity type not found: " + id);
        }
        Map<String, Object> row = rowBase(id, payload);
        if (!payload.containsKey("createdBy")) {
            row.put("createdBy", existing.get("createdBy"));
        }
        row.put("createdAt", existing.get("createdAt"));
        row.put("updatedAt", LocalDateTime.now());
        rows.put(id, row);
        return copy(row);
    }

    /**
     * 删除delete。
     */
    public Map<String, Object> delete(Long id) {
        Map<String, Object> existing = rows.get(requireId(id));
        if (existing == null) {
            throw new IllegalArgumentException("identity type not found: " + id);
        }
        if (usedIdentityTypeIds.contains(id)) {
            throw new IllegalArgumentException("identity type is in use: " + existing.get("code"));
        }
        rows.remove(id);
        usedIdentityTypeIds.remove(id);
        Map<String, Object> result = ordered();
        result.put("id", id);
        result.put("deleted", true);
        return result;
    }

    /**
     * 执行 recordIdentityUse 对应的 CDP 业务操作。
     */
    public void recordIdentityUse(Long id) {
        usedIdentityTypeIds.add(requireId(id));
    }

    /**
     * 执行 clearIdentityUse 对应的 CDP 业务操作。
     */
    public void clearIdentityUse(Long id) {
        usedIdentityTypeIds.remove(requireId(id));
    }

    /**
     * 执行 rowBase 对应的 CDP 业务操作。
     */
    private static Map<String, Object> rowBase(Long id, Map<String, Object> payload) {
        String code = normalizeCode(payload.get("code"));
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code is required");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("invalid code: " + code);
        }
        String name = stringValue(payload.get("name"));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        Map<String, Object> row = ordered();
        row.put("id", id);
        row.put("code", code);
        row.put("name", name.trim());
        row.put("description", stringValue(payload.get("description")));
        row.put("enabled", integerOrDefault(payload.get("enabled"), 1));
        row.put("allowImport", integerOrDefault(payload.get("allowImport"), 1));
        row.put("multiValue", integerOrDefault(payload.get("multiValue"), 0));
        row.put("priority", integerOrDefault(payload.get("priority"), 100));
        row.put("participateMapping", integerOrDefault(payload.get("participateMapping"), 0));
        row.put("createdBy", stringValue(payload.get("createdBy")));
        return row;
    }

    /**
     * 读取并校验必填的Id。
     */
    private static Long requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("identity type not found: " + id);
        }
        return id;
    }

    /**
     * 执行 matchesInteger 对应的 CDP 业务操作。
     */
    private static boolean matchesInteger(Object actual, Integer expected) {
        return expected == null || expected.equals(integerOrDefault(actual, null));
    }

    /**
     * 归一化Code。
     */
    private static String normalizeCode(Object value) {
        String text = stringValue(value);
        return text == null ? null : text.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 执行 stringValue 对应的 CDP 业务操作。
     */
    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 执行 integerOrDefault 对应的 CDP 业务操作。
     */
    private static Integer integerOrDefault(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : Integer.valueOf(text);
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
