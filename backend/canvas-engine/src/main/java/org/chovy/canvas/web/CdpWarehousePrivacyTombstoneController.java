package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehousePrivacyTombstoneService;
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
@RequestMapping("/warehouse/privacy/tombstones")
public class CdpWarehousePrivacyTombstoneController {

    private final CdpWarehousePrivacyTombstoneService tombstoneService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehousePrivacyTombstoneController(CdpWarehousePrivacyTombstoneService tombstoneService) {
        this(tombstoneService, null);
    }

    @Autowired
    public CdpWarehousePrivacyTombstoneController(CdpWarehousePrivacyTombstoneService tombstoneService,
                                                  TenantContextResolver tenantContextResolver) {
        this.tombstoneService = tombstoneService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> create(
            @RequestBody CdpWarehousePrivacyTombstoneService.TombstoneCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.create(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/from-erasure-request")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> createFromErasureRequest(
            @RequestBody CdpWarehousePrivacyTombstoneService.ErasureRequestTombstoneCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.createFromErasureRequest(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{id}/revoke")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneView>> revoke(
            @PathVariable Long id,
            @RequestBody CdpWarehousePrivacyTombstoneService.RevokeCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.revoke(tenantId, id, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<CdpWarehousePrivacyTombstoneService.TombstoneView>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.list(tenantId, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/decision")
    public Mono<R<CdpWarehousePrivacyTombstoneService.TombstoneDecision>> decision(
            @RequestParam(defaultValue = "USER_ID") String subjectType,
            @RequestParam String subjectValue) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(tombstoneService.decide(tenantId, subjectType, subjectValue)))
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
