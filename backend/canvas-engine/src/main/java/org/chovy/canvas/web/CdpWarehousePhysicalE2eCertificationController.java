package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePhysicalE2eCertificationService;
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
@RequestMapping("/warehouse/e2e-certification")
public class CdpWarehousePhysicalE2eCertificationController {

    private final CdpWarehousePhysicalE2eCertificationService certificationService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehousePhysicalE2eCertificationController(
            CdpWarehousePhysicalE2eCertificationService certificationService) {
        this(certificationService, null);
    }

    @Autowired
    public CdpWarehousePhysicalE2eCertificationController(
            CdpWarehousePhysicalE2eCertificationService certificationService,
            TenantContextResolver tenantContextResolver) {
        this.certificationService = certificationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<CdpWarehousePhysicalE2eCertificationService.PhysicalE2eCertification>> e2eCertification(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "true") boolean requirePhysical,
            @RequestParam(defaultValue = "true") boolean requireRealtime,
            @RequestParam(defaultValue = "true") boolean requireDataPathProof) {
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(certificationService.certify(
                                tenantId, from, to, mode, safeContractKeys,
                                requirePhysical, requireRealtime, requireDataPathProof)))
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
