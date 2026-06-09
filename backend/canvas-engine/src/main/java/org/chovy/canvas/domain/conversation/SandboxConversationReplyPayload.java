package org.chovy.canvas.domain.conversation;

import java.util.Map;

/**
 * SandboxConversationReplyPayload 承载 domain.conversation 场景中的不可变数据快照。
 * @param canvasId canvasId 字段。
 * @param versionId versionId 字段。
 * @param executionId executionId 字段。
 * @param userId userId 字段。
 * @param externalMessageId externalMessageId 字段。
 * @param eventId eventId 字段。
 * @param text text 字段。
 * @param intent intent 字段。
 * @param attributes attributes 字段。
 */
public record SandboxConversationReplyPayload(
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String externalMessageId,
        String eventId,
        String text,
        String intent,
        Map<String, Object> attributes) {
}
