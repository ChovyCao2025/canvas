package org.chovy.canvas.conversation.api;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会话工单的对外查询视图。
 *
 * @param id 工单标识
 * @param tenantId 租户标识
 * @param sessionId 会话标识
 * @param userId 用户标识
 * @param channel 来源渠道
 * @param provider 来源服务商
 * @param subject 工单主题
 * @param status 工单状态
 * @param priority 工单优先级
 * @param assignedTo 当前处理人
 * @param assignedTeam 当前处理团队
 * @param source 工单来源
 * @param slaDueAt SLA 到期时间
 * @param nextFollowUpAt 下一次跟进时间
 * @param lastCustomerMessageAt 最近客户消息时间
 * @param lastOperatorActivityAt 最近运营处理时间
 * @param tags 工单标签
 * @param attributes 工单扩展属性
 * @param routingStatus 路由状态
 * @param requiredSkills 要求技能
 * @param routingReason 路由原因
 * @param routedAt 路由完成时间
 * @param slaPolicyKey SLA 策略键
 */
public record ConversationWorkItemView(
        /**
         * 工单标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 会话标识。
         */
        Long sessionId,
        /**
         * 用户标识。
         */
        String userId,
        /**
         * 来源渠道。
         */
        String channel,
        /**
         * 来源服务商。
         */
        String provider,
        /**
         * 工单主题。
         */
        String subject,
        /**
         * 工单状态。
         */
        String status,
        /**
         * 工单优先级。
         */
        String priority,
        /**
         * 当前处理人。
         */
        String assignedTo,
        /**
         * 当前处理团队。
         */
        String assignedTeam,
        /**
         * 工单来源。
         */
        String source,
        /**
         * SLA 到期时间。
         */
        LocalDateTime slaDueAt,
        /**
         * 下一次跟进时间。
         */
        LocalDateTime nextFollowUpAt,
        /**
         * 最近客户消息时间。
         */
        LocalDateTime lastCustomerMessageAt,
        /**
         * 最近运营处理时间。
         */
        LocalDateTime lastOperatorActivityAt,
        /**
         * 工单标签。
         */
        List<String> tags,
        /**
         * 工单扩展属性。
         */
        Map<String, Object> attributes,
        /**
         * 路由状态。
         */
        String routingStatus,
        /**
         * 要求技能。
         */
        List<String> requiredSkills,
        /**
         * 路由原因。
         */
        String routingReason,
        /**
         * 路由完成时间。
         */
        LocalDateTime routedAt,
        /**
         * SLA 策略键。
         */
        String slaPolicyKey) {
}
