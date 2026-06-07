package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.MarketingCampaignCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkCommand;
import org.chovy.canvas.domain.marketing.MarketingCampaignLinkView;
import org.chovy.canvas.domain.marketing.MarketingCampaignReadinessView;
import org.chovy.canvas.domain.marketing.MarketingCampaignService;
import org.chovy.canvas.domain.marketing.MarketingCampaignView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/canvas/marketing-campaigns")
public class MarketingCampaignController {

    private final MarketingCampaignService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingCampaignController(MarketingCampaignService service,
                                       TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<MarketingCampaignView>> upsertCampaign(@RequestBody MarketingCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<MarketingCampaignView>>> listCampaigns(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listCampaigns(tenantId(context), status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/links")
    public Mono<R<MarketingCampaignLinkView>> linkResource(@RequestBody MarketingCampaignLinkCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.linkResource(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{campaignId}/links")
    public Mono<R<List<MarketingCampaignLinkView>>> listLinks(@PathVariable Long campaignId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listLinks(tenantId(context), campaignId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{campaignId}/readiness")
    public Mono<R<MarketingCampaignReadinessView>> readiness(@PathVariable Long campaignId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.readiness(tenantId(context), campaignId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @DeleteMapping("/links/{linkId}")
    public Mono<R<Void>> unlinkResource(@PathVariable Long linkId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() -> {
                    service.unlinkResource(tenantId(context), linkId);
                    return R.ok();
                })
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
