package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityIncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/warehouse/availability/consumer-incidents")
public class CdpWarehouseConsumerAvailabilityIncidentController {

    private final CdpWarehouseConsumerAvailabilityIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseConsumerAvailabilityIncidentController(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseConsumerAvailabilityIncidentController(
            CdpWarehouseConsumerAvailabilityIncidentService incidentService,
            TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/scan")
    public Mono<R<CdpWarehouseConsumerAvailabilityIncidentService.ScanResult>> scan(
            @RequestBody(required = false) ScanReq req) {
        ScanReq request = req == null ? new ScanReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(incidentService.scan(
                                normalizeTenant(context),
                                request.getContractKey(),
                                request.getConsumerType(),
                                request.getFrom(),
                                request.getTo(),
                                operator(request.getOperator(), context))))
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

    private String operator(String requestedOperator, TenantContext context) {
        if (requestedOperator != null && !requestedOperator.isBlank()) {
            return requestedOperator.trim();
        }
        if (context != null && context.username() != null && !context.username().isBlank()) {
            return context.username();
        }
        return "operator";
    }

    @Data
    public static class ScanReq {
        private String contractKey;
        private String consumerType;
        private LocalDateTime from;
        private LocalDateTime to;
        private String operator;
    }
}
