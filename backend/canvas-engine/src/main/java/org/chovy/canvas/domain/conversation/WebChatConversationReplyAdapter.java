package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WebChatConversationReplyAdapter 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class WebChatConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<WebChatConversationReplyPayload> {

    /**
     * 执行 WebChatConversationReplyAdapter 流程，围绕 web chat conversation reply adapter 完成校验、计算或结果组装。
     */
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
