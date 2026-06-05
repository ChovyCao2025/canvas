package org.chovy.canvas.domain.warehouse;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
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

    public CdpWarehouseAvailabilityService(CdpWarehouseOperationsService operationsService,
                                           CdpWarehouseRealtimePipelineService realtimePipelineService,
                                           CdpWarehouseSloPolicyService sloPolicyService) {
        this.operationsService = operationsService;
        this.realtimePipelineService = realtimePipelineService;
        this.sloPolicyService = sloPolicyService;
    }

    public AvailabilityDecision evaluate(Long tenantId,
                                         LocalDateTime from,
                                         LocalDateTime to,
                                         String mode) {
        Long scopedTenantId = normalizeTenant(tenantId);
        LocalDateTime requestedTo = to == null ? LocalDateTime.now() : to;
        LocalDateTime requestedFrom = from == null ? requestedTo.minusHours(1) : from;
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
        return new AvailabilityDecision(
                scopedTenantId,
                normalizedMode,
                requestedFrom,
                requestedTo,
                LocalDateTime.now(),
                overallStatus(gates),
                List.copyOf(gates));
    }

    private AvailabilityGate offlineGate(Long tenantId,
                                         LocalDateTime requestedTo,
                                         CdpWarehouseSloPolicyService.SloPolicyView policy) {
        try {
            CdpWarehouseOperationsService.WarehouseStatus status =
                    operationsService.status(tenantId, STATUS_LIMIT);
            List<CdpWarehouseOperationsService.WatermarkRow> watermarks =
                    status.watermarks() == null ? List.of() : status.watermarks();
            CdpWarehouseOperationsService.WatermarkRow aggregate = watermarks.stream()
                    .filter(row -> AGGREGATE_JOB.equalsIgnoreCase(nullToEmpty(row.jobName()))
                            && AGGREGATE_WATERMARK.equalsIgnoreCase(nullToEmpty(row.watermarkType())))
                    .findFirst()
                    .orElse(null);
            LocalDateTime availableUntil = parseWatermarkTime(aggregate);
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
            return new AvailabilityGate(
                    "offline_aggregate",
                    STATUS_FAIL,
                    "offline availability evaluation failed: " + e.getMessage(),
                    null,
                    null,
                    0);
        }
    }

    private AvailabilityGate realtimeGate(Long tenantId, LocalDateTime requestedTo) {
        try {
            CdpWarehouseRealtimePipelineService.PipelineStatusSummary status =
                    realtimePipelineService.status(tenantId, 3);
            List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines =
                    status.pipelines() == null ? List.of() : status.pipelines();
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
            return new AvailabilityGate(
                    "realtime_pipelines",
                    STATUS_FAIL,
                    "realtime availability evaluation failed: " + e.getMessage(),
                    null,
                    null,
                    0);
        }
    }

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

    private LocalDateTime minWatermark(List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines) {
        return pipelines.stream()
                .map(CdpWarehouseRealtimePipelineService.PipelineRuntimeView::lastWatermarkTime)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private long minRealtimeToleranceMs(
            List<CdpWarehouseRealtimePipelineService.PipelineRuntimeView> pipelines) {
        return pipelines.stream()
                .map(CdpWarehouseRealtimePipelineService.PipelineRuntimeView::contract)
                .filter(contract -> contract != null && contract.maxLagMs() != null && contract.maxLagMs() > 0)
                .mapToLong(CdpWarehouseRealtimePipelineService.PipelineContractView::maxLagMs)
                .min()
                .orElse(600_000L);
    }

    private long positiveMinutesBetween(LocalDateTime availableUntil, LocalDateTime requestedTo) {
        if (availableUntil == null || requestedTo == null || !requestedTo.isAfter(availableUntil)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(availableUntil, requestedTo).toMinutes());
    }

    private String overallStatus(List<AvailabilityGate> gates) {
        if (gates.stream().anyMatch(gate -> STATUS_FAIL.equals(gate.status()))) {
            return STATUS_FAIL;
        }
        if (gates.stream().anyMatch(gate -> STATUS_WARN.equals(gate.status()))) {
            return STATUS_WARN;
        }
        return STATUS_PASS;
    }

    private String normalizeMode(String mode) {
        String value = hasText(mode) ? mode.trim().toUpperCase(Locale.ROOT) : MODE_HYBRID;
        if (!MODE_OFFLINE.equals(value) && !MODE_REALTIME.equals(value) && !MODE_HYBRID.equals(value)) {
            throw new IllegalArgumentException("mode must be OFFLINE, REALTIME, or HYBRID");
        }
        return value;
    }

    private Long normalizeTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record AvailabilityDecision(
            Long tenantId,
            String mode,
            LocalDateTime requestedFrom,
            LocalDateTime requestedTo,
            LocalDateTime generatedAt,
            String status,
            List<AvailabilityGate> gates) {
    }

    public record AvailabilityGate(
            String gateKey,
            String status,
            String reason,
            LocalDateTime availableUntil,
            Long lagMinutes,
            int evidenceCount) {
    }
}
