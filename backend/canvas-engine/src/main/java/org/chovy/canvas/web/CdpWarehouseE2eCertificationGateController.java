package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationGateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/e2e-certification/gate")
public class CdpWarehouseE2eCertificationGateController {

    private final CdpWarehouseE2eCertificationGateService gateService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseE2eCertificationGateController(CdpWarehouseE2eCertificationGateService gateService) {
        this(gateService, null);
    }

    @Autowired
    public CdpWarehouseE2eCertificationGateController(CdpWarehouseE2eCertificationGateService gateService,
                                                      TenantContextResolver tenantContextResolver) {
        this.gateService = gateService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<CdpWarehouseE2eCertificationGateService.GateDecision>> gate(
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "true") boolean requirePhysical,
            @RequestParam(defaultValue = "true") boolean requireRealtime,
            @RequestParam(defaultValue = "true") boolean requireDataPathProof,
            @RequestParam(defaultValue = "60") long maxAgeMinutes) {
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(gateService.evaluate(
                                tenantId, mode, safeContractKeys,
                                requirePhysical, requireRealtime, requireDataPathProof, maxAgeMinutes)))
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
