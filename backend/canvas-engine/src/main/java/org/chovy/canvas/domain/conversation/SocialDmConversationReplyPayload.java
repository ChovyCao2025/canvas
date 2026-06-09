package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * SocialDmConversationReplyPayload 承载 domain.conversation 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param versionId versionId 字段。
 * @param executionId executionId 字段。
 * @param userId userId 字段。
 * @param platform platform 字段。
 * @param provider provider 字段。
 * @param pageId pageId 字段。
 * @param threadId threadId 字段。
 * @param externalMessageId externalMessageId 字段。
 * @param eventId eventId 字段。
 * @param text text 字段。
 * @param quickReplyPayload quickReplyPayload 字段。
 * @param quickReplyTitle quickReplyTitle 字段。
 * @param intent intent 字段。
 * @param attributes attributes 字段。
 * @param occurredAt occurredAt 字段。
 */
public record SocialDmConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String platform,
        String provider,
        String pageId,
        String threadId,
        String externalMessageId,
        String eventId,
        String text,
        String quickReplyPayload,
        String quickReplyTitle,
        String intent,
        Map<String, Object> attributes,
        LocalDateTime occurredAt) implements ProviderConversationReplyPayload {
}
