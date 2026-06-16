package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话消息领域对象。
 *
 * @param id 消息标识
 * @param tenantId 租户标识
 * @param sessionId 会话标识
 * @param direction 消息方向
 * @param messageType 消息类型
 * @param externalMessageId 外部消息标识
 * @param idempotencyKey 幂等键
 * @param content 结构化消息内容
 * @param textContent 文本内容
 * @param intent 消息意图
 * @param processed 是否已处理
 * @param createdAt 创建时间
 */
public record ConversationMessage(
        /**
         * 消息标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 会话标识。
         */
        Long sessionId,
        /**
         * 消息方向。
         */
        String direction,
        /**
         * 消息类型。
         */
        String messageType,
        /**
         * 外部消息标识。
         */
        String externalMessageId,
        /**
         * 幂等键。
         */
        String idempotencyKey,
        /**
         * 结构化消息内容。
         */
        Map<String, Object> content,
        /**
         * 文本内容。
         */
        String textContent,
        /**
         * 消息意图。
         */
        String intent,
        /**
         * 是否已处理。
         */
        boolean processed,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt) {

    /**
     * 创建会话消息并复制结构化内容。
     */
    public ConversationMessage {
        content = DomainMaps.copy(content);
    }

    /**
     * 返回替换持久化标识后的消息副本。
     *
     * @param id 持久化生成的消息标识
     * @return 带新标识的消息
     */
    public ConversationMessage withId(Long id) {
        return new ConversationMessage(id, tenantId, sessionId, direction, messageType, externalMessageId,
                idempotencyKey, content, textContent, intent, processed, createdAt);
    }
}
