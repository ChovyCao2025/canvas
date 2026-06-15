package org.chovy.canvas.cdp.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CdpWarehouseRealtimeCatalog {

    private final Map<Long, TenantRealtime> tenants = new LinkedHashMap<>();

    public Map<String, Object> realtimeStatus(Long tenantId) {
        TenantRealtime realtime = realtime(tenantId);
        return Map.of(
                "tenantId", tenantId,
                "pipelineCount", realtime.pipelines.size(),
                "schemaCount", realtime.schemas.size(),
                "checkpointCount", realtime.checkpoints.size(),
                "jobCount", realtime.jobs.size(),
                "probeTargetCount", realtime.probeTargets.size(),
                "status", hasFailedCheckpoint(realtime) ? "WARN" : "PASS");
    }

    public Map<String, Object> registerSchema(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "pipelineKey");
        TenantRealtime realtime = realtime(tenantId);
        Map<String, Object> schema = record(payload);
        schema.put("tenantId", tenantId);
        schema.put("schemaKey", "schema-" + (realtime.schemas.size() + 1));
        schema.put("schemaRole", upper(value(payload.get("schemaRole"), "SOURCE")));
        schema.put("schemaVersion", value(payload.get("schemaVersion"), "v" + (realtime.schemas.size() + 1)));
        schema.put("active", payload.get("active") == null || Boolean.TRUE.equals(payload.get("active")));
        schema.put("updatedBy", actor);
        realtime.schemas.add(schema);
        return copy(schema);
    }

    public List<Map<String, Object>> listSchemas(Long tenantId, String pipelineKey, String schemaRole, int limit) {
        return realtime(tenantId).schemas.stream()
                .filter(item -> matches(item, "pipelineKey", pipelineKey))
                .filter(item -> matches(item, "schemaRole", upper(schemaRole)))
                .limit(limit)
                .map(CdpWarehouseRealtimeCatalog::copy)
                .toList();
    }

    public Map<String, Object> latestSchema(Long tenantId, String pipelineKey, String schemaRole) {
        List<Map<String, Object>> matches = listSchemas(tenantId, pipelineKey, schemaRole, 100);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("schema not found");
        }
        return matches.get(matches.size() - 1);
    }

    public List<Map<String, Object>> listPipelineContracts(Long tenantId, String lifecycleStatus) {
        return realtime(tenantId).pipelines.stream()
                .filter(item -> matches(item, "lifecycleStatus", upper(lifecycleStatus)))
                .map(CdpWarehouseRealtimeCatalog::copy)
                .toList();
    }

    public Map<String, Object> upsertPipelineContract(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "pipelineKey");
        TenantRealtime realtime = realtime(tenantId);
        String pipelineKey = String.valueOf(payload.get("pipelineKey"));
        Map<String, Object> pipeline = findOptional(realtime.pipelines, "pipelineKey", pipelineKey);
        if (pipeline == null) {
            pipeline = new LinkedHashMap<>();
            pipeline.put("pipelineId", (long) realtime.pipelines.size() + 1);
            pipeline.put("pipelineKey", pipelineKey);
            realtime.pipelines.add(pipeline);
        }
        pipeline.putAll(payload);
        pipeline.put("tenantId", tenantId);
        pipeline.put("pipelineKey", pipelineKey);
        pipeline.put("lifecycleStatus", upper(value(payload.get("lifecycleStatus"), "ACTIVE")));
        pipeline.put("updatedBy", actor);
        return copy(pipeline);
    }

    public Map<String, Object> reportCheckpoint(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "pipelineKey");
        TenantRealtime realtime = realtime(tenantId);
        Map<String, Object> checkpoint = record(payload);
        checkpoint.put("tenantId", tenantId);
        checkpoint.put("checkpointKey", "checkpoint-" + (realtime.checkpoints.size() + 1));
        checkpoint.put("status", upper(value(payload.get("status"), "PASSED")));
        checkpoint.put("reportedBy", actor);
        realtime.checkpoints.add(checkpoint);
        return copy(checkpoint);
    }

    public Map<String, Object> pipelineStatus(Long tenantId, int recentLimit) {
        TenantRealtime realtime = realtime(tenantId);
        return Map.of(
                "tenantId", tenantId,
                "pipelineCount", realtime.pipelines.size(),
                "checkpointCount", realtime.checkpoints.size(),
                "recentCheckpoints", copies(realtime.checkpoints, recentLimit),
                "pipelines", copies(realtime.pipelines, recentLimit));
    }

    public Map<String, Object> scanJobIncidents(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds,
                                                int limit) {
        TenantRealtime realtime = realtime(tenantId);
        long affected = realtime.jobs.stream().filter(job -> matches(job, "pipelineKey", pipelineKey)).count();
        Map<String, Object> incident = Map.of(
                "tenantId", tenantId,
                "scanKey", "job-incident-scan-" + (realtime.jobIncidentScans.size() + 1),
                "pipelineKey", value(pipelineKey, "ALL"),
                "maxHeartbeatAgeSeconds", maxHeartbeatAgeSeconds,
                "scannedCount", Math.min(affected, limit),
                "incidentCount", 0);
        realtime.jobIncidentScans.add(new LinkedHashMap<>(incident));
        return incident;
    }

    public Map<String, Object> heartbeat(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "jobKey");
        TenantRealtime realtime = realtime(tenantId);
        String jobKey = String.valueOf(payload.get("jobKey"));
        Map<String, Object> job = findOptional(realtime.jobs, "jobKey", jobKey);
        if (job == null) {
            job = new LinkedHashMap<>();
            job.put("jobId", (long) realtime.jobs.size() + 1);
            job.put("jobKey", jobKey);
            realtime.jobs.add(job);
        }
        job.putAll(payload);
        job.put("tenantId", tenantId);
        job.put("jobKey", jobKey);
        job.put("runtimeStatus", upper(value(payload.get("runtimeStatus"), "RUNNING")));
        job.put("updatedBy", actor);
        return copy(job);
    }

    public Map<String, Object> jobStatus(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds, int limit) {
        List<Map<String, Object>> jobs = realtime(tenantId).jobs.stream()
                .filter(item -> matches(item, "pipelineKey", pipelineKey))
                .limit(limit)
                .map(CdpWarehouseRealtimeCatalog::copy)
                .toList();
        return Map.of(
                "tenantId", tenantId,
                "pipelineKey", value(pipelineKey, "ALL"),
                "maxHeartbeatAgeSeconds", maxHeartbeatAgeSeconds,
                "jobCount", jobs.size(),
                "jobs", jobs);
    }

    public Map<String, Object> requestAction(Long tenantId, Map<String, Object> payload, String actor) {
        required(payload, "jobKey");
        TenantRealtime realtime = realtime(tenantId);
        Map<String, Object> action = record(payload);
        action.put("tenantId", tenantId);
        action.put("actionId", (long) realtime.actions.size() + 1);
        action.put("action", upper(value(payload.get("action"), "RESTART")));
        action.put("status", "PENDING");
        action.put("requestedBy", actor);
        realtime.actions.add(action);
        return copy(action);
    }

    public List<Map<String, Object>> pendingActions(Long tenantId, String pipelineKey, String jobKey, int limit) {
        return realtime(tenantId).actions.stream()
                .filter(item -> matches(item, "pipelineKey", pipelineKey))
                .filter(item -> matches(item, "jobKey", jobKey))
                .filter(item -> matches(item, "status", "PENDING"))
                .limit(limit)
                .map(CdpWarehouseRealtimeCatalog::copy)
                .toList();
    }

    public Map<String, Object> acknowledgeAction(Long tenantId, Long actionId) {
        Map<String, Object> action = find(realtime(tenantId).actions, "actionId", actionId, "action not found");
        action.put("status", "ACKNOWLEDGED");
        return copy(action);
    }

    public Map<String, Object> completeAction(Long tenantId, Long actionId, String status, String resultMessage) {
        Map<String, Object> action = find(realtime(tenantId).actions, "actionId", actionId, "action not found");
        action.put("status", upper(value(status, "COMPLETED")));
        action.put("resultMessage", value(resultMessage, ""));
        return copy(action);
    }

    public Map<String, Object> scanPipelineIncidents(Long tenantId, int recentLimit) {
        TenantRealtime realtime = realtime(tenantId);
        Map<String, Object> scan = Map.of(
                "tenantId", tenantId,
                "scanKey", "pipeline-incident-scan-" + (realtime.pipelineIncidentScans.size() + 1),
                "recentLimit", recentLimit,
                "pipelineCount", Math.min(realtime.pipelines.size(), recentLimit),
                "incidentCount", hasFailedCheckpoint(realtime) ? 1 : 0);
        realtime.pipelineIncidentScans.add(new LinkedHashMap<>(scan));
        return scan;
    }

    public Map<String, Object> upsertProbeTarget(Long tenantId, Map<String, Object> payload, String actor) {
        TenantRealtime realtime = realtime(tenantId);
        Map<String, Object> target = record(payload);
        target.put("tenantId", tenantId);
        target.put("targetId", (long) realtime.probeTargets.size() + 1);
        target.put("enabled", payload.get("enabled") == null || Boolean.TRUE.equals(payload.get("enabled")));
        target.put("updatedBy", actor);
        realtime.probeTargets.add(target);
        return copy(target);
    }

    public List<Map<String, Object>> listProbeTargets(Long tenantId, boolean includeDisabled, int limit) {
        return realtime(tenantId).probeTargets.stream()
                .filter(item -> includeDisabled || Boolean.TRUE.equals(item.get("enabled")))
                .limit(limit)
                .map(CdpWarehouseRealtimeCatalog::copy)
                .toList();
    }

    public Map<String, Object> setProbeTargetEnabled(Long tenantId, Long targetId, boolean enabled) {
        Map<String, Object> target = find(realtime(tenantId).probeTargets, "targetId", targetId, "target not found");
        target.put("enabled", enabled);
        return copy(target);
    }

    public Map<String, Object> scanProbeTargets(Long tenantId, Long targetId, int limit) {
        long scanned = realtime(tenantId).probeTargets.stream()
                .filter(item -> targetId == null || Objects.equals(item.get("targetId"), targetId))
                .limit(limit)
                .count();
        return Map.of("tenantId", tenantId, "targetId", targetId, "scannedCount", scanned, "incidentCount", 0);
    }

    private TenantRealtime realtime(Long tenantId) {
        return tenants.computeIfAbsent(tenantId, ignored -> new TenantRealtime());
    }

    private static boolean hasFailedCheckpoint(TenantRealtime realtime) {
        return realtime.checkpoints.stream().anyMatch(item -> "FAILED".equals(item.get("status")));
    }

    private static Map<String, Object> record(Map<String, Object> payload) {
        return new LinkedHashMap<>(payload);
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        return new LinkedHashMap<>(source);
    }

    private static List<Map<String, Object>> copies(List<Map<String, Object>> source, int limit) {
        return source.stream().limit(limit).map(CdpWarehouseRealtimeCatalog::copy).toList();
    }

    private static boolean matches(Map<String, Object> item, String field, Object expected) {
        return expected == null || String.valueOf(expected).isBlank() || Objects.equals(item.get(field), expected);
    }

    private static Map<String, Object> find(List<Map<String, Object>> rows, String key, Object value, String message) {
        Map<String, Object> match = findOptional(rows, key, value);
        if (match == null) {
            throw new IllegalArgumentException(message);
        }
        return match;
    }

    private static Map<String, Object> findOptional(List<Map<String, Object>> rows, String key, Object value) {
        return rows.stream().filter(row -> Objects.equals(row.get(key), value)).findFirst().orElse(null);
    }

    private static void required(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key + " is required");
        }
    }

    private static String value(Object value, String fallback) {
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private static String upper(String value) {
        return value == null || value.isBlank() ? value : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class TenantRealtime {
        private final List<Map<String, Object>> schemas = new ArrayList<>();
        private final List<Map<String, Object>> pipelines = new ArrayList<>();
        private final List<Map<String, Object>> checkpoints = new ArrayList<>();
        private final List<Map<String, Object>> jobs = new ArrayList<>();
        private final List<Map<String, Object>> actions = new ArrayList<>();
        private final List<Map<String, Object>> probeTargets = new ArrayList<>();
        private final List<Map<String, Object>> jobIncidentScans = new ArrayList<>();
        private final List<Map<String, Object>> pipelineIncidentScans = new ArrayList<>();
    }
}
