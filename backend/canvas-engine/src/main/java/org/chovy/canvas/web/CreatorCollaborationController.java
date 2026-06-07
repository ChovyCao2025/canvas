package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.creator.CreatorCampaignCommand;
import org.chovy.canvas.domain.creator.CreatorCampaignView;
import org.chovy.canvas.domain.creator.CreatorCollaborationCommand;
import org.chovy.canvas.domain.creator.CreatorCollaborationService;
import org.chovy.canvas.domain.creator.CreatorCollaborationView;
import org.chovy.canvas.domain.creator.CreatorDeliverableCommand;
import org.chovy.canvas.domain.creator.CreatorDeliverableView;
import org.chovy.canvas.domain.creator.CreatorPerformanceSummaryQuery;
import org.chovy.canvas.domain.creator.CreatorPerformanceSummaryView;
import org.chovy.canvas.domain.creator.CreatorProfileCommand;
import org.chovy.canvas.domain.creator.CreatorProfileView;
import org.chovy.canvas.domain.creator.CreatorProviderMutationApprovalCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationExecuteCommand;
import org.chovy.canvas.domain.creator.CreatorProviderMutationQuery;
import org.chovy.canvas.domain.creator.CreatorProviderMutationService;
import org.chovy.canvas.domain.creator.CreatorProviderMutationView;
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

import java.time.LocalDateTime;

@RestController
@RequestMapping("/canvas/creator-collaboration")
public class CreatorCollaborationController {

    private final CreatorCollaborationService service;
    private final CreatorProviderMutationService mutationService;
    private final TenantContextResolver tenantContextResolver;

    public CreatorCollaborationController(CreatorCollaborationService service,
                                          CreatorProviderMutationService mutationService,
                                          TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.mutationService = mutationService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping("/creators")
    public Mono<R<CreatorProfileView>> upsertCreator(@RequestBody CreatorProfileCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCreator(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/campaigns")
    public Mono<R<CreatorCampaignView>> upsertCampaign(@RequestBody CreatorCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/collaborations")
    public Mono<R<CreatorCollaborationView>> upsertCollaboration(
            @RequestBody CreatorCollaborationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCollaboration(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/deliverables")
    public Mono<R<CreatorDeliverableView>> upsertDeliverable(@RequestBody CreatorDeliverableCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertDeliverable(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations")
    public Mono<R<CreatorProviderMutationView>> proposeMutation(@RequestBody CreatorProviderMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<CreatorProviderMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody CreatorProviderMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<CreatorProviderMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody CreatorProviderMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/mutations")
    public Mono<R<java.util.List<CreatorProviderMutationView>>> listMutations(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long collaborationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String approvalStatus,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.list(tenantId(context),
                                new CreatorProviderMutationQuery(campaignId, collaborationId, status,
                                        approvalStatus, limit))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/summary")
    public Mono<R<CreatorPerformanceSummaryView>> summary(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long creatorId,
            @RequestParam(required = false) Long collaborationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime evaluatedAt) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.summary(tenantId(context),
                                new CreatorPerformanceSummaryQuery(campaignId, creatorId, collaborationId,
                                        evaluatedAt))))
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
