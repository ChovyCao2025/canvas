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
/**
 * CdpWarehouseRealtimeJobControlService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 CdpWarehouseRealtimeJobControlService 实例。
     *
     * @param instanceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param actionMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeJobControlService(CdpWarehouseStreamJobInstanceMapper instanceMapper,
                                                 CdpWarehouseStreamJobActionMapper actionMapper) {
        this(instanceMapper, actionMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseRealtimeJobControlService 实例。
     *
     * @param instanceMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param actionMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehouseRealtimeJobControlService(CdpWarehouseStreamJobInstanceMapper instanceMapper,
                                          CdpWarehouseStreamJobActionMapper actionMapper,
                                          Clock clock) {
        this.instanceMapper = instanceMapper;
        this.actionMapper = actionMapper;
        this.clock = clock;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 heartbeat 流程生成的业务结果。
     */
    public JobInstanceView heartbeat(Long tenantId, HeartbeatCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("heartbeat command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        String jobKey = required(command.jobKey(), "jobKey");
        CdpWarehouseStreamJobInstanceDO existing =
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toJob(row, evaluate(row, DEFAULT_HEARTBEAT_AGE_SECONDS));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param maxHeartbeatAgeSeconds max heartbeat age seconds 参数，用于 status 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 status 流程生成的业务结果。
     */
    public JobStatusSummary status(Long tenantId, String pipelineKey, long maxHeartbeatAgeSeconds, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        long heartbeatAge = maxHeartbeatAgeSeconds <= 0 ? DEFAULT_HEARTBEAT_AGE_SECONDS : maxHeartbeatAgeSeconds;
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        List<JobInstanceView> jobs = safeList(instanceMapper.listInstances(
                        scopedTenantId, blankToNull(pipelineKey), boundLimit(limit)))
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .stream()
                .map(row -> toJob(row, evaluate(row, heartbeatAge)))
                .toList();
        long passed = jobs.stream().filter(job -> STATUS_PASS.equals(job.healthStatus())).count();
        long warned = jobs.stream().filter(job -> STATUS_WARN.equals(job.healthStatus())).count();
        long failed = jobs.stream().filter(job -> STATUS_FAIL.equals(job.healthStatus())).count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new JobStatusSummary(scopedTenantId, jobs.size(), passed, warned, failed, jobs);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回 requestAction 流程生成的业务结果。
     */
    public JobActionView requestAction(Long tenantId, ActionRequestCommand command, String operator) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("action command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        String jobKey = required(command.jobKey(), "jobKey");
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toAction(row);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param jobKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回 pending actions 汇总后的集合、分页或映射视图。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actionId 业务对象 ID，用于定位具体记录。
     * @return 返回 acknowledge 流程生成的业务结果。
     */
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

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actionId 业务对象 ID，用于定位具体记录。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param resultMessage result message 参数，用于 complete 流程中的校验、计算或对象转换。
     * @return 返回 complete 流程生成的业务结果。
     */
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

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param actionId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireAction 流程生成的业务结果。
     */
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

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param maxHeartbeatAgeSeconds max heartbeat age seconds 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    private HealthEvaluation evaluate(CdpWarehouseStreamJobInstanceDO row, long maxHeartbeatAgeSeconds) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> reasons = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 汇总前面计算出的状态和明细，返回给调用方。
        return reasons.isEmpty()
                ? new HealthEvaluation(STATUS_PASS, List.of())
                : new HealthEvaluation(STATUS_WARN, reasons);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param evaluation evaluation 参数，用于 toJob 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private JobInstanceView toJob(CdpWarehouseStreamJobInstanceDO row, HealthEvaluation evaluation) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param action action 参数，用于 desiredStatusFor 流程中的校验、计算或对象转换。
     * @return 返回 desired status for 生成的文本或业务键。
     */
    private String desiredStatusFor(String action) {
        return ACTION_PAUSE.equals(action) ? RUNTIME_PAUSED : RUNTIME_RUNNING;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 action 生成的文本或业务键。
     */
    private String action(String value) {
        String normalized = upperRequired(value, "action");
        if (!ACTION_PAUSE.equals(normalized) && !ACTION_RESUME.equals(normalized) && !ACTION_RESTART.equals(normalized)) {
            throw new IllegalArgumentException("action must be PAUSE, RESUME, or RESTART");
        }
        return normalized;
    }

    /**
     * 推进状态流转并记录本次处理结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 complete status 生成的文本或业务键。
     */
    private String completeStatus(String value) {
        String normalized = upperDefault(value, ACTION_COMPLETED);
        if (!ACTION_COMPLETED.equals(normalized) && !ACTION_FAILED.equals(normalized)) {
            throw new IllegalArgumentException("status must be COMPLETED or FAILED");
        }
        return normalized;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 upper required 生成的文本或业务键。
     */
    private String upperRequired(String value, String fieldName) {
        return required(value, fieldName).toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param maxLength max length 参数，用于 limit 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回布尔判断结果。
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 now 流程生成的业务结果。
     */
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /**
     * HealthEvaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record HealthEvaluation(String status, List<String> reasons) {
    }

    /**
     * HeartbeatCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ActionRequestCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ActionRequestCommand(
            String pipelineKey,
            String jobKey,
            String action,
            String reason) {
    }

    /**
     * JobInstanceView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * JobStatusSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record JobStatusSummary(
            Long tenantId,
            int total,
            long passed,
            long warned,
            long failed,
            List<JobInstanceView> jobs) {
    }

    /**
     * JobActionView 承载对应领域的业务规则、流程编排和结果转换。
     */
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
