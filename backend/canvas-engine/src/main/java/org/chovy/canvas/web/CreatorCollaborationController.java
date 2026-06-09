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

/**
 * CreatorCollaborationController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/creator-collaboration")
public class CreatorCollaborationController {

    private final CreatorCollaborationService service;
    private final CreatorProviderMutationService mutationService;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 CreatorCollaborationController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public CreatorCollaborationController(CreatorCollaborationService service,
                                          CreatorProviderMutationService mutationService,
                                          TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.mutationService = mutationService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 达人协作接口，对应 POST /creators。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 达人协作后的业务数据。
     */
    @PostMapping("/creators")
    public Mono<R<CreatorProfileView>> upsertCreator(@RequestBody CreatorProfileCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCreator(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 达人协作接口，对应 POST /campaigns。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 达人协作后的业务数据。
     */
    @PostMapping("/campaigns")
    public Mono<R<CreatorCampaignView>> upsertCampaign(@RequestBody CreatorCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 达人协作接口，对应 POST /collaborations。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 达人协作后的业务数据。
     */
    @PostMapping("/collaborations")
    public Mono<R<CreatorCollaborationView>> upsertCollaboration(
            @RequestBody CreatorCollaborationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCollaboration(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 达人协作接口，对应 POST /deliverables。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 达人协作后的业务数据。
     */
    @PostMapping("/deliverables")
    public Mono<R<CreatorDeliverableView>> upsertDeliverable(@RequestBody CreatorDeliverableCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertDeliverable(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 达人协作 请求接口，对应 POST /mutations。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.propose 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 达人协作 请求后的业务数据。
     */
    @PostMapping("/mutations")
    public Mono<R<CreatorProviderMutationView>> proposeMutation(@RequestBody CreatorProviderMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 审批通过 达人协作接口，对应 POST /mutations/{mutationId}/approve。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.approve 完成业务处理。
     * 副作用：会推进审批状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param mutationId 变更 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含审批通过 达人协作后的业务数据。
     */
    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<CreatorProviderMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody CreatorProviderMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 执行达人协作查询接口，对应 POST /mutations/{mutationId}/execute。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.execute 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param mutationId 变更 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含执行达人协作查询后的业务数据。
     */
    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<CreatorProviderMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody CreatorProviderMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询达人协作列表接口，对应 GET /mutations。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 mutationService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param campaignId campaign ID，可选。
     * @param collaborationId collaboration ID，可选。
     * @param status 状态过滤条件，可选。
     * @param approvalStatus 请求参数，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
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
    /**
     * 查询达人协作汇总接口，对应 GET /summary。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param campaignId campaign ID，可选。
     * @param creatorId creator ID，可选。
     * @param collaborationId collaboration ID，可选。
     * @param evaluatedAt 请求参数，可选。
     * @return 异步返回统一响应，包含查询达人协作汇总后的业务数据。
     */
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

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.currentOrError();
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 actor 生成的文本或业务键。
     */
    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
