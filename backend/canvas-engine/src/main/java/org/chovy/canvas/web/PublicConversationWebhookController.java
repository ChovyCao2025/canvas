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

    public PublicConversationWebhookController(ConversationAdapterHarness adapterHarness,
                                               WhatsAppWebhookPayloadMapper whatsAppMapper,
                                               WhatsAppWebhookSecurityService securityService,
                                               ObjectMapper objectMapper) {
        this(adapterHarness, whatsAppMapper, securityService, objectMapper, null);
    }

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

    @PostMapping("/{tenantId}/whatsapp")
    public Mono<R<List<ConversationIngressResp>>> receiveWhatsApp(
            @PathVariable Long tenantId,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawBody) {
        return Mono.fromCallable(() -> {
                    securityService.verifySignature(rawBody, signature);
                    Map<String, Object> payload = objectMapper.readValue(rawBody, MAP_TYPE);
                    if (outboxService != null) {
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
