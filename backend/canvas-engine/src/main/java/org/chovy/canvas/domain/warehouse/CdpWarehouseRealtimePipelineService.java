package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamCheckpointDO;
import org.chovy.canvas.dal.dataobject.CdpWarehouseStreamPipelineDO;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamCheckpointMapper;
import org.chovy.canvas.dal.mapper.CdpWarehouseStreamPipelineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
/**
 * CdpWarehouseRealtimePipelineService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRealtimePipelineService {

    private static final int DEFAULT_CHECKPOINT_INTERVAL_SECONDS = 60;
    private static final int DEFAULT_MAX_CHECKPOINT_AGE_SECONDS = 300;
    private static final long DEFAULT_MAX_LAG_MS = 600_000L;
    private static final int MAX_STATUS_LIMIT = 20;
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String SEMANTICS_AT_LEAST_ONCE = "AT_LEAST_ONCE";
    private static final String SEMANTICS_EXACTLY_ONCE = "EXACTLY_ONCE";
    private static final String STARTUP_SOURCE_PARTITION = "job-startup";
    private static final String STARTUP_OFFSET = "submitted";
    private static final String STARTUP_EVIDENCE_REASON =
            "startup submission is not runtime checkpoint evidence";
    private static final String ASSET_TYPE_TABLE = "TABLE";
    private static final String AVAILABILITY_MODE_REALTIME = "REALTIME";
    private static final String EVIDENCE_SOURCE_REALTIME_CHECKPOINT = "REALTIME_CHECKPOINT";

    private final CdpWarehouseStreamPipelineMapper pipelineMapper;
    private final CdpWarehouseStreamCheckpointMapper checkpointMapper;
    private final ObjectProvider<CdpWarehouseIncidentService> incidentServiceProvider;
    private final ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaServiceProvider;
    private final ObjectProvider<CdpWarehouseConsumerAvailabilityService> consumerAvailabilityServiceProvider;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 CdpWarehouseRealtimePipelineService 实例。
     *
     * @param pipelineMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param incidentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimePipelineService(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                               CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                               ObjectProvider<CdpWarehouseIncidentService> incidentServiceProvider,
                                               ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaServiceProvider,
                                               ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                                       consumerAvailabilityServiceProvider) {
        this(pipelineMapper, checkpointMapper, Clock.systemDefaultZone(), incidentServiceProvider,
                schemaServiceProvider, consumerAvailabilityServiceProvider);
    }

    /**
     * 初始化 CdpWarehouseRealtimePipelineService 实例。
     *
     * @param pipelineMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehouseRealtimePipelineService(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                        Clock clock) {
        this(pipelineMapper, checkpointMapper, clock, null, null, null);
    }

    /**
     * 初始化 CdpWarehouseRealtimePipelineService 实例。
     *
     * @param pipelineMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    CdpWarehouseRealtimePipelineService(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                        Clock clock,
                                        ObjectProvider<CdpWarehouseIncidentService> incidentServiceProvider) {
        this(pipelineMapper, checkpointMapper, clock, incidentServiceProvider, null, null);
    }

    /**
     * 初始化 CdpWarehouseRealtimePipelineService 实例。
     *
     * @param pipelineMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    CdpWarehouseRealtimePipelineService(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                        Clock clock,
                                        ObjectProvider<CdpWarehouseIncidentService> incidentServiceProvider,
                                        ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaServiceProvider) {
        this(pipelineMapper, checkpointMapper, clock, incidentServiceProvider, schemaServiceProvider, null);
    }

    /**
     * 初始化 CdpWarehouseRealtimePipelineService 实例。
     *
     * @param pipelineMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param checkpointMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     * @param incidentServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param schemaServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     * @param consumerAvailabilityServiceProvider 依赖组件，用于完成数据访问或外部能力调用。
     */
    CdpWarehouseRealtimePipelineService(CdpWarehouseStreamPipelineMapper pipelineMapper,
                                        CdpWarehouseStreamCheckpointMapper checkpointMapper,
                                        Clock clock,
                                        ObjectProvider<CdpWarehouseIncidentService> incidentServiceProvider,
                                        ObjectProvider<CdpWarehouseRealtimeSchemaService> schemaServiceProvider,
                                        ObjectProvider<CdpWarehouseConsumerAvailabilityService>
                                                consumerAvailabilityServiceProvider) {
        this.pipelineMapper = pipelineMapper;
        this.checkpointMapper = checkpointMapper;
        this.clock = clock;
        this.incidentServiceProvider = incidentServiceProvider;
        this.schemaServiceProvider = schemaServiceProvider;
        this.consumerAvailabilityServiceProvider = consumerAvailabilityServiceProvider;
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public PipelineContractView upsertPipeline(Long tenantId, PipelineContractCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("pipeline command is required");
        }
        CdpWarehouseStreamPipelineDO row = new CdpWarehouseStreamPipelineDO();
        row.setTenantId(normalizeTenant(tenantId));
        row.setPipelineKey(required(command.pipelineKey(), "pipelineKey"));
        row.setDisplayName(defaultString(command.displayName(), row.getPipelineKey()));
        row.setSourceType(upperRequired(command.sourceType(), "sourceType"));
        row.setSourceRef(required(command.sourceRef(), "sourceRef"));
        row.setSourceTopic(blankToNull(command.sourceTopic()));
        row.setConsumerGroup(blankToNull(command.consumerGroup()));
        row.setProcessorType(upperRequired(command.processorType(), "processorType"));
        row.setSinkType(upperRequired(command.sinkType(), "sinkType"));
        row.setSinkRef(required(command.sinkRef(), "sinkRef"));
        row.setDeliverySemantics(semantics(command.deliverySemantics()));
        row.setCheckpointIntervalSeconds(positiveOrDefault(
                command.checkpointIntervalSeconds(), DEFAULT_CHECKPOINT_INTERVAL_SECONDS, "checkpointIntervalSeconds"));
        row.setMaxLagMs(positiveOrDefault(command.maxLagMs(), DEFAULT_MAX_LAG_MS, "maxLagMs"));
        row.setMaxCheckpointAgeSeconds(positiveOrDefault(
                command.maxCheckpointAgeSeconds(), DEFAULT_MAX_CHECKPOINT_AGE_SECONDS,
                "maxCheckpointAgeSeconds"));
        row.setLifecycleStatus(upperDefault(command.lifecycleStatus(), STATUS_ACTIVE));
        row.setOwnerName(blankToNull(command.ownerName()));
        row.setConfigJson(blankToNull(command.configJson()));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        pipelineMapper.upsert(row);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return toPipeline(row);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param lifecycleStatus 业务状态，用于筛选或推进状态流转。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<PipelineContractView> listPipelines(Long tenantId, String lifecycleStatus) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LambdaQueryWrapper<CdpWarehouseStreamPipelineDO> query =
                new LambdaQueryWrapper<CdpWarehouseStreamPipelineDO>()
                        .in(CdpWarehouseStreamPipelineDO::getTenantId, tenantScope(scopedTenantId))
                        .orderByAsc(CdpWarehouseStreamPipelineDO::getTenantId)
                        .orderByAsc(CdpWarehouseStreamPipelineDO::getPipelineKey);
        if (hasText(lifecycleStatus)) {
            query.eq(CdpWarehouseStreamPipelineDO::getLifecycleStatus,
                    lifecycleStatus.trim().toUpperCase(Locale.ROOT));
        }

        Map<String, PipelineContractView> byKey = new LinkedHashMap<>();
        for (CdpWarehouseStreamPipelineDO row : safeList(pipelineMapper.selectList(query))) {
            byKey.put(row.getPipelineKey(), toPipeline(row));
        }
        return new ArrayList<>(byKey.values());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 report checkpoint 计算得到的数量、金额或指标值。
     */
    public CheckpointReport reportCheckpoint(Long tenantId, CheckpointCommand command) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (command == null) {
            throw new IllegalArgumentException("checkpoint command is required");
        }
        Long scopedTenantId = normalizeTenant(tenantId);
        String pipelineKey = required(command.pipelineKey(), "pipelineKey");
        CdpWarehouseStreamPipelineDO pipeline = findPipeline(scopedTenantId, pipelineKey);
        LocalDateTime now = now();
        LocalDateTime checkpointTime = command.checkpointTime() == null ? now : command.checkpointTime();
        String reportedStatus = upperDefault(command.status(), STATUS_PASS);
        RuntimeEvaluation evaluation = withSchemaEvaluation(
                evaluate(pipeline, command, reportedStatus, checkpointTime, now),
                scopedTenantId,
                pipelineKey,
                command.sourceSchemaVersion(),
                command.sinkSchemaVersion());

        CdpWarehouseStreamCheckpointDO row = new CdpWarehouseStreamCheckpointDO();
        row.setTenantId(scopedTenantId);
        row.setPipelineKey(pipelineKey);
        row.setCheckpointId(defaultString(command.checkpointId(), ""));
        row.setSourcePartition(defaultString(command.sourcePartition(), ""));
        row.setSourceOffset(blankToNull(command.sourceOffset()));
        row.setCommittedOffset(blankToNull(command.committedOffset()));
        row.setWatermarkTime(command.watermarkTime());
        row.setCheckpointTime(checkpointTime);
        row.setLagMs(command.lagMs());
        row.setRowCount(command.rowCount() == null ? 0L : Math.max(0L, command.rowCount()));
        row.setStatus(evaluation.status());
        row.setErrorMessage(limit(evaluation.message()));
        row.setReportedBy(normalizeOperator(command.reportedBy()));
        row.setSourceSchemaVersion(blankToNull(command.sourceSchemaVersion()));
        row.setSinkSchemaVersion(blankToNull(command.sinkSchemaVersion()));
        row.setSchemaStatus(schemaStatus(command, evaluation));
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        checkpointMapper.upsert(row);

        CdpWarehouseStreamPipelineDO latest = new CdpWarehouseStreamPipelineDO();
        latest.setLastCheckpointId(row.getCheckpointId());
        latest.setLastSourceOffset(row.getSourceOffset());
        latest.setLastCommittedOffset(row.getCommittedOffset());
        latest.setLastWatermarkTime(row.getWatermarkTime());
        latest.setLastCheckpointAt(row.getCheckpointTime());
        latest.setLastLagMs(row.getLagMs());
        latest.setLastRuntimeStatus(row.getStatus());
        latest.setLastStatusMessage(row.getErrorMessage());
        latest.setLastReportedBy(row.getReportedBy());
        if (pipeline.getTenantId() != null && pipeline.getTenantId().equals(scopedTenantId)) {
            pipelineMapper.updateRuntime(pipeline.getTenantId(), pipeline.getPipelineKey(), latest, now);
        }

        CheckpointReport report = new CheckpointReport(
                row.getId(),
                scopedTenantId,
                pipelineKey,
                row.getCheckpointId(),
                row.getSourcePartition(),
                row.getSourceOffset(),
                row.getCommittedOffset(),
                row.getWatermarkTime(),
                row.getCheckpointTime(),
                row.getLagMs(),
                row.getRowCount(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getReportedBy(),
                row.getSourceSchemaVersion(),
                row.getSinkSchemaVersion(),
                row.getSchemaStatus(),
                evaluation.reasons());
        recordRealtimeAssetAvailability(pipeline, report);
        recordIncidentIfNeeded(pipeline, report);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return report;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param recentLimit recent limit 参数，用于 status 流程中的校验、计算或对象转换。
     * @return 返回 status 流程生成的业务结果。
     */
    public PipelineStatusSummary status(Long tenantId, int recentLimit) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        List<PipelineContractView> pipelines = listPipelines(scopedTenantId, STATUS_ACTIVE);
        int limit = boundLimit(recentLimit);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<PipelineRuntimeView> runtimeRows = pipelines.stream()
                .map(pipeline -> runtime(scopedTenantId, pipeline, limit))
                .toList();
        long passed = runtimeRows.stream().filter(row -> STATUS_PASS.equals(row.runtimeStatus())).count();
        long warned = runtimeRows.stream().filter(row -> STATUS_WARN.equals(row.runtimeStatus())).count();
        long failed = runtimeRows.stream().filter(row -> STATUS_FAIL.equals(row.runtimeStatus())).count();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PipelineStatusSummary(scopedTenantId, runtimeRows.size(), passed, warned, failed, runtimeRows);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipeline pipeline 参数，用于 runtime 流程中的校验、计算或对象转换。
     * @param recentLimit recent limit 参数，用于 runtime 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private PipelineRuntimeView runtime(Long tenantId, PipelineContractView pipeline, int recentLimit) {
        List<CheckpointReport> recent = recentCheckpoints(tenantId, pipeline.pipelineKey(), recentLimit);
        CheckpointReport latest = recent.isEmpty() ? null : recent.get(0);
        RuntimeEvaluation evaluation = latest == null ? evaluateStored(pipeline) : evaluateCheckpoint(pipeline, latest);
        return new PipelineRuntimeView(
                pipeline,
                evaluation.status(),
                evaluation.message(),
                evaluation.reasons(),
                latest == null ? pipeline.lastCheckpointId() : latest.checkpointId(),
                latest == null ? pipeline.lastSourceOffset() : latest.sourceOffset(),
                latest == null ? pipeline.lastCommittedOffset() : latest.committedOffset(),
                latest == null ? pipeline.lastWatermarkTime() : latest.watermarkTime(),
                latest == null ? pipeline.lastCheckpointAt() : latest.checkpointTime(),
                latest == null ? pipeline.lastLagMs() : latest.lagMs(),
                latest == null ? pipeline.lastReportedBy() : latest.reportedBy(),
                recent);
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    private List<CheckpointReport> recentCheckpoints(Long tenantId, String pipelineKey, int limit) {
        List<CdpWarehouseStreamCheckpointDO> rows = checkpointMapper.selectList(
                new LambdaQueryWrapper<CdpWarehouseStreamCheckpointDO>()
                        .eq(CdpWarehouseStreamCheckpointDO::getTenantId, tenantId)
                        .eq(CdpWarehouseStreamCheckpointDO::getPipelineKey, pipelineKey)
                        .orderByDesc(CdpWarehouseStreamCheckpointDO::getCheckpointTime)
                        .orderByDesc(CdpWarehouseStreamCheckpointDO::getId)
                        .last("LIMIT " + limit));
        return safeList(rows).stream().map(this::toCheckpoint).toList();
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @return 返回符合条件的数据列表或视图。
     */
    private CdpWarehouseStreamPipelineDO findPipeline(Long tenantId, String pipelineKey) {
        LambdaQueryWrapper<CdpWarehouseStreamPipelineDO> query =
                new LambdaQueryWrapper<CdpWarehouseStreamPipelineDO>()
                        .in(CdpWarehouseStreamPipelineDO::getTenantId, tenantScope(tenantId))
                        .eq(CdpWarehouseStreamPipelineDO::getPipelineKey, pipelineKey)
                        .orderByAsc(CdpWarehouseStreamPipelineDO::getTenantId);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        CdpWarehouseStreamPipelineDO selected = null;
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseStreamPipelineDO row : safeList(pipelineMapper.selectList(query))) {
            selected = row;
        }
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (selected == null) {
            throw new IllegalArgumentException("stream pipeline not found: " + pipelineKey);
        }
        return selected;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param pipeline pipeline 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param reportedStatus 业务状态，用于筛选或推进状态流转。
     * @param checkpointTime 时间参数，用于计算窗口、过期或审计时间。
     * @param now 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    private RuntimeEvaluation evaluate(CdpWarehouseStreamPipelineDO pipeline,
                                       CheckpointCommand command,
                                       String reportedStatus,
                                       LocalDateTime checkpointTime,
                                       LocalDateTime now) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> reasons = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (isFail(reportedStatus)) {
            String detail = hasText(command.errorMessage()) ? ": " + command.errorMessage().trim() : "";
            reasons.add("reported status is " + reportedStatus + detail);
        }
        if (isStartupSubmission(command.checkpointId(), command.sourcePartition(),
                command.sourceOffset(), command.committedOffset())) {
            reasons.add(STARTUP_EVIDENCE_REASON);
        }
        if (SEMANTICS_EXACTLY_ONCE.equalsIgnoreCase(pipeline.getDeliverySemantics())) {
            if (!hasText(command.checkpointId())) {
                reasons.add("exactly-once checkpoint id is missing");
            }
            if (!hasText(command.committedOffset())) {
                reasons.add("exactly-once committed offset is missing");
            }
        }
        if (command.lagMs() != null && pipeline.getMaxLagMs() != null
                && command.lagMs() > pipeline.getMaxLagMs()) {
            reasons.add("lag " + command.lagMs() + "ms exceeds maxLagMs " + pipeline.getMaxLagMs());
        }
        if (pipeline.getMaxCheckpointAgeSeconds() != null) {
            long ageSeconds = Duration.between(checkpointTime, now).getSeconds();
            if (ageSeconds > pipeline.getMaxCheckpointAgeSeconds()) {
                reasons.add("checkpoint age " + ageSeconds + "s exceeds maxCheckpointAgeSeconds "
                        + pipeline.getMaxCheckpointAgeSeconds());
            }
        }
        String status = isFail(reportedStatus) ? STATUS_FAIL : (reasons.isEmpty() ? STATUS_PASS : STATUS_WARN);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RuntimeEvaluation(status, reasons.isEmpty() ? "Realtime pipeline healthy" : String.join("; ", reasons),
                List.copyOf(reasons));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param pipeline pipeline 参数，用于 evaluateStored 流程中的校验、计算或对象转换。
     * @return 返回 evaluateStored 流程生成的业务结果。
     */
    private RuntimeEvaluation evaluateStored(PipelineContractView pipeline) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> reasons = new ArrayList<>();
        String storedStatus = upperDefault(pipeline.lastRuntimeStatus(), "");
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (STATUS_FAIL.equals(storedStatus)) {
            reasons.add(defaultString(pipeline.lastStatusMessage(), "last runtime status is FAIL"));
            return new RuntimeEvaluation(STATUS_FAIL, String.join("; ", reasons), List.copyOf(reasons));
        }
        if (pipeline.lastCheckpointAt() == null) {
            reasons.add("checkpoint has not been reported");
        } else if (pipeline.maxCheckpointAgeSeconds() != null) {
            long ageSeconds = Duration.between(pipeline.lastCheckpointAt(), now()).getSeconds();
            if (ageSeconds > pipeline.maxCheckpointAgeSeconds()) {
                reasons.add("checkpoint age " + ageSeconds + "s exceeds maxCheckpointAgeSeconds "
                        + pipeline.maxCheckpointAgeSeconds());
            }
        }
        if (pipeline.lastLagMs() != null && pipeline.maxLagMs() != null
                && pipeline.lastLagMs() > pipeline.maxLagMs()) {
            reasons.add("lag " + pipeline.lastLagMs() + "ms exceeds maxLagMs " + pipeline.maxLagMs());
        }
        if (SEMANTICS_EXACTLY_ONCE.equalsIgnoreCase(pipeline.deliverySemantics())) {
            if (!hasText(pipeline.lastCheckpointId())) {
                reasons.add("exactly-once checkpoint id is missing");
            }
            if (!hasText(pipeline.lastCommittedOffset())) {
                reasons.add("exactly-once committed offset is missing");
            }
        }
        if (isStartupSubmission(pipeline.lastCheckpointId(), null,
                pipeline.lastSourceOffset(), pipeline.lastCommittedOffset())) {
            reasons.add(STARTUP_EVIDENCE_REASON);
        }
        String status = reasons.isEmpty() ? STATUS_PASS : STATUS_WARN;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RuntimeEvaluation(status, reasons.isEmpty() ? "Realtime pipeline healthy" : String.join("; ", reasons),
                List.copyOf(reasons));
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param pipeline pipeline 参数，用于 evaluateCheckpoint 流程中的校验、计算或对象转换。
     * @param checkpoint checkpoint 参数，用于 evaluateCheckpoint 流程中的校验、计算或对象转换。
     * @return 返回 evaluateCheckpoint 流程生成的业务结果。
     */
    private RuntimeEvaluation evaluateCheckpoint(PipelineContractView pipeline, CheckpointReport checkpoint) {
        // 准备本次处理所需的上下文和中间变量。
        List<String> reasons = new ArrayList<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (STATUS_FAIL.equals(checkpoint.status())) {
            reasons.add(defaultString(checkpoint.message(), "last checkpoint failed"));
            return new RuntimeEvaluation(STATUS_FAIL, String.join("; ", reasons), List.copyOf(reasons));
        }
        if (isStartupSubmission(checkpoint.checkpointId(), checkpoint.sourcePartition(),
                checkpoint.sourceOffset(), checkpoint.committedOffset())) {
            reasons.add(STARTUP_EVIDENCE_REASON);
        }
        if (pipeline.maxCheckpointAgeSeconds() != null && checkpoint.checkpointTime() != null) {
            long ageSeconds = Duration.between(checkpoint.checkpointTime(), now()).getSeconds();
            if (ageSeconds > pipeline.maxCheckpointAgeSeconds()) {
                reasons.add("checkpoint age " + ageSeconds + "s exceeds maxCheckpointAgeSeconds "
                        + pipeline.maxCheckpointAgeSeconds());
            }
        }
        if (checkpoint.lagMs() != null && pipeline.maxLagMs() != null
                && checkpoint.lagMs() > pipeline.maxLagMs()) {
            reasons.add("lag " + checkpoint.lagMs() + "ms exceeds maxLagMs " + pipeline.maxLagMs());
        }
        if (SEMANTICS_EXACTLY_ONCE.equalsIgnoreCase(pipeline.deliverySemantics())) {
            if (!hasText(checkpoint.checkpointId())) {
                reasons.add("exactly-once checkpoint id is missing");
            }
            if (!hasText(checkpoint.committedOffset())) {
                reasons.add("exactly-once committed offset is missing");
            }
        }
        if (STATUS_WARN.equals(checkpoint.status()) && hasText(checkpoint.message())) {
            reasons.add(checkpoint.message());
        }
        String status = reasons.isEmpty() ? STATUS_PASS : STATUS_WARN;
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new RuntimeEvaluation(status, reasons.isEmpty() ? "Realtime pipeline healthy" : String.join("; ", reasons),
                List.copyOf(reasons));
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param checkpointId 业务对象 ID，用于定位具体记录。
     * @param sourcePartition source partition 参数，用于 isStartupSubmission 流程中的校验、计算或对象转换。
     * @param sourceOffset source offset 参数，用于 isStartupSubmission 流程中的校验、计算或对象转换。
     * @param committedOffset committed offset 参数，用于 isStartupSubmission 流程中的校验、计算或对象转换。
     * @return 返回布尔判断结果。
     */
    private boolean isStartupSubmission(String checkpointId,
                                        String sourcePartition,
                                        String sourceOffset,
                                        String committedOffset) {
        return defaultString(checkpointId, "").endsWith("-startup")
                || STARTUP_SOURCE_PARTITION.equalsIgnoreCase(defaultString(sourcePartition, ""))
                || (STARTUP_OFFSET.equalsIgnoreCase(defaultString(sourceOffset, ""))
                && STARTUP_OFFSET.equalsIgnoreCase(defaultString(committedOffset, "")));
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private CheckpointReport toCheckpoint(CdpWarehouseStreamCheckpointDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CheckpointReport(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getCheckpointId(),
                row.getSourcePartition(),
                row.getSourceOffset(),
                row.getCommittedOffset(),
                row.getWatermarkTime(),
                row.getCheckpointTime(),
                row.getLagMs(),
                row.getRowCount(),
                row.getStatus(),
                row.getErrorMessage(),
                row.getReportedBy(),
                row.getSourceSchemaVersion(),
                row.getSinkSchemaVersion(),
                row.getSchemaStatus(),
                List.of());
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param pipeline pipeline 参数，用于 recordIncidentIfNeeded 流程中的校验、计算或对象转换。
     * @param report report 参数，用于 recordIncidentIfNeeded 流程中的校验、计算或对象转换。
     */
    private void recordIncidentIfNeeded(CdpWarehouseStreamPipelineDO pipeline, CheckpointReport report) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (report == null || STATUS_PASS.equals(report.status()) || incidentServiceProvider == null) {
            return;
        }
        CdpWarehouseIncidentService incidentService = incidentServiceProvider.getIfAvailable();
        if (incidentService == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        try {
            incidentService.recordRealtimePipelineIncident(new CdpWarehouseIncidentService.RealtimePipelineIncidentInput(
                    report.tenantId(),
                    pipeline.getId(),
                    report.pipelineKey(),
                    pipeline.getSinkRef(),
                    report.status(),
                    report.message(),
                    report.checkpointId(),
                    report.checkpointTime(),
                    report.lagMs(),
                    report.reasons()));
        } catch (RuntimeException e) {
            log.warn("[WAREHOUSE_REALTIME_PIPELINE] incident side effect failed tenantId={} pipelineKey={}: {}",
                    report.tenantId(), report.pipelineKey(), e.getMessage());
        }
    }

    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param pipeline pipeline 参数，用于 recordRealtimeAssetAvailability 流程中的校验、计算或对象转换。
     * @param report report 参数，用于 recordRealtimeAssetAvailability 流程中的校验、计算或对象转换。
     */
    private void recordRealtimeAssetAvailability(CdpWarehouseStreamPipelineDO pipeline, CheckpointReport report) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (report == null || pipeline == null || consumerAvailabilityServiceProvider == null) {
            return;
        }
        CdpWarehouseConsumerAvailabilityService service = consumerAvailabilityServiceProvider.getIfAvailable();
        if (service == null) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return;
        }
        try {
            LocalDateTime availableUntil = report.watermarkTime() == null
                    ? report.checkpointTime()
                    : report.watermarkTime();
            String status = report.watermarkTime() == null ? STATUS_FAIL : report.status();
            String reason = report.watermarkTime() == null
                    ? "checkpoint watermark is missing"
                    : defaultString(report.message(), "realtime checkpoint reported " + status);
            service.recordAssetAvailability(report.tenantId(),
                    new CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand(
                            ASSET_TYPE_TABLE,
                            pipeline.getSinkRef(),
                            AVAILABILITY_MODE_REALTIME,
                            null,
                            report.watermarkTime(),
                            availableUntil,
                            status,
                            EVIDENCE_SOURCE_REALTIME_CHECKPOINT,
                            "checkpoint:" + defaultString(report.checkpointId(), "unknown"),
                            reason,
                            report.checkpointTime()));
        } catch (RuntimeException e) {
            log.warn("[WAREHOUSE_REALTIME_PIPELINE] asset availability side effect failed tenantId={} pipelineKey={}: {}",
                    report.tenantId(), report.pipelineKey(), e.getMessage());
        }
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回组装或转换后的结果对象。
     */
    private PipelineContractView toPipeline(CdpWarehouseStreamPipelineDO row) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new PipelineContractView(
                row.getId(),
                row.getTenantId(),
                row.getPipelineKey(),
                row.getDisplayName(),
                row.getSourceType(),
                row.getSourceRef(),
                row.getSourceTopic(),
                row.getConsumerGroup(),
                row.getProcessorType(),
                row.getSinkType(),
                row.getSinkRef(),
                row.getDeliverySemantics(),
                row.getCheckpointIntervalSeconds(),
                row.getMaxLagMs(),
                row.getMaxCheckpointAgeSeconds(),
                row.getLifecycleStatus(),
                row.getOwnerName(),
                row.getConfigJson(),
                row.getLastCheckpointId(),
                row.getLastSourceOffset(),
                row.getLastCommittedOffset(),
                row.getLastWatermarkTime(),
                row.getLastCheckpointAt(),
                row.getLastLagMs(),
                row.getLastRuntimeStatus(),
                row.getLastStatusMessage(),
                row.getLastReportedBy());
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回布尔判断结果。
     */
    private boolean isFail(String status) {
        String normalized = upperDefault(status, STATUS_PASS);
        return STATUS_FAIL.equals(normalized) || "FAILED".equals(normalized) || "ERROR".equals(normalized);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param evaluation evaluation 参数，用于 withSchemaEvaluation 流程中的校验、计算或对象转换。
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param sourceSchemaVersion source schema version 参数，用于 withSchemaEvaluation 流程中的校验、计算或对象转换。
     * @param sinkSchemaVersion sink schema version 参数，用于 withSchemaEvaluation 流程中的校验、计算或对象转换。
     * @return 返回 withSchemaEvaluation 流程生成的业务结果。
     */
    private RuntimeEvaluation withSchemaEvaluation(RuntimeEvaluation evaluation,
                                                   Long tenantId,
                                                   String pipelineKey,
                                                   String sourceSchemaVersion,
                                                   String sinkSchemaVersion) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (!hasText(sourceSchemaVersion) && !hasText(sinkSchemaVersion)) {
            return evaluation;
        }
        CdpWarehouseRealtimeSchemaService schemaService =
                schemaServiceProvider == null ? null : schemaServiceProvider.getIfAvailable();
        if (schemaService == null) {
            List<String> reasons = new ArrayList<>(evaluation.reasons());
            reasons.add("schema guard is not configured");
            return mergeEvaluation(evaluation, STATUS_WARN, reasons);
        }
        CdpWarehouseRealtimeSchemaService.SchemaCheckpointEvaluation schema =
                schemaService.evaluateCheckpoint(tenantId, pipelineKey, sourceSchemaVersion, sinkSchemaVersion);
        if (STATUS_PASS.equals(schema.status()) && schema.reasons().isEmpty()) {
            return evaluation;
        }
        List<String> reasons = new ArrayList<>(evaluation.reasons());
        reasons.addAll(schema.reasons());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return mergeEvaluation(evaluation, schema.status(), reasons);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param current current 参数，用于 mergeEvaluation 流程中的校验、计算或对象转换。
     * @param nextStatus 业务状态，用于筛选或推进状态流转。
     * @param reasons reasons 参数，用于 mergeEvaluation 流程中的校验、计算或对象转换。
     * @return 返回 mergeEvaluation 流程生成的业务结果。
     */
    private RuntimeEvaluation mergeEvaluation(RuntimeEvaluation current, String nextStatus, List<String> reasons) {
        String mergedStatus;
        if (STATUS_FAIL.equals(current.status()) || STATUS_FAIL.equals(nextStatus)) {
            mergedStatus = STATUS_FAIL;
        } else if (STATUS_WARN.equals(current.status()) || STATUS_WARN.equals(nextStatus)) {
            mergedStatus = STATUS_WARN;
        } else {
            mergedStatus = STATUS_PASS;
        }
        return new RuntimeEvaluation(
                mergedStatus,
                reasons.isEmpty() ? "Realtime pipeline healthy" : String.join("; ", reasons),
                List.copyOf(reasons));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param evaluation evaluation 参数，用于 schemaStatus 流程中的校验、计算或对象转换。
     * @return 返回 schema status 生成的文本或业务键。
     */
    private String schemaStatus(CheckpointCommand command, RuntimeEvaluation evaluation) {
        if (!hasText(command.sourceSchemaVersion()) && !hasText(command.sinkSchemaVersion())) {
            return null;
        }
        if (evaluation.reasons().stream().anyMatch(reason -> reason.contains("schema"))) {
            return evaluation.status();
        }
        return STATUS_PASS;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 semantics 生成的文本或业务键。
     */
    private String semantics(String value) {
        String normalized = upperDefault(value, SEMANTICS_AT_LEAST_ONCE);
        if (!SEMANTICS_AT_LEAST_ONCE.equals(normalized) && !SEMANTICS_EXACTLY_ONCE.equals(normalized)) {
            throw new IllegalArgumentException("deliverySemantics must be AT_LEAST_ONCE or EXACTLY_ONCE");
        }
        return normalized;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 positive or default 计算得到的数量、金额或指标值。
     */
    private Integer positiveOrDefault(Integer value, int defaultValue, String fieldName) {
        int resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return resolved;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @param defaultValue 待处理值，用于规则计算或转换。
     * @param fieldName 名称文本，用于展示或唯一性校验。
     * @return 返回 positive or default 计算得到的数量、金额或指标值。
     */
    private Long positiveOrDefault(Long value, long defaultValue, String fieldName) {
        long resolved = value == null ? defaultValue : value;
        if (resolved <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return resolved;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, MAX_STATUS_LIMIT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 tenant scope 汇总后的集合、分页或映射视图。
     */
    private List<Long> tenantScope(Long tenantId) {
        if (tenantId == null || tenantId == 0L) {
            return List.of(0L);
        }
        return List.of(0L, tenantId);
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
     * @param operator 操作人标识，用于审计和权限判断。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeOperator(String operator) {
        return hasText(operator) ? operator.trim() : "operator";
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
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String limit(String value) {
        if (value == null || value.length() <= MAX_MESSAGE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_MESSAGE_LENGTH);
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
     * RuntimeEvaluation 承载对应领域的业务规则、流程编排和结果转换。
     */
    private record RuntimeEvaluation(String status, String message, List<String> reasons) {
    }

    /**
     * PipelineContractCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PipelineContractCommand(
            String pipelineKey,
            String displayName,
            String sourceType,
            String sourceRef,
            String sourceTopic,
            String consumerGroup,
            String processorType,
            String sinkType,
            String sinkRef,
            String deliverySemantics,
            Integer checkpointIntervalSeconds,
            Long maxLagMs,
            Integer maxCheckpointAgeSeconds,
            String lifecycleStatus,
            String ownerName,
            String configJson) {
    }

    /**
     * CheckpointCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CheckpointCommand(
            String pipelineKey,
            String checkpointId,
            String sourcePartition,
            String sourceOffset,
            String committedOffset,
            LocalDateTime watermarkTime,
            LocalDateTime checkpointTime,
            Long lagMs,
            Long rowCount,
            String status,
            String errorMessage,
            String reportedBy,
            String sourceSchemaVersion,
            String sinkSchemaVersion) {

        /**
         * 初始化 CheckpointCommand 实例。
         *
         * @param pipelineKey 业务键，用于在同一租户下定位资源。
         * @param checkpointId 业务对象 ID，用于定位具体记录。
         * @param sourcePartition source partition 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param sourceOffset source offset 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param committedOffset committed offset 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param watermarkTime 时间参数，用于计算窗口、过期或审计时间。
         * @param checkpointTime 时间参数，用于计算窗口、过期或审计时间。
         * @param lagMs lag ms 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param rowCount row count 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param status 业务状态，用于筛选或推进状态流转。
         * @param errorMessage error message 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         * @param reportedBy reported by 参数，用于 CheckpointCommand 流程中的校验、计算或对象转换。
         */
        public CheckpointCommand(String pipelineKey,
                                 String checkpointId,
                                 String sourcePartition,
                                 String sourceOffset,
                                 String committedOffset,
                                 LocalDateTime watermarkTime,
                                 LocalDateTime checkpointTime,
                                 Long lagMs,
                                 Long rowCount,
                                 String status,
                                 String errorMessage,
                                 String reportedBy) {
            this(pipelineKey, checkpointId, sourcePartition, sourceOffset, committedOffset,
                    watermarkTime, checkpointTime, lagMs, rowCount, status, errorMessage, reportedBy, null, null);
        }
    }

    /**
     * PipelineContractView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PipelineContractView(
            Long id,
            Long tenantId,
            String pipelineKey,
            String displayName,
            String sourceType,
            String sourceRef,
            String sourceTopic,
            String consumerGroup,
            String processorType,
            String sinkType,
            String sinkRef,
            String deliverySemantics,
            Integer checkpointIntervalSeconds,
            Long maxLagMs,
            Integer maxCheckpointAgeSeconds,
            String lifecycleStatus,
            String ownerName,
            String configJson,
            String lastCheckpointId,
            String lastSourceOffset,
            String lastCommittedOffset,
            LocalDateTime lastWatermarkTime,
            LocalDateTime lastCheckpointAt,
            Long lastLagMs,
            String lastRuntimeStatus,
            String lastStatusMessage,
            String lastReportedBy) {
    }

    /**
     * CheckpointReport 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CheckpointReport(
            Long id,
            Long tenantId,
            String pipelineKey,
            String checkpointId,
            String sourcePartition,
            String sourceOffset,
            String committedOffset,
            LocalDateTime watermarkTime,
            LocalDateTime checkpointTime,
            Long lagMs,
            Long rowCount,
            String status,
            String message,
            String reportedBy,
            String sourceSchemaVersion,
            String sinkSchemaVersion,
            String schemaStatus,
            List<String> reasons) {

        /**
         * 初始化 CheckpointReport 实例。
         *
         * @param id 业务对象 ID，用于定位具体记录。
         * @param tenantId 租户 ID，用于限定数据隔离范围。
         * @param pipelineKey 业务键，用于在同一租户下定位资源。
         * @param checkpointId 业务对象 ID，用于定位具体记录。
         * @param sourcePartition source partition 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param sourceOffset source offset 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param committedOffset committed offset 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param watermarkTime 时间参数，用于计算窗口、过期或审计时间。
         * @param checkpointTime 时间参数，用于计算窗口、过期或审计时间。
         * @param lagMs lag ms 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param rowCount row count 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param status 业务状态，用于筛选或推进状态流转。
         * @param message 原因或消息文本，用于记录状态变化的业务依据。
         * @param reportedBy reported by 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         * @param reasons reasons 参数，用于 CheckpointReport 流程中的校验、计算或对象转换。
         */
        public CheckpointReport(Long id,
                                Long tenantId,
                                String pipelineKey,
                                String checkpointId,
                                String sourcePartition,
                                String sourceOffset,
                                String committedOffset,
                                LocalDateTime watermarkTime,
                                LocalDateTime checkpointTime,
                                Long lagMs,
                                Long rowCount,
                                String status,
                                String message,
                                String reportedBy,
                                List<String> reasons) {
            this(id, tenantId, pipelineKey, checkpointId, sourcePartition, sourceOffset, committedOffset,
                    watermarkTime, checkpointTime, lagMs, rowCount, status, message, reportedBy,
                    null, null, null, reasons);
        }
    }

    /**
     * PipelineRuntimeView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PipelineRuntimeView(
            PipelineContractView contract,
            String runtimeStatus,
            String message,
            List<String> reasons,
            String lastCheckpointId,
            String lastSourceOffset,
            String lastCommittedOffset,
            LocalDateTime lastWatermarkTime,
            LocalDateTime lastCheckpointAt,
            Long lastLagMs,
            String lastReportedBy,
            List<CheckpointReport> recentCheckpoints) {
    }

    /**
     * PipelineStatusSummary 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record PipelineStatusSummary(
            Long tenantId,
            int total,
            long passed,
            long warned,
            long failed,
            List<PipelineRuntimeView> pipelines) {
    }
}
