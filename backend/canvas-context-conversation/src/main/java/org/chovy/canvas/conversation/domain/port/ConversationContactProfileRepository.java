package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationContactProfile;

import java.util.Optional;

/**
 * 联系人画像的领域仓储端口。
 */
public interface ConversationContactProfileRepository {

    /**
     * 按租户和用户标识查找联系人画像。
     *
     * @param tenantId 租户标识
     * @param userId 会话用户标识
     * @return 匹配的联系人画像
     */
    Optional<ConversationContactProfile> byUser(Long tenantId, String userId);

    /**
     * 保存联系人画像并返回带持久化标识的领域对象。
     *
     * @param profile 待保存的联系人画像
     * @return 保存后的联系人画像
     */
    ConversationContactProfile save(ConversationContactProfile profile);
}
