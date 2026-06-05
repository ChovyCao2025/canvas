package org.chovy.canvas.web;

import org.chovy.canvas.common.PageResult;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.analytics.AnalyticsQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsQueryService service;
    private final TenantContextResolver tenantContextResolver;

    public AnalyticsController(AnalyticsQueryService service) {
        this(service, null);
    }

    @Autowired
    public AnalyticsController(AnalyticsQueryService service, TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping("/events/counts")
    public Mono<R<List<AnalyticsQueryService.EventCountRow>>> eventCounts(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.eventCounts(tenantId, startDate, endDate)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/events/count")
    public Mono<R<AnalyticsQueryService.EventTotal>> countEvents(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) String eventCode) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.countEvents(tenantId, startDate, endDate, eventCode)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/users/{userId}/timeline")
    public Mono<R<PageResult<AnalyticsQueryService.UserTimelineRow>>> userTimeline(
            @PathVariable String userId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.userTimeline(tenantId, userId, startDate, endDate, page, size)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/events/attributes/{attribute}/distribution")
    public Mono<R<List<AnalyticsQueryService.AttributeDistributionRow>>> attributeDistribution(
            @PathVariable String attribute,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return currentTenantId().flatMap(tenantId -> Mono.fromCallable(
                        () -> R.ok(service.attributeDistribution(tenantId, attribute, startDate, endDate)))
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
