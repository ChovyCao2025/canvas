package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * WebChatConversationReplyPayload 承载 domain.conversation 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param versionId versionId 字段。
 * @param executionId executionId 字段。
 * @param userId userId 字段。
 * @param webChatSessionId webChatSessionId 字段。
 * @param provider provider 字段。
 * @param externalMessageId externalMessageId 字段。
 * @param eventId eventId 字段。
 * @param text text 字段。
 * @param actionId actionId 字段。
 * @param actionLabel actionLabel 字段。
 * @param intent intent 字段。
 * @param attributes attributes 字段。
 * @param occurredAt occurredAt 字段。
 */
public record WebChatConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String webChatSessionId,
        String provider,
        String externalMessageId,
        String eventId,
        String text,
        String actionId,
        String actionLabel,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
