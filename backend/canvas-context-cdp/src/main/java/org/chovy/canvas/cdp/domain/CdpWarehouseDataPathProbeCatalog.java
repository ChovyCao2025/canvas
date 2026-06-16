package org.chovy.canvas.cdp.domain;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.cdp.api.CdpWarehouseDataPathProbeFacade.RunCommand;

/**
 * 维护 CdpWarehouseDataPathProbe 的内存目录和查询视图。
 */
public class CdpWarehouseDataPathProbeCatalog {

    /**
     * DEFAULT LIMIT。
     */
    private static final int DEFAULT_LIMIT = 20;

    /**
     * MAX LIMIT。
     */
    private static final int MAX_LIMIT = 100;

    /**
     * DEFAULT VERIFY ATTEMPTS。
     */
    private static final int DEFAULT_VERIFY_ATTEMPTS = 3;

    /**
     * MAX VERIFY ATTEMPTS。
     */
    private static final int MAX_VERIFY_ATTEMPTS = 10;

    /**
     * DEFAULT VERIFY DELAY MS。
     */
    private static final int DEFAULT_VERIFY_DELAY_MS = 100;

    /**
     * MAX VERIFY DELAY MS。
     */
    private static final int MAX_VERIFY_DELAY_MS = 5_000;

    /**
     * DEFAULT PROBE KEY。
     */
    private static final String DEFAULT_PROBE_KEY = "synthetic-ods";

    /**
     * DEFAULT EVENT CODE。
     */
    private static final String DEFAULT_EVENT_CODE = "__warehouse_probe__";

    /**
     * DEFAULT SOURCE MODE。
     */
    private static final String DEFAULT_SOURCE_MODE = "DIRECT_SINK";

    /**
     * SOURCE MYSQL CDC。
     */
    private static final String SOURCE_MYSQL_CDC = "MYSQL_CDC";

    /**
     * STATUS PASS。
     */
    private static final String STATUS_PASS = "PASS";

    /**
     * STATUS FAIL。
     */
    private static final String STATUS_FAIL = "FAIL";

    /**
     * STATUS WARN。
     */
    private static final String STATUS_WARN = "WARN";

    /**
     * STATUS SKIPPED。
     */
    private static final String STATUS_SKIPPED = "SKIPPED";

    /**
     * 执行 AtomicLong 对应的 CDP 业务操作。
     */
    private final AtomicLong ids = new AtomicLong(3000L);
    private final Map<Long, Map<String, Object>> runs = new ConcurrentHashMap<>();

    /**
     * 执行 run 对应的 CDP 业务操作。
     */
    public Map<String, Object> run(Long tenantId, RunCommand command) {
        RunCommand normalized = normalize(command);
        boolean mysqlCdc = SOURCE_MYSQL_CDC.equals(normalized.sourceMode());
        String sourceStatus = mysqlCdc ? STATUS_PASS : STATUS_SKIPPED;
        String sinkStatus = mysqlCdc ? STATUS_SKIPPED : STATUS_PASS;
        String odsStatus = normalized.strict() ? STATUS_FAIL : STATUS_WARN;
        String status = worstStatus(sourceStatus, sinkStatus, odsStatus);
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> view = ordered();
        view.put("id", ids.incrementAndGet());
        view.put("tenantId", tenantId);
        view.put("probeKey", normalized.probeKey());
        view.put("sourceMode", normalized.sourceMode());
        view.put("messageId", "warehouse-probe-" + UUID.randomUUID());
        view.put("eventCode", normalized.eventCode());
        view.put("userId", "__warehouse_probe_user_" + UUID.randomUUID().toString().replace("-", ""));
        view.put("strict", normalized.strict());
        view.put("status", status);
        view.put("sourceStatus", sourceStatus);
        view.put("sinkStatus", sinkStatus);
        view.put("odsStatus", odsStatus);
        view.put("odsRowCount", 0L);
        view.put("verifyAttempts", normalized.verifyAttempts());
        view.put("verifyDelayMs", normalized.verifyDelayMs());
        view.put("startedAt", now);
        view.put("finishedAt", now);
        view.put("errorMessage", "Doris ODS did not expose the synthetic event");
        view.put("evidenceJson", evidenceJson(mysqlCdc, sourceStatus, sinkStatus, odsStatus));
        view.put("createdAt", now);
        view.put("updatedAt", now);
        runs.put((Long) view.get("id"), view);
        return view;
    }

