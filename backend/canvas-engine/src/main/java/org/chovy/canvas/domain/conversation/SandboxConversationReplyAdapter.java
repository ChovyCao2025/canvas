package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SandboxConversationReplyAdapter implements ConversationReplyAdapter<SandboxConversationReplyPayload> {

    @Override
    public String adapterKey() {
        return "SANDBOX";
    }

    @Override
    public Class<?> payloadType() {
        return SandboxConversationReplyPayload.class;
    }

    @Override
    public ConversationIngressReq toIngress(SandboxConversationReplyPayload payload, ConversationAdapterContext context) {
        if (payload == null) {
            throw new IllegalArgumentException("sandbox conversation reply payload is required");
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (payload.attributes() != null) {
            attributes.putAll(payload.attributes());
        }
        attributes.put("adapter", "SANDBOX");
        if (context != null && context.operator() != null && !context.operator().isBlank()) {
            attributes.put("sandboxOperator", context.operator().trim());
        }
        return new ConversationIngressReq(
                payload.canvasId(),
                payload.versionId(),
                payload.executionId(),
                payload.userId(),
                "SANDBOX",
                "DEFAULT",
                payload.externalMessageId(),
                payload.eventId(),
                "TEXT",
                payload.text(),
                payload.intent(),
                attributes,
                null);
    }
}
