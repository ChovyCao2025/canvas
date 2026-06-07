package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceDestinationView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberQuery;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceMemberView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceRunQuery;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncCommand;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncRunView;
import org.chovy.canvas.domain.paidmedia.PaidMediaAudienceSyncService;
import org.chovy.canvas.domain.paidmedia.PaidMediaDestinationCommand;
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
@RequestMapping("/canvas/paid-media/audience-sync")
public class PaidMediaAudienceSyncController {

    private final PaidMediaAudienceSyncService service;
    private final TenantContextResolver tenantContextResolver;

    public PaidMediaAudienceSyncController(PaidMediaAudienceSyncService service,
                                           TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/destinations")
    public Mono<R<PaidMediaAudienceDestinationView>> upsertDestination(
            @RequestBody PaidMediaDestinationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertDestination(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/runs")
    public Mono<R<PaidMediaAudienceSyncRunView>> syncAudience(
            @RequestBody PaidMediaAudienceSyncCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.syncAudience(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/runs")
    public Mono<R<List<PaidMediaAudienceSyncRunView>>> runs(
            @RequestParam(required = false) Long destinationId,
            @RequestParam(required = false) Long audienceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.runs(tenantId(context),
                                new PaidMediaAudienceRunQuery(destinationId, audienceId, status, boundedLimit(limit)))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/runs/{runId}/members")
    public Mono<R<List<PaidMediaAudienceMemberView>>> members(
            @PathVariable Long runId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.members(tenantId(context),
                                new PaidMediaAudienceMemberQuery(runId, status, boundedLimit(limit)))))
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
