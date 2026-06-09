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
/**
 * GrowthActivityController 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
 */
public class GrowthActivityController {

    private final GrowthActivityService service;
    private final GrowthActivityReadinessService readinessService;
    private final GrowthActivityReportService reportService;
    private final GrowthRewardPoolService rewardPoolService;
    private final GrowthRewardGrantService rewardGrantService;
    private final GrowthReferralService referralService;
    private final GrowthTaskService taskService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, null, (GrowthRewardGrantService) null,
                (GrowthReferralService) null, (GrowthTaskService) null, tenantContextResolver);
    }

    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardPoolService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, (GrowthRewardGrantService) null,
                (GrowthReferralService) null, (GrowthTaskService) null, tenantContextResolver);
    }

    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardPoolService 依赖组件，用于完成数据访问或外部能力调用。
     * @param referralService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthReferralService referralService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, null, referralService, null, tenantContextResolver);
    }

    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardPoolService 依赖组件，用于完成数据访问或外部能力调用。
     * @param referralService 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public GrowthActivityController(GrowthActivityService service,
                                    GrowthActivityReadinessService readinessService,
                                    GrowthActivityReportService reportService,
                                    GrowthRewardPoolService rewardPoolService,
                                    GrowthReferralService referralService,
                                    GrowthTaskService taskService,
                                    TenantContextResolver tenantContextResolver) {
        this(service, readinessService, reportService, rewardPoolService, null, referralService, taskService, tenantContextResolver);
    }

    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardPoolService 依赖组件，用于完成数据访问或外部能力调用。
     * @param referralService 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
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
    /**
     * 初始化 GrowthActivityController 实例。
     *
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param readinessService 依赖组件，用于完成数据访问或外部能力调用。
     * @param reportService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardPoolService 依赖组件，用于完成数据访问或外部能力调用。
     * @param rewardGrantService 依赖组件，用于完成数据访问或外部能力调用。
     * @param referralService 依赖组件，用于完成数据访问或外部能力调用。
     * @param taskService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
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
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthActivityView>> upsertActivity(@RequestBody GrowthActivityCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertActivity(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityType 类型标识，用于选择对应处理分支。
     * @param status 业务状态，用于筛选或推进状态流转。
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthActivityView>>> listActivities(
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.listActivities(tenantId(context), activityType, status, limit)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 getActivity 流程生成的业务结果。
     */
    public Mono<R<GrowthActivityView>> getActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.getActivity(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/report")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 getReport 流程生成的业务结果。
     */
    public Mono<R<GrowthActivityReportView>> getReport(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(reportService.summarize(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/readiness")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 getReadiness 流程生成的业务结果。
     */
    public Mono<R<GrowthActivityReadinessView>> getReadiness(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(readinessService.evaluate(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/reward-pools")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthRewardPoolView>>> listRewardPools(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardPoolService().listPools(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/reward-pools")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthRewardPoolView>> upsertRewardPool(@PathVariable Long activityId,
                                                          @RequestBody GrowthRewardPoolCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardPoolService().upsertPool(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/grants")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthRewardGrantView>>> listGrants(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().listGrants(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants")
    /**
     * 创建业务对象并完成必要的初始化。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthRewardGrantView>> createGrant(@PathVariable Long activityId,
                                                      @RequestBody GrowthRewardGrantCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().createGrant(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/retry")
    /**
     * 执行核心业务流程，并协调依赖组件完成处理。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param grantId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthRewardGrantView>> retryGrant(@PathVariable Long activityId,
                                                     @PathVariable Long grantId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().retryGrant(tenantId(context), grantId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/reconcile")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param grantId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 reconcileGrant 流程生成的业务结果。
     */
    public Mono<R<GrowthRewardGrantView>> reconcileGrant(@PathVariable Long activityId,
                                                         @PathVariable Long grantId,
                                                         @RequestBody GrantReconcileRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().reconcileGrant(
                                tenantId(context), grantId, request.providerStatus(), request.providerResponse(), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/grants/{grantId}/cancel")
    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param grantId 业务对象 ID，用于定位具体记录。
     * @return 返回布尔判断结果。
     */
    public Mono<R<GrowthRewardGrantView>> cancelGrant(@PathVariable Long activityId,
                                                      @PathVariable Long grantId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireRewardGrantService().cancelGrant(tenantId(context), grantId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/referral-codes")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthReferralCodeView>>> listReferralCodes(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().listCodes(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referral-codes")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param request 请求对象，承载本次操作的输入参数。
     * @return 返回 generateReferralCode 流程生成的业务结果。
     */
    public Mono<R<GrowthReferralCodeView>> generateReferralCode(@PathVariable Long activityId,
                                                                @RequestBody ReferralCodeRequest request) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().generateCode(
                                tenantId(context), activityId, request.participantId(), actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/referrals")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthReferralRelationView>>> listReferralRelations(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().listRelations(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referrals")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthReferralRelationView>> upsertReferralRelation(@PathVariable Long activityId,
                                                                      @RequestBody GrowthReferralRelationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().upsertRelation(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/referrals/{relationId}/qualify")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param relationId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回 qualifyReferral 流程生成的业务结果。
     */
    public Mono<R<GrowthReferralRelationView>> qualifyReferral(@PathVariable Long activityId,
                                                               @PathVariable Long relationId,
                                                               @RequestBody GrowthReferralQualificationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireReferralService().qualifyRelation(tenantId(context), relationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/tasks")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthTaskDefinitionView>>> listTaskDefinitions(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().listTaskDefinitions(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/tasks")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthTaskDefinitionView>> upsertTaskDefinition(@PathVariable Long activityId,
                                                                  @RequestBody GrowthTaskDefinitionCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().upsertTaskDefinition(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @GetMapping("/{activityId}/task-progress")
    /**
     * 查询并组装符合条件的业务数据。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回符合条件的数据列表或视图。
     */
    public Mono<R<List<GrowthTaskProgressView>>> listTaskProgress(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().listTaskProgress(tenantId(context), activityId)))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/task-progress")
    /**
     * 写入或更新业务数据，并保持关联状态一致。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param command 命令对象，描述本次业务动作及其参数。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthTaskProgressView>> recordTaskProgress(@PathVariable Long activityId,
                                                              @RequestBody GrowthTaskProgressCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().recordProgress(tenantId(context), activityId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/task-progress/{progressId}/reset")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @param progressId 业务对象 ID，用于定位具体记录。
     * @return 返回 resetTaskProgress 流程生成的业务结果。
     */
    public Mono<R<GrowthTaskProgressView>> resetTaskProgress(@PathVariable Long activityId,
                                                             @PathVariable Long progressId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(requireTaskService().resetProgress(tenantId(context), progressId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/publish")
    /**
     * 执行业务决策动作，并同步后续状态。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回流程执行后的业务结果。
     */
    public Mono<R<GrowthActivityView>> publishActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.publishActivity(tenantId(context), activityId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/pause")
    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 pauseActivity 流程生成的业务结果。
     */
    public Mono<R<GrowthActivityView>> pauseActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.pauseActivity(tenantId(context), activityId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @PostMapping("/{activityId}/close")
    /**
     * 清理、停用或释放指定业务资源。
     *
     * @param activityId 业务对象 ID，用于定位具体记录。
     * @return 返回 closeActivity 流程生成的业务结果。
     */
    public Mono<R<GrowthActivityView>> closeActivity(@PathVariable Long activityId) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.closeActivity(tenantId(context), activityId, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 根据方法职责完成对应的业务处理流程。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 requireRewardPoolService 流程生成的业务结果。
     */
    private GrowthRewardPoolService requireRewardPoolService() {
        if (rewardPoolService == null) {
            throw new IllegalStateException("growth reward pool service is not configured");
        }
        return rewardPoolService;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 requireReferralService 流程生成的业务结果。
     */
    private GrowthReferralService requireReferralService() {
        if (referralService == null) {
            throw new IllegalStateException("growth referral service is not configured");
        }
        return referralService;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 requireRewardGrantService 流程生成的业务结果。
     */
    private GrowthRewardGrantService requireRewardGrantService() {
        if (rewardGrantService == null) {
            throw new IllegalStateException("growth reward grant service is not configured");
        }
        return rewardGrantService;
    }

    /**
     * 校验输入、权限或业务前置条件。
     *
     * @return 返回 requireTaskService 流程生成的业务结果。
     */
    private GrowthTaskService requireTaskService() {
        if (taskService == null) {
            throw new IllegalStateException("growth task service is not configured");
        }
        return taskService;
    }

    /**
     * ReferralCodeRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record ReferralCodeRequest(Long participantId) {
    }

    /**
     * GrantReconcileRequest 提供相关 HTTP 接口入口，负责请求校验、身份上下文解析和服务编排。
     */
    public record GrantReconcileRequest(String providerStatus, java.util.Map<String, Object> providerResponse) {
    }
}
