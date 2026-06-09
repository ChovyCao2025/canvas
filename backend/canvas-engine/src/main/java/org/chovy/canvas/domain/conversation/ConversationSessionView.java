package org.chovy.canvas.domain.conversation;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConversationSessionView 承载 domain.conversation 场景中的不可变数据快照。
 * @param id id 字段。
 * @param tenantId tenantId 字段。
 * @param canvasId canvasId 字段。
 * @param versionId versionId 字段。
 * @param executionId executionId 字段。
 * @param userId userId 字段。
 * @param channel channel 字段。
 * @param provider provider 字段。
 * @param status status 字段。
 * @param turnCount turnCount 字段。
 * @param context context 字段。
 * @param lastMessageAt lastMessageAt 字段。
 * @param expiresAt expiresAt 字段。
 * @param createdAt createdAt 字段。
 * @param updatedAt updatedAt 字段。
 */
public record ConversationSessionView(
        Long id,
        Long tenantId,
        Long canvasId,
        Long versionId,
        String executionId,
        String userId,
        String channel,
        String provider,
        String status,
        int turnCount,
        Map<String, Object> context,
        LocalDateTime lastMessageAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
