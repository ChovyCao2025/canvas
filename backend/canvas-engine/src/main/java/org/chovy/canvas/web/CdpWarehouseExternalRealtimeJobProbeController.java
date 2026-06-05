package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseExternalRealtimeJobProbeService;
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

import java.util.List;

@RestController
@RequestMapping("/warehouse/realtime/job-probes")
public class CdpWarehouseExternalRealtimeJobProbeController {

    private final CdpWarehouseExternalRealtimeJobProbeService probeService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseExternalRealtimeJobProbeController(
            CdpWarehouseExternalRealtimeJobProbeService probeService) {
        this(probeService, null);
    }

    @Autowired
    public CdpWarehouseExternalRealtimeJobProbeController(
            CdpWarehouseExternalRealtimeJobProbeService probeService,
            TenantContextResolver tenantContextResolver) {
        this.probeService = probeService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/targets")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>> upsertTarget(
            @RequestBody TargetReq req) {
        TargetReq request = req == null ? new TargetReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.upsertTarget(normalizeTenant(context), request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/targets")
    public Mono<R<List<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>>> listTargets(
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.listTargets(normalizeTenant(context), includeDisabled, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/targets/{targetId}/enabled")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ProbeTargetView>> setEnabled(
            @PathVariable Long targetId,
            @RequestParam boolean enabled) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.setEnabled(normalizeTenant(context), targetId, enabled)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseExternalRealtimeJobProbeService.ScanSummary>> scan(
            @RequestParam(required = false) Long targetId,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(probeService.scan(
                                normalizeTenant(context),
                                new CdpWarehouseExternalRealtimeJobProbeService.ScanCommand(targetId, limit))))
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

    @Data
    public static class TargetReq {
        private String pipelineKey;
        private String jobKey;
        private String engineType;
        private String endpointUrl;
        private String authRef;
        private String externalJobId;
        private String connectorName;
        private String deploymentRef;
        private Boolean enabled;
        private String ownerName;
        private Integer maxStalenessSeconds;
        private String configJson;

        CdpWarehouseExternalRealtimeJobProbeService.TargetCommand toCommand() {
            return new CdpWarehouseExternalRealtimeJobProbeService.TargetCommand(
                    pipelineKey,
                    jobKey,
                    engineType,
                    endpointUrl,
                    authRef,
                    externalJobId,
                    connectorName,
                    deploymentRef,
                    enabled,
                    ownerName,
                    maxStalenessSeconds,
                    configJson);
        }
    }
}
