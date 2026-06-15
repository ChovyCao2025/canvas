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

public class CdpWarehouseDataPathProbeCatalog {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;
    private static final int DEFAULT_VERIFY_ATTEMPTS = 3;
    private static final int MAX_VERIFY_ATTEMPTS = 10;
    private static final int DEFAULT_VERIFY_DELAY_MS = 100;
    private static final int MAX_VERIFY_DELAY_MS = 5_000;
    private static final String DEFAULT_PROBE_KEY = "synthetic-ods";
    private static final String DEFAULT_EVENT_CODE = "__warehouse_probe__";
    private static final String DEFAULT_SOURCE_MODE = "DIRECT_SINK";
    private static final String SOURCE_MYSQL_CDC = "MYSQL_CDC";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_SKIPPED = "SKIPPED";

    private final AtomicLong ids = new AtomicLong(3000L);
    private final Map<Long, Map<String, Object>> runs = new ConcurrentHashMap<>();

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

    private static int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

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

    private static int boundedAttempts(int value) {
        int attempts = value <= 0 ? DEFAULT_VERIFY_ATTEMPTS : value;
        return Math.min(attempts, MAX_VERIFY_ATTEMPTS);
    }

    private static int boundedDelayMs(int value) {
        int delay = value < 0 ? DEFAULT_VERIFY_DELAY_MS : value;
        return Math.min(delay, MAX_VERIFY_DELAY_MS);
    }

    private static String worstStatus(String sourceStatus, String sinkStatus, String odsStatus) {
        if (STATUS_FAIL.equals(sourceStatus) || STATUS_FAIL.equals(sinkStatus) || STATUS_FAIL.equals(odsStatus)) {
            return STATUS_FAIL;
        }
        if (STATUS_WARN.equals(sourceStatus) || STATUS_WARN.equals(sinkStatus) || STATUS_WARN.equals(odsStatus)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private static String evidenceJson(boolean mysqlCdc, String sourceStatus, String sinkStatus, String odsStatus) {
        String writeStep = mysqlCdc ? "source_mysql_write" : "sink_write";
        return "[{\"step\":\"" + writeStep + "\",\"status\":\""
                + (mysqlCdc ? sourceStatus : sinkStatus)
                + "\"},{\"step\":\"ods_read\",\"status\":\"" + odsStatus + "\"}]";
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }
}
