package org.chovy.canvas.web;

import lombok.Data;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.warehouse.CdpWarehouseMetricChangeReviewService;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/warehouse/metric-change-reviews")
public class CdpWarehouseMetricChangeReviewController {

    private final CdpWarehouseMetricChangeReviewService reviewService;
    private final TenantContextResolver tenantContextResolver;

    public CdpWarehouseMetricChangeReviewController(CdpWarehouseMetricChangeReviewService reviewService) {
        this(reviewService, null);
    }

    @Autowired
    public CdpWarehouseMetricChangeReviewController(CdpWarehouseMetricChangeReviewService reviewService,
                                                    TenantContextResolver tenantContextResolver) {
        this.reviewService = reviewService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public Mono<R<List<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>>> list(
            @RequestParam(required = false) String datasetKey,
            @RequestParam(required = false) String metricKey,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.list(context.tenantId(), datasetKey, metricKey, status)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> requestChange(
            @RequestBody MetricChangeReq req) {
        MetricChangeReq request = req == null ? new MetricChangeReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.requestChange(
                                context.tenantId(),
                                context.username(),
                                request.toCommand())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{reviewId}/approve")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> approve(
            @PathVariable Long reviewId,
            @RequestBody ReviewDecisionReq req) {
        ReviewDecisionReq request = req == null ? new ReviewDecisionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.approve(
                                context.tenantId(),
                                reviewId,
                                context.username(),
                                request.getReviewNote())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{reviewId}/reject")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> reject(
            @PathVariable Long reviewId,
            @RequestBody ReviewDecisionReq req) {
        ReviewDecisionReq request = req == null ? new ReviewDecisionReq() : req;
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.reject(
                                context.tenantId(),
                                reviewId,
                                context.username(),
                                request.getReviewNote())))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{reviewId}/apply")
    public Mono<R<CdpWarehouseMetricChangeReviewService.MetricChangeReviewView>> apply(
            @PathVariable Long reviewId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(
                        () -> R.ok(reviewService.apply(context.tenantId(), reviewId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    private Mono<TenantContext> currentTenant() {
        if (tenantContextResolver == null) {
            return Mono.just(new TenantContext(0L, null, "system"));
        }
        return tenantContextResolver.current()
                .defaultIfEmpty(new TenantContext(0L, null, "system"))
                .map(context -> new TenantContext(
                        context.tenantId() == null ? 0L : context.tenantId(),
                        context.role(),
                        context.username() == null || context.username().isBlank() ? "system" : context.username()));
    }

    @Data
    public static class MetricChangeReq {
        private String datasetKey;
        private String metricKey;
        private String proposedExpression;
        private List<String> proposedAllowedDimensions;
        private String reason;

        CdpWarehouseMetricChangeReviewService.MetricChangeCommand toCommand() {
            return new CdpWarehouseMetricChangeReviewService.MetricChangeCommand(
                    datasetKey,
                    metricKey,
                    proposedExpression,
                    proposedAllowedDimensions,
                    reason);
        }
    }

    @Data
    public static class ReviewDecisionReq {
        private String reviewNote;
    }
}
