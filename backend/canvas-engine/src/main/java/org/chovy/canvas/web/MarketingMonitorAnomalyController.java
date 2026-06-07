package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionService;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyDetectionView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventQuery;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyEventView;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleCommand;
import org.chovy.canvas.domain.monitoring.MarketingMonitorAnomalyRuleView;
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
@RequestMapping("/canvas/marketing-monitoring")
public class MarketingMonitorAnomalyController {

    private final MarketingMonitorAnomalyDetectionService service;
    private final TenantContextResolver tenantContextResolver;

    public MarketingMonitorAnomalyController(MarketingMonitorAnomalyDetectionService service,
                                             TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/anomaly-rules")
    public Mono<R<MarketingMonitorAnomalyRuleView>> upsertRule(
            @RequestBody MarketingMonitorAnomalyRuleCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertRule(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/anomalies/detect")
    public Mono<R<MarketingMonitorAnomalyDetectionView>> detect(
            @RequestBody MarketingMonitorAnomalyDetectionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.detect(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/anomalies")
    public Mono<R<List<MarketingMonitorAnomalyEventView>>> events(
            @RequestParam(required = false) Long ruleId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.events(tenantId(context),
                                new MarketingMonitorAnomalyEventQuery(ruleId, status, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/anomalies/{eventId}/resolve")
    public Mono<R<MarketingMonitorAnomalyEventView>> resolveEvent(@PathVariable Long eventId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.resolveEvent(tenantId(context), eventId, actor(context))))
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

    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
