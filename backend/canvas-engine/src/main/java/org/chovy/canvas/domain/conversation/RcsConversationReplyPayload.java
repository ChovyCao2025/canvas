package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * RcsConversationReplyPayload 承载 domain.conversation 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param versionId versionId 字段。
 * @param executionId executionId 字段。
 * @param userId userId 字段。
 * @param provider provider 字段。
 * @param agentId agentId 字段。
 * @param conversationId conversationId 字段。
 * @param externalMessageId externalMessageId 字段。
 * @param eventId eventId 字段。
 * @param text text 字段。
 * @param suggestionReplyId suggestionReplyId 字段。
 * @param suggestionText suggestionText 字段。
 * @param intent intent 字段。
 * @param attributes attributes 字段。
 * @param occurredAt occurredAt 字段。
 */
public record RcsConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String provider,
        String agentId,
        String conversationId,
        String externalMessageId,
        String eventId,
        String text,
        String suggestionReplyId,
        String suggestionText,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
