package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConversationMessageView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param sessionId sessionId 字段。
 * @param direction direction 字段。
 * @param messageType messageType 字段。
 * @param externalMessageId externalMessageId 字段。
 * @param text text 字段。
 * @param intent intent 字段。
 * @param content content 字段。
 * @param createdAt createdAt 字段。
 */
public record ConversationMessageView(
        Long id,
        Long tenantId,
        Long sessionId,
        String direction,
        String messageType,
        String externalMessageId,
        String text,
        String intent,
        Map<String, Object> content,
        LocalDateTime createdAt) {
}
