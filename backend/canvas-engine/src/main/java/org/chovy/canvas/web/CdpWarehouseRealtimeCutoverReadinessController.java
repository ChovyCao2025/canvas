package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseRealtimeCutoverReadinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/realtime/cutover-readiness")
public class CdpWarehouseRealtimeCutoverReadinessController {

    private final CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseRealtimeCutoverReadinessController(
            CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService) {
        this(cutoverReadinessService, null);
    }

    @Autowired
    public CdpWarehouseRealtimeCutoverReadinessController(
            CdpWarehouseRealtimeCutoverReadinessService cutoverReadinessService,
            TenantContextResolver tenantContextResolver) {
        this.cutoverReadinessService = cutoverReadinessService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<CdpWarehouseRealtimeCutoverReadinessService.CutoverDecision>> cutoverReadiness(
            @RequestParam(defaultValue = "FLINK_FIRST") String targetMode,
            @RequestParam(name = "pipelineKey", required = false) List<String> pipelineKeys,
            @RequestParam(name = "contractKey", required = false) List<String> contractKeys,
            @RequestParam(defaultValue = "HYBRID") String certificationMode,
            @RequestParam(defaultValue = "60") Long maxCertificationAgeMinutes) {
        List<String> safePipelineKeys = pipelineKeys == null ? List.of() : pipelineKeys;
        List<String> safeContractKeys = contractKeys == null ? List.of() : contractKeys;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() ->
                        R.ok(cutoverReadinessService.evaluate(
                                tenantId,
                                new CdpWarehouseRealtimeCutoverReadinessService.CutoverCommand(
                                        targetMode,
                                        safePipelineKeys,
                                        safeContractKeys,
                                        certificationMode,
                                        maxCertificationAgeMinutes))))
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
