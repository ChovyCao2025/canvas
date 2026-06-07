package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebChatConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<WebChatConversationReplyPayload> {

    public WebChatConversationReplyAdapter() {
        super(
                "WEB_CHAT",
                WebChatConversationReplyPayload.class,
                "web chat conversation reply payload is required",
                List.of(
                        providerAttribute("webChatSessionId", WebChatConversationReplyPayload::webChatSessionId),
                        providerAttribute("actionId", WebChatConversationReplyPayload::actionId),
                        providerAttribute("actionLabel", WebChatConversationReplyPayload::actionLabel)),
                WebChatConversationReplyPayload::actionLabel,
                List.of(
                        WebChatConversationReplyPayload::actionId,
                        WebChatConversationReplyPayload::actionLabel));
    }
}
