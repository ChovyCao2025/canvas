package org.chovy.canvas.marketing.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护Audience相关的内存业务目录。
 */
public class AudienceCatalog {

    /**
     * 保存DEFAULT_SOURCE字段值。
     */
    private static final String DEFAULT_SOURCE = "cdp_profile";

    /**
     * 用于生成确定性业务时间的时钟。
     */
    private final Clock clock;
    private final Map<Long, Map<String, Object>> audiences = new LinkedHashMap<>();
    private final Map<Long, Map<String, Object>> stats = new LinkedHashMap<>();

    /**
     * 保存nextAudienceId字段值。
     */
    private long nextAudienceId = 1L;

    /**
     * 保存nextTaskId字段值。
     */
    private long nextTaskId = 1L;

    /**
     * 创建AudienceCatalog实例。
     */
    public AudienceCatalog(Clock clock) {
        this.clock = clock;
    }

    /**
     * 查询列表。
     */
    public synchronized Map<String, Object> list(Long tenantId, int page, int size) {
        List<Map<String, Object>> scoped = audiences.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id"), Comparator.reverseOrder()))
                .map(AudienceCatalog::copy)
                .toList();
        int from = Math.min((page - 1) * size, scoped.size());
        int to = Math.min(from + size, scoped.size());
        return mapOf(
                "total", (long) scoped.size(),
                "page", page,
                "size", size,
                "records", new ArrayList<>(scoped.subList(from, to)));
    }

    /**
     * 执行sourceFields业务操作。
     */
    public List<Map<String, Object>> sourceFields(String dataSourceType) {
        String source = normalizeSource(dataSourceType);
        ensureSupportedSource(source);
        return List.of(
                field(source, "userId", "STRING", "User ID"),
                field(source, "segment", "STRING", "Segment"),
                field(source, "lifetimeValue", "NUMBER", "Lifetime value"));
    }

    /**
     * 执行preview业务操作。
     */
    public Map<String, Object> preview(Long tenantId, Map<String, Object> payload) {
        Map<String, Object> safe = safePayload(payload);
        String source = normalizeSource(text(safe.get("dataSourceType"), DEFAULT_SOURCE));
        ensureSupportedSource(source);
        List<String> userIds = resolveUserIds(text(safe.get("ruleJson"), ""));
        int limit = Math.max(1, Math.min(intValue(safe.get("sampleLimit"), 10), 100));
        return mapOf(
                "tenantId", tenantId,
                "dataSourceType", source,
                "total", userIds.size(),
                "sampleUserIds", userIds.stream().limit(limit).toList());
    }

    /**
     * 返回字段值。
     */
    public synchronized Map<String, Object> get(Long tenantId, Long id) {
        Map<String, Object> audience = audiences.get(id);
        if (audience == null || !tenantId.equals(audience.get("tenantId"))) {
            return Map.of();
        }
        return copy(audience);
    }

