package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseIncidentService;
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
@RequestMapping("/warehouse/incidents")
public class CdpWarehouseIncidentController {

    private final CdpWarehouseIncidentService incidentService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseIncidentController(CdpWarehouseIncidentService incidentService) {
        this(incidentService, null);
    }

    @Autowired
    public CdpWarehouseIncidentController(CdpWarehouseIncidentService incidentService,
                                          TenantContextResolver tenantContextResolver) {
        this.incidentService = incidentService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseIncidentService.IncidentView>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.listIncidents(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/ack")
    public Mono<R<Boolean>> acknowledge(@PathVariable Long id, @RequestBody OperatorReq req) {
        OperatorReq request = req == null ? new OperatorReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.acknowledge(tenantId, id, request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/resolve")
    public Mono<R<Boolean>> resolve(@PathVariable Long id, @RequestBody OperatorReq req) {
        OperatorReq request = req == null ? new OperatorReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(incidentService.resolve(tenantId, id, request.getOperator())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<Long> currentTenantId() {
        if (tenantContextResolver == null) {
            return Mono.just(0L);
        }
        return tenantContextResolver.current()
                .map(context -> context.tenantId() == null ? 0L : context.tenantId())
                .defaultIfEmpty(0L)
                .map(tenantId -> tenantId == null ? 0L : tenantId);
    }

    @Data
    public static class OperatorReq {
        private String operator;
    }
}
