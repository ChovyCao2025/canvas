package org.chovy.canvas.domain.warehouse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseExternalRealtimeJobProbeTargetDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseExternalRealtimeJobProbeTargetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseExternalRealtimeJobProbeService {

    private static final int DEFAULT_MAX_STALENESS_SECONDS = 300;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_SKIPPED = "SKIPPED";
    private static final String RUNTIME_FAILED = "FAILED";

    private final CdpWarehouseExternalRealtimeJobProbeTargetMapper targetMapper;
    private final CdpWarehouseExternalRealtimeJobProbeClient probeClient;
    private final CdpWarehouseRealtimeJobControlService jobControlService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CdpWarehouseExternalRealtimeJobProbeService(
            CdpWarehouseExternalRealtimeJobProbeTargetMapper targetMapper,
            CdpWarehouseExternalRealtimeJobProbeClient probeClient,
            CdpWarehouseRealtimeJobControlService jobControlService,
            ObjectMapper objectMapper) {
        this(targetMapper, probeClient, jobControlService, objectMapper, Clock.systemDefaultZone());
    }

    CdpWarehouseExternalRealtimeJobProbeService(
            CdpWarehouseExternalRealtimeJobProbeTargetMapper targetMapper,
            CdpWarehouseExternalRealtimeJobProbeClient probeClient,
            CdpWarehouseRealtimeJobControlService jobControlService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.targetMapper = targetMapper;
        this.probeClient = probeClient;
        this.jobControlService = jobControlService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.clock = clock;
    }

    public ProbeTargetView upsertTarget(Long tenantId, TargetCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("target command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseExternalRealtimeJobProbeTargetDO row = new CdpWarehouseExternalRealtimeJobProbeTargetDO();
        row.setTenantId(scopedTenantId);
        row.setPipelineKey(required(command.pipelineKey(), "pipelineKey"));
        row.setJobKey(required(command.jobKey(), "jobKey"));
        row.setEngineType(engineType(command.engineType()));
        row.setEndpointUrl(required(command.endpointUrl(), "endpointUrl"));
        row.setAuthRef(blankToNull(command.authRef()));
        row.setExternalJobId(blankToNull(command.externalJobId()));
        row.setConnectorName(blankToNull(command.connectorName()));
        row.setDeploymentRef(blankToNull(command.deploymentRef()));
        row.setEnabled(Boolean.FALSE.equals(command.enabled()) ? 0 : 1);
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setMaxStalenessSeconds(positiveOrDefault(command.maxStalenessSeconds(),
                DEFAULT_MAX_STALENESS_SECONDS, "maxStalenessSeconds"));
        row.setConfigJson(blankToNull(command.configJson()));
        targetMapper.upsert(row);
        CdpWarehouseExternalRealtimeJobProbeTargetDO saved =
                targetMapper.findByKey(scopedTenantId, row.getPipelineKey(), row.getJobKey());
        return toTarget(saved == null ? row : saved);
    }

    public List<ProbeTargetView> listTargets(Long tenantId, boolean includeDisabled, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundLimit(limit);
        List<CdpWarehouseExternalRealtimeJobProbeTargetDO> rows = includeDisabled
                ? targetMapper.listTargets(scopedTenantId, boundedLimit)
                : targetMapper.listEnabledTargets(scopedTenantId, boundedLimit);
        return safeList(rows).stream().map(this::toTarget).toList();
    }

    public ProbeTargetView setEnabled(Long tenantId, Long targetId, boolean enabled) {
        Long scopedTenantId = normalizeTenant(tenantId);
        Long scopedTargetId = requirePositive(targetId, "targetId");
        targetMapper.updateEnabled(scopedTenantId, scopedTargetId, enabled ? 1 : 0);
        CdpWarehouseExternalRealtimeJobProbeTargetDO row =
                targetMapper.findByTenantAndId(scopedTenantId, scopedTargetId);
        if (row == null) {
            throw new IllegalArgumentException("probe target not found: " + scopedTargetId);
        }
        return toTarget(row);
    }

    public ScanSummary scan(Long tenantId, ScanCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ScanCommand scopedCommand = command == null ? new ScanCommand(null, DEFAULT_LIMIT) : command;
        List<CdpWarehouseExternalRealtimeJobProbeTargetDO> targets;
        if (scopedCommand.targetId() != null) {
            targets = List.of(requireTarget(scopedTenantId, scopedCommand.targetId()));
        } else {
            targets = safeList(targetMapper.listEnabledTargets(scopedTenantId, boundLimit(scopedCommand.limit())));
        }
        List<ProbeScanResult> results = targets.stream().map(this::scanTarget).toList();
        long passed = results.stream().filter(result -> STATUS_PASS.equals(result.status())).count();
        long failed = results.stream().filter(result -> STATUS_FAIL.equals(result.status())).count();
        long skipped = results.stream().filter(result -> STATUS_SKIPPED.equals(result.status())).count();
        return new ScanSummary(scopedTenantId, results.size(), passed, failed, skipped, results);
    }

    private ProbeScanResult scanTarget(CdpWarehouseExternalRealtimeJobProbeTargetDO target) {
        if (!enabled(target)) {
            return new ProbeScanResult(target.getId(), target.getPipelineKey(), target.getJobKey(),
                    STATUS_SKIPPED, null, "probe target is disabled", null);
        }
        LocalDateTime probedAt = now();
        try {
            CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult probe =
                    probeClient.probe(toProbeTarget(target));
            CdpWarehouseRealtimeJobControlService.JobInstanceView heartbeat =
                    writeHeartbeat(target, probe, probedAt);
            String status = RUNTIME_FAILED.equals(upper(probe.runtimeStatus())) ? STATUS_FAIL : STATUS_PASS;
            String message = limit(defaultString(probe.message(), "external probe " + status.toLowerCase(Locale.ROOT)));
            targetMapper.updateProbeResult(target.getTenantId(), target.getId(), probedAt, status, message);
            return new ProbeScanResult(target.getId(), target.getPipelineKey(), target.getJobKey(),
                    status, heartbeat.runtimeStatus(), message, heartbeat);
        } catch (RuntimeException e) {
            String message = limit(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            CdpWarehouseRealtimeJobControlService.JobInstanceView heartbeat =
                    writeFailedHeartbeat(target, probedAt, message);
            targetMapper.updateProbeResult(target.getTenantId(), target.getId(), probedAt, STATUS_FAIL, message);
            return new ProbeScanResult(target.getId(), target.getPipelineKey(), target.getJobKey(),
                    STATUS_FAIL, RUNTIME_FAILED, message, heartbeat);
        }
    }

    private CdpWarehouseRealtimeJobControlService.JobInstanceView writeHeartbeat(
            CdpWarehouseExternalRealtimeJobProbeTargetDO target,
            CdpWarehouseExternalRealtimeJobProbeClient.ProbeResult probe,
            LocalDateTime probedAt) {
        String runtimeStatus = upperDefault(probe.runtimeStatus(), "RUNNING");
        return jobControlService.heartbeat(target.getTenantId(),
                new CdpWarehouseRealtimeJobControlService.HeartbeatCommand(
                        target.getPipelineKey(),
                        target.getJobKey(),
                        target.getEngineType(),
                        defaultString(probe.engineJobId(), engineJobId(target)),
                        defaultString(target.getDeploymentRef(), target.getEndpointUrl()),
                        runtimeStatus,
                        null,
                        probedAt,
                        defaultString(probe.payloadJson(), probePayload(target, runtimeStatus, probe.message())),
                        RUNTIME_FAILED.equals(runtimeStatus) ? probe.message() : null,
                        target.getOwnerName()));
    }

    private CdpWarehouseRealtimeJobControlService.JobInstanceView writeFailedHeartbeat(
            CdpWarehouseExternalRealtimeJobProbeTargetDO target,
            LocalDateTime probedAt,
            String errorMessage) {
        return jobControlService.heartbeat(target.getTenantId(),
                new CdpWarehouseRealtimeJobControlService.HeartbeatCommand(
                        target.getPipelineKey(),
                        target.getJobKey(),
                        target.getEngineType(),
                        engineJobId(target),
                        defaultString(target.getDeploymentRef(), target.getEndpointUrl()),
                        RUNTIME_FAILED,
                        null,
                        probedAt,
                        probePayload(target, RUNTIME_FAILED, errorMessage),
                        errorMessage,
                        target.getOwnerName()));
    }

    private String probePayload(CdpWarehouseExternalRealtimeJobProbeTargetDO target,
                                String runtimeStatus,
                                String message) {
        try {
            var node = objectMapper.createObjectNode();
            node.put("source", "EXTERNAL_REALTIME_JOB_PROBE");
            node.put("probeTargetId", target.getId());
            node.put("engineType", target.getEngineType());
            node.put("endpointUrl", target.getEndpointUrl());
            node.put("runtimeStatus", runtimeStatus);
            node.put("message", message);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"source\":\"EXTERNAL_REALTIME_JOB_PROBE\"}";
        }
    }

    private CdpWarehouseExternalRealtimeJobProbeClient.ProbeTarget toProbeTarget(
            CdpWarehouseExternalRealtimeJobProbeTargetDO target) {
        return new CdpWarehouseExternalRealtimeJobProbeClient.ProbeTarget(
                target.getId(),
                target.getTenantId(),
                target.getPipelineKey(),
                target.getJobKey(),
                target.getEngineType(),
                target.getEndpointUrl(),
                target.getAuthRef(),
                target.getExternalJobId(),
                target.getConnectorName(),
                target.getConfigJson());
    }

    private ProbeTargetView toTarget(CdpWarehouseExternalRealtimeJobProbeTargetDO row) {
        return new ProbeTargetView(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getJobKey(),
                row.getEngineType(),
                row.getEndpointUrl(),
                row.getAuthRef(),
                row.getExternalJobId(),
                row.getConnectorName(),
                row.getDeploymentRef(),
                enabled(row),
                row.getOwnerName(),
                row.getMaxStalenessSeconds(),
                row.getConfigJson(),
                row.getLastProbedAt(),
                row.getLastProbeStatus(),
                row.getLastProbeMessage(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private CdpWarehouseExternalRealtimeJobProbeTargetDO requireTarget(Long tenantId, Long targetId) {
        CdpWarehouseExternalRealtimeJobProbeTargetDO row =
                targetMapper.findByTenantAndId(tenantId, requirePositive(targetId, "targetId"));
        if (row == null) {
            throw new IllegalArgumentException("probe target not found: " + targetId);
        }
        return row;
    }

    private String engineType(String value) {
        String engine = upper(required(value, "engineType"));
        if (!"FLINK_REST".equals(engine) && !"KAFKA_CONNECT".equals(engine)
                && !"DORIS_ROUTINE_LOAD".equals(engine) && !"GENERIC_HTTP".equals(engine)) {
            throw new IllegalArgumentException(
                    "engineType must be FLINK_REST, KAFKA_CONNECT, DORIS_ROUTINE_LOAD, or GENERIC_HTTP");
        }
        return engine;
    }

    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private int boundLimit(Integer value) {
        int limit = value == null || value <= 0 ? DEFAULT_LIMIT : value;
        return Math.min(limit, MAX_LIMIT);
    }

    private Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private boolean enabled(CdpWarehouseExternalRealtimeJobProbeTargetDO row) {
        return row != null && !Integer.valueOf(0).equals(row.getEnabled());
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        String normalized = upper(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String engineJobId(CdpWarehouseExternalRealtimeJobProbeTargetDO target) {
        return defaultString(target.getExternalJobId(), target.getConnectorName());
    }

    private List<CdpWarehouseExternalRealtimeJobProbeTargetDO> safeList(
            List<CdpWarehouseExternalRealtimeJobProbeTargetDO> rows) {
        return rows == null ? List.of() : rows;
    }

    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record TargetCommand(
            String pipelineKey,
            String jobKey,
            String engineType,
            String endpointUrl,
            String authRef,
            String externalJobId,
            String connectorName,
            String deploymentRef,
            Boolean enabled,
            String ownerName,
            Integer maxStalenessSeconds,
            String configJson) {
    }

    public record ScanCommand(
            Long targetId,
            Integer limit) {
    }

    public record ProbeTargetView(
            Long id,
            Long tenantId,
            String pipelineKey,
            String jobKey,
            String engineType,
            String endpointUrl,
            String authRef,
            String externalJobId,
            String connectorName,
            String deploymentRef,
            boolean enabled,
            String ownerName,
            Integer maxStalenessSeconds,
            String configJson,
            LocalDateTime lastProbedAt,
            String lastProbeStatus,
            String lastProbeMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record ScanSummary(
            Long tenantId,
            int total,
            long passed,
            long failed,
            long skipped,
            List<ProbeScanResult> results) {
        public ScanSummary {
            results = results == null ? List.of() : List.copyOf(results);
        }
    }

    public record ProbeScanResult(
            Long targetId,
            String pipelineKey,
            String jobKey,
            String status,
            String runtimeStatus,
            String message,
            CdpWarehouseRealtimeJobControlService.JobInstanceView heartbeat) {
    }
}
