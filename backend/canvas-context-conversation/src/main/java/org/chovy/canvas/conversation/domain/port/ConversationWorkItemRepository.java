package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationWorkItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话工单的领域仓储端口。
 */
public interface ConversationWorkItemRepository {

    /**
     * 按租户和会话查找已存在的工单。
     *
     * @param tenantId 租户标识
     * @param sessionId 会话标识
     * @return 匹配的工单
     */
    Optional<ConversationWorkItem> bySession(Long tenantId, Long sessionId);

    /**
     * 按数据库主键查找工单。
     *
     * @param id 工单主键
     * @return 匹配的工单
     */
    Optional<ConversationWorkItem> byId(Long id);

    /**
     * 查询到达 SLA 检查时间的工单。
     *
     * @param tenantId 租户标识
     * @param now SLA 检查基准时间
     * @param limit 返回数量上限
     * @return 待检查 SLA 的工单列表
     */
    List<ConversationWorkItem> dueForSla(Long tenantId, LocalDateTime now, int limit);

    /**
     * 保存工单并返回带持久化标识的领域对象。
     *
     * @param workItem 待保存的工单
     * @return 保存后的工单
     */
    ConversationWorkItem save(ConversationWorkItem workItem);
}
