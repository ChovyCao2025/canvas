package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeJobControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouse/realtime/jobs")
public class CdpWarehouseRealtimeJobController {

    private final CdpWarehouseRealtimeJobControlService jobService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimeJobController(CdpWarehouseRealtimeJobControlService jobService) {
        this(jobService, null);
    }

    @Autowired
    public CdpWarehouseRealtimeJobController(CdpWarehouseRealtimeJobControlService jobService,
                                             TenantContextResolver tenantContextResolver) {
        this.jobService = jobService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/heartbeats")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobInstanceView>> heartbeat(
            @RequestBody HeartbeatReq req) {
        HeartbeatReq request = req == null ? new HeartbeatReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.heartbeat(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/status")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobStatusSummary>> status(
            @RequestParam(required = false) String pipelineKey,
            @RequestParam(defaultValue = "300") long maxHeartbeatAgeSeconds,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.status(normalizeTenant(context), pipelineKey, maxHeartbeatAgeSeconds, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/actions")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> requestAction(
            @RequestBody ActionReq req) {
        ActionReq request = req == null ? new ActionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.requestAction(
                                normalizeTenant(context), request.toCommand(), operator(request, context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/actions/pending")
    public Mono<R<List<CdpWarehouseRealtimeJobControlService.JobActionView>>> pendingActions(
            @RequestParam String pipelineKey,
            @RequestParam String jobKey,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.pendingActions(normalizeTenant(context), pipelineKey, jobKey, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/actions/{actionId}/ack")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> acknowledge(
            @PathVariable Long actionId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.acknowledge(normalizeTenant(context), actionId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/actions/{actionId}/complete")
    public Mono<R<CdpWarehouseRealtimeJobControlService.JobActionView>> complete(
            @PathVariable Long actionId,
            @RequestBody(required = false) CompleteReq req) {
        CompleteReq request = req == null ? new CompleteReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(jobService.complete(
                                normalizeTenant(context), actionId, request.getStatus(), request.getResultMessage())))
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

    private String operator(ActionReq request, TenantContext context) {
        if (request != null && request.getRequestedBy() != null && !request.getRequestedBy().isBlank()) {
            return request.getRequestedBy().trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class HeartbeatReq {
        private String pipelineKey;
        private String jobKey;
        private String engineType;
        private String engineJobId;
        private String deploymentRef;
        private String runtimeStatus;
        private String desiredStatus;
        private LocalDateTime heartbeatAt;
        private String heartbeatPayloadJson;
        private String errorMessage;
        private String ownerName;

        CdpWarehouseRealtimeJobControlService.HeartbeatCommand toCommand() {
            return new CdpWarehouseRealtimeJobControlService.HeartbeatCommand(
                    pipelineKey,
                    jobKey,
                    engineType,
                    engineJobId,
                    deploymentRef,
                    runtimeStatus,
                    desiredStatus,
                    heartbeatAt,
                    heartbeatPayloadJson,
                    errorMessage,
                    ownerName);
        }
    }

    @Data
    public static class ActionReq {
        private String pipelineKey;
        private String jobKey;
        private String action;
        private String reason;
        private String requestedBy;

        CdpWarehouseRealtimeJobControlService.ActionRequestCommand toCommand() {
            return new CdpWarehouseRealtimeJobControlService.ActionRequestCommand(
                    pipelineKey,
                    jobKey,
                    action,
                    reason);
        }
    }

    @Data
    public static class CompleteReq {
        private String status;
        private String resultMessage;
    }
}