    /**
     * 执行 recent 对应的 CDP 业务操作。
     */
    public List<Map<String, Object>> recent(Long tenantId, int limit) {
        return runs.values().stream()
                .filter(run -> run.get("tenantId").equals(tenantId))
                .sorted(Comparator.comparing((Map<String, Object> run) -> (LocalDateTime) run.get("createdAt"))
                        .reversed()
                        .thenComparing(Comparator.comparing((Map<String, Object> run) -> (Long) run.get("id"))
                                .reversed()))
                .limit(boundLimit(limit))
                .toList();
    }

    /**
     * 归一化normalize。
     */
    private static RunCommand normalize(RunCommand command) {
        RunCommand safe = command == null
                ? new RunCommand(null, null, true, DEFAULT_VERIFY_ATTEMPTS, DEFAULT_VERIFY_DELAY_MS, null)
                : command;
        String eventCode = defaultString(safe.eventCode(), DEFAULT_EVENT_CODE);
        if (!eventCode.startsWith("__warehouse_probe")) {
            throw new IllegalArgumentException("eventCode must use reserved __warehouse_probe prefix");
        }
        return new RunCommand(
                defaultString(safe.probeKey(), DEFAULT_PROBE_KEY),
                eventCode,
                safe.strict(),
                boundedAttempts(safe.verifyAttempts()),
                boundedDelayMs(safe.verifyDelayMs()),
                sourceMode(safe.sourceMode()));
    }

    /**
     * 执行 boundLimit 对应的 CDP 业务操作。
     */
    private static int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 返回默认的String。
     */
    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    /**
     * 执行 sourceMode 对应的 CDP 业务操作。
     */
    private static String sourceMode(String value) {
        String normalized = defaultString(value, DEFAULT_SOURCE_MODE)
                .toUpperCase(Locale.ROOT)
                .replace('-', '_');
        if ("FLINK_CDC".equals(normalized) || "MYSQL_SOURCE".equals(normalized)) {
            return SOURCE_MYSQL_CDC;
        }
        if (DEFAULT_SOURCE_MODE.equals(normalized) || SOURCE_MYSQL_CDC.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("sourceMode must be DIRECT_SINK or MYSQL_CDC");
    }

    /**
     * 执行 boundedAttempts 对应的 CDP 业务操作。
     */
    private static int boundedAttempts(int value) {
        int attempts = value <= 0 ? DEFAULT_VERIFY_ATTEMPTS : value;
        return Math.min(attempts, MAX_VERIFY_ATTEMPTS);
    }

    /**
     * 执行 boundedDelayMs 对应的 CDP 业务操作。
     */
    private static int boundedDelayMs(int value) {
        int delay = value < 0 ? DEFAULT_VERIFY_DELAY_MS : value;
        return Math.min(delay, MAX_VERIFY_DELAY_MS);
    }

    /**
     * 执行 worstStatus 对应的 CDP 业务操作。
     */
    private static String worstStatus(String sourceStatus, String sinkStatus, String odsStatus) {
        if (STATUS_FAIL.equals(sourceStatus) || STATUS_FAIL.equals(sinkStatus) || STATUS_FAIL.equals(odsStatus)) {
            return STATUS_FAIL;
        }
        if (STATUS_WARN.equals(sourceStatus) || STATUS_WARN.equals(sinkStatus) || STATUS_WARN.equals(odsStatus)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 执行 evidenceJson 对应的 CDP 业务操作。
     */
    private static String evidenceJson(boolean mysqlCdc, String sourceStatus, String sinkStatus, String odsStatus) {
        String writeStep = mysqlCdc ? "source_mysql_write" : "sink_write";
        return "[{\"step\":\"" + writeStep + "\",\"status\":\""
                + (mysqlCdc ? sourceStatus : sinkStatus)
                + "\"},{\"step\":\"ods_read\",\"status\":\"" + odsStatus + "\"}]";
    }

    /**
     * 执行 ordered 对应的 CDP 业务操作。
     */
    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
