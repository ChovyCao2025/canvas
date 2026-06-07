package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialDmConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<SocialDmConversationReplyPayload> {

    public SocialDmConversationReplyAdapter() {
        super(
                "SOCIAL_DM",
                SocialDmConversationReplyPayload.class,
                "social DM conversation reply payload is required",
                List.of(
                        providerAttribute("platform", SocialDmConversationReplyPayload::platform),
                        providerAttribute("pageId", SocialDmConversationReplyPayload::pageId),
                        providerAttribute("threadId", SocialDmConversationReplyPayload::threadId),
                        providerAttribute("quickReplyPayload", SocialDmConversationReplyPayload::quickReplyPayload),
                        providerAttribute("quickReplyTitle", SocialDmConversationReplyPayload::quickReplyTitle)),
                SocialDmConversationReplyPayload::quickReplyTitle,
                List.of(
                        SocialDmConversationReplyPayload::quickReplyPayload,
                        SocialDmConversationReplyPayload::quickReplyTitle));
    }
}
