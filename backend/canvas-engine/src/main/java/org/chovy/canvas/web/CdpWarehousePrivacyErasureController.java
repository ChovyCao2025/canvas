package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyAudienceBitmapRebuildService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureExecutionService;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyErasureService;
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
@RequestMapping("/warehouse/privacy/erasure")
public class CdpWarehousePrivacyErasureController {

    private final CdpWarehousePrivacyErasureService erasureService;
    private final CdpWarehousePrivacyErasureExecutionService executionService;
    private final CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService) {
        this(erasureService, null, null, null);
    }

    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, null, null, tenantContextResolver);
    }

    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                TenantContextResolver tenantContextResolver) {
        this(erasureService, executionService, null, tenantContextResolver);
    }

    @Autowired
    public CdpWarehousePrivacyErasureController(CdpWarehousePrivacyErasureService erasureService,
                                                CdpWarehousePrivacyErasureExecutionService executionService,
                                                CdpWarehousePrivacyAudienceBitmapRebuildService rebuildService,
                                                TenantContextResolver tenantContextResolver) {
        this.erasureService = erasureService;
        this.executionService = executionService;
        this.rebuildService = rebuildService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/requests")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> create(
            @RequestBody CdpWarehousePrivacyErasureService.ErasureRequestCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.create(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/requests/{id}/proofs")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> recordProof(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyErasureService.AssetProofCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.recordAssetProof(tenantId, id, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/requests/{id}/execute")
    public Mono<R<CdpWarehousePrivacyErasureExecutionService.ErasureExecutionResult>> execute(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyErasureExecutionService.ErasureExecutionCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (executionService == null) {
                        throw new IllegalStateException("privacy erasure execution service is not configured");
                    }
                    return R.ok(executionService.execute(tenantId, id, command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/requests/{id}/audience-rebuild")
    public Mono<R<CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildResult>> rebuildAudienceBitmaps(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyAudienceBitmapRebuildService.AudienceBitmapRebuildCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
                    if (rebuildService == null) {
                        throw new IllegalStateException("privacy audience bitmap rebuild service is not configured");
                    }
                    return R.ok(rebuildService.rebuild(tenantId, id, command));
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/requests")
    public Mono<R<List<CdpWarehousePrivacyErasureService.ErasureRequestView>>> recent(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.recent(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/requests/{id}")
    public Mono<R<CdpWarehousePrivacyErasureService.ErasureRequestView>> get(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.get(tenantId, id)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/summary")
    public Mono<R<CdpWarehousePrivacyErasureService.BacklogSummary>> summary() {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(erasureService.summary(tenantId)))
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
