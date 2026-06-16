package org.chovy.canvas.conversation.domain.port;

import org.chovy.canvas.conversation.domain.ConversationRoutingRule;

import java.util.List;
import java.util.Optional;

/**
 * 路由规则的领域仓储端口。
 */
public interface ConversationRoutingRuleRepository {

    /**
     * 按租户和规则业务键查找路由规则。
     *
     * @param tenantId 租户标识
     * @param ruleKey 规则业务键
     * @return 匹配的路由规则
     */
    Optional<ConversationRoutingRule> byKey(Long tenantId, String ruleKey);

    /**
     * 查询租户内已启用的路由规则。
     *
     * @param tenantId 租户标识
     * @return 已启用的路由规则列表
     */
    List<ConversationRoutingRule> enabled(Long tenantId);

    /**
     * 保存路由规则并返回带持久化标识的领域对象。
     *
     * @param rule 待保存的路由规则
     * @return 保存后的路由规则
     */
    ConversationRoutingRule save(ConversationRoutingRule rule);
}
