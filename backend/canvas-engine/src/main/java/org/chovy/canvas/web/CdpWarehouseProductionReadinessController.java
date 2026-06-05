package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouse/production-readiness")
public class CdpWarehouseProductionReadinessController {

    private final CdpWarehouseProductionReadinessProofService proofService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseProductionReadinessController(
            CdpWarehouseProductionReadinessProofService proofService) {
        this(proofService, null);
    }

    @Autowired
    public CdpWarehouseProductionReadinessController(
            CdpWarehouseProductionReadinessProofService proofService,
            TenantContextResolver tenantContextResolver) {
        this.proofService = proofService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<CdpWarehouseProductionReadinessProofService.ProductionReadinessProof>> productionReadiness(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys) {
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(proofService.proof(tenantId, from, to, mode, safeContractKeys)))
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
}
