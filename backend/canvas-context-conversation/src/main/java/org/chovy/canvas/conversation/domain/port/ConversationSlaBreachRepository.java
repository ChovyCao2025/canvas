package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationSlaBreach;

import java.util.Optional;

/**
 * SLA 违约记录的领域仓储端口。
 */
public interface ConversationSlaBreachRepository {

    /**
     * 查找指定工单当前未关闭的 SLA 违约记录。
     *
     * @param tenantId 租户标识
     * @param workItemId 工单标识
     * @return 未关闭的 SLA 违约记录
     */
    Optional<ConversationSlaBreach> openByWorkItem(Long tenantId, Long workItemId);

    /**
     * 保存 SLA 违约记录并返回带持久化标识的领域对象。
     *
     * @param breach 待保存的 SLA 违约记录
     * @return 保存后的 SLA 违约记录
     */
    ConversationSlaBreach save(ConversationSlaBreach breach);
}
