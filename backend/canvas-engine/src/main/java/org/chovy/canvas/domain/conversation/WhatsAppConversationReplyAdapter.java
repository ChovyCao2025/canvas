package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WhatsAppConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<WhatsAppConversationReplyPayload> {

    public WhatsAppConversationReplyAdapter() {
        super(
                "WHATSAPP",
                WhatsAppConversationReplyPayload.class,
                "whatsapp conversation reply payload is required",
                List.of(
                        providerAttribute("interactiveReplyId", WhatsAppConversationReplyPayload::interactiveReplyId),
                        providerAttribute("interactiveReplyTitle", WhatsAppConversationReplyPayload::interactiveReplyTitle)),
                WhatsAppConversationReplyPayload::interactiveReplyTitle,
                List.of(
                        WhatsAppConversationReplyPayload::interactiveReplyId,
                        WhatsAppConversationReplyPayload::interactiveReplyTitle));
    }
}
