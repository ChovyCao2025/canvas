package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 由会话沉淀出的客服或运营处理工单。
 *
 * @param id 工单标识
 * @param tenantId 租户标识
 * @param sessionId 会话标识
 * @param contactProfileId 联系人画像标识
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
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationWorkItem(
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
         * 联系人画像标识。
         */
        Long contactProfileId,
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
        String slaPolicyKey,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建工单并复制可变集合属性。
     */
    public ConversationWorkItem {
        tags = DomainMaps.copyList(tags);
        attributes = DomainMaps.copy(attributes);
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }

    /**
     * 返回替换持久化标识后的工单副本。
     *
     * @param id 持久化生成的工单标识
     * @return 带新标识的工单
     */
    public ConversationWorkItem withId(Long id) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    /**
     * 返回更新分配信息后的工单副本。
     *
     * @param assignedTo 新处理人
     * @param assignedTeam 新处理团队
     * @param updatedAt 更新时间
     * @return 更新分配信息后的工单
     */
    public ConversationWorkItem withAssignment(String assignedTo, String assignedTeam, LocalDateTime updatedAt) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, updatedAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    /**
     * 返回更新状态、优先级和跟进时间后的工单副本。
     *
     * @param status 新状态
     * @param priority 新优先级
     * @param nextFollowUpAt 下一次跟进时间
     * @param updatedAt 更新时间
     * @return 更新状态后的工单
     */
    public ConversationWorkItem withStatus(String status, String priority, LocalDateTime nextFollowUpAt, LocalDateTime updatedAt) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, updatedAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }

    /**
     * 返回应用路由决策后的工单副本。
     *
     * @param routingStatus 路由状态
     * @param assignedTo 路由分配处理人
     * @param assignedTeam 路由分配团队
     * @param requiredSkills 路由要求技能
     * @param routingReason 路由原因
     * @param routedAt 路由完成时间
     * @param slaDueAt 路由计算出的 SLA 到期时间
     * @return 更新路由信息后的工单
     */
    public ConversationWorkItem withRouting(String routingStatus,
                                            String assignedTo,
                                            String assignedTeam,
                                            List<String> requiredSkills,
                                            String routingReason,
                                            LocalDateTime routedAt,
                                            LocalDateTime slaDueAt) {
        // 路由可能失败而没有 routedAt，此时保留原更新时间，避免无路由结果误推进工单时间线。
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, routedAt == null ? updatedAt : routedAt);
    }

    /**
     * 返回更新 SLA 策略键后的工单副本。
     *
     * @param slaPolicyKey SLA 策略键
     * @return 更新 SLA 策略键后的工单
     */
    public ConversationWorkItem withSlaPolicy(String slaPolicyKey) {
        return new ConversationWorkItem(id, tenantId, sessionId, contactProfileId, userId, channel, provider,
                subject, status, priority, assignedTo, assignedTeam, source, slaDueAt, nextFollowUpAt,
                lastCustomerMessageAt, lastOperatorActivityAt, tags, attributes, routingStatus, requiredSkills,
                routingReason, routedAt, slaPolicyKey, createdAt, updatedAt);
    }
}
