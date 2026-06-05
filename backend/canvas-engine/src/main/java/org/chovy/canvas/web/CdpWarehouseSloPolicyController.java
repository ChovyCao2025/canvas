package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseSloPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/warehouse/slo-policies")
public class CdpWarehouseSloPolicyController {

    private final CdpWarehouseSloPolicyService sloPolicyService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseSloPolicyController(CdpWarehouseSloPolicyService sloPolicyService) {
        this(sloPolicyService, null);
    }

    @Autowired
    public CdpWarehouseSloPolicyController(CdpWarehouseSloPolicyService sloPolicyService,
                                           TenantContextResolver tenantContextResolver) {
        this.sloPolicyService = sloPolicyService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseSloPolicyService.SloPolicyView>>> listPolicies(
            @RequestParam(required = false) String status) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.listPolicies(tenantId, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/effective")
    public Mono<R<CdpWarehouseSloPolicyService.SloPolicyView>> effective(
            @RequestParam(defaultValue = CdpWarehouseSloPolicyService.DEFAULT_POLICY_KEY) String policyKey) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.effectivePolicy(tenantId, policyKey)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<CdpWarehouseSloPolicyService.SloPolicyView>> upsert(@RequestBody SloPolicyReq req) {
        SloPolicyReq request = req == null ? new SloPolicyReq() : req;
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(sloPolicyService.upsertPolicy(tenantId, request.toCommand())))
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
    public static class SloPolicyReq {
        private String policyKey;
        private String displayName;
        private Integer offlineWarnRunGapMinutes;
        private Integer offlineFailRunGapMinutes;
        private Integer offlineWarnWatermarkLagMinutes;
        private Integer offlineFailWatermarkLagMinutes;
        private Integer audienceWarnRunGapMinutes;
        private Integer audienceFailRunGapMinutes;
        private String status;
        private String ownerName;
        private String description;

        CdpWarehouseSloPolicyService.SloPolicyCommand toCommand() {
            return new CdpWarehouseSloPolicyService.SloPolicyCommand(
                    policyKey,
                    displayName,
                    offlineWarnRunGapMinutes,
                    offlineFailRunGapMinutes,
                    offlineWarnWatermarkLagMinutes,
                    offlineFailWatermarkLagMinutes,
                    audienceWarnRunGapMinutes,
                    audienceFailRunGapMinutes,
                    status,
                    ownerName,
                    description);
        }
    }
}
