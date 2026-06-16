package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户在某个画布执行链路中的会话聚合。
 *
 * @param id 会话标识
 * @param tenantId 租户标识
 * @param canvasId 画布标识
 * @param versionId 画布版本标识
 * @param executionId 外部执行标识
 * @param userId 会话用户标识
 * @param channel 来源渠道
 * @param provider 来源服务商
 * @param status 会话状态
 * @param turnCount 累计轮次数
 * @param context 会话上下文快照
 * @param lastMessageAt 最近消息时间
 * @param expiresAt 会话过期时间
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationSession(
        /**
         * 会话标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 画布标识。
         */
        Long canvasId,
        /**
         * 画布版本标识。
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
         * 来源渠道。
         */
        String channel,
        /**
         * 来源服务商。
         */
        String provider,
        /**
         * 会话状态。
         */
        String status,
        /**
         * 累计轮次数。
         */
        int turnCount,
        /**
         * 会话上下文快照。
         */
        Map<String, Object> context,
        /**
         * 最近消息时间。
         */
        LocalDateTime lastMessageAt,
        /**
         * 会话过期时间。
         */
        LocalDateTime expiresAt,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建会话并复制上下文快照。
     */
    public ConversationSession {
        context = DomainMaps.copy(context);
    }

    /**
     * 返回替换持久化标识后的会话副本。
     *
     * @param id 持久化生成的会话标识
     * @return 带新标识的会话
     */
    public ConversationSession withId(Long id) {
        return new ConversationSession(id, tenantId, canvasId, versionId, executionId, userId, channel, provider,
                status, turnCount, context, lastMessageAt, expiresAt, createdAt, updatedAt);
    }

    /**
     * 记录一条消息对会话上下文和轮次数的影响。
     *
     * @param message 新记录的会话消息
     * @param occurredAt 消息发生时间
     * @return 更新消息摘要后的会话副本
     */
    public ConversationSession recorded(ConversationMessage message, LocalDateTime occurredAt) {
        Map<String, Object> merged = new LinkedHashMap<>(context);
        // 上下文只保留后续编排需要的轻量消息摘要，避免把完整消息载荷反复写入会话。
        if (message.intent() != null) {
            merged.put("intent", message.intent());
        }
        if (message.textContent() != null) {
            merged.put("lastText", message.textContent());
        }
        merged.put("lastMessageId", message.id());
        return new ConversationSession(id, tenantId, canvasId, versionId, executionId, userId, channel, provider,
                status, turnCount + 1, merged, occurredAt, expiresAt, createdAt, occurredAt);
    }
}
