package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 记录外部渠道入站会话消息的命令。
 *
 * @param tenantId 租户标识
 * @param canvasId 关联画布标识
 * @param versionId 关联画布版本标识
 * @param executionId 外部执行标识
 * @param userId 会话用户标识
 * @param channel 消息来源渠道
 * @param provider 消息来源服务商
 * @param externalMessageId 渠道侧消息标识
 * @param eventId 入站事件幂等标识
 * @param messageType 消息类型
 * @param text 消息文本
 * @param intent 解析得到的意图
 * @param attributes 消息扩展属性
 * @param occurredAt 消息发生时间
 */
public record ConversationInboundCommand(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 关联画布标识。
         */
        Long canvasId,
        /**
         * 关联画布版本标识。
         */
        Long versionId,
        /**
         * 外部执行标识。
         */
        String executionId,
        /**
         * 会话用户标识。
         */
        String userId,
        /**
         * 消息来源渠道。
         */
        String channel,
        /**
         * 消息来源服务商。
         */
        String provider,
        /**
         * 渠道侧消息标识。
         */
        String externalMessageId,
        /**
         * 入站事件幂等标识。
         */
        String eventId,
        /**
         * 消息类型。
         */
        String messageType,
        /**
         * 消息文本。
         */
        String text,
        /**
         * 解析得到的意图。
         */
        String intent,
        /**
         * 消息扩展属性。
         */
        Map<String, Object> attributes,
        /**
         * 消息发生时间。
         */
        LocalDateTime occurredAt) {
}
