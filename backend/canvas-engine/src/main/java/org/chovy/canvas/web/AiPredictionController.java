package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.ChurnPredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/ai/predictions")
public class AiPredictionController {

    private final ChurnPredictionService churnPredictionService;
    private final TenantContextResolver tenantContextResolver;

    public AiPredictionController(ChurnPredictionService churnPredictionService) {
        this(churnPredictionService, null);
    }

    @Autowired
    public AiPredictionController(ChurnPredictionService churnPredictionService,
                                  TenantContextResolver tenantContextResolver) {
        this.churnPredictionService = churnPredictionService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/latest-run")
    public Mono<R<ChurnPredictionService.PredictionRunView>> latestRun() {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.latestRun(tenantId(context)).orElse(null)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/churn-distribution")
    public Mono<R<List<ChurnPredictionService.RiskDistributionItem>>> distribution() {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.churnDistribution(tenantId(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/top-risk-users")
    public Mono<R<List<ChurnPredictionService.TopRiskUser>>> topRiskUsers(@RequestParam(defaultValue = "100") int limit) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.topRiskUsers(tenantId(context), limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/recompute")
    public Mono<R<ChurnPredictionService.PredictionRunView>> recompute(
            @RequestBody(required = false) ChurnPredictionService.RecomputeRequest request) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(churnPredictionService.recompute(tenantId(context),
                                request == null ? new ChurnPredictionService.RecomputeRequest(false, null, null) : request)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentAdmin() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, "ADMIN", "test"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI prediction requires admin access")))
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI prediction requires admin access")));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }
}
