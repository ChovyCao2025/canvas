package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
/**
 * CdpWarehouseAvailabilityService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseAvailabilityService {

    private static final int STATUS_LIMIT = 20;
    private static final String MODE_OFFLINE = "OFFLINE";
    private static final String MODE_REALTIME = "REALTIME";
    private static final String MODE_HYBRID = "HYBRID";
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String AGGREGATE_JOB = "CDP_EVENT_AGGREGATE";
    private static final String AGGREGATE_WATERMARK = "WINDOW_END";

    private final CdpWarehouseOperationsService operationsService;
    private final CdpWarehouseRealtimePipelineService realtimePipelineService;
    private final CdpWarehouseSloPolicyService sloPolicyService;

    /**
     * 初始化 CdpWarehouseAvailabilityService 实例。
     *
     * @param operationsService 依赖组件，用于完成数据访问或外部能力调用。
     * @param realtimePipelineService 时间参数，用于计算窗口、过期或审计时间。
     * @param sloPolicyService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseAvailabilityService(CdpWarehouseOperationsService operationsService,
                                           CdpWarehouseRealtimePipelineService realtimePipelineService,
                                           CdpWarehouseSloPolicyService sloPolicyService) {
        this.operationsService = operationsService;
        this.realtimePipelineService = realtimePipelineService;
        this.sloPolicyService = sloPolicyService;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param from 时间或范围边界，用于限定统计窗口。
     * @param to 时间或范围边界，用于限定统计窗口。
     * @param mode mode 参数，用于 evaluate 流程中的校验、计算或对象转换。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public AvailabilityDecision evaluate(Long tenantId,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         String mode) {
        // 准备本次处理所需的上下文和中间变量。
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime requestedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime requestedFrom = from == null ? requestedTo.minusHours(1) : from;
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (requestedFrom.isAfter(requestedTo)) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
        String normalizedMode = normalizeMode(mode);
        CdpWarehouseSloPolicyService.SloPolicyView policy = effectivePolicy(scopedTenantId);
        List<AvailabilityGate> gates = new ArrayList<>();
        if (MODE_OFFLINE.equals(normalizedMode) || MODE_HYBRID.equals(normalizedMode)) {
            gates.add(offlineGate(scopedTenantId, requestedTo, policy));
        }
        if (MODE_REALTIME.equals(normalizedMode) || MODE_HYBRID.equals(normalizedMode)) {
            gates.add(realtimeGate(scopedTenantId, requestedTo));
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new AvailabilityDecision(
                scopedTenantId,
                normalizedMode,
                requestedFrom,
                requestedTo,
                LocalDateTime.now(),
                overallStatus(gates),
                List.copyOf(gates));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestedTo 时间或范围边界，用于限定统计窗口。
     * @param policy policy 参数，用于 offlineGate 流程中的校验、计算或对象转换。
     * @return 返回 offlineGate 流程生成的业务结果。
     */
    private AvailabilityGate offlineGate(Long tenantId,
                                         LocalDateTime requestedTo,
                                         CdpWarehouseSloPolicyService.SloPolicyView policy) {
        try {
            CdpWarehouseOperationsService.WarehouseStatus status =
                    operationsService.status(tenantId, STATUS_LIMIT);
            List<CdpWarehouseOperationsService.WatermarkRow> watermarks =
                    status.watermarks() == null ? List.of() : status.watermarks();
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            CdpWarehouseOperationsService.WatermarkRow aggregate = watermarks.stream()
                    .filter(row -> AGGREGATE_JOB.equalsIgnoreCase(nullToEmpty(row.jobName()))
                            && AGGREGATE_WATERMARK.equalsIgnoreCase(nullToEmpty(row.watermarkType())))
                    .findFirst()
                    .orElse(null);
            LocalDateTime availableUntil = parseWatermarkTime(aggregate);
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (availableUntil == null) {
                return new AvailabilityGate(
                        "offline_aggregate",
                        STATUS_FAIL,
                        "aggregate WINDOW_END watermark is missing",
                        null,
                        null,
                        watermarks.size());
            }
            long lagMinutes = positiveMinutesBetween(availableUntil, requestedTo);
            if (!requestedTo.isAfter(availableUntil)) {
                return new AvailabilityGate(
                        "offline_aggregate",
                        STATUS_PASS,
                        "offline aggregate watermark covers requested window",
                        availableUntil,
                        0L,
                        1);
            }
            String gateStatus = lagMinutes >= policy.offlineFailWatermarkLagMinutes()
                    ? STATUS_FAIL
                    : STATUS_WARN;
            return new AvailabilityGate(
                    "offline_aggregate",
                    gateStatus,
                    "requested window extends " + lagMinutes
                            + "m past offline aggregate watermark",
                    availableUntil,
                    lagMinutes,
                    1);
        } catch (RuntimeException e) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new AvailabilityGate(
                    "offline_aggregate",
                    STATUS_FAIL,
                    "offline availability evaluation failed: " + e.getMessage(),
                    null,
                    null,
                    0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param requestedTo 时间或范围边界，用于限定统计窗口。
     * @return 返回 realtimeGate 流程生成的业务结果。
     */
    private AvailabilityGate realtimeGate(Long tenantId, LocalDateTime requestedTo) {
        try {
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary status =
                    realtimePipelineService.status(tenantId, 3);
            List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines =
                    status.pipelines() == null ? List.of() : status.pipelines();
            // 校验关键输入和前置条件，避免无效状态继续进入主流程。
            if (pipelines.isEmpty()) {
                return new AvailabilityGate(
                        "realtime_pipelines",
                        STATUS_FAIL,
                        "no active realtime pipeline evidence",
                        null,
                        null,
                        0);
            }
            if (status.failed() > 0) {
                return new AvailabilityGate(
                        "realtime_pipelines",
                        STATUS_FAIL,
                        status.failed() + " realtime pipeline(s) failed",
                        minWatermark(pipelines),
                        null,
                        pipelines.size());
            }
            // 遍历候选数据并按业务规则筛选、转换或聚合。
            if (pipelines.stream().anyMatch(pipeline -> pipeline.lastWatermarkTime() == null)) {
                return new AvailabilityGate(
                        "realtime_pipelines",
                        STATUS_FAIL,
                        "one or more realtime pipelines have no watermark",
                        minWatermark(pipelines),
                        null,
                        pipelines.size());
            }
            LocalDateTime availableUntil = minWatermark(pipelines);
            long lagMinutes = positiveMinutesBetween(availableUntil, requestedTo);
            long toleranceMs = minRealtimeToleranceMs(pipelines);
            if (requestedTo.isAfter(availableUntil)) {
                long lagMs = Math.max(0L, Duration.between(availableUntil, requestedTo).toMillis());
                String gateStatus = lagMs > toleranceMs ? STATUS_FAIL : STATUS_WARN;
                return new AvailabilityGate(
                        "realtime_pipelines",
                        gateStatus,
                        "requested window extends " + lagMinutes
                                + "m past realtime pipeline watermark",
                        availableUntil,
                        lagMinutes,
                        pipelines.size());
            }
            if (status.warned() > 0) {
                return new AvailabilityGate(
                        "realtime_pipelines",
                        STATUS_WARN,
                        status.warned() + " realtime pipeline(s) warning",
                        availableUntil,
                        0L,
                        pipelines.size());
            }
            return new AvailabilityGate(
                    "realtime_pipelines",
                    STATUS_PASS,
                    "realtime pipeline watermarks cover requested window",
                    availableUntil,
                    0L,
                    pipelines.size());
        } catch (RuntimeException e) {
            // 汇总前面计算出的状态和明细，返回给调用方。
            return new AvailabilityGate(
                    "realtime_pipelines",
                    STATUS_FAIL,
                    "realtime availability evaluation failed: " + e.getMessage(),
                    null,
                    null,
                    0);
        }
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @return 返回 effectivePolicy 流程生成的业务结果。
     */
    private CdpWarehouseSloPolicyService.SloPolicyView effectivePolicy(Long tenantId) {
        if (sloPolicyService == null) {
            return CdpWarehouseSloPolicyService.defaultPolicy(tenantId);
        }
        try {
            return sloPolicyService.effectivePolicy(tenantId);
        } catch (RuntimeException e) {
            return CdpWarehouseSloPolicyService.defaultPolicy(tenantId);
        }
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param row 持久化行数据，承载数据库记录内容。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private LocalDateTime parseWatermarkTime(CdpWarehouseOperationsService.WatermarkRow row) {
        if (row == null) {
            return null;
        }
        if (hasText(row.watermarkValue())) {
            try {
                return LocalDateTime.parse(row.watermarkValue().trim());
            } catch (RuntimeException ignored) {
                // Fall back to the typed watermark time below.
            }
        }
        return row.watermarkTime();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param pipelines pipelines 参数，用于 minWatermark 流程中的校验、计算或对象转换。
     * @return 返回 minWatermark 流程生成的业务结果。
     */
    private LocalDateTime minWatermark(List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return pipelines.stream()
                .map(CdpWarehouseRealtimePipelineService.PipelineRuntimeView::lastWatermarkTime)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param pipelines pipelines 参数，用于 minRealtimeToleranceMs 流程中的校验、计算或对象转换。
     * @return 返回 min realtime tolerance ms 计算得到的数量、金额或指标值。
     */
    private long minRealtimeToleranceMs(
            List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        return pipelines.stream()
                .map(CdpWarehouseRealtimePipelineService.PipelineRuntimeView::contract)
                .filter(contract -> contract != null && contract.maxLagMs() != null && contract.maxLagMs() > 0)
                .mapToLong(CdpWarehouseRealtimePipelineService.PipelineContractView::maxLagMs)
                .min()
                .orElse(600_000L);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param availableUntil available until 参数，用于 positiveMinutesBetween 流程中的校验、计算或对象转换。
     * @param requestedTo 时间或范围边界，用于限定统计窗口。
     * @return 返回 positive minutes between 计算得到的数量、金额或指标值。
     */
    private long positiveMinutesBetween(LocalDateTime availableUntil, LocalDateTime requestedTo) {
        if (availableUntil == null || requestedTo == null || !requestedTo.isAfter(availableUntil)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(availableUntil, requestedTo).toMinutes());
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param gates gates 参数，用于 overallStatus 流程中的校验、计算或对象转换。
     * @return 返回 overall status 生成的文本或业务键。
     */
    private String overallStatus(List<AvailabilityGate> gates) {
        if (gates.stream().anyMatch(gate -> STATUS_FAIL.equals(gate.status()))) {
            return STATUS_FAIL;
        }
        if (gates.stream().anyMatch(gate -> STATUS_WARN.equals(gate.status()))) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param mode mode 参数，用于 normalizeMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeMode(String mode) {
        String value = hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : MODE_HYBRID;
        if (!MODE_OFFLINE.equals(value) && !MODE_REALTIME.equals(value) && !MODE_HYBRID.equals(value)) {
            throw new IllegalArgumentException("mode must be OFFLINE, REALTIME, or HYBRID");
        }
        return value;
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 null to empty 生成的文本或业务键。
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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
     * AvailabilityDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AvailabilityDecision(
            Long tenantId,
            String mode,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime generatedAt,
            String status,
            List<AvailabilityGate> gates) {
    }

    /**
     * AvailabilityGate 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record AvailabilityGate(
            String gateKey,
            String status,
            String reason,
            LocalDateTime availableUntil,
            Long lagMinutes,
            int evidenceCount) {
    }
}
