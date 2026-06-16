package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationSession;

import java.util.Optional;

/**
 * 会话聚合的领域仓储端口。
 */
public interface ConversationSessionRepository {

    /**
     * 查找给定用户、渠道、服务商和执行标识下的活跃会话。
     *
     * @param tenantId 租户标识
     * @param userId 会话用户标识
     * @param channel 会话渠道
     * @param provider 会话服务商
     * @param executionId 外部执行标识
     * @return 当前活跃会话
     */
    Optional<ConversationSession> findActive(Long tenantId, String userId, String channel, String provider, String executionId);

    /**
     * 按数据库主键查找会话。
     *
     * @param id 会话主键
     * @return 匹配的会话
     */
    Optional<ConversationSession> byId(Long id);

    /**
     * 保存会话并返回带持久化标识的领域对象。
     *
     * @param session 待保存的会话
     * @return 保存后的会话
     */
    ConversationSession save(ConversationSession session);
}
