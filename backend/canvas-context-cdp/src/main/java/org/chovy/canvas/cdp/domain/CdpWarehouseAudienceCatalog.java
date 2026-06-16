package org.chovy.canvas.cdp.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 维护 CdpWarehouseAudience 的内存目录和查询视图。
 */
public class CdpWarehouseAudienceCatalog {

    /**
     * 时间源。
     */
    private final Clock clock;
    private final Map<Long, List<Map<String, Object>>> runsByTenant = new LinkedHashMap<>();

    /**
     * 创建当前组件实例。
     */
    public CdpWarehouseAudienceCatalog(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    /**
     * 执行 materialize 对应的 CDP 业务操作。
     */
    public Map<String, Object> materialize(Long tenantId, Long audienceId, String actor) {
        return appendRun(tenantId, audienceId, "MATERIALIZE", "SUCCEEDED", actor, Map.of());
    }

    /**
     * 执行 materializeGated 对应的 CDP 业务操作。
     */
    public Map<String, Object> materializeGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        Map<String, Object> extra = ordered();
        extra.put("gateMode", value(payload.get("mode"), "HYBRID"));
        extra.put("allowWarn", booleanValue(payload.get("allowWarn"), false));
        extra.put("from", string(payload.get("from")));
        extra.put("to", string(payload.get("to")));
        extra.put("gateDecision", booleanValue(payload.get("allowWarn"), false) ? "WARN_ALLOWED" : "PASSED");
        return appendRun(tenantId, audienceId, "MATERIALIZE_GATED", "SUCCEEDED", actor, extra);
    }

    /**
     * 执行 materializeContractGated 对应的 CDP 业务操作。
     */
    public Map<String, Object> materializeContractGated(
            Long tenantId,
            Long audienceId,
            Map<String, Object> payload,
            String actor) {
        required(payload, "contractKey");
        Map<String, Object> extra = ordered();
        extra.put("contractKey", String.valueOf(payload.get("contractKey")).trim());
        extra.put("from", string(payload.get("from")));
        extra.put("to", string(payload.get("to")));
        extra.put("gateDecision", "PASSED");
        return appendRun(tenantId, audienceId, "MATERIALIZE_CONTRACT_GATED", "SUCCEEDED", actor, extra);
    }

    /**
     * 执行 rollback 对应的 CDP 业务操作。
     */
    public Map<String, Object> rollback(Long tenantId, Long audienceId, Map<String, Object> payload, String actor) {
        required(payload, "targetVersion");
        Map<String, Object> extra = ordered();
        extra.put("targetVersion", longValue(payload.get("targetVersion"), "targetVersion"));
        extra.put("reason", stringOrDefault(payload.get("reason"), "manual rollback"));
        return appendRun(tenantId, audienceId, "ROLLBACK", "ROLLED_BACK", actor, extra);
    }

    /**
     * 执行 refreshDue 对应的 CDP 业务操作。
     */
    public Map<String, Object> refreshDue(Long tenantId, Map<String, Object> payload, String actor, boolean gated) {
        int limit = boundedLimit(payload.get("limit"), 0);
        List<Long> audienceIds = dueAudienceIds(limit);
        List<Map<String, Object>> runs = new ArrayList<>();
        for (Long audienceId : audienceIds) {
            Map<String, Object> extra = ordered();
            if (gated) {
                extra.put("gateMode", value(payload.get("mode"), "HYBRID"));
                extra.put("allowWarn", booleanValue(payload.get("allowWarn"), false));
                extra.put("gateDecision", booleanValue(payload.get("allowWarn"), false) ? "WARN_ALLOWED" : "PASSED");
            }
            runs.add(appendRun(tenantId, audienceId, gated ? "REFRESH_DUE_GATED" : "REFRESH_DUE", "SUCCEEDED",
                    actor, extra));
        }
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantId);
        result.put("gated", gated);
        result.put("limit", limit);
        result.put("refreshedCount", runs.size());
        result.put("runIds", runs.stream().map(run -> run.get("runId")).toList());
        result.put("operator", actor);
        result.put("refreshedAt", now());
        return result;
    }

    /**
     * 执行 recentRuns 对应的 CDP 业务操作。
     */
    public List<Map<String, Object>> recentRuns(Long tenantId, Long audienceId, String status, Integer limit) {
        int boundedLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 200);
        return runsByTenant.getOrDefault(tenantId, List.of()).stream()
                .filter(run -> audienceId == null || Objects.equals(run.get("audienceId"), audienceId))
                .filter(run -> status == null || status.isBlank()
                        || status.trim().equalsIgnoreCase(String.valueOf(run.get("status"))))
                .limit(boundedLimit)
                .map(CdpWarehouseAudienceCatalog::copy)
                .toList();
    }

    /**
     * 执行 appendRun 对应的 CDP 业务操作。
     */
    private Map<String, Object> appendRun(
            Long tenantId,
            Long audienceId,
            String operation,
            String status,
            String actor,
            Map<String, Object> extra) {
        List<Map<String, Object>> runs = runsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>());
        Map<String, Object> run = ordered();
        run.put("tenantId", tenantId);
        run.put("runId", "audience-run-" + (runs.size() + 1));
        run.put("audienceId", audienceId);
        run.put("operation", operation);
        run.put("status", status);
        run.put("version", (long) runs.size() + 1);
        run.put("rowCount", 1000L + audienceId);
        run.put("operator", actor);
        run.put("startedAt", now());
        run.put("finishedAt", now());
        run.putAll(extra);
        runs.add(run);
        return copy(run);
    }

    /**
     * 执行 dueAudienceIds 对应的 CDP 业务操作。
     */
    private static List<Long> dueAudienceIds(int limit) {
        int count = Math.min(Math.max(limit, 0), 3);
        List<Long> ids = new ArrayList<>();
        for (long index = 1; index <= count; index++) {
            ids.add(index);
        }
        return ids;
    }

    /**
     * 执行 now 对应的 CDP 业务操作。
     */
    private String now() {
        return Instant.now(clock).toString();
    }

    /**
     * 读取并校验必填的d。
     */
    private static void required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    /**
     * 执行 boundedLimit 对应的 CDP 业务操作。
     */
    private static int boundedLimit(Object value, int fallback) {
        int parsed = value == null || String.valueOf(value).isBlank() ? fallback : Integer.parseInt(String.valueOf(value));
        return Math.min(Math.max(parsed, 0), 200);
    }

    /**
     * 执行 longValue 对应的 CDP 业务操作。
     */
    private static Long longValue(Object value, String field) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return Long.parseLong(String.valueOf(value));
    }

    /**
     * 执行 booleanValue 对应的 CDP 业务操作。
     */
    private static boolean booleanValue(Object value, boolean fallback) {
        return value == null ? fallback : Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 执行 value 对应的 CDP 业务操作。
     */
    private static String value(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank()
                ? fallback
                : String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 执行 stringOrDefault 对应的 CDP 业务操作。
     */
    private static String stringOrDefault(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value).trim();
    }

    /**
     * 执行 string 对应的 CDP 业务操作。
     */
    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
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
