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

@RestController
@RequestMapping("/canvas/conversations/provider-webhooks")
public class ConversationProviderWebhookController {

    private final TenantContextResolver tenantContextResolver;
    private final ConversationAdapterHarness adapterHarness;
    private final WhatsAppWebhookPayloadMapper whatsAppMapper;

    public ConversationProviderWebhookController(TenantContextResolver tenantContextResolver,
                                                 ConversationAdapterHarness adapterHarness,
                                                 WhatsAppWebhookPayloadMapper whatsAppMapper) {
        this.tenantContextResolver = tenantContextResolver;
        this.adapterHarness = adapterHarness;
        this.whatsAppMapper = whatsAppMapper;
    }

    @PostMapping("/whatsapp")
    public Mono<R<List<ConversationIngressResp>>> ingestWhatsApp(@RequestBody Map<String, Object> rawPayload) {
        return tenantContextResolver.currentOrError()
                .flatMap(context -> Mono.fromCallable(() -> {
                    List<ConversationIngressResp> responses = whatsAppMapper.toAdapterPayloads(rawPayload)
                            .stream()
                            .map(payload -> adapterHarness.ingest(tenantId(context), "WHATSAPP", payload, actor(context)))
                            .toList();
                    return R.ok(responses);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    private Long tenantId(TenantContext context) {
        return context == null || context.tenantId() == null ? 0L : context.tenantId();
    }

    private String actor(TenantContext context) {
        return context == null || context.username() == null || context.username().isBlank()
                ? "system"
                : context.username();
    }
}
