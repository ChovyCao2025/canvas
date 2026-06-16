package org.chovy.canvas.conversation.domain;

import java.util.List;

/**
 * 领域层执行工单路由时使用的请求参数。
 *
 * @param requiredSkills 人工指定的要求技能
 * @param targetTeam 人工指定的目标团队
 * @param slaMinutes 人工指定的 SLA 时长，单位为分钟
 * @param note 路由备注
 */
public record ConversationRouteRequest(
        /**
         * 人工指定的要求技能。
         */
        List<String> requiredSkills,
        /**
         * 人工指定的目标团队。
         */
        String targetTeam,
        /**
         * 人工指定的 SLA 时长，单位为分钟。
         */
        Integer slaMinutes,
        /**
         * 路由备注。
         */
        String note) {

    /**
     * 创建路由请求并复制可变技能列表。
     */
    public ConversationRouteRequest {
        requiredSkills = DomainMaps.copyList(requiredSkills);
    }
}
