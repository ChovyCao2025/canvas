package org.chovy.canvas.conversation.api;

import java.util.List;
import java.util.Map;

/**
 * 路由规则配置的查询视图。
 *
 * @param id 规则记录标识
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
 */
public record ConversationRoutingRuleView(
        /**
         * 规则记录标识。
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
        Map<String, Object> metadata) {
}
