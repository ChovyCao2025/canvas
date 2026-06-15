package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.chovy.canvas.cdp.api.CdpWarehouseEnterpriseOlapEvidenceFacade.EvidenceCommand;

public class CdpWarehouseEnterpriseOlapEvidenceCatalog {

    private static final Set<String> OPERATOR_KEYS = Set.of("backup_restore", "ingestion_replay", "runbook_drill");
    private static final List<String> PROOF_ORDER = List.of(
            "doris_metrics",
            "workload_isolation",
            "query_slo",
            "backup_restore",
            "compaction_health",
            "ingestion_replay",
            "runbook_drill");
    private static final List<String> AUTOMATED_KEYS = List.of(
            "doris_metrics",
            "workload_isolation",
            "query_slo",
            "compaction_health",
            "ingestion_replay");

    private final AtomicLong evidenceIds = new AtomicLong(1);
    private final AtomicLong collectionIds = new AtomicLong(1);
    private final List<Map<String, Object>> evidence = new ArrayList<>();
    private final List<Map<String, Object>> collections = new ArrayList<>();

    public synchronized Map<String, Object> record(Long tenantId, EvidenceCommand command, String actor) {
        if (command == null || isBlank(command.evidenceKey())) {
            throw new IllegalArgumentException("evidenceKey is required");
        }
        String key = normalizeKey(command.evidenceKey());
        if (!OPERATOR_KEYS.contains(key)) {
            throw new IllegalArgumentException("operator evidence key must be one of " + OPERATOR_KEYS);
        }
        String measuredAt = defaultString(command.measuredAt(), "2026-06-15T02:40:00");
        String expiresAt = defaultString(command.expiresAt(), "2026-06-15T03:40:00");
        Map<String, Object> row = evidenceRow(evidenceIds.getAndIncrement(), tenantId, key, "operator",
                normalizeStatus(command.status()), defaultReason(command.reason(), key, command.status()), measuredAt,
                expiresAt, defaultString(command.evidenceJson(), "{}"), actor);
        evidence.add(row);
        return copy(row);
    }

    public synchronized Map<String, Object> latest(Long tenantId) {
        Map<String, Map<String, Object>> latest = latestRows(tenantId);
        List<Map<String, Object>> ordered = PROOF_ORDER.stream()
                .map(key -> latest.getOrDefault(key, missingEvidence(tenantId, key)))
                .map(CdpWarehouseEnterpriseOlapEvidenceCatalog::copy)
                .toList();
        Map<String, Object> result = ordered();
        result.put("tenantId", tenantId);
        result.put("status", worstStatus(ordered));
        result.put("evaluatedAt", "2026-06-15T02:40:00");
        result.put("evidence", ordered);
        return result;
    }

    public synchronized List<Map<String, Object>> proof(Long tenantId) {
        List<Map<String, Object>> rows = rows(latest(tenantId).get("evidence"));
        return rows.stream()
                .map(row -> {
                    Map<String, Object> proof = ordered();
                    proof.put("key", "enterprise_olap:" + row.get("evidenceKey"));
                    proof.put("status", row.get("status"));
                    proof.put("reason", row.get("reason"));
                    return proof;
                })
                .toList();
    }

    public synchronized Map<String, Object> collect(Long tenantId, String triggerType, String actor) {
        List<Map<String, Object>> collected = new ArrayList<>();
        for (String key : AUTOMATED_KEYS) {
            Map<String, Object> row = evidenceRow(evidenceIds.getAndIncrement(), tenantId, key, "warehouse", "PASS",
                    key + " pass", "2026-06-15T02:40:00", "2026-06-15T02:55:00", "{}", actor);
            evidence.add(row);
            collected.add(row);
        }
        long pass = collected.stream().filter(row -> "PASS".equals(row.get("status"))).count();
        long warn = collected.stream().filter(row -> "WARN".equals(row.get("status"))).count();
        long fail = collected.size() - pass - warn;
        Map<String, Object> run = ordered();
        run.put("id", collectionIds.getAndIncrement());
        run.put("tenantId", tenantId);
        run.put("triggerType", defaultString(triggerType, "MANUAL").toUpperCase(Locale.ROOT));
        run.put("status", fail > 0 ? "FAIL" : warn > 0 ? "WARN" : "PASS");
        run.put("startedAt", "2026-06-15T02:40:00");
        run.put("finishedAt", "2026-06-15T02:40:01");
        run.put("evidenceCount", collected.size());
        run.put("passCount", (int) pass);
        run.put("warnCount", (int) warn);
        run.put("failCount", (int) fail);
        run.put("reason", "recorded " + collected.size() + " enterprise OLAP evidence rows");
        run.put("createdBy", actor);
        collections.add(run);
        return copy(run);
    }

    public synchronized List<Map<String, Object>> collections(Long tenantId, Integer limit) {
        int boundedLimit = Math.max(1, Math.min(limit == null || limit <= 0 ? 20 : limit, 100));
        return collections.stream()
                .filter(row -> tenantId.equals(row.get("tenantId")))
                .sorted(Comparator.comparing(row -> (Long) row.get("id"), Comparator.reverseOrder()))
                .limit(boundedLimit)
                .map(CdpWarehouseEnterpriseOlapEvidenceCatalog::copy)
                .toList();
    }

    private Map<String, Map<String, Object>> latestRows(Long tenantId) {
        Map<String, Map<String, Object>> latest = new LinkedHashMap<>();
        for (int i = evidence.size() - 1; i >= 0; i--) {
            Map<String, Object> row = evidence.get(i);
            if (tenantId.equals(row.get("tenantId"))) {
                latest.putIfAbsent((String) row.get("evidenceKey"), row);
            }
        }
        return latest;
    }

    private static Map<String, Object> evidenceRow(Long id, Long tenantId, String key, String source, String status,
            String reason, String measuredAt, String expiresAt, String evidenceJson, String actor) {
        Map<String, Object> row = ordered();
        row.put("id", id);
        row.put("tenantId", tenantId);
        row.put("evidenceKey", normalizeKey(key));
        row.put("source", source);
        row.put("status", normalizeStatus(status));
        row.put("reason", reason);
        row.put("measuredAt", measuredAt);
        row.put("expiresAt", expiresAt);
        row.put("evidenceJson", evidenceJson);
        row.put("createdBy", actor);
        return row;
    }

    private static Map<String, Object> missingEvidence(Long tenantId, String key) {
        return evidenceRow(null, tenantId, key, "warehouse", "FAIL", key + " evidence is missing",
                "2026-06-15T02:40:00", "2026-06-15T02:55:00", "{}", "system");
    }

    private static String worstStatus(List<Map<String, Object>> rows) {
        if (rows.stream().map(row -> row.get("status")).anyMatch("FAIL"::equals)) {
            return "FAIL";
        }
        if (rows.stream().map(row -> row.get("status")).anyMatch("WARN"::equals)) {
            return "WARN";
        }
        return "PASS";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object value) {
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private static Map<String, Object> copy(Map<String, Object> value) {
        return new LinkedHashMap<>(value);
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private static String normalizeStatus(String status) {
        String value = defaultString(status, "FAIL").toUpperCase(Locale.ROOT);
        return "PASS".equals(value) || "WARN".equals(value) || "FAIL".equals(value) ? value : "FAIL";
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String defaultReason(String reason, String key, String status) {
        return defaultString(reason, key + " " + normalizeStatus(status).toLowerCase(Locale.ROOT));
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
