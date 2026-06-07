package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.marketing.GrowthActivityCommand;
import org.chovy.canvas.domain.marketing.GrowthActivityReadinessService;
import org.chovy.canvas.domain.marketing.GrowthActivityReadinessView;
import org.chovy.canvas.domain.marketing.GrowthActivityReportService;
import org.chovy.canvas.domain.marketing.GrowthActivityReportView;
import org.chovy.canvas.domain.marketing.GrowthActivityService;
import org.chovy.canvas.domain.marketing.GrowthActivityView;
import org.chovy.canvas.domain.marketing.GrowthReferralCodeView;
import org.chovy.canvas.domain.marketing.GrowthReferralQualificationCommand;
import org.chovy.canvas.domain.marketing.GrowthReferralRelationCommand;
import org.chovy.canvas.domain.marketing.GrowthReferralRelationView;
import org.chovy.canvas.domain.marketing.GrowthReferralService;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantCommand;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantService;
import org.chovy.canvas.domain.marketing.GrowthRewardGrantView;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolCommand;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolService;
import org.chovy.canvas.domain.marketing.GrowthRewardPoolView;
import org.chovy.canvas.domain.marketing.GrowthTaskDefinitionCommand;
import org.chovy.canvas.domain.marketing.GrowthTaskDefinitionView;
import org.chovy.canvas.domain.marketing.GrowthTaskProgressCommand;
import org.chovy.canvas.domain.marketing.GrowthTaskProgressView;
import org.chovy.canvas.domain.marketing.GrowthTaskService;
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
@RequestMapping("/canvas/growth-activities")
public class GrowthActivityController {

