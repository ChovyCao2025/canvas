package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalRequestCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalReviewCommand;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalService;
import org.chovy.canvas.domain.bi.resource.BiPublishApprovalView;
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
 * BiPublishApprovalController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/resources/publish-approvals")
public class BiPublishApprovalController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * 审批服务，用于承接对应业务能力和领域编排。
     */
    private final BiPublishApprovalService approvalService;

    /**
     * 创建 BiPublishApprovalController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param approvalService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiPublishApprovalController(TenantContextResolver tenantContextResolver,
                                       BiPublishApprovalService approvalService) {
        this.tenantContextResolver = tenantContextResolver;
        this.approvalService = approvalService;
    }
    /**
     * 查询 BI 发布审批列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 approvalService.listApprovals 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数，可选。
     * @param resourceKey resource 唯一键，可选。
     * @param status 状态过滤条件，可选。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("")
    public Mono<R<List<BiPublishApprovalView>>> list(
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceKey,
            @RequestParam(required = false) String status) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.listApprovals(
                                tenantId(context), resourceType, resourceKey, status)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 发布审批 请求接口，对应 POST 请求。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 approvalService.requestApproval 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 发布审批 请求后的业务数据。
     */
    @PostMapping("")
    public Mono<R<BiPublishApprovalView>> requestApproval(
            @RequestBody BiPublishApprovalRequestCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.requestApproval(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 发布审批 请求接口，对应 POST /{approvalId}/review。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 approvalService.reviewApproval 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param approvalId approval ID。
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 发布审批 请求后的业务数据。
     */
    @PostMapping("/{approvalId}/review")
    public Mono<R<BiPublishApprovalView>> reviewApproval(
            @PathVariable Long approvalId,
            @RequestBody BiPublishApprovalReviewCommand command) {
        BiPublishApprovalReviewCommand merged = new BiPublishApprovalReviewCommand(
                approvalId,
                command == null ? null : command.status(),
                command == null ? null : command.reviewComment());
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(approvalService.reviewApproval(
                                tenantId(context), username(context), merged)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    /**
     * 获取当前请求的登录上下文或租户信息。
     *
     * @return 返回 currentTenant 流程生成的业务结果。
     */
    private Mono<TenantContext> currentTenant() {
        return tenantContextResolver.current()
                .switchIfEmpty(Mono.error(new SecurityException("AUTH_003: missing tenant context")));
    }

    /**
     * 解析并规范化租户 ID。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 tenant id 计算得到的数量、金额或指标值。
     */
    private Long tenantId(TenantContext context) {
        return context.tenantId() == null ? 0L : context.tenantId();
    }

    /**
     * 解析操作人标识。
     *
     * @param context 上下文对象，承载租户、身份或运行时信息。
     * @return 返回 username 生成的文本或业务键。
     */
    private String username(TenantContext context) {
        return context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
