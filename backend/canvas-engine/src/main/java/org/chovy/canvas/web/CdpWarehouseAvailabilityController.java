package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseAvailabilityService;
import org.chovy.canvas.domain.warehouse.CdpWarehouseConsumerAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/warehouse/availability")
public class CdpWarehouseAvailabilityController {

    private final CdpWarehouseAvailabilityService availabilityService;
    private final CdpWarehouseConsumerAvailabilityService consumerAvailabilityService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService) {
        this(availabilityService, null, null);
    }

    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService,
                                              TenantContextResolver tenantContextResolver) {
        this(availabilityService, tenantContextResolver, null);
    }

    @Autowired
    public CdpWarehouseAvailabilityController(CdpWarehouseAvailabilityService availabilityService,
                                              TenantContextResolver tenantContextResolver,
                                              CdpWarehouseConsumerAvailabilityService consumerAvailabilityService) {
        this.availabilityService = availabilityService;
        this.consumerAvailabilityService = consumerAvailabilityService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<CdpWarehouseAvailabilityService.AvailabilityDecision>> availability(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "HYBRID") String mode) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(availabilityService.evaluate(tenantId, from, to, mode)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/assets")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView>> recordAssetAvailability(
            @RequestBody CdpWarehouseConsumerAvailabilityService.AssetAvailabilityCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().recordAssetAvailability(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/assets")
    public Mono<R<List<CdpWarehouseConsumerAvailabilityService.AssetAvailabilityView>>> listAssetAvailability(
            @RequestParam(required = false) String assetType,
            @RequestParam(required = false) String assetKey,
            @RequestParam(required = false) String mode,
            @RequestParam(defaultValue = "50") Integer limit) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().listAssetAvailability(tenantId, assetType, assetKey, mode, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView>> upsertContract(
            @RequestBody CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractCommand command) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().upsertContract(tenantId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contracts")
    public Mono<R<List<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityContractView>>> listContracts(
            @RequestParam(required = false) String consumerType,
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().listContracts(tenantId, consumerType, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/contracts/{contractKey}/evaluate")
    public Mono<R<CdpWarehouseConsumerAvailabilityService.ConsumerAvailabilityEvaluation>> evaluateContract(
            @PathVariable String contractKey,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(consumerService().evaluateContract(tenantId, contractKey, from, to)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private CdpWarehouseConsumerAvailabilityService consumerService() {
        if (consumerAvailabilityService == null) {
            throw new IllegalStateException("warehouse consumer availability service is not configured");
        }
        return consumerAvailabilityService;
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
