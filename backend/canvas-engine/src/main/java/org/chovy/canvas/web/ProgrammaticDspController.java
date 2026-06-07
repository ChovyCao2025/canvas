package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspCampaignView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspLineItemView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationApprovalCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationExecuteCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationQuery;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationService;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspMutationView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSeatView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspService;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSnapshotView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryQuery;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSummaryView;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathCommand;
import org.chovy.canvas.domain.programmatic.ProgrammaticDspSupplyPathView;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/canvas/programmatic-dsp")
public class ProgrammaticDspController {

    private final ProgrammaticDspService service;
    private final ProgrammaticDspMutationService mutationService;
    private final TenantContextResolver tenantContextResolver;

    public ProgrammaticDspController(ProgrammaticDspService service,
                                     ProgrammaticDspMutationService mutationService,
                                     TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.mutationService = mutationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/seats")
    public Mono<R<ProgrammaticDspSeatView>> upsertSeat(@RequestBody ProgrammaticDspSeatCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSeat(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/campaigns")
    public Mono<R<ProgrammaticDspCampaignView>> upsertCampaign(@RequestBody ProgrammaticDspCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/line-items")
    public Mono<R<ProgrammaticDspLineItemView>> upsertLineItem(@RequestBody ProgrammaticDspLineItemCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertLineItem(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/supply-paths")
    public Mono<R<ProgrammaticDspSupplyPathView>> upsertSupplyPath(
            @RequestBody ProgrammaticDspSupplyPathCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSupplyPath(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/snapshots")
    public Mono<R<ProgrammaticDspSnapshotView>> recordSnapshot(@RequestBody ProgrammaticDspSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/summary")
    public Mono<R<ProgrammaticDspSummaryView>> summary(
            @RequestParam(required = false) Long seatId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long lineItemId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime evaluatedAt) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.summary(tenantId(context), new ProgrammaticDspSummaryQuery(
                                seatId, campaignId, lineItemId, startDate, endDate, evaluatedAt))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations")
    public Mono<R<ProgrammaticDspMutationView>> proposeMutation(
            @RequestBody ProgrammaticDspMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<ProgrammaticDspMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody ProgrammaticDspMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<ProgrammaticDspMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody ProgrammaticDspMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/mutations")
    public Mono<R<java.util.List<ProgrammaticDspMutationView>>> listMutations(
            @RequestParam(required = false) Long seatId,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long lineItemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.list(tenantId(context), new ProgrammaticDspMutationQuery(
                                seatId, campaignId, lineItemId, status, approvalStatus, limit))))
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
}
