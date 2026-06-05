package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.policy.MarketingPreferenceCenterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/canvas/marketing-preferences")
public class MarketingPreferenceCenterController {

    private final MarketingPreferenceCenterService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service) {
        this(service, null);
    }

    @Autowired
    public MarketingPreferenceCenterController(MarketingPreferenceCenterService service,
                                               TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/users/{userId}")
    public Mono<R<MarketingPreferenceCenterService.PreferenceReport>> report(@PathVariable String userId) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.report(tenantId, userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/consents/{channel}")
    public Mono<R<MarketingPreferenceCenterService.ConsentRow>> updateConsent(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ConsentUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertConsent(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ConsentUpdateCommand(channel, req.consentStatus(), req.source()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/users/{userId}/channels/{channel}")
    public Mono<R<MarketingPreferenceCenterService.ChannelRow>> updateChannel(
            @PathVariable String userId,
            @PathVariable String channel,
            @RequestBody ChannelUpdateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.upsertChannel(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.ChannelUpdateCommand(
                        channel, req.address(), req.enabled(), req.verified(), req.metadata()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/users/{userId}/suppressions")
    public Mono<R<MarketingPreferenceCenterService.SuppressionRow>> addSuppression(
            @PathVariable String userId,
            @RequestBody SuppressionCreateReq req) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> R.ok(service.addSuppression(
                tenantId,
                userId,
                new MarketingPreferenceCenterService.SuppressionCreateCommand(
                        req.channel(), req.reason(), req.active(), req.expiresAt()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PutMapping("/suppressions/{id}/deactivate")
    public Mono<R<Void>> deactivateSuppression(@PathVariable Long id) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(() -> {
            service.deactivateSuppression(tenantId, id);
            return R.<Void>ok();
        }).subscribeOn(Schedulers.boundedElastic()));
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

    public record ConsentUpdateReq(String consentStatus, String source) {
    }

    public record ChannelUpdateReq(String address, Boolean enabled, Boolean verified, String metadata) {
    }

    public record SuppressionCreateReq(String channel, String reason, Boolean active, LocalDateTime expiresAt) {
    }
}
