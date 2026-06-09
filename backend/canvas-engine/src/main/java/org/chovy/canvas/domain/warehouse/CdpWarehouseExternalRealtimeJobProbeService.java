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
/**
 * CdpWarehouseExternalRealtimeJobProbeService 承载对应领域的业务规则、流程编排和结果转换。
 */
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
    /**
     * 初始化 CdpWarehouseExternalRealtimeJobProbeService 实例。
     *
     * @param targetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param jobControlService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseExternalRealtimeJobProbeService(
            CdpWarehouseExternalRealtimeJobProbeTargetMapper targetMapper,
            CdpWarehouseExternalRealtimeJobProbeClient probeClient,
            CdpWarehouseRealtimeJobControlService jobControlService,
            ObjectMapper objectMapper) {
        this(targetMapper, probeClient, jobControlService, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehouseExternalRealtimeJobProbeService 实例。
     *
     * @param targetMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param probeClient 依赖组件，用于完成数据访问或外部能力调用。
     * @param jobControlService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ProbeTargetView upsertTarget(Long tenantId, TargetCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        targetMapper.upsert(row);
        CdpWarehouseExternalRealtimeJobProbeTargetDO saved =
                targetMapper.findByKey(scopedTenantId, row.getPipelineKey(), row.getJobKey());
        return toTarget(saved == null ? row : saved);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param includeDisabled include disabled 参数，用于 listTargets 流程中的校验、计算或对象转换。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<ProbeTargetView> listTargets(Long tenantId, boolean includeDisabled, int limit) {
        Long scopedTenantId = normalizeTenant(tenantId);
        int boundedLimit = boundLimit(limit);
        List<CdpWarehouseExternalRealtimeJobProbeTargetDO> rows = includeDisabled
                ? targetMapper.listTargets(scopedTenantId, boundedLimit)
                : targetMapper.listEnabledTargets(scopedTenantId, boundedLimit);
        return safeList(rows).stream().map(this::toTarget).toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @param enabled enabled 参数，用于 setEnabled 流程中的校验、计算或对象转换。
     * @return 返回 setEnabled 流程生成的业务结果。
     */
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

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public ScanSummary scan(Long tenantId, ScanCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        ScanCommand scopedCommand = command == null ? new ScanCommand(null, DEFAULT_LIMIT) : command;
        List<CdpWarehouseExternalRealtimeJobProbeTargetDO> targets;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (scopedCommand.targetId() != null) {
            targets = List.of(requireTarget(scopedTenantId, scopedCommand.targetId()));
        } else {
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            targets = safeList(targetMapper.listEnabledTargets(scopedTenantId, boundLimit(scopedCommand.limit())));
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<ProbeScanResult> results = targets.stream().map(this::scanTarget).toList();
        long passed = results.stream().filter(result -> STATUS_PASS.equals(result.status())).count();
        long failed = results.stream().filter(result -> STATUS_FAIL.equals(result.status())).count();
        long skipped = results.stream().filter(result -> STATUS_SKIPPED.equals(result.status())).count();
        return new ScanSummary(scopedTenantId, results.size(), passed, failed, skipped, results);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param target target 参数，用于 scanTarget 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private ProbeScanResult scanTarget(CdpWarehouseExternalRealtimeJobProbeTargetDO target) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
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
            // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
            targetMapper.updateProbeResult(target.getTenantId(), target.getId(), probedAt, status, message);
            return new ProbeScanResult(target.getId(), target.getPipelineKey(), target.getJobKey(),
                    status, heartbeat.runtimeStatus(), message, heartbeat);
        } catch (RuntimeException e) {
            String message = limit(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
            CdpWarehouseRealtimeJobControlService.JobInstanceView heartbeat =
                    writeFailedHeartbeat(target, probedAt, message);
            targetMapper.updateProbeResult(target.getTenantId(), target.getId(), probedAt, STATUS_FAIL, message);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new ProbeScanResult(target.getId(), target.getPipelineKey(), target.getJobKey(),
                    STATUS_FAIL, RUNTIME_FAILED, message, heartbeat);
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param target target 参数，用于 writeHeartbeat 流程中的校验、计算或对象转换。
     * @param probe probe 参数，用于 writeHeartbeat 流程中的校验、计算或对象转换。
     * @param probedAt 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 writeHeartbeat 流程生成的业务结果。
     */
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

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param target target 参数，用于 writeFailedHeartbeat 流程中的校验、计算或对象转换。
     * @param probedAt 时间参数，用于计算窗口、过期或审计时间。
     * @param errorMessage error message 参数，用于 writeFailedHeartbeat 流程中的校验、计算或对象转换。
     * @return 返回 writeFailedHeartbeat 流程生成的业务结果。
     */
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

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param target target 参数，用于 probePayload 流程中的校验、计算或对象转换。
     * @param runtimeStatus 业务状态，用于筛选或推进状态流转。
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 probe payload 生成的文本或业务键。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param target target 参数，用于 toProbeTarget 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
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

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private ProbeTargetView toTarget(CdpWarehouseExternalRealtimeJobProbeTargetDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
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
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param targetId 业务对象 ID，用于定位具体记录。
     * @return 返回 requireTarget 流程生成的业务结果。
     */
    private CdpWarehouseExternalRealtimeJobProbeTargetDO requireTarget(Long tenantId, Long targetId) {
        CdpWarehouseExternalRealtimeJobProbeTargetDO row =
                targetMapper.findByTenantAndId(tenantId, requirePositive(targetId, "targetId"));
        if (row == null) {
            throw new IllegalArgumentException("probe target not found: " + targetId);
        }
        return row;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 engine type 生成的文本或业务键。
     */
    private String engineType(String value) {
        String engine = upper(required(value, "engineType"));
        if (!"FLINK_REST".equals(engine) && !"KAFKA_CONNECT".equals(engine)
                && !"DORIS_ROUTINE_LOAD".equals(engine) && !"GENERIC_HTTP".equals(engine)) {
            throw new IllegalArgumentException(
                    "engineType must be FLINK_REST, KAFKA_CONNECT, DORIS_ROUTINE_LOAD, or GENERIC_HTTP");
        }
        return engine;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 positive or default 计算得到的数量、金额或指标值。
     */
    private int positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        if (value == null) {
            return defaultValue;
        }
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(Integer value) {
        int limit = value == null || value <= 0 ? DEFAULT_LIMIT : value;
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 require positive 计算得到的数量、金额或指标值。
     */
    private Long requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回 enabled 的布尔判断结果。
     */
    private boolean enabled(CdpWarehouseExternalRealtimeJobProbeTargetDO row) {
        return row != null && !Integer.valueOf(0).equals(row.getEnabled());
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
     * 校验输入、权限或业务前置条件。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 required 生成的文本或业务键。
     */
    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 upper 生成的文本或业务键。
     */
    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @return 返回 upper default 生成的文本或业务键。
     */
    private String upperDefault(String value, String defaultValue) {
        String normalized = upper(value);
        return normalized.isBlank() ? defaultValue : normalized;
    }

    /**
     * 生成默认值或兜底结果，保证调用链稳定。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param fallback fallback 参数，用于 defaultString 流程中的校验、计算或对象转换。
     * @return 返回 default string 生成的文本或业务键。
     */
    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param target target 参数，用于 engineJobId 流程中的校验、计算或对象转换。
     * @return 返回 engine job id 生成的文本或业务键。
     */
    private String engineJobId(CdpWarehouseExternalRealtimeJobProbeTargetDO target) {
        return defaultString(target.getExternalJobId(), target.getConnectorName());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param rows rows 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private List<CdpWarehouseExternalRealtimeJobProbeTargetDO> safeList(
            List<CdpWarehouseExternalRealtimeJobProbeTargetDO> rows) {
        return rows == null ? List.of() : rows;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= MAX_MESSAGE_LENGTH ? value : value.substring(0, MAX_MESSAGE_LENGTH);
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
     * TargetCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ScanCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record ScanCommand(
            Long targetId,
            Integer limit) {
    }

    /**
     * ProbeTargetView 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ScanSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
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

    /**
     * ProbeScanResult 承载对应领域的业务规则、流程编排和结果转换。
     */
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
