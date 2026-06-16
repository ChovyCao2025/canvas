package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 路由策略对单个工单做出的决策结果。
 *
 * @param routed 是否成功路由到坐席
 * @param routingStatus 路由状态
 * @param agent 命中的坐席
 * @param assignedTeam 分配到的团队
 * @param requiredSkills 本次路由要求的技能
 * @param reason 路由决策说明
 * @param routedAt 路由决策时间
 * @param slaDueAt 计算出的 SLA 到期时间
 * @param slaPolicyKey 命中的 SLA 策略键
 */
public record ConversationRoutingDecision(
        /**
         * 是否成功路由到坐席。
         */
        boolean routed,
        /**
         * 路由状态。
         */
        String routingStatus,
        /**
         * 命中的坐席。
         */
        Optional<ConversationRoutingAgent> agent,
        /**
         * 分配到的团队。
         */
        String assignedTeam,
        /**
         * 本次路由要求的技能。
         */
        List<String> requiredSkills,
        /**
         * 路由决策说明。
         */
        String reason,
        /**
         * 路由决策时间。
         */
        LocalDateTime routedAt,
        /**
         * 计算出的 SLA 到期时间。
         */
        LocalDateTime slaDueAt,
        /**
         * 命中的 SLA 策略键。
         */
        String slaPolicyKey) {

    /**
     * 创建路由决策并规整 Optional 与可变技能列表。
     */
    public ConversationRoutingDecision {
        agent = agent == null ? Optional.empty() : agent;
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }
}
