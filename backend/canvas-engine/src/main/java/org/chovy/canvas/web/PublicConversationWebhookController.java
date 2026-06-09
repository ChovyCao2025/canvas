package org.chovy.canvas.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chovy.canvas.common.R;
import org.chovy.canvas.domain.conversation.ConversationAdapterHarness;
import org.chovy.canvas.domain.conversation.ConversationIngressResp;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookPayloadMapper;
import org.chovy.canvas.domain.conversation.WhatsAppWebhookSecurityService;
import org.chovy.canvas.engine.delivery.DeliveryOutboxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

/**
 * PublicConversationWebhookController 暴露 web 场景的 HTTP 接口。
 */
@RestController
@RequestMapping({"/public/conversation-webhooks", "/public/conversations/webhooks"})
public class PublicConversationWebhookController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ConversationAdapterHarness adapterHarness;
    private final WhatsAppWebhookPayloadMapper whatsAppMapper;
    private final WhatsAppWebhookSecurityService securityService;
    private final ObjectMapper objectMapper;
    private final DeliveryOutboxService outboxService;

    /**
     * 创建 PublicConversationWebhookController 实例并注入 web 场景依赖。
     * @param adapterHarness adapter harness 参数，用于 PublicConversationWebhookController 流程中的校验、计算或对象转换。
     * @param whatsAppMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param securityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     */
    public PublicConversationWebhookController(ConversationAdapterHarness adapterHarness,
                                               WhatsAppWebhookPayloadMapper whatsAppMapper,
                                               WhatsAppWebhookSecurityService securityService,
                                               ObjectMapper objectMapper) {
        this(adapterHarness, whatsAppMapper, securityService, objectMapper, null);
    }

    /**
     * 创建 PublicConversationWebhookController 实例并注入 web 场景依赖。
     * @param adapterHarness adapter harness 参数，用于 PublicConversationWebhookController 流程中的校验、计算或对象转换。
     * @param whatsAppMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param securityService 依赖组件，用于完成数据访问或外部能力调用。
     * @param objectMapper 依赖组件，用于完成数据访问或外部能力调用。
     * @param outboxService 依赖组件，用于完成数据访问或外部能力调用。
     */
    @Autowired
    public PublicConversationWebhookController(ConversationAdapterHarness adapterHarness,
                                               WhatsAppWebhookPayloadMapper whatsAppMapper,
                                               WhatsAppWebhookSecurityService securityService,
                                               ObjectMapper objectMapper,
                                               DeliveryOutboxService outboxService) {
        this.adapterHarness = adapterHarness;
        this.whatsAppMapper = whatsAppMapper;
        this.securityService = securityService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.outboxService = outboxService;
    }
    /**
     * 校验公开会话 Webhook回调接口，对应 GET /{tenantId}/whatsapp。
     * 接口按路径租户 ID 定位回调配置，签名、令牌或幂等控制由服务层完成。
     * 主要委托 securityService.verifyChallenge 完成业务处理。
     * 该接口只读取数据，不主动触发业务写入。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tenantId 租户 ID。
     * @param mode 请求参数，可选。
     * @param verifyToken 请求参数，可选。
     * @param challenge 请求参数，可选。
     * @return 异步返回文本响应。
     */
    @GetMapping("/{tenantId}/whatsapp")
    public Mono<ResponseEntity<String>> verifyWhatsApp(
            @PathVariable Long tenantId,
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        return Mono.fromCallable(() -> ResponseEntity.ok(
                        securityService.verifyChallenge(mode, verifyToken, challenge)))
                .subscribeOn(Schedulers.boundedElastic());
    }
    /**
     * 接收公开会话 Webhook回调接口，对应 POST /{tenantId}/whatsapp。
     * 接口按路径租户 ID 定位回调配置，签名、令牌或幂等控制由服务层完成。
     * 主要委托 securityService.verifySignature 完成业务处理。
     * 副作用：会处理外部回调载荷。
     * 阻塞型服务调用被包在 Mono 中，并调度到 boundedElastic 线程池执行。
     *
     * @param tenantId 租户 ID。
     * @param signature 请求头参数，可选。
     * @param rawBody 请求体，包含本次操作所需字段。
     * @return 异步返回统一响应，包含列表结果。
     */
    @PostMapping("/{tenantId}/whatsapp")
    public Mono<R<List<ConversationIngressResp>>> receiveWhatsApp(
            @PathVariable Long tenantId,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        return Mono.fromCallable(() -> {
                    securityService.verifySignature(rawBody, signature);
                    // 访问持久化或外部依赖，获取或写入本次流程需要的数据。
                    Map<String, Object> payload = objectMapper.readValue(rawBody, MAP_TYPE);
                    // 校验关键输入和前置条件，避免无效状态继续进入主流程。
                    if (outboxService != null) {
                        // 遍历候选数据并按业务规则筛选、转换或聚合。
                        whatsAppMapper.toDeliveryReceipts(payload).forEach(outboxService::recordReceipt);
                    }
                    List<ConversationIngressResp> responses = whatsAppMapper.toAdapterPayloads(payload)
                            .stream()
                            .map(adapterPayload -> adapterHarness.ingest(
                                    tenantId == null ? 0L : tenantId,
                                    "WHATSAPP",
                                    adapterPayload,
                                    "whatsapp-webhook"))
                            .toList();
                    return R.ok(responses);
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
