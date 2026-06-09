package org.chovy.canvas.domain.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.dal.dataobject.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO;
import org.chovy.canvas.dal.mapper.CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService {

    private static final int DEFAULT_SCAN_LIMIT = 50;
    private static final int DEFAULT_AUDIENCE_LIMIT = 100;
    private static final int MAX_LIST_LIMIT = 100;
    private static final int MAX_ERROR_LENGTH = 1024;

    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    /**
     * 初始化 CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService 实例。
     *
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper,
            ObjectMapper objectMapper) {
        this(automationService, mapper, objectMapper, Clock.systemDefaultZone());
    }

    /**
     * 初始化 CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService 实例。
     *
     * @param automationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param mapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param clock 时间参数，用于计算窗口、过期或审计时间。
     */
    CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService automationService,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunMapper mapper,
            ObjectMapper objectMapper,
            Clock clock) {
        this.automationService = automationService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param triggerSource trigger source 参数，用于 runAndRecord 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    public AutomationRunView runAndRecord(
            Long tenantId,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command,
            String triggerSource) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand scopedCommand =
                command == null
                        ? new CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand(
                        null, null, null, null)
                        : command;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row =
                runningRow(scopedTenantId, scopedCommand, triggerSource);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        mapper.insert(row);
        try {
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result =
                    automationService.run(scopedTenantId, scopedCommand);
            row.setStatus(status(result == null ? null : result.status()));
            row.setScanned(result == null ? 0 : result.scanned());
            row.setEligible(result == null ? 0 : result.eligible());
            row.setTriggered(result == null ? 0 : result.triggered());
            row.setSkipped(result == null ? 0 : result.skipped());
            row.setFailed(result == null ? 0 : result.failed());
            row.setResultJson(toJson(result));
            row.setFinishedAt(now());
            mapper.updateById(row);
            // 汇总前面计算出的状态和明细，返回给调用方。
            return toView(row, result);
        } catch (RuntimeException e) {
            row.setStatus("FAIL");
            row.setErrorMessage(truncate(e.getMessage()));
            row.setFinishedAt(now());
            mapper.updateById(row);
            throw e;
        }
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public List<AutomationRunView> recent(Long tenantId, int limit) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        int scopedLimit = limit <= 0 ? 20 : Math.min(limit, MAX_LIST_LIMIT);
        List<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO> rows = mapper.selectList(
                new LambdaQueryWrapper<CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO>()
                        .eq(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO::getTenantId, scopedTenantId)
                        .orderByDesc(CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO::getStartedAt)
                        .last("LIMIT " + scopedLimit));
        return rows == null ? List.of() : rows.stream().map(row -> toView(row, null)).toList();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param id 业务对象 ID，用于定位具体记录。
     * @return 返回 get 流程生成的业务结果。
     */
    public AutomationRunView get(Long tenantId, Long id) {
        Long scopedTenantId = tenantId == null ? 0L : tenantId;
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row = id == null ? null : mapper.selectById(id);
        if (row == null || !scopedTenantId.equals(row.getTenantId())) {
            throw new IllegalArgumentException("privacy audience rebuild automation run not found");
        }
        return toView(row, null);
    }

    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @param triggerSource trigger source 参数，用于 runningRow 流程中的校验、计算或对象转换。
     * @return 返回流程执行后的业务结果。
     */
    private CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO runningRow(
            Long tenantId,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationCommand command,
            String triggerSource) {
        // 准备本次处理所需的上下文和中间变量。
        LocalDateTime now = now();
        CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row =
                new CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO();
        row.setTenantId(tenantId);
        row.setTriggerSource(source(triggerSource));
        row.setStatus("RUNNING");
        row.setActor(command.actor());
        row.setScanLimit(command.scanLimit() == null ? DEFAULT_SCAN_LIMIT : command.scanLimit());
        row.setAudienceLimit(command.audienceLimit() == null ? DEFAULT_AUDIENCE_LIMIT : command.audienceLimit());
        row.setRetryFailed(Boolean.TRUE.equals(command.retryFailed()) ? 1 : 0);
        row.setScanned(0);
        row.setEligible(0);
        row.setTriggered(0);
        row.setSkipped(0);
        row.setFailed(0);
        row.setStartedAt(now);
        row.setCreatedAt(now);
        // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
        row.setUpdatedAt(now);
        // 汇总前面计算出的状态和明细，返回给调用方。
        return row;
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @param result result 参数，用于 toView 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private AutomationRunView toView(
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunDO row,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AutomationRunView(
                row.getId(),
                row.getTenantId(),
                row.getTriggerSource(),
                row.getStatus(),
                row.getActor(),
                row.getScanLimit(),
                row.getAudienceLimit(),
                row.getRetryFailed() != null && row.getRetryFailed() == 1,
                value(row.getScanned()),
                value(row.getEligible()),
                value(row.getTriggered()),
                value(row.getSkipped()),
                value(row.getFailed()),
                row.getResultJson(),
                row.getErrorMessage(),
                row.getStartedAt(),
                row.getFinishedAt(),
                row.getCreatedAt(),
                // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                row.getUpdatedAt(),
                result);
    }

    /**
     * 组装输出结构或完成对象转换。
     *
     * @param result result 参数，用于 toJson 流程中的校验、计算或对象转换。
     * @return 返回组装或转换后的结果对象。
     */
    private String toJson(CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
        if (result == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize privacy audience rebuild automation result", e);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 value 计算得到的数量、金额或指标值。
     */
    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回 status 生成的文本或业务键。
     */
    private String status(String status) {
        return status == null || status.isBlank() ? "FAIL" : status.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param triggerSource trigger source 参数，用于 source 流程中的校验、计算或对象转换。
     * @return 返回 source 生成的文本或业务键。
     */
    private String source(String triggerSource) {
        return triggerSource == null || triggerSource.isBlank()
                ? "MANUAL"
                : triggerSource.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param message 原因或消息文本，用于记录状态变化的业务依据。
     * @return 返回 truncate 生成的文本或业务键。
     */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
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
     * AutomationRunView 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AutomationRunView(
            Long id,
            Long tenantId,
            String triggerSource,
            String status,
            String actor,
            Integer scanLimit,
            Integer audienceLimit,
            boolean retryFailed,
            int scanned,
            int eligible,
            int triggered,
            int skipped,
            int failed,
            String resultJson,
            String errorMessage,
            LocalDateTime startedAt,
            LocalDateTime finishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            CdpWarehousePrivacyAudienceBitmapRebuildAutomationService.AutomationResult result) {
    }
}
