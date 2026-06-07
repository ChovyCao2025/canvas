package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
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

    public CdpWarehouseRealtimeCutoverReadinessService(
            CdpWarehouseRealtimePipelineService pipelineService,
            CdpWarehouseE2eCertificationGateService certificationGateService) {
        this.pipelineService = pipelineService;
        this.certificationGateService = certificationGateService;
    }

    public CutoverDecision evaluate(Long tenantId, CutoverCommand command) {
        Long scopedTenantId = normalizeTenant(tenantId);
        CutoverCommand scopedCommand = command == null
                ? new CutoverCommand(MODE_FLINK_FIRST, DEFAULT_REQUIRED_PIPELINES, List.of(), MODE_HYBRID, 60L)
                : command;
        String targetMode = normalizeTargetMode(scopedCommand.targetMode());
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
        return new CutoverDecision(
                scopedTenantId,
                targetMode,
                status,
                STATUS_PASS.equals(status),
                summary(targetMode, status, gates),
                List.copyOf(gates));
    }

    private CutoverGate pipelineGate(String pipelineKey,
                                     CdpWarehouseRealtimePipelineService.PipelineRuntimeView runtime) {
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
        return new CutoverGate(
                "pipeline:" + pipelineKey,
                STATUS_PASS,
                defaultString(runtime.message(), "realtime pipeline PASS"),
                null,
                runtime.lastCheckpointAt(),
                null);
    }

    private Map<String, CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelinesByKey(
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary summary) {
        Map<String, CdpWarehouseRealtimePipelineService.PipelineRuntimeView> byKey = new LinkedHashMap<>();
        if (summary == null || summary.pipelines() == null) {
            return byKey;
        }
        for (CdpWarehouseRealtimePipelineService.PipelineRuntimeView row : summary.pipelines()) {
            if (row != null && row.contract() != null && hasText(row.contract().pipelineKey())) {
                byKey.put(row.contract().pipelineKey().trim(), row);
            }
        }
        return byKey;
    }

    private List<String> requiredPipelineKeys(List<String> pipelineKeys) {
        List<String> keys = safeList(pipelineKeys).stream()
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        return keys.isEmpty() ? DEFAULT_REQUIRED_PIPELINES : keys;
    }

    private String summary(String targetMode, String status, List<CutoverGate> gates) {
        if (STATUS_PASS.equals(status)) {
            return "realtime warehouse cutover " + targetMode + " PASS";
        }
        List<String> blockers = gates.stream()
                .filter(gate -> !STATUS_PASS.equals(gate.status()))
                .map(gate -> gate.key() + "=" + gate.status())
                .toList();
        return "realtime warehouse cutover " + targetMode + " " + status
                + ": " + String.join(", ", blockers);
    }

    private String worstStatus(List<String> statuses) {
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_FAIL::equals)) {
            return STATUS_FAIL;
        }
        if (statuses.stream().map(this::normalizeStatus).anyMatch(STATUS_WARN::equals)) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String normalizeStatus(String status) {
        String value = status == null ? STATUS_FAIL : status.trim().toUpperCase(Locale.ROOT);
        if (STATUS_PASS.equals(value) || STATUS_WARN.equals(value) || STATUS_FAIL.equals(value)) {
            return value;
        }
        return STATUS_FAIL;
    }

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

    private long maxAgeMinutes(Long value) {
        return value == null || value <= 0 ? 60L : value;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String defaultString(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

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

    public record CutoverGate(
            String key,
            String status,
            String reason,
            Long evidenceId,
            LocalDateTime observedAt,
            LocalDateTime expiresAt) {
    }
}
