package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationMessage;

import java.util.Optional;

/**
 * 会话消息的领域仓储端口。
 */
public interface ConversationMessageRepository {

    /**
     * 通过幂等键查找租户内已记录的消息。
     *
     * @param tenantId 租户标识
     * @param idempotencyKey 渠道消息幂等键
     * @return 已存在的消息记录
     */
    Optional<ConversationMessage> byIdempotencyKey(Long tenantId, String idempotencyKey);

    /**
     * 保存消息并返回带持久化标识的领域对象。
     *
     * @param message 待保存的会话消息
     * @return 保存后的会话消息
     */
    ConversationMessage save(ConversationMessage message);
}
