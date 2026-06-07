package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceCollectionService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseEnterpriseOlapEvidenceService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseProductionReadinessProofService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/enterprise-olap/evidence")
public class CdpWarehouseEnterpriseOlapEvidenceController {

    private final CdpWarehouseEnterpriseOlapEvidenceService service;
    private final CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseEnterpriseOlapEvidenceController(
            CdpWarehouseEnterpriseOlapEvidenceService service,
            TenantContextResolver tenantContextResolver) {
        this(service, null, tenantContextResolver);
    }

    @Autowired
    public CdpWarehouseEnterpriseOlapEvidenceController(
            CdpWarehouseEnterpriseOlapEvidenceService service,
            CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService,
            TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.collectionService = collectionService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceView>> record(
            @RequestBody CdpWarehouseEnterpriseOlapEvidenceService.EvidenceCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(
                                service.recordOperatorEvidence(context.tenantId(), command, context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/latest")
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceService.EvidenceBundle>> latest() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.latestEvidence(context.tenantId())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/proof")
    public Mono<R<List<CdpWarehouseProductionReadinessProofService.ProofEvidence>>> proof() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.proofEvidence(context.tenantId())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/collect")
    public Mono<R<CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView>> collect() {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(collectionService().run(
                                context.tenantId(), "MANUAL", context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/collections")
    public Mono<R<List<CdpWarehouseEnterpriseOlapEvidenceCollectionService.CollectionRunView>>> collections(
            @RequestParam(defaultValue = "20") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(collectionService().recentRuns(
                                context.tenantId(), limit)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private CdpWarehouseEnterpriseOlapEvidenceCollectionService collectionService() {
        if (collectionService == null) {
            throw new IllegalStateException("enterprise OLAP evidence collection service is not configured");
        }
        return collectionService;
    }
}
