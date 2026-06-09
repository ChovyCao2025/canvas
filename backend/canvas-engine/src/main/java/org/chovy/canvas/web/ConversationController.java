package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressReq;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.ConversationIngressService;
import org.chovy.canvas.domain.conversation.ConversationMessageView;
import org.chovy.canvas.domain.conversation.ConversationSessionView;
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
import java.util.Map;

/**
 * ConversationController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/conversations")
public class ConversationController {

    private final ConversationIngressService service;
    private final TenantContextResolver tenantContextResolver;
    private final ConversationAdapterHarness adapterHarness;

    /**
     * 创建 ConversationController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     */
    public ConversationController(ConversationIngressService service,
                                  TenantContextResolver tenantContextResolver) {
        this(service, tenantContextResolver, null);
    }

    /**
     * 创建 ConversationController 实例并注入 web 场景依赖。
     * @param service 依赖组件，用于完成数据访问或外部能力调用。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param adapterHarness adapter harness 参数，用于 ConversationController 流程中的校验、计算或对象转换。
     */
    @Autowired
    public ConversationController(ConversationIngressService service,
                                  TenantContextResolver tenantContextResolver,
                                  ConversationAdapterHarness adapterHarness) {
        this.service = service;
        this.tenantContextResolver = tenantContextResolver;
        this.adapterHarness = adapterHarness;
    }
    /**
     * 接收 会话 入站数据接口，对应 POST /ingress。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会接收入站事件并写入处理结果。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param req 请求体。
     * @return 异步返回统一响应，包含接收 会话 入站数据后的业务数据。
     */
    @PostMapping("/ingress")
    public Mono<R<ConversationIngressResp>> ingest(@RequestBody ConversationIngressReq req) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.ingest(tenantId(context), req)))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 接收 会话 入站数据接口，对应 POST /adapters/{adapterKey}/ingress。
     * 接口先解析当前租户上下文，并把操作人和角色传入服务层执行权限校验。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会接收入站事件并写入处理结果。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param adapterKey adapter 唯一键。
     * @param payload 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含接收 会话 入站数据后的业务数据。
     */
    @PostMapping("/adapters/{adapterKey}/ingress")
    public Mono<R<ConversationIngressResp>> ingestAdapter(@PathVariable String adapterKey,
                                                          @RequestBody Map<String, Object> payload) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(requireAdapterHarness().ingestRaw(
                                tenantId(context),
                                adapterKey,
                                payload,
                                context.username())))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 查询会话列表接口，对应 GET 请求。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param userId user ID，可选。
     * @param channel 渠道过滤条件，可选。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping
    public Mono<R<List<ConversationSessionView>>> list(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String channel,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.listRecentSessions(
                                tenantId(context),
                                userId,
                                channel,
                                boundedLimit(limit))))
                        .subscribeOn(Schedulers.boundedElastic()));
    }
    /**
     * 处理 会话 请求接口，对应 GET /{sessionId}/messages。
     * 接口先解析当前租户上下文，按租户隔离读取数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param sessionId session ID。
     * @param limit 返回数量上限，默认值为 50。
     * @return 异步返回统一响应，包含列表结果。
     */
    @GetMapping("/{sessionId}/messages")
    public Mono<R<List<ConversationMessageView>>> messages(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> R.ok(service.listMessages(
                                tenantId(context),
                                sessionId,
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

    /**
     * 校验并获取必需参数、资源或权限。
     *
     * @return 返回 requireAdapterHarness 流程生成的业务结果。
     */
    private ConversationAdapterHarness requireAdapterHarness() {
        if (adapterHarness == null) {
            throw new IllegalStateException("conversation adapter harness is required");
        }
        return adapterHarness;
    }
}
