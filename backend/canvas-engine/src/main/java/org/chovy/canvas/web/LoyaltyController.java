package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.loyalty.LoyaltyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/canvas/loyalty")
public class LoyaltyController {

    private final TenantContextResolver tenantContextResolver;
    private final LoyaltyService loyaltyService;

    public LoyaltyController(TenantContextResolver tenantContextResolver,
                             LoyaltyService loyaltyService) {
        this.tenantContextResolver = tenantContextResolver;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/users/{userId}/account")
    public Mono<R<LoyaltyService.LoyaltyAccountView>> account(@PathVariable String userId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.account(normalizeTenant(context), userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/users/{userId}/earn")
    public Mono<R<LoyaltyService.LoyaltyAccountView>> earn(@PathVariable String userId,
                                                           @RequestBody LoyaltyService.EarnCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.earn(normalizeTenant(context), userId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/users/{userId}/redeem")
    public Mono<R<LoyaltyService.RedemptionView>> redeem(@PathVariable String userId,
                                                         @RequestBody LoyaltyService.RedemptionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.redeem(normalizeTenant(context), userId, command)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/users/{userId}/benefits")
    public Mono<R<List<LoyaltyService.BenefitEligibilityView>>> benefits(@PathVariable String userId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(loyaltyService.eligibleBenefits(normalizeTenant(context), userId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"));
    }

    private Long normalizeTenant(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