    /**
     * 执行ready业务操作。
     */
    public synchronized List<Map<String, Object>> ready(Long tenantId) {
        return audiences.values().stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .filter(row -> Boolean.TRUE.equals(row.get("enabled")))
                .filter(row -> "READY".equals(row.get("status")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id")))
                .map(AudienceCatalog::copy)
                .toList();
    }

    /**
     * 创建业务对象。
     */
    public synchronized Map<String, Object> create(Long tenantId, Map<String, Object> payload, String actor) {
        Map<String, Object> safe = safePayload(payload);
        String source = normalizeSource(text(safe.get("dataSourceType"), DEFAULT_SOURCE));
        ensureSupportedSource(source);
        Long id = nextAudienceId++;
        boolean enabled = boolValue(safe.get("enabled"), true);
        List<String> userIds = resolveUserIds(text(safe.get("ruleJson"), ""));
        Map<String, Object> audience = mapOf(
                "id", id,
                "tenantId", tenantId,
                "name", text(safe.get("name"), "Audience " + id),
                "dataSourceType", source,
                "ruleJson", text(safe.get("ruleJson"), "{}"),
                "enabled", enabled,
                "status", enabled ? "READY" : "DISABLED",
                "defaultSnapshotMode", text(safe.get("defaultSnapshotMode"), "FULL"),
                "createdBy", actor,
                "updatedBy", actor,
                "createdAt", now(),
                "updatedAt", now());
        audiences.put(id, audience);
        stats.put(id, statRow(tenantId, id, enabled ? "READY" : "DISABLED", userIds.size()));
        return copy(audience);
    }

    /**
     * 更新业务对象。
     */
    public synchronized Map<String, Object> update(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> audience = requireAudience(tenantId, id);
        Map<String, Object> safe = safePayload(payload);
        if (safe.containsKey("dataSourceType")) {
            String source = normalizeSource(text(safe.get("dataSourceType"), DEFAULT_SOURCE));
            ensureSupportedSource(source);
            audience.put("dataSourceType", source);
        }
        if (safe.containsKey("name")) {
            audience.put("name", text(safe.get("name"), "Audience " + id));
        }
        if (safe.containsKey("ruleJson")) {
            audience.put("ruleJson", text(safe.get("ruleJson"), "{}"));
        }
        if (safe.containsKey("defaultSnapshotMode")) {
            audience.put("defaultSnapshotMode", text(safe.get("defaultSnapshotMode"), "FULL").toUpperCase());
        }
        if (safe.containsKey("enabled")) {
            audience.put("enabled", boolValue(safe.get("enabled"), true));
        }
        boolean enabled = Boolean.TRUE.equals(audience.get("enabled"));
        audience.put("status", enabled ? "READY" : "DISABLED");
        audience.put("updatedBy", actor);
        audience.put("updatedAt", now());
        stats.put(id, statRow(tenantId, id, enabled ? "READY" : "DISABLED",
                resolveUserIds(text(audience.get("ruleJson"), "")).size()));
        return copy(audience);
    }

    /**
     * 删除或停用业务对象。
     */
    public synchronized Map<String, Object> delete(Long tenantId, Long id) {
        requireAudience(tenantId, id);
        audiences.remove(id);
        stats.remove(id);
        return mapOf("tenantId", tenantId, "audienceId", id, "deleted", true);
    }

    /**
     * 执行compute业务操作。
     */
    public synchronized Map<String, Object> compute(Long tenantId, Long id, Map<String, Object> payload, String actor) {
        Map<String, Object> audience = requireAudience(tenantId, id);
        if (!Boolean.TRUE.equals(audience.get("enabled"))) {
            throw new IllegalArgumentException("Audience disabled: " + id);
        }
        String taskId = "audience-compute-" + id + "-" + nextTaskId++;
        return mapOf(
                "tenantId", tenantId,
                "audienceId", id,
                "taskId", taskId,
                "status", "QUEUED",
                "operator", actor,
                "payload", safePayload(payload));
    }

    /**
     * 执行stat业务操作。
     */
    public synchronized Map<String, Object> stat(Long tenantId, Long id) {
        requireAudience(tenantId, id);
        return copy(stats.get(id));
    }

    /**
     * 校验并返回audience必填值。
     */
    private Map<String, Object> requireAudience(Long tenantId, Long id) {
        Map<String, Object> audience = audiences.get(id);
        if (audience == null || !tenantId.equals(audience.get("tenantId"))) {
            throw new IllegalArgumentException("Audience not found: " + id);
        }
        return audience;
    }

    /**
     * 执行statRow业务操作。
     */
    private Map<String, Object> statRow(Long tenantId, Long audienceId, String status, long memberCount) {
        return mapOf(
                "tenantId", tenantId,
                "audienceId", audienceId,
                "status", status,
                "memberCount", memberCount,
                "computedAt", now());
    }

    /**
     * 执行resolveUserIds业务操作。
     */
    private static List<String> resolveUserIds(String ruleJson) {
        if (ruleJson.toLowerCase().contains("vip")) {
            return List.of("vip-user-1", "vip-user-2", "vip-user-3");
        }
        return List.of("user-1", "user-2", "user-3");
    }

    /**
     * 执行field业务操作。
     */
    private static Map<String, Object> field(String source, String name, String type, String label) {
        return mapOf("dataSourceType", source, "name", name, "type", type, "label", label);
    }

    /**
     * 执行ensureSupportedSource业务操作。
     */
    private static void ensureSupportedSource(String dataSourceType) {
        if (!DEFAULT_SOURCE.equals(dataSourceType) && !"cdp_event".equals(dataSourceType)) {
            throw new IllegalArgumentException("Unsupported CDP audience source: " + dataSourceType);
        }
    }

    /**
     * 规范化source输入值。
     */
    private static String normalizeSource(String dataSourceType) {
        return text(dataSourceType, DEFAULT_SOURCE).trim().toLowerCase();
    }

    /**
     * 执行boolValue业务操作。
     */
    private static boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return fallback;
    }

    /**
     * 执行intValue业务操作。
     */
    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }

    /**
     * 执行text业务操作。
     */
    private static String text(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    /**
     * 执行safePayload业务操作。
     */
    private static Map<String, Object> safePayload(Map<String, Object> payload) {
        return payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
    }

    /**
     * 执行copy业务操作。
     */
    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    /**
     * 执行now业务操作。
     */
    private String now() {
        return Instant.now(clock).toString();
    }

    /**
     * 执行mapOf业务操作。
     */
    private static Map<String, Object> mapOf(Object... keysAndValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }
}
