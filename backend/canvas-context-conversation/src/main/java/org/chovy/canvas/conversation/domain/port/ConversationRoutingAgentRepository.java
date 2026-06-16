package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationRoutingAgent;

import java.util.List;
import java.util.Optional;

/**
 * 路由坐席的领域仓储端口。
 */
public interface ConversationRoutingAgentRepository {

    /**
     * 按租户和坐席业务键查找坐席配置。
     *
     * @param tenantId 租户标识
     * @param agentKey 坐席业务键
     * @return 匹配的坐席配置
     */
    Optional<ConversationRoutingAgent> byKey(Long tenantId, String agentKey);

    /**
     * 查找可参与指定团队路由的坐席候选集。
     *
     * @param tenantId 租户标识
     * @param teamKey 目标团队键
     * @return 候选坐席列表
     */
    List<ConversationRoutingAgent> candidates(Long tenantId, String teamKey);

    /**
     * 保存坐席配置并返回带持久化标识的领域对象。
     *
     * @param agent 待保存的坐席配置
     * @return 保存后的坐席配置
     */
    ConversationRoutingAgent save(ConversationRoutingAgent agent);
}
