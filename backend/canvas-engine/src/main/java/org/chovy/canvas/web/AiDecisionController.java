package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackCommand;
import org.chovy.canvas.domain.ai.AiDecisionFeedbackView;
import org.chovy.canvas.domain.ai.AiDecisionModelService;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationQuery;
import org.chovy.canvas.domain.ai.AiDecisionRecommendationView;
import org.chovy.canvas.domain.ai.AiDecisionRecomputeCommand;
import org.chovy.canvas.domain.ai.AiDecisionRunView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
@RequestMapping("/ai/decisions")
public class AiDecisionController {

    private final AiDecisionModelService decisionModelService;
    private final TenantContextResolver tenantContextResolver;

    public AiDecisionController(AiDecisionModelService decisionModelService) {
        this(decisionModelService, null);
    }

    @Autowired
    public AiDecisionController(AiDecisionModelService decisionModelService,
                                TenantContextResolver tenantContextResolver) {
        this.decisionModelService = decisionModelService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/recompute")
    public Mono<R<AiDecisionRunView>> recompute(@RequestBody(required = false) AiDecisionRecomputeCommand command) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recompute(tenantId(context), command, username(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/latest-run")
    public Mono<R<AiDecisionRunView>> latestRun(@RequestParam(defaultValue = "DAILY_MARKETING") String decisionScope) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.latestRun(tenantId(context), decisionScope).orElse(null)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/recommendations")
    public Mono<R<List<AiDecisionRecommendationView>>> recommendations(
            @RequestParam(required = false) Long runId,
            @RequestParam(required = false) String decisionType,
            @RequestParam(required = false) String eligibilityStatus,
            @RequestParam(defaultValue = "100") int limit) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recommendations(
                                tenantId(context),
                                new AiDecisionRecommendationQuery(runId, decisionType, eligibilityStatus, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/recommendations/{recommendationId}/feedback")
    public Mono<R<AiDecisionFeedbackView>> recordFeedback(@PathVariable Long recommendationId,
                                                          @RequestBody AiDecisionFeedbackCommand command) {
        return currentAdmin().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(decisionModelService.recordFeedback(
                                tenantId(context), recommendationId, command, username(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentAdmin() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, "ADMIN", "system"));
        }
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI decision requires admin access")))
                .filter(context -> context.isSuperAdmin() || context.isTenantAdmin())
                .switchIfEmpty(Mono.error(new AccessDeniedException("AI decision requires admin access")));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String username(TenantContext context) {
        return context == null || context.username() == null ? "system" : context.username();
    }

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
