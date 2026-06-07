package org.chovy.canvas.domain.warehouse;

import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobActionDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamJobInstanceDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobActionMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamJobInstanceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CdpWarehouseRealtimeJobControlService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String RUNTIME_RUNNING = "RUNNING";
    private static final String RUNTIME_PAUSED = "PAUSED";
    private static final String RUNTIME_FAILED = "FAILED";
    private static final String RUNTIME_STOPPED = "STOPPED";
    private static final String ACTION_PAUSE = "PAUSE";
    private static final String ACTION_RESUME = "RESUME";
    private static final String ACTION_RESTART = "RESTART";
    private static final String ACTION_PENDING = "PENDING";
    private static final String ACTION_ACKNOWLEDGED = "ACKNOWLEDGED";
    private static final String ACTION_COMPLETED = "COMPLETED";
    private static final String ACTION_FAILED = "FAILED";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;
    private static final int MAX_REASON_LENGTH = 512;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final long DEFAULT_HEARTBEAT_AGE_SECONDS = 300;

    private final CdpWarehouseStreamJobInstanceMapper instanceMapper;
    private final CdpWarehouseStreamJobActionMapper actionMapper;
    private final Clock clock;

    @Autowired
    public CdpWarehouseRealtimeJobControlService(CdpWarehouseStreamJobInstanceMapper instanceMapper,
                                                 CdpWarehouseStreamJobActionMapper actionMapper) {
        this(instanceMapper, actionMapper, Clock.systemDefaultZone());
    }

    CdpWarehouseRealtimeJobControlService(CdpWarehouseStreamJobInstanceMapper instanceMapper,
                                          CdpWarehouseStreamJobActionMapper actionMapper,
                                          Clock clock) {
        this.instanceMapper = instanceMapper;
        this.actionMapper = actionMapper;
        this.clock = clock;
    }

    public JobInstanceView heartbeat(Long tenantId, HeartbeatCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("heartbeat command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        String jobKey = required(command.jobKey(), "jobKey");
        CdpWarehouseStreamJobInstanceDO existing =
                instanceMapper.findByKey(scopedTenantId, pipelineKey, jobKey);
        String runtimeStatus = upperDefault(command.runtimeStatus(), RUNTIME_RUNNING);
        String desiredStatus = hasText(command.desiredStatus())
                ? upperDefault(command.desiredStatus(), RUNTIME_RUNNING)
                : existing == null ? RUNTIME_RUNNING : existing.getDesiredStatus();

        CdpWarehouseStreamJobInstanceDO row = new CdpWarehouseStreamJobInstanceDO();
        row.setTenantId(scopedTenantId);
        row.setPipelineKey(pipelineKey);
        row.setJobKey(jobKey);
        row.setEngineType(upperRequired(command.engineType(), "engineType"));
        row.setEngineJobId(blankToNull(command.engineJobId()));
        row.setDeploymentRef(blankToNull(command.deploymentRef()));
        row.setRuntimeStatus(runtimeStatus);
        row.setDesiredStatus(upperDefault(desiredStatus, RUNTIME_RUNNING));
        row.setLastHeartbeatAt(command.heartbeatAt() == null ? now() : command.heartbeatAt());
        row.setHeartbeatPayloadJson(blankToNull(command.heartbeatPayloadJson()));
        row.setLastErrorMessage(limit(command.errorMessage(), MAX_MESSAGE_LENGTH));
        row.setOwnerName(blankToNull(command.ownerName()));
        instanceMapper.upsertHeartbeat(row);
        return toJob(row, evaluate(row, DEFAULT_HEARTBEAT_AGE_SECONDS));
    }

    public JobStatusSummary status(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        long heartbeatAge = maxHeartbeatAgeSeconds <= 0 ? DEFAULT_HEARTBEAT_AGE_SECONDS : maxHeartbeatAgeSeconds;
        List<JobInstanceView> jobs = safeList(instanceMapper.listInstances(
                        scopedTenantId, blankToNull(pipelineKey), boundLimit(limit)))
                .stream()
                .map(row -> toJob(row, evaluate(row, heartbeatAge)))
                .toList();
        long passed = jobs.stream().filter(job -> STATUS_PASS.equals(job.healthStatus())).count();
        long warned = jobs.stream().filter(job -> STATUS_WARN.equals(job.healthStatus())).count();
        long failed = jobs.stream().filter(job -> STATUS_FAIL.equals(job.healthStatus())).count();
        return new JobStatusSummary(scopedTenantId, jobs.size(), passed, warned, failed, jobs);
    }

    public JobActionView requestAction(Long tenantId, ActionRequestCommand command, String operator) {
        if (command == null) {
            throw new IllegalArgumentException("action command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        String jobKey = required(command.jobKey(), "jobKey");
        CdpWarehouseStreamJobInstanceDO job = instanceMapper.findByKey(scopedTenantId, pipelineKey, jobKey);
        if (job == null) {
            throw new IllegalArgumentException("stream job instance not found: " + jobKey);
        }
        String action = action(command.action());
        String desiredStatus = desiredStatusFor(action);
        instanceMapper.updateDesiredStatus(scopedTenantId, pipelineKey, jobKey, desiredStatus);

        CdpWarehouseStreamJobActionDO row = new CdpWarehouseStreamJobActionDO();
        row.setTenantId(scopedTenantId);
        row.setPipelineKey(pipelineKey);
        row.setJobKey(jobKey);
        row.setAction(action);
        row.setStatus(ACTION_PENDING);
        row.setRequestedBy(normalizeOperator(operator));
        row.setReason(limit(required(command.reason(), "reason"), MAX_REASON_LENGTH));
        row.setRequestedAt(now());
        actionMapper.insert(row);
        return toAction(row);
    }

    public List<JobActionView> pendingActions(Long tenantId, String pipelineKey, String jobKey, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        return safeList(actionMapper.selectPending(
                        scopedTenantId,
                        required(pipelineKey, "pipelineKey"),
                        required(jobKey, "jobKey"),
                        boundLimit(limit)))
                .stream()
                .map(this::toAction)
                .toList();
    }

    public JobActionView acknowledge(Long tenantId, Long actionId) {
        CdpWarehouseStreamJobActionDO row = requireAction(tenantId, actionId);
        if (!ACTION_PENDING.equals(row.getStatus())) {
            throw new IllegalArgumentException("only PENDING action can be acknowledged");
        }
        LocalDateTime acknowledgedAt = now();
        actionMapper.updateStatus(row.getTenantId(), row.getId(), ACTION_ACKNOWLEDGED, acknowledgedAt, null, null);
        row.setStatus(ACTION_ACKNOWLEDGED);
        row.setAcknowledgedAt(acknowledgedAt);
        return toAction(row);
    }

    public JobActionView complete(Long tenantId, Long actionId, String status, String resultMessage) {
        CdpWarehouseStreamJobActionDO row = requireAction(tenantId, actionId);
        String completedStatus = completeStatus(status);
        LocalDateTime completedAt = now();
        LocalDateTime acknowledgedAt = row.getAcknowledgedAt() == null ? completedAt : row.getAcknowledgedAt();
        String message = limit(resultMessage, MAX_MESSAGE_LENGTH);
        actionMapper.updateStatus(row.getTenantId(), row.getId(), completedStatus, acknowledgedAt, completedAt, message);
        row.setStatus(completedStatus);
        row.setAcknowledgedAt(acknowledgedAt);
        row.setCompletedAt(completedAt);
        row.setResultMessage(message);
        return toAction(row);
    }

    private CdpWarehouseStreamJobActionDO requireAction(Long tenantId, Long actionId) {
        if (actionId == null || actionId <= 0) {
            throw new IllegalArgumentException("actionId must be positive");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        CdpWarehouseStreamJobActionDO row = actionMapper.findByTenantAndId(scopedTenantId, actionId);
        if (row == null) {
            throw new IllegalArgumentException("stream job action not found: " + actionId);
        }
        return row;
    }

    private HealthEvaluation evaluate(CdpWarehouseStreamJobInstanceDO row, long maxHeartbeatAgeSeconds) {
        List<String> reasons = new ArrayList<>();
        if (RUNTIME_FAILED.equals(row.getRuntimeStatus())) {
            reasons.add(defaultString(row.getLastErrorMessage(), "job runtime status is FAILED"));
            return new HealthEvaluation(STATUS_FAIL, reasons);
        }
        if (row.getLastHeartbeatAt() == null) {
            reasons.add("heartbeat has not been reported");
        } else {
            long ageSeconds = Duration.between(row.getLastHeartbeatAt(), now()).getSeconds();
            if (ageSeconds > maxHeartbeatAgeSeconds) {
                reasons.add("heartbeat age " + ageSeconds + "s exceeds maxHeartbeatAgeSeconds "
                        + maxHeartbeatAgeSeconds);
            }
        }
        if (RUNTIME_PAUSED.equals(row.getDesiredStatus()) && !RUNTIME_PAUSED.equals(row.getRuntimeStatus())) {
            reasons.add("desired PAUSED but runtime is " + row.getRuntimeStatus());
        }
        if (RUNTIME_RUNNING.equals(row.getDesiredStatus())
                && (RUNTIME_PAUSED.equals(row.getRuntimeStatus()) || RUNTIME_STOPPED.equals(row.getRuntimeStatus()))) {
            reasons.add("desired RUNNING but runtime is " + row.getRuntimeStatus());
        }
        return reasons.isEmpty()
                ? new HealthEvaluation(STATUS_PASS, List.of())
                : new HealthEvaluation(STATUS_WARN, reasons);
    }

    private JobInstanceView toJob(CdpWarehouseStreamJobInstanceDO row, HealthEvaluation evaluation) {
        return new JobInstanceView(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getJobKey(),
                row.getEngineType(),
                row.getEngineJobId(),
                row.getDeploymentRef(),
                row.getRuntimeStatus(),
                row.getDesiredStatus(),
                row.getLastHeartbeatAt(),
                row.getHeartbeatPayloadJson(),
                row.getLastErrorMessage(),
                row.getOwnerName(),
                evaluation.status(),
                evaluation.reasons(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }

    private JobActionView toAction(CdpWarehouseStreamJobActionDO row) {
        return new JobActionView(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getJobKey(),
                row.getAction(),
                row.getStatus(),
                row.getRequestedBy(),
                row.getReason(),
                row.getRequestedAt(),
                row.getAcknowledgedAt(),
                row.getCompletedAt(),
                row.getResultMessage());
    }

    private String desiredStatusFor(String action) {
        return ACTION_PAUSE.equals(action) ? RUNTIME_PAUSED : RUNTIME_RUNNING;
    }

    private String action(String value) {
        String normalized = upperRequired(value, "action");
        if (!ACTION_PAUSE.equals(normalized) && !ACTION_RESUME.equals(normalized) && !ACTION_RESTART.equals(normalized)) {
            throw new IllegalArgumentException("action must be PAUSE, RESUME, or RESTART");
        }
        return normalized;
    }

    private String completeStatus(String value) {
        String normalized = upperDefault(value, ACTION_COMPLETED);
        if (!ACTION_COMPLETED.equals(normalized) && !ACTION_FAILED.equals(normalized)) {
            throw new IllegalArgumentException("status must be COMPLETED or FAILED");
        }
        return normalized;
    }

    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private record HealthEvaluation(String status, List<String> reasons) {
    }

    public record HeartbeatCommand(
            String pipelineKey,
            String jobKey,
            String engineType,
            String engineJobId,
            String deploymentRef,
            String runtimeStatus,
            String desiredStatus,
            LocalDateTime heartbeatAt,
            String heartbeatPayloadJson,
            String errorMessage,
            String ownerName) {
    }

    public record ActionRequestCommand(
            String pipelineKey,
            String jobKey,
            String action,
            String reason) {
    }

    public record JobInstanceView(
            Long id,
            Long tenantId,
            String pipelineKey,
            String jobKey,
            String engineType,
            String engineJobId,
            String deploymentRef,
            String runtimeStatus,
            String desiredStatus,
            LocalDateTime lastHeartbeatAt,
            String heartbeatPayloadJson,
            String lastErrorMessage,
            String ownerName,
            String healthStatus,
            List<String> reasons,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record JobStatusSummary(
            Long tenantId,
            int total,
            long passed,
            long warned,
            long failed,
            List<JobInstanceView> jobs) {
    }

    public record JobActionView(
            Long id,
            Long tenantId,
            String pipelineKey,
            String jobKey,
            String action,
            String status,
            String requestedBy,
            String reason,
            LocalDateTime requestedAt,
            LocalDateTime acknowledgedAt,
            LocalDateTime completedAt,
            String resultMessage) {
    }
}
