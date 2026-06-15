package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class CdpWarehouseE2eCertificationCatalog {

    private static final String GENERATED_AT = "2026-06-15T01:30:00";
    private static final String DEFAULT_WINDOW_START = "2026-06-15T00:30:00";
    private static final String DEFAULT_WINDOW_END = "2026-06-15T01:30:00";

    private final AtomicLong runIds = new AtomicLong(100L);
    private final Map<Long, List<Map<String, Object>>> runsByTenant = new ConcurrentHashMap<>();

    public Map<String, Object> certification(Long tenantId, String from, String to, String mode,
            List<String> contractKeys, boolean requirePhysical, boolean requireRealtime,
            boolean requireDataPathProof) {
        Map<String, Object> certification = ordered();
        certification.put("tenantId", tenantId);
        certification.put("status", "PASS");
        certification.put("generatedAt", GENERATED_AT);
        certification.put("windowStart", defaultString(from, DEFAULT_WINDOW_START));
        certification.put("windowEnd", defaultString(to, DEFAULT_WINDOW_END));
        certification.put("mode", mode);
        certification.put("requirePhysical", requirePhysical);
        certification.put("requireRealtime", requireRealtime);
        certification.put("requireDataPathProof", requireDataPathProof);
        certification.put("contractKeys", List.copyOf(contractKeys));
        certification.put("evidence", evidence(requirePhysical, requireRealtime, requireDataPathProof));
        certification.put("productionReadiness", productionReadiness(mode, contractKeys));
        certification.put("liveTableInspection", liveTableInspection(requirePhysical));
        certification.put("realtimePipelineStatus", realtimePipelineStatus(requireRealtime));
        certification.put("realtimeJobStatus", realtimeJobStatus(requireRealtime));
        certification.put("dataPathProof", dataPathProof(requireDataPathProof));
        return copy(certification);
    }

    public Map<String, Object> createRun(Long tenantId, String from, String to, String mode, List<String> contractKeys,
            boolean requirePhysical, boolean requireRealtime, boolean requireDataPathProof, String requestedBy) {
        Map<String, Object> certification = certification(tenantId, from, to, mode, contractKeys, requirePhysical,
                requireRealtime, requireDataPathProof);

        Map<String, Object> run = ordered();
        run.put("id", runIds.incrementAndGet());
        run.put("tenantId", tenantId);
        run.put("status", certification.get("status"));
        run.put("mode", mode);
        run.put("requirePhysical", requirePhysical);
        run.put("requireRealtime", requireRealtime);
        run.put("requireDataPathProof", requireDataPathProof);
        run.put("windowStart", certification.get("windowStart"));
        run.put("windowEnd", certification.get("windowEnd"));
        run.put("contractKeysJson", stringListJson(contractKeys));
        run.put("evidenceJson", evidenceJson());
        run.put("productionReadinessJson", "{\"status\":\"PASS\",\"mode\":\"" + mode + "\"}");
        run.put("liveTableInspectionJson", "{\"status\":\"PASS\",\"inspectedTableCount\":3}");
        run.put("realtimePipelineStatusJson", "{\"status\":\"PASS\",\"runningPipelineCount\":2}");
        run.put("realtimeJobStatusJson", "{\"status\":\"PASS\",\"runningJobCount\":2}");
        run.put("dataPathProofJson", "{\"status\":\"PASS\",\"sourceMode\":\"MYSQL_CDC\","
                + "\"sourceStatus\":\"PASS\",\"odsStatus\":\"PASS\"}");
        run.put("requestedBy", requestedBy);
        run.put("startedAt", "2026-06-15T01:29:00");
        run.put("finishedAt", GENERATED_AT);
        run.put("errorMessage", null);

        runsByTenant.computeIfAbsent(tenantId, ignored -> new ArrayList<>()).add(run);
        return copy(run);
    }

    public List<Map<String, Object>> recent(Long tenantId, Integer limit) {
        int safeLimit = limit == null || limit < 1 ? 20 : Math.min(limit, 100);
        List<Map<String, Object>> runs = runsByTenant.getOrDefault(tenantId, List.of());
        List<Map<String, Object>> result = new ArrayList<>();
        for (int index = runs.size() - 1; index >= 0 && result.size() < safeLimit; index--) {
            result.add(copy(runs.get(index)));
        }
        return result;
    }

    public Map<String, Object> get(Long tenantId, Long id) {
        return runsByTenant.getOrDefault(tenantId, List.of()).stream()
                .filter(run -> id != null && id.equals(run.get("id")))
                .findFirst()
                .map(CdpWarehouseE2eCertificationCatalog::copy)
                .orElseThrow(() -> new IllegalArgumentException("certification run not found: " + id));
    }

    public Map<String, Object> latestMatchingRun(Long tenantId, String mode, boolean requirePhysical,
            boolean requireRealtime, boolean requireDataPathProof) {
        return recent(tenantId, 100).stream()
                .filter(run -> mode.equals(run.get("mode")))
                .filter(run -> !requirePhysical || Boolean.TRUE.equals(run.get("requirePhysical")))
                .filter(run -> !requireRealtime || Boolean.TRUE.equals(run.get("requireRealtime")))
                .filter(run -> !requireDataPathProof || Boolean.TRUE.equals(run.get("requireDataPathProof")))
                .findFirst()
                .map(CdpWarehouseE2eCertificationCatalog::copy)
                .orElse(null);
    }

    private static List<Map<String, Object>> evidence(boolean requirePhysical, boolean requireRealtime,
            boolean requireDataPathProof) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.add(evidenceItem("production_readiness", "PASS", "production readiness proof passed"));
        evidence.add(evidenceItem("doris_jdbc_connectivity", requirePhysical ? "PASS" : "WARN",
                requirePhysical ? "Doris JDBC SELECT 1 passed" : "physical proof not required"));
        evidence.add(evidenceItem("live_table_contracts", requirePhysical ? "PASS" : "WARN",
                requirePhysical ? "live table contracts passed" : "physical inspection not required"));
        evidence.add(evidenceItem("realtime_pipeline_status", requireRealtime ? "PASS" : "WARN",
                requireRealtime ? "realtime pipelines are healthy" : "realtime proof not required"));
        evidence.add(evidenceItem("realtime_job_status", requireRealtime ? "PASS" : "WARN",
                requireRealtime ? "realtime jobs are running" : "realtime job proof not required"));
        evidence.add(evidenceItem("data_path_proof", requireDataPathProof ? "PASS" : "WARN",
                requireDataPathProof ? "synthetic data path proof passed" : "data path proof not required"));
        return evidence;
    }

    private static Map<String, Object> productionReadiness(String mode, List<String> contractKeys) {
        Map<String, Object> proof = ordered();
        proof.put("status", "PASS");
        proof.put("mode", mode);
        proof.put("windowStart", DEFAULT_WINDOW_START);
        proof.put("windowEnd", DEFAULT_WINDOW_END);
        proof.put("contractKeys", List.copyOf(contractKeys));
        proof.put("checkedContractCount", contractKeys.size());
        return proof;
    }

    private static Map<String, Object> liveTableInspection(boolean required) {
        Map<String, Object> inspection = ordered();
        inspection.put("status", required ? "PASS" : "WARN");
        inspection.put("inspectedTableCount", required ? 3 : 0);
        inspection.put("missingTableCount", 0);
        inspection.put("inspectedBy", "warehouse-e2e-certification");
        return inspection;
    }

    private static Map<String, Object> realtimePipelineStatus(boolean required) {
        Map<String, Object> status = ordered();
        status.put("status", required ? "PASS" : "WARN");
        status.put("runningPipelineCount", required ? 2 : 0);
        status.put("failedPipelineCount", 0);
        return status;
    }

    private static Map<String, Object> realtimeJobStatus(boolean required) {
        Map<String, Object> status = ordered();
        status.put("status", required ? "PASS" : "WARN");
        status.put("runningJobCount", required ? 2 : 0);
        status.put("failedJobCount", 0);
        return status;
    }

    private static Map<String, Object> dataPathProof(boolean required) {
        Map<String, Object> proof = ordered();
        proof.put("status", required ? "PASS" : "WARN");
        proof.put("sourceMode", "MYSQL_CDC");
        proof.put("sourceStatus", required ? "PASS" : "WARN");
        proof.put("odsStatus", required ? "PASS" : "WARN");
        proof.put("sampleEventCount", required ? 4 : 0);
        return proof;
    }

    private static Map<String, Object> evidenceItem(String key, String status, String reason) {
        Map<String, Object> item = ordered();
        item.put("key", key);
        item.put("status", status);
        item.put("reason", reason);
        return item;
    }

    private static String evidenceJson() {
        return "[{\"key\":\"production_readiness\",\"status\":\"PASS\"},"
                + "{\"key\":\"doris_jdbc_connectivity\",\"status\":\"PASS\"},"
                + "{\"key\":\"live_table_contracts\",\"status\":\"PASS\"},"
                + "{\"key\":\"realtime_pipeline_status\",\"status\":\"PASS\"},"
                + "{\"key\":\"realtime_job_status\",\"status\":\"PASS\"},"
                + "{\"key\":\"data_path_proof\",\"status\":\"PASS\"}]";
    }

    private static String stringListJson(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .reduce("[", (json, value) -> "[".equals(json) ? json + value : json + "," + value) + "]";
    }

    private static String defaultString(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static Map<String, Object> ordered() {
        return new LinkedHashMap<>();
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }
}
