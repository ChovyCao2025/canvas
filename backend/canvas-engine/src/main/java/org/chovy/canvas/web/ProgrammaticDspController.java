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

/**
 * ProgrammaticDspController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/programmatic-dsp")
public class ProgrammaticDspController {

    /**
     * 服务，用于承接对应业务能力和领域编排。
     */
    private final ProgrammaticDspService service;
    /**
     * mutation服务，用于承接对应业务能力和领域编排。
     */
    private final ProgrammaticDspMutationService mutationService;
    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 ProgrammaticDspController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param mutationService 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ProgrammaticDspController(ProgrammaticDspService service,
                                     ProgrammaticDspMutationService mutationService,
                                     TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.mutationService = mutationService;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 程序化 DSP接口，对应 POST /seats。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 程序化 DSP后的业务数据。
     */
    @PostMapping("/seats")
    public Mono<R<ProgrammaticDspSeatView>> upsertSeat(@RequestBody ProgrammaticDspSeatCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSeat(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 程序化 DSP接口，对应 POST /campaigns。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 程序化 DSP后的业务数据。
     */
    @PostMapping("/campaigns")
    public Mono<R<ProgrammaticDspCampaignView>> upsertCampaign(@RequestBody ProgrammaticDspCampaignCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertCampaign(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 程序化 DSP接口，对应 POST /line-items。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 程序化 DSP后的业务数据。
     */
    @PostMapping("/line-items")
    public Mono<R<ProgrammaticDspLineItemView>> upsertLineItem(@RequestBody ProgrammaticDspLineItemCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertLineItem(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 创建或更新 程序化 DSP接口，对应 POST /supply-paths。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 程序化 DSP后的业务数据。
     */
    @PostMapping("/supply-paths")
    public Mono<R<ProgrammaticDspSupplyPathView>> upsertSupplyPath(
            @RequestBody ProgrammaticDspSupplyPathCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertSupplyPath(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 记录程序化 DSP数据接口，对应 POST /snapshots。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会记录业务快照。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含记录程序化 DSP数据后的业务数据。
     */
    @PostMapping("/snapshots")
    public Mono<R<ProgrammaticDspSnapshotView>> recordSnapshot(@RequestBody ProgrammaticDspSnapshotCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.recordSnapshot(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询程序化 DSP汇总接口，对应 GET /summary。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param seatId seat ID，可选。
     * @param campaignId campaign ID，可选。
     * @param lineItemId line Item ID，可选。
     * @param startDate 请求参数，可选。
     * @param endDate 请求参数，可选。
     * @param evaluatedAt 请求参数，可选。
     * @return 异步返回统一响应，包含查询程序化 DSP汇总后的业务数据。
     */
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
    /**
     * 处理 程序化 DSP 请求接口，对应 POST /mutations。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.propose 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 程序化 DSP 请求后的业务数据。
     */
    @PostMapping("/mutations")
    public Mono<R<ProgrammaticDspMutationView>> proposeMutation(
            @RequestBody ProgrammaticDspMutationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.propose(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 审批通过 程序化 DSP接口，对应 POST /mutations/{mutationId}/approve。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.approve 完成业务处理。
     * 副作用：会推进审批状态。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param mutationId 变更 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含审批通过 程序化 DSP后的业务数据。
     */
    @PostMapping("/mutations/{mutationId}/approve")
    public Mono<R<ProgrammaticDspMutationView>> approveMutation(
            @PathVariable Long mutationId,
            @RequestBody ProgrammaticDspMutationApprovalCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.approve(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 执行程序化 DSP查询接口，对应 POST /mutations/{mutationId}/execute。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 mutationService.execute 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param mutationId 变更 ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含执行程序化 DSP查询后的业务数据。
     */
    @PostMapping("/mutations/{mutationId}/execute")
    public Mono<R<ProgrammaticDspMutationView>> executeMutation(
            @PathVariable Long mutationId,
            @RequestBody ProgrammaticDspMutationExecuteCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(mutationService.execute(tenantId(context), mutationId, command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询程序化 DSP列表接口，对应 GET /mutations。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 mutationService.list 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param seatId seat ID，可选。
     * @param campaignId campaign ID，可选。
     * @param lineItemId line Item ID，可选。
     * @param status 状态过滤条件，可选。
     * @param approvalStatus 请求参数，可选。
     * @param limit 返回数量上限，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
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
