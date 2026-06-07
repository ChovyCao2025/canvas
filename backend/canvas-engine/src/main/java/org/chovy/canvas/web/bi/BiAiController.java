package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.ai.BiAskDataAgentService;
import org.chovy.canvas.domain.bi.ai.BiAskDataRequest;
import org.chovy.canvas.domain.bi.ai.BiAskDataResponse;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftAgentService;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftRequest;
import org.chovy.canvas.domain.bi.ai.BiDashboardDraftResponse;
import org.chovy.canvas.domain.bi.ai.BiInsightAgentService;
import org.chovy.canvas.domain.bi.ai.BiInsightRequest;
import org.chovy.canvas.domain.bi.ai.BiInsightResponse;
import org.chovy.canvas.domain.bi.ai.BiInterpretationAgentService;
import org.chovy.canvas.domain.bi.ai.BiInterpretationRequest;
import org.chovy.canvas.domain.bi.ai.BiInterpretationResponse;
import org.chovy.canvas.domain.bi.ai.BiReportAgentService;
import org.chovy.canvas.domain.bi.ai.BiReportRequest;
import org.chovy.canvas.domain.bi.ai.BiReportResponse;
import org.chovy.canvas.domain.bi.query.BiQueryContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/bi/ai")
public class BiAiController {

    private final TenantContextResolver tenantContextResolver;
    private final BiAskDataAgentService askDataAgentService;
    private final BiInterpretationAgentService interpretationAgentService;
    private final BiReportAgentService reportAgentService;
    private final BiDashboardDraftAgentService dashboardDraftAgentService;
    private final BiInsightAgentService insightAgentService;

    public BiAiController(TenantContextResolver tenantContextResolver,
                          BiAskDataAgentService askDataAgentService) {
        this(tenantContextResolver, askDataAgentService, null, null, null, null);
    }

    @Autowired
    public BiAiController(TenantContextResolver tenantContextResolver,
                          BiAskDataAgentService askDataAgentService,
                          BiInterpretationAgentService interpretationAgentService,
                          BiReportAgentService reportAgentService,
                          BiDashboardDraftAgentService dashboardDraftAgentService,
                          BiInsightAgentService insightAgentService) {
        this.tenantContextResolver = tenantContextResolver;
        this.askDataAgentService = askDataAgentService;
        this.interpretationAgentService = interpretationAgentService;
        this.reportAgentService = reportAgentService;
        this.dashboardDraftAgentService = dashboardDraftAgentService;
        this.insightAgentService = insightAgentService;
    }

    @PostMapping("/ask")
    public Mono<R<BiAskDataResponse>> askData(@RequestBody BiAskDataRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(askDataAgentService.ask(request, new BiQueryContext(
                                normalizeTenant(context),
                                context == null ? "system" : context.username(),
                                context == null ? null : context.role()))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/interpret")
    public Mono<R<BiInterpretationResponse>> interpret(@RequestBody BiInterpretationRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(interpretationAgentService.interpret(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/report")
    public Mono<R<BiReportResponse>> report(@RequestBody BiReportRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(reportAgentService.generate(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/dashboard-draft")
    public Mono<R<BiDashboardDraftResponse>> dashboardDraft(@RequestBody BiDashboardDraftRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(dashboardDraftAgentService.generate(request, queryContext(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/insights")
    public Mono<R<BiInsightResponse>> insights(@RequestBody BiInsightRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(insightAgentService.inspect(request, queryContext(context))))
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

    private BiQueryContext queryContext(TenantContext context) {
        return new BiQueryContext(
                normalizeTenant(context),
                context == null ? "system" : context.username(),
                context == null ? null : context.role());
    }
}
