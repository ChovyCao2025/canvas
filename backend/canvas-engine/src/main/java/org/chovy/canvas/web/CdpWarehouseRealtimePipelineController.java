package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimePipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/warehouse/realtime/pipelines")
public class CdpWarehouseRealtimePipelineController {

    private final CdpWarehouseRealtimePipelineService pipelineService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimePipelineController(CdpWarehouseRealtimePipelineService pipelineService) {
        this(pipelineService, null);
    }

    @Autowired
    public CdpWarehouseRealtimePipelineController(CdpWarehouseRealtimePipelineService pipelineService,
                                                  TenantContextResolver tenantContextResolver) {
        this.pipelineService = pipelineService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseRealtimePipelineService.PipelineContractView>>> listContracts(
            @RequestParam(required = false) String lifecycleStatus) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.listPipelines(normalizeTenant(context), lifecycleStatus)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseRealtimePipelineService.PipelineContractView>> upsertContract(
            @RequestBody PipelineContractReq req) {
        PipelineContractReq request = req == null ? new PipelineContractReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.upsertPipeline(
                                normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/checkpoints")
    public Mono<R<CdpWarehouseRealtimePipelineService.CheckpointReport>> reportCheckpoint(
            @RequestBody CheckpointReq req) {
        CheckpointReq request = req == null ? new CheckpointReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.reportCheckpoint(
                                normalizeTenant(context), request.toCommand(operator(request, context)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/status")
    public Mono<R<CdpWarehouseRealtimePipelineService.PipelineStatusSummary>> status(
            @RequestParam(defaultValue = "5") int recentLimit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(pipelineService.status(normalizeTenant(context), recentLimit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String operator(CheckpointReq request, TenantContext context) {
        if (request != null && request.getReportedBy() != null && !request.getReportedBy().isBlank()) {
            return request.getReportedBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class PipelineContractReq {
        private String pipelineKey;
        private String displayName;
        private String sourceType;
        private String sourceRef;
        private String sourceTopic;
        private String consumerGroup;
        private String processorType;
        private String sinkType;
        private String sinkRef;
        private String deliverySemantics;
        private Integer checkpointIntervalSeconds;
        private Long maxLagMs;
        private Integer maxCheckpointAgeSeconds;
        private String lifecycleStatus;
        private String ownerName;
        private String configJson;

        CdpWarehouseRealtimePipelineService.PipelineContractCommand toCommand() {
            return new CdpWarehouseRealtimePipelineService.PipelineContractCommand(
                    pipelineKey,
                    displayName,
                    sourceType,
                    sourceRef,
                    sourceTopic,
                    consumerGroup,
                    processorType,
                    sinkType,
                    sinkRef,
                    deliverySemantics,
                    checkpointIntervalSeconds,
                    maxLagMs,
                    maxCheckpointAgeSeconds,
                    lifecycleStatus,
                    ownerName,
                    configJson);
        }
    }

    @Data
    public static class CheckpointReq {
        private String pipelineKey;
        private String checkpointId;
        private String sourcePartition;
        private String sourceOffset;
        private String committedOffset;
        private String watermarkTime;
        private String checkpointTime;
        private Long lagMs;
        private Long rowCount;
        private String status;
        private String errorMessage;
        private String reportedBy;
        private String sourceSchemaVersion;
        private String sinkSchemaVersion;

        CdpWarehouseRealtimePipelineService.CheckpointCommand toCommand(String operator) {
            return new CdpWarehouseRealtimePipelineService.CheckpointCommand(
                    pipelineKey,
                    checkpointId,
                    sourcePartition,
                    sourceOffset,
                    committedOffset,
                    parseDateTime(watermarkTime, "watermarkTime"),
                    parseDateTime(checkpointTime, "checkpointTime"),
                    lagMs,
                    rowCount,
                    status,
                    errorMessage,
                    operator,
                    sourceSchemaVersion,
                    sinkSchemaVersion);
        }

        private static LocalDateTime parseDateTime(String value, String fieldName) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String trimmed = value.trim();
            try {
                return LocalDateTime.parse(trimmed);
            } catch (DateTimeParseException localFailure) {
                try {
                    return OffsetDateTime.parse(trimmed)
                            .atZoneSameInstant(ZoneId.systemDefault())
                            .toLocalDateTime();
                } catch (DateTimeParseException offsetFailure) {
                    throw new IllegalArgumentException(fieldName + " must be ISO-8601 datetime", offsetFailure);
                }
            }
        }
    }
}
