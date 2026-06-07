package org.chovy.canvas.web;

import lombok.RequiredArgsConstructor;
import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.cdp.RealtimeAudienceService;
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
@RequestMapping("/cdp")
@RequiredArgsConstructor
public class RealtimeAudienceController {
    private final TenantContextResolver tenantContextResolver;
    private final RealtimeAudienceService service;

    @PostMapping("/realtime-audiences/{id}/events")
    public Mono<R<RealtimeAudienceService.EventResult>> processEvent(
            @PathVariable Long id,
            @RequestBody RealtimeEventRequest request) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.processEvent(
                                tenantId(ctx),
                                id,
                                request.toEvent(),
                                request.removeOnNoMatchOrDefault())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/realtime-audiences/{id}/snapshot")
    public Mono<R<RealtimeAudienceService.SnapshotResult>> snapshot(@PathVariable Long id) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.createSnapshot(tenantId(ctx), id, "MANUAL", ctx.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/realtime-audiences/{id}/snapshots")
    public Mono<R<List<RealtimeAudienceService.SnapshotRow>>> snapshots(@PathVariable Long id,
                                                                        @RequestParam(required = false) Integer limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.listSnapshots(tenantId(ctx), id, normalizeLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/audiences/{leftId}/overlap/{rightId}")
    public Mono<R<RealtimeAudienceService.OverlapResult>> overlap(@PathVariable Long leftId,
                                                                  @PathVariable Long rightId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.overlap(leftId, rightId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/audiences/merge")
    public Mono<R<RealtimeAudienceService.SetOperationResult>> merge(@RequestParam Long leftId,
                                                                     @RequestParam Long rightId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.merge(leftId, rightId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/audiences/exclude")
    public Mono<R<RealtimeAudienceService.SetOperationResult>> exclude(@RequestParam Long baseId,
                                                                       @RequestParam Long excludedId) {
        return tenantContextResolver.currentOrError()
                .flatMap(ctx -> Mono.fromCallable(() -> R.ok(service.exclude(baseId, excludedId)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext ctx) {
        return ctx.tenantId() == null ? 0L : ctx.tenantId();
    }

    private int normalizeLimit(Integer limit) {
        return limit == null || limit <= 0 ? 100 : Math.min(limit, 500);
    }

    public record RealtimeEventRequest(String sourceEventId,
                                       String userId,
                                       java.time.Instant eventTime,
                                       java.util.Map<String, Object> properties,
                                       Boolean removeOnNoMatch) {
        public RealtimeAudienceService.CdpEvent toEvent() {
            return new RealtimeAudienceService.CdpEvent(sourceEventId, userId, eventTime, properties);
        }

        boolean removeOnNoMatchOrDefault() {
            return removeOnNoMatch == null || removeOnNoMatch;
        }
    }
}
