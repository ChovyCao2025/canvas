package org.chovy.canvas.conversation.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用于筛选工单并提供路由目标的规则。
 *
 * @param id 规则标识
 * @param tenantId 租户标识
 * @param ruleKey 规则业务键
 * @param channel 适用渠道
 * @param minPriority 最低优先级
 * @param requiredSkills 要求技能列表
 * @param targetTeam 目标团队
 * @param slaMinutes SLA 时长，单位为分钟
 * @param enabled 是否启用
 * @param sortOrder 匹配排序值
 * @param metadata 扩展元数据
 * @param createdBy 创建操作者
 * @param createdAt 创建时间
 * @param updatedAt 更新时间
 */
public record ConversationRoutingRule(
        /**
         * 规则标识。
         */
        Long id,
        /**
         * 租户标识。
         */
        Long tenantId,
        /**
         * 规则业务键。
         */
        String ruleKey,
        /**
         * 适用渠道。
         */
        String channel,
        /**
         * 最低优先级。
         */
        String minPriority,
        /**
         * 要求技能列表。
         */
        List<String> requiredSkills,
        /**
         * 目标团队。
         */
        String targetTeam,
        /**
         * SLA 时长，单位为分钟。
         */
        int slaMinutes,
        /**
         * 是否启用。
         */
        boolean enabled,
        /**
         * 匹配排序值。
         */
        int sortOrder,
        /**
         * 扩展元数据。
         */
        Map<String, Object> metadata,
        /**
         * 创建操作者。
         */
        String createdBy,
        /**
         * 创建时间。
         */
        LocalDateTime createdAt,
        /**
         * 更新时间。
         */
        LocalDateTime updatedAt) {

    /**
     * 创建路由规则并复制可变集合属性。
     */
    public ConversationRoutingRule {
        requiredSkills = DomainMaps.copyList(requiredSkills);
        metadata = DomainMaps.copy(metadata);
    }

    /**
     * 返回替换持久化标识后的规则副本。
     *
     * @param id 持久化生成的规则标识
     * @return 带新标识的路由规则
     */
    public ConversationRoutingRule withId(Long id) {
        return new ConversationRoutingRule(id, tenantId, ruleKey, channel, minPriority, requiredSkills,
                targetTeam, slaMinutes, enabled, sortOrder, metadata, createdBy, createdAt, updatedAt);
    }

    /**
     * 判断规则是否适用于给定工单。
     *
     * @param item 待路由工单
     * @return 规则启用且渠道、优先级匹配时返回 true
     */
    boolean matches(ConversationWorkItem item) {
        return enabled
                && (channel == null || channel.equals(item.channel()))
                && priorityRank(item.priority()) >= priorityRank(minPriority);
    }

    /**
     * 将优先级文本映射为可比较的排序权重。
     *
     * @param priority 优先级文本
     * @return 优先级权重
     */
    private static int priorityRank(String priority) {
        return switch (priority == null ? "NORMAL" : priority) {
            case "LOW" -> 1;
            case "HIGH" -> 3;
            case "URGENT" -> 4;
            default -> 2;
        };
    }
}
