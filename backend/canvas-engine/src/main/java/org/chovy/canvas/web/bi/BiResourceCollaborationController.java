package org.chovy.canvas.web.bi;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.bi.resource.BiResourceCollaborationService;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceCommentView;
import org.chovy.canvas.domain.bi.resource.BiResourceLockCommand;
import org.chovy.canvas.domain.bi.resource.BiResourceLockView;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * BiResourceCollaborationController 暴露 web.bi 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/bi/resources")
public class BiResourceCollaborationController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * collaboration服务，用于承接对应业务能力和领域编排。
     */
    private final BiResourceCollaborationService collaborationService;

    /**
     * 创建 BiResourceCollaborationController 实例并注入 web.bi 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param collaborationService 依赖组件，用于完成数据访问或外部能力调用。
     */
    public BiResourceCollaborationController(TenantContextResolver tenantContextResolver,
                                             BiResourceCollaborationService collaborationService) {
        this.tenantContextResolver = tenantContextResolver;
        this.collaborationService = collaborationService;
    }
    /**
     * 处理 BI 资源协作 请求接口，对应 POST /comments。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 collaborationService.addComment 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 资源协作 请求后的业务数据。
     */
    @PostMapping("/comments")
    public Mono<R<BiResourceCommentView>> addComment(@RequestBody BiResourceCommentCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.addComment(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询 BI 资源协作列表接口，对应 GET /comments。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 collaborationService.listComments 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数。
     * @param resourceKey resource 唯一键。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/comments")
    public Mono<R<List<BiResourceCommentView>>> listComments(
            @RequestParam String resourceType,
            @RequestParam String resourceKey) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.listComments(
                                tenantId(context), resourceType, resourceKey)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 删除 BI 资源协作接口，对应 DELETE /comments/{commentId}。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 collaborationService.deleteComment 完成业务处理。
     * 副作用：会删除或逻辑移除记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param commentId 评论 ID。
     * @return 异步返回统一响应，表示操作完成。
     */
    @DeleteMapping("/comments/{commentId}")
    public Mono<R<Void>> deleteComment(@PathVariable Long commentId) {
        return currentTenant().flatMap(context ->
                Mono.fromRunnable(() -> collaborationService.deleteComment(
                                tenantId(context), username(context), commentId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
    }
    /**
     * 处理 BI 资源协作 请求接口，对应 POST /locks/acquire。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 collaborationService.acquireLock 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含处理 BI 资源协作 请求后的业务数据。
     */
    @PostMapping("/locks/acquire")
    public Mono<R<BiResourceLockView>> acquireLock(@RequestBody BiResourceLockCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.acquireLock(
                                tenantId(context), username(context), command)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 资源协作 请求接口，对应 GET /locks。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 collaborationService.currentLock 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param resourceType 请求参数。
     * @param resourceKey resource 唯一键。
     * @return 异步返回统一响应，包含处理 BI 资源协作 请求后的业务数据。
     */
    @GetMapping("/locks")
    public Mono<R<BiResourceLockView>> currentLock(
            @RequestParam String resourceType,
            @RequestParam String resourceKey) {
        return currentTenant().flatMap(context ->
                Mono.fromCallable(() -> R.ok(collaborationService.currentLock(
                                tenantId(context), resourceType, resourceKey)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 BI 资源协作 请求接口，对应 POST /locks/release。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 collaborationService.releaseLock 完成业务处理。
     * 副作用由下游服务封装，通常会写入状态、审计或任务记录。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，表示操作完成。
     */
    @PostMapping("/locks/release")
    public Mono<R<Void>> releaseLock(@RequestBody BiResourceLockCommand command) {
        return currentTenant().flatMap(context ->
                Mono.fromRunnable(() -> collaborationService.releaseLock(
                                tenantId(context), username(context), command))
                        .subscribeOn(Schedulers.boundedElastic())
                        .thenReturn(R.ok()));
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
