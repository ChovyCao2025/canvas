package org.chovy.canvas.web;

import org.chovy.canvas.common.R;
import org.chovy.canvas.common.tenant.TenantContext;
import org.chovy.canvas.common.tenant.TenantContextResolver;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * ConversationProviderWebhookController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping("/canvas/conversations/provider-webhooks")
public class ConversationProviderWebhookController {

    /**
     * 租户上下文解析器，用于保证接口在当前租户边界内执行。
     */
    private final TenantContextResolver tenantContextResolver;
    /**
     * adapterharness，用于保存请求处理过程中需要的业务数据。
     */
    private final ConversationAdapterHarness adapterHarness;
    /**
     * whatsapp数据访问组件，用于访问和持久化对应数据。
     */
    private final WhatsAppWebhookPayloadMapper whatsAppMapper;

    /**
     * 创建 ConversationProviderWebhookController 实例并注入 web 场景依赖。
     * @param tenantContextResolver 依赖组件，用于完成数据访问、计算或外部能力调用。
     * @param adapterHarness adapter harness 参数，用于 ConversationProviderWebhookController 流程中的校验、计算或对象转换。
     * @param whatsAppMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public ConversationProviderWebhookController(TenantContextResolver tenantContextResolver,
                                                 ConversationAdapterHarness adapterHarness,
                                                 WhatsAppWebhookPayloadMapper whatsAppMapper) {
        this.tenantContextResolver = tenantContextResolver;
        this.adapterHarness = adapterHarness;
        this.whatsAppMapper = whatsAppMapper;
    }
    /**
     * 接收 会话 Provider Webhook 入站数据接口，对应 POST /whatsapp。
     * 接口先解析当前租户上下文，按租户隔离处理数据。
     * 主要委托 tenantContextResolver.currentOrError 完成业务处理。
     * 副作用：会接收入站事件并写入处理结果。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param rawPayload 原始回调载荷。
     * @return 异步返回统一响应，包含列表结果。
     */
    @PostMapping("/whatsapp")
    public Mono<R<List<ConversationIngressResp>>> ingestWhatsApp(@RequestBody Map<String, Object> rawPayload) {
        return tenantContextResolver.currentOrError()
                // 遍历候选数据并按业务规则筛选、转换或聚合。
                .flatMap(context -> Mono.fromCallable(() -> {
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    List<ConversationIngressResp> responses = whatsAppMapper.toAdapterPayloads(rawPayload)
                            .stream()
                            .map(payload -> adapterHarness.ingest(tenantId(context), "WHATSAPP", payload, actor(context)))
                            .toList();
                    // 汇总前面计算出的状态和明细，返回给调用方。
                    return R.ok(responses);
                }).subscribeOn(Schedulers.boundedElastic()));
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
