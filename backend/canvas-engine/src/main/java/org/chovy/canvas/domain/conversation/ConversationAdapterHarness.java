package org.chovy.canvas.domain.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ConversationAdapterHarness {

    private final ConversationIngressService ingressService;
    private final ConversationAdapterCatalog adapterCatalog;
    private final ObjectMapper objectMapper;

    public ConversationAdapterHarness(ConversationIngressService ingressService) {
        this(ingressService, null, new ObjectMapper());
    }

    public ConversationAdapterHarness(ConversationIngressService ingressService,
                                      ConversationAdapterCatalog adapterCatalog) {
        this(ingressService, adapterCatalog, new ObjectMapper());
    }

    @Autowired
    public ConversationAdapterHarness(ConversationIngressService ingressService,
                                      ConversationAdapterCatalog adapterCatalog,
                                      ObjectMapper objectMapper) {
        this.ingressService = ingressService;
        this.adapterCatalog = adapterCatalog;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public <T> ConversationIngressResp ingest(Long tenantId,
                                              ConversationReplyAdapter<T> adapter,
                                              T payload,
                                              String operator) {
        if (adapter == null) {
            throw new IllegalArgumentException("conversation reply adapter is required");
        }
        ConversationIngressReq req = adapter.toIngress(payload, new ConversationAdapterContext(tenantId, operator));
        validate(req);
        return ingressService.ingest(tenantId, req);
    }

    public <T> ConversationIngressResp ingest(Long tenantId,
                                              String adapterKey,
                                              T payload,
                                              String operator) {
        if (adapterCatalog == null) {
            throw new IllegalStateException("conversation adapter catalog is required for key-based ingest");
        }
        return ingest(tenantId, adapterCatalog.require(adapterKey), payload, operator);
    }

    public ConversationIngressResp ingestRaw(Long tenantId,
                                             String adapterKey,
                                             Map<String, Object> rawPayload,
                                             String operator) {
        if (adapterCatalog == null) {
            throw new IllegalStateException("conversation adapter catalog is required for raw payload ingest");
        }
        ConversationReplyAdapter<Object> adapter = adapterCatalog.require(adapterKey);
        Object payload = objectMapper.convertValue(rawPayload == null ? Map.of() : rawPayload, adapter.payloadType());
        return ingest(tenantId, adapter, payload, operator);
    }

    private static void validate(ConversationIngressReq req) {
        if (req == null) {
            throw new IllegalArgumentException("conversation adapter must produce an ingress request");
        }
        if (isBlank(req.userId()) || isBlank(req.channel())) {
            throw new IllegalArgumentException("conversation adapter ingress must include userId and channel");
        }
        if (isBlank(req.provider()) || isBlank(req.externalMessageId()) || isBlank(req.eventId())) {
            throw new IllegalArgumentException(
                    "conversation adapter ingress must include provider, externalMessageId, and eventId");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
