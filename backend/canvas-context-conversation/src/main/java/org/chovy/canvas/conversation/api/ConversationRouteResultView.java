package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单路由执行结果视图。
 *
 * @param tenantId 租户标识
 * @param workItemId 工单标识
 * @param routed 是否成功路由到坐席
 * @param routingStatus 路由状态
 * @param assignedTo 分配到的处理人
 * @param assignedTeam 分配到的团队
 * @param requiredSkills 本次路由要求的技能
 * @param routingReason 路由决策说明
 * @param routedAt 路由完成时间
 * @param slaDueAt 路由后计算出的 SLA 到期时间
 * @param slaPolicyKey 命中的 SLA 策略键
 */
public record ConversationRouteResultView(
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 工单标识。
         */
        Long workItemId,
        /**
         * 是否成功路由到坐席。
         */
        boolean routed,
        /**
         * 路由状态。
         */
        String routingStatus,
        /**
         * 分配到的处理人。
         */
        String assignedTo,
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
        String routingReason,
        /**
         * 路由完成时间。
         */
        LocalDateTime routedAt,
        /**
         * 路由后计算出的 SLA 到期时间。
         */
        LocalDateTime slaDueAt,
        /**
         * 命中的 SLA 策略键。
         */
        String slaPolicyKey) {
}
