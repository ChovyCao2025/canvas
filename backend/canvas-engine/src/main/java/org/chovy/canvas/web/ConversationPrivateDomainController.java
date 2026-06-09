package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationPrivateDomainSyncService;
import org.chovy.canvas.domain.conversation.PrivateDomainContactQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainContactView;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupQuery;
import org.chovy.canvas.domain.conversation.PrivateDomainGroupView;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncCommand;
import org.chovy.canvas.domain.conversation.PrivateDomainSyncRunView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * ConversationPrivateDomainController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/conversations/private-domain")
public class ConversationPrivateDomainController {

    private final ConversationPrivateDomainSyncService service;
    private final TenantContextResolver tenantContextResolver;

    /**
     * 创建 ConversationPrivateDomainController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ConversationPrivateDomainController(ConversationPrivateDomainSyncService service,
                                               TenantContextResolver tenantContextResolver) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
    }
    /**
     * 接收 会话 Private Domain 入站数据接口，对应 POST /sync-runs。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会接收入站事件并写入处理结果。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param command 命令请求体。
     * @return 异步返回统一响应，包含接收 会话 Private Domain 入站数据后的业务数据。
     */
    @PostMapping("/sync-runs")
    public Mono<R<PrivateDomainSyncRunView>> ingest(@RequestBody PrivateDomainSyncCommand command) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.ingestSnapshot(
                                tenantId(context),
                                command,
                                actor(context))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Private Domain 请求接口，对应 GET /contacts。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param provider 供应商过滤条件。
     * @param ownerUserId owner User ID，可选。
     * @param keyword 搜索关键字，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/contacts")
    public Mono<R<List<PrivateDomainContactView>>> contacts(
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.contacts(
                                tenantId(context),
                                new PrivateDomainContactQuery(provider, ownerUserId, keyword, boundedLimit(limit)))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 Private Domain 请求接口，对应 GET /groups。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param provider 供应商过滤条件。
     * @param ownerUserId owner User ID，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/groups")
    public Mono<R<List<PrivateDomainGroupView>>> groups(
            @RequestParam String provider,
            @RequestParam(required = false) String ownerUserId,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.groups(
                                tenantId(context),
                                new PrivateDomainGroupQuery(provider, ownerUserId, boundedLimit(limit)))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 触发会话 Private Domain运行接口，对应 GET /sync-runs。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param provider 供应商过滤条件。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/sync-runs")
    public Mono<R<List<PrivateDomainSyncRunView>>> syncRuns(
            @RequestParam String provider,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.syncRuns(
                                tenantId(context),
                                provider,
                                boundedLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
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
