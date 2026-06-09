package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SocialDmConversationReplyAdapter 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class SocialDmConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<SocialDmConversationReplyPayload> {

    /**
     * 执行 SocialDmConversationReplyAdapter 流程，围绕 social dm conversation reply adapter 完成校验、计算或结果组装。
     */
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
