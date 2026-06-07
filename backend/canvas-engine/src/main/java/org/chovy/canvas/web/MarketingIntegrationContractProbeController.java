package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeCommand;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeService;
import org.chovy.canvas.domain.marketing.MarketingIntegrationContractProbeView;
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
@RequestMapping("/canvas/marketing-integrations")
public class MarketingIntegrationContractProbeController {

    private final MarketingIntegrationContractProbeService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingIntegrationContractProbeController(MarketingIntegrationContractProbeService service,
                                                       TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/contracts/{contractId}/probes")
    public Mono<R<MarketingIntegrationContractProbeView>> recordProbe(
            @PathVariable Long contractId,
            @RequestBody MarketingIntegrationContractProbeCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordProbe(tenantId(context), contractId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/contracts/{contractId}/probes")
    public Mono<R<List<MarketingIntegrationContractProbeView>>> listContractProbes(
            @PathVariable Long contractId,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listContractProbes(tenantId(context), contractId, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/probes")
    public Mono<R<List<MarketingIntegrationContractProbeView>>> listRecentProbes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listRecentProbes(tenantId(context), status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
