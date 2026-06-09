package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
/**
 * CdpWarehouseRealtimeCutoverReadinessService 承载对应领域的业务规则、流程编排和结果转换。
 */
public class CdpWarehouseRealtimeCutoverReadinessService {

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WARN = "WARN";
    private static final String STATUS_FAIL = "FAIL";
    private static final String MODE_DIRECT_STREAM_LOAD = "DIRECT_STREAM_LOAD";
    private static final String MODE_FLINK_FIRST = "FLINK_FIRST";
    private static final String MODE_HYBRID = "HYBRID";
    private static final List<String> DEFAULT_REQUIRED_PIPELINES = List.of(
            "mysql_cdp_event_log_to_doris_ods",
            "mysql_canvas_trace_to_doris_ods",
            "doris_ods_cdp_event_to_dwd_fact",
            "doris_dwd_user_fact_to_dws_metric_daily");

    private final CdpWarehouseRealtimePipelineService pipelineService;
    private final CdpWarehouseE2eCertificationGateService certificationGateService;

    /**
     * 初始化 CdpWarehouseRealtimeCutoverReadinessService 实例。
     *
     * @param pipelineService 依赖组件，用于完成数据访问或外部能力调用。
     * @param certificationGateService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public CdpWarehouseRealtimeCutoverReadinessService(
            CdpWarehouseRealtimePipelineService pipelineService,
            CdpWarehouseE2eCertificationGateService certificationGateService) {
        this.pipelineService = pipelineService;
        this.certificationGateService = certificationGateService;
    }

    /**
     * 根据输入和依赖数据计算业务判断结果。
     *
     * @param tenantId 租户 ID，用于限定数据隔离范围。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 evaluate 流程生成的业务结果。
     */
    public CutoverDecision evaluate(Long tenantId, CutoverCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CutoverCommand scopedCommand = command == null
                ? new CutoverCommand(MODE_FLINK_FIRST, DEFAULT_REQUIRED_PIPELINES, List.of(), MODE_HYBRID, 60L)
                : command;
        String targetMode = normalizeTargetMode(scopedCommand.targetMode());
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (MODE_DIRECT_STREAM_LOAD.equals(targetMode)) {
            return new CutoverDecision(
                    scopedTenantId,
                    targetMode,
                    STATUS_PASS,
                    true,
                    "direct Doris Stream Load fallback remains allowed; no Flink-first cutover is approved",
                    List.of(new CutoverGate(
                            "direct_stream_load_fallback",
                            STATUS_PASS,
                            "direct Stream Load fallback remains available",
                            null,
                            null,
                            null)));
        }

        List<String> requiredPipelineKeys = requiredPipelineKeys(scopedCommand.pipelineKeys());
        List<CutoverGate> gates = new ArrayList<>();
        CdpWarehouseRealtimePipelineService.PipelineStatusSummary pipelineStatus =
                pipelineService.status(scopedTenantId, 20);
        Map<String, CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines =
                pipelinesByKey(pipelineStatus);
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (String pipelineKey : requiredPipelineKeys) {
            CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime = pipelines.get(pipelineKey);
            gates.add(pipelineGate(pipelineKey, runtime));
        }

        CdpWarehouseE2eCertificationGateService.GateDecision certification =
                certificationGateService.evaluate(
                        scopedTenantId,
                        defaultString(scopedCommand.certificationMode(), MODE_HYBRID),
                        safeList(scopedCommand.contractKeys()),
                        true,
                        true,
                        true,
                        maxAgeMinutes(scopedCommand.maxCertificationAgeMinutes()));
        gates.add(new CutoverGate(
                "e2e_certification",
                normalizeStatus(certification.status()),
                certification.reason(),
                certification.matchedRunId(),
                certification.matchedFinishedAt(),
                certification.expiresAt()));

        String status = worstStatus(gates.stream().map(CutoverGate::status).toList());
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CutoverDecision(
                scopedTenantId,
                targetMode,
                status,
                STATUS_PASS.equals(status),
                summary(targetMode, status, gates),
                List.copyOf(gates));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param pipelineKey 业务键，用于在同一租户下定位资源。
     * @param runtime 时间参数，用于计算窗口、过期或审计时间。
     * @return 返回 pipelineGate 流程生成的业务结果。
     */
    private CutoverGate pipelineGate(String pipelineKey,
                                     CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (runtime == null) {
            return new CutoverGate(
                    "pipeline:" + pipelineKey,
                    STATUS_FAIL,
                    "required realtime pipeline is missing: " + pipelineKey,
                    null,
                    null,
                    null);
        }
        String status = normalizeStatus(runtime.runtimeStatus());
        if (!STATUS_PASS.equals(status)) {
            return new CutoverGate(
                    "pipeline:" + pipelineKey,
                    status,
                    defaultString(runtime.message(), "realtime pipeline is not PASS"),
                    null,
                    runtime.lastCheckpointAt(),
                    null);
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return new CutoverGate(
                "pipeline:" + pipelineKey,
                STATUS_PASS,
                defaultString(runtime.message(), "realtime pipeline PASS"),
                null,
                runtime.lastCheckpointAt(),
                null);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param summary summary 参数，用于 pipelinesByKey 流程中的校验、计算或对象转换。
     * @return 返回 pipelinesByKey 流程生成的业务结果。
     */
    private Map<String, CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelinesByKey(
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary) {
        Map<String, CdpWarehouseRealtimePipelineService.PipelineRuntimeView> byKey = new LinkedHashMap<>();
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (summary == null || summary.pipelines() == null) {
            return byKey;
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        for (CdpWarehouseRealtimePipelineService.PipelineRuntimeView row : summary.pipelines()) {
            if (row != null && row.contract() != null && hasText(row.contract().pipelineKey())) {
                byKey.put(row.contract().pipelineKey().trim(), row);
            }
        }
        // 汇总前面计算出的状态和明细，返回给调用方。
        return byKey;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @param pipelineKeys pipeline keys 参数，用于 requiredPipelineKeys 流程中的校验、计算或对象转换。
     * @return 返回 required pipeline keys 汇总后的集合、分页或映射视图。
     */
    private List<String> requiredPipelineKeys(List<String> pipelineKeys) {
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> keys = safeList(pipelineKeys).stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return keys.isEmpty() ? DEFAULT_REQUIRED_PIPELINES : keys;
    }

    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param targetMode target mode 参数，用于 summary 流程中的校验、计算或对象转换。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param gates gates 参数，用于 summary 流程中的校验、计算或对象转换。
     * @return 返回 summary 生成的文本或业务键。
     */
    private String summary(String targetMode, String status, List<CutoverGate> gates) {
        // 校验关键输入和前置条件，避免无效状态继续进入主流程。
        if (STATUS_PASS.equals(status)) {
            return "realtime warehouse cutover " + targetMode + " PASS";
        }
        // 遍历候选数据并按业务规则筛选、转换或聚合。
        List<String> blockers = gates.stream()
                .filter(gate -> !STATUS_PASS.equals(gate.status()))
                .map(gate -> gate.key() + "=" + gate.status())
                .toList();
        // 汇总前面计算出的状态和明细，返回给调用方。
        return "realtime warehouse cutover " + targetMode + " " + status
                + ": " + String.join(", ", blockers);
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param statuses 待处理业务值，用于规则计算、转换或外部调用。
     * @return 返回 worst status 生成的文本或业务键。
     */
    private String worstStatus(List<String> statuses) {
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param status 业务状态，用于筛选或推进状态流转。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

    /**
     * 解析、归一化或保护输入值，生成安全可用的中间结果。
     *
     * @param targetMode target mode 参数，用于 normalizeTargetMode 流程中的校验、计算或对象转换。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private String normalizeTargetMode(String targetMode) {
        String value = defaultString(targetMode, MODE_FLINK_FIRST).toUpperCase(Locale.ROOT).replace('-', '_');
        if ("DIRECT_SINK".equals(value) || "STREAM_LOAD".equals(value)) {
            return MODE_DIRECT_STREAM_LOAD;
        }
        if (MODE_DIRECT_STREAM_LOAD.equals(value) || MODE_FLINK_FIRST.equals(value) || MODE_HYBRID.equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("targetMode must be DIRECT_STREAM_LOAD, HYBRID, or FLINK_FIRST");
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param value 待处理值，用于规则计算或转换。
     * @return 返回 max age minutes 计算得到的数量、金额或指标值。
     */
    private long maxAgeMinutes(Long value) {
        return value == null || value <= 0 ? 60L : value;
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
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param values values 参数，用于 safeList 流程中的校验、计算或对象转换。
     * @return 返回 safe list 汇总后的集合、分页或映射视图。
     */
    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
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
     * CutoverCommand 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CutoverCommand(
            String targetMode,
            List<String> pipelineKeys,
            List<String> contractKeys,
            String certificationMode,
            Long maxCertificationAgeMinutes) {
        public CutoverCommand {
            pipelineKeys = pipelineKeys == null ? List.of() : List.copyOf(pipelineKeys);
            contractKeys = contractKeys == null ? List.of() : List.copyOf(contractKeys);
        }
    }

    /**
     * CutoverDecision 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CutoverDecision(
            Long tenantId,
            String targetMode,
            String status,
            boolean allowed,
            String summary,
            List<CutoverGate> gates) {
        public CutoverDecision {
            gates = gates == null ? List.of() : List.copyOf(gates);
        }
    }

    /**
     * CutoverGate 承载对应领域的业务规则、流程编排和结果转换。
     */
    public record CutoverGate(
            String key,
            String status,
            String reason,
            Long evidenceId,
            LocalDateTime observedAt,
            LocalDateTime expiresAt) {
    }
}
