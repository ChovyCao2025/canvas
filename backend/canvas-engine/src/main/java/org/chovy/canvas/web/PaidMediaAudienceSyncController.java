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

/**
 * PaidMediaAudienceSyncController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/paid-media/audience-sync")
public class PaidMediaAudienceSyncController {

    private final PaidMediaAudienceSyncService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 PaidMediaAudienceSyncController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public PaidMediaAudienceSyncController(PaidMediaAudienceSyncService service,
                                           TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 创建或更新 付费媒体受众同步接口，对应 POST /destinations。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会新增或覆盖已有配置。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含创建或更新 付费媒体受众同步后的业务数据。
     */
    @PostMapping("/destinations")
    public Mono<R<PaidMediaAudienceDestinationView>> upsertDestination(
            @RequestBody PaidMediaDestinationCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.upsertDestination(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 同步 付费媒体受众同步接口，对应 POST /runs。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 副作用：会触发同步流程。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含同步 付费媒体受众同步后的业务数据。
     */
    @PostMapping("/runs")
    public Mono<R<PaidMediaAudienceSyncRunView>> syncAudience(
            @RequestBody PaidMediaAudienceSyncCommand command) {
        return currentTenant().flatMap(context -> Mono.fromCallable(() ->
                        R.ok(service.syncAudience(tenantId(context), command, actor(context))))
                .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发付费媒体受众同步运行接口，对应 GET /runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param destinationId destination ID，可选。
     * @param audienceId audience ID，可选。
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
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
    /**
     * 处理 付费媒体受众同步 请求接口，对应 GET /runs/{runId}/members。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param runId run ID。
     * @param status 状态过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
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

    /**
     * 按安全边界裁剪或保护输入值。
     *
     * @param limit 分页或数量限制，避免一次处理过多数据。
     * @return 返回解析、归一化或安全处理后的值。
     */
    private int boundedLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, 100);
    }
}
