package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseE2eCertificationRunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping({"/warehouse/e2e-certification-runs", "/warehouse/e2e-certification/runs"})
public class CdpWarehouseE2eCertificationRunController {

    private final CdpWarehouseE2eCertificationRunService runService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseE2eCertificationRunController(CdpWarehouseE2eCertificationRunService runService) {
        this(runService, null);
    }

    @Autowired
    public CdpWarehouseE2eCertificationRunController(CdpWarehouseE2eCertificationRunService runService,
                                                     TenantContextResolver tenantContextResolver) {
        this.runService = runService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<CdpWarehouseE2eCertificationRunService.CertificationRunView>> runCertification(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "true") boolean requirePhysical,
            @RequestParam(defaultValue = "true") boolean requireRealtime,
            @RequestParam(defaultValue = "true") boolean requireDataPathProof,
            @RequestParam(defaultValue = "system") String requestedBy) {
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.run(
                                tenantId, from, to, mode, safeContractKeys, requirePhysical, requireRealtime,
                                requireDataPathProof, requestedBy)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseE2eCertificationRunService.CertificationRunView>>> recentRuns(
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.recent(tenantId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{id}")
    public Mono<R<CdpWarehouseE2eCertificationRunService.CertificationRunView>> getRun(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(runService.get(tenantId, id)))
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
