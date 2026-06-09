package org.chovy.canvas.domain.conversation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WhatsAppConversationReplyAdapter 编排 domain.conversation 场景的领域业务规则。
 */
@Component
public class WhatsAppConversationReplyAdapter
        extends AbstractProviderConversationReplyAdapter<WhatsAppConversationReplyPayload> {

    /**
     * 执行 WhatsAppConversationReplyAdapter 流程，围绕 whats app conversation reply adapter 完成校验、计算或结果组装。
     */
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
