package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RcsConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<RcsConversationReplyPayload> {

    public RcsConversationReplyAdapter() {
        super(
                "RCS",
                RcsConversationReplyPayload.class,
                "RCS conversation reply payload is required",
                List.of(
                        providerAttribute("agentId", RcsConversationReplyPayload::agentId),
                        providerAttribute("conversationId", RcsConversationReplyPayload::conversationId),
                        providerAttribute("suggestionReplyId", RcsConversationReplyPayload::suggestionReplyId),
                        providerAttribute("suggestionText", RcsConversationReplyPayload::suggestionText)),
                RcsConversationReplyPayload::suggestionText,
                List.of(
                        RcsConversationReplyPayload::suggestionReplyId,
                        RcsConversationReplyPayload::suggestionText));
    }
}