    private final GrowthActivityService service;
    private final GrowthActivityReadinessService readinessService;
    private final GrowthActivityReportService reportService;
    private final GrowthRewardPoolService rewardPoolService;
    private final GrowthRewardGrantService rewardGrantService;
    private final GrowthReferralService referralService;
    private final GrowthTaskService taskService;
    private final TenantContextResolver tenantContextResolver;

    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, null, (GrowthRewardGrantService) null,
                (GrowthReferralService) null, (GrowthTaskService) null, tenantContextResolver);
    }

    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, (GrowthRewardGrantService) null,
                (GrowthReferralService) null, (GrowthTaskService) null, tenantContextResolver);
    }

    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthReferralService referralService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, null, referralService, null, tenantContextResolver);
    }

    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthReferralService referralService,
                                    GrowthTaskService taskService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, null, referralService, taskService, tenantContextResolver);
    }

    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthReferralService referralService,
                                    GrowthTaskService taskService,
                                    GrowthRewardGrantService rewardGrantService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, rewardGrantService, referralService, taskService, tenantContextResolver);
    }

    @Autowired
    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthRewardGrantService rewardGrantService,
                                    GrowthReferralService referralService,
                                    GrowthTaskService taskService,
                                    TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.readinessService = readinessService;
        this.reportService = reportService;
        this.rewardPoolService = rewardPoolService;
        this.rewardGrantService = rewardGrantService;
        this.referralService = referralService;
        this.taskService = taskService;
        this.tenantContextResolver = tenantContextResolver;
    }

    @PostMapping
    public Mono<R<GrowthActivityView>> upsertActivity(@RequestBody GrowthActivityCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertActivity(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    public Mono<R<List<GrowthActivityView>>> listActivities(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listActivities(tenantId(context), activityType, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}")
    public Mono<R<GrowthActivityView>> getActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.getActivity(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/report")
    public Mono<R<GrowthActivityReportView>> getReport(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(reportService.summarize(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/readiness")
    public Mono<R<GrowthActivityReadinessView>> getReadiness(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(readinessService.evaluate(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/reward-pools")
    public Mono<R<List<GrowthRewardPoolView>>> listRewardPools(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardPoolService().listPools(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/reward-pools")
    public Mono<R<GrowthRewardPoolView>> upsertRewardPool(@PathVariable Long activityId,
                                                          @RequestBody GrowthRewardPoolCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardPoolService().upsertPool(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/grants")
    public Mono<R<List<GrowthRewardGrantView>>> listGrants(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().listGrants(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants")
    public Mono<R<GrowthRewardGrantView>> createGrant(@PathVariable Long activityId,
                                                      @RequestBody GrowthRewardGrantCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().createGrant(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/retry")
    public Mono<R<GrowthRewardGrantView>> retryGrant(@PathVariable Long activityId,
                                                     @PathVariable Long grantId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().retryGrant(tenantId(context), grantId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/reconcile")
    public Mono<R<GrowthRewardGrantView>> reconcileGrant(@PathVariable Long activityId,
                                                         @PathVariable Long grantId,
                                                         @RequestBody GrantReconcileRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().reconcileGrant(
                                tenantId(context), grantId, request.providerStatus(), request.providerResponse(), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/cancel")
    public Mono<R<GrowthRewardGrantView>> cancelGrant(@PathVariable Long activityId,
                                                      @PathVariable Long grantId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().cancelGrant(tenantId(context), grantId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/referral-codes")
    public Mono<R<List<GrowthReferralCodeView>>> listReferralCodes(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().listCodes(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referral-codes")
    public Mono<R<GrowthReferralCodeView>> generateReferralCode(@PathVariable Long activityId,
                                                                @RequestBody ReferralCodeRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().generateCode(
                                tenantId(context), activityId, request.participantId(), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/referrals")
    public Mono<R<List<GrowthReferralRelationView>>> listReferralRelations(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().listRelations(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referrals")
    public Mono<R<GrowthReferralRelationView>> upsertReferralRelation(@PathVariable Long activityId,
                                                                      @RequestBody GrowthReferralRelationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().upsertRelation(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referrals/{relationId}/qualify")
    public Mono<R<GrowthReferralRelationView>> qualifyReferral(@PathVariable Long activityId,
                                                               @PathVariable Long relationId,
                                                               @RequestBody GrowthReferralQualificationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().qualifyRelation(tenantId(context), relationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/tasks")
    public Mono<R<List<GrowthTaskDefinitionView>>> listTaskDefinitions(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().listTaskDefinitions(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/tasks")
    public Mono<R<GrowthTaskDefinitionView>> upsertTaskDefinition(@PathVariable Long activityId,
                                                                  @RequestBody GrowthTaskDefinitionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().upsertTaskDefinition(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/task-progress")
    public Mono<R<List<GrowthTaskProgressView>>> listTaskProgress(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().listTaskProgress(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/task-progress")
    public Mono<R<GrowthTaskProgressView>> recordTaskProgress(@PathVariable Long activityId,
                                                              @RequestBody GrowthTaskProgressCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().recordProgress(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/task-progress/{progressId}/reset")
    public Mono<R<GrowthTaskProgressView>> resetTaskProgress(@PathVariable Long activityId,
                                                             @PathVariable Long progressId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().resetProgress(tenantId(context), progressId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/publish")
    public Mono<R<GrowthActivityView>> publishActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.publishActivity(tenantId(context), activityId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/pause")
    public Mono<R<GrowthActivityView>> pauseActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.pauseActivity(tenantId(context), activityId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/close")
    public Mono<R<GrowthActivityView>> closeActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.closeActivity(tenantId(context), activityId, actor(context))))
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

    private GrowthRewardPoolService requireRewardPoolService() {
        if (rewardPoolService == null) {
            throw new IllegalStateException("growth reward pool service is not configured");
        }
        return rewardPoolService;
    }

    private GrowthReferralService requireReferralService() {
        if (referralService == null) {
            throw new IllegalStateException("growth referral service is not configured");
        }
        return referralService;
    }

    private GrowthRewardGrantService requireRewardGrantService() {
        if (rewardGrantService == null) {
            throw new IllegalStateException("growth reward grant service is not configured");
        }
        return rewardGrantService;
    }

    private GrowthTaskService requireTaskService() {
        if (taskService == null) {
            throw new IllegalStateException("growth task service is not configured");
        }
        return taskService;
    }

    public record ReferralCodeRequest(Long participantId) {
    }

    public record GrantReconcileRequest(String providerStatus, java.util.Map<String, Object> providerResponse) {
    }
}
