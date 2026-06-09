package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RcsConversationReplyAdapter 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class RcsConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<RcsConversationReplyPayload> {

    /**
     * 执行 RcsConversationReplyAdapter 流程，围绕 rcs conversation reply adapter 完成校验、计算或结果组装。
     */
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
